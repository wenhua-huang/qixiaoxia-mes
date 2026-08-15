package com.ruoyi.system.service.mes.pro;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.system.domain.mes.pro.CardScanResultVO;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.service.mes.pro.impl.ProCardServiceImpl;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 扫流转卡码反查报工上下文 Service 单元测试
 *
 * <p>覆盖 ProCardServiceImpl.scanForReport 各分支：卡不存在 / 卡已完成 / ACTIVE 且有可报任务 /
 * ACTIVE 无可报任务 / 路线位置过滤（游标语义=最近完成的工序，可报=当前工序续报+下一波工序）。
 *
 * <p>注：routeId 为空的任务走「不过滤」兜底路径；带 routeId 的用例镜像真实路线
 * pos1=204分切(外协) → pos2=203贴绳(外协) → pos3=200印刷(厂内)。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
class ProCardScanServiceUnitTest {

    private static final Long ROUTE_ID = 209L;

    @Mock private ProCardMapper proCardMapper;
    @Mock private ProTaskMapper proTaskMapper;
    @Mock private ProFeedbackMapper proFeedbackMapper;
    @Mock private ProRouteProcessMapper routeProcessMapper;
    @Mock private IProFeedbackService proFeedbackService;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private AutoCodeGenerator autoCodeGenerator;

    @InjectMocks private ProCardServiceImpl service;

    private ProRouteProcess node(Long processId, Integer orderNum) {
        ProRouteProcess rp = new ProRouteProcess();
        rp.setProcessId(processId);
        rp.setOrderNum(orderNum);
        return rp;
    }

    /** 分切(204,外协) → 贴绳(203,外协) → 印刷(200,厂内) 的真实路线镜像 */
    private void stubRealRoute() {
        when(routeProcessMapper.selectProRouteProcessByRouteId(ROUTE_ID)).thenReturn(List.of(
                node(204L, 1), node(203L, 2), node(200L, 3)));
    }

    private ProTask task(Long taskId, Long processId, String wsCode, String status) {
        ProTask t = new ProTask();
        t.setTaskId(taskId);
        t.setProcessId(processId);
        t.setWorkstationCode(wsCode);
        t.setStatus(status);
        t.setRouteId(ROUTE_ID);
        return t;
    }

    @Test
    void cardNotFound() {
        when(proCardMapper.selectProCardList(any())).thenReturn(Collections.emptyList());
        CardScanResultVO vo = service.scanForReport("NOPE");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReason()).isEqualTo("CARD_NOT_FOUND");
    }

    @Test
    void cardCompleted_cannotReport() {
        ProCard c = new ProCard();
        c.setCardCode("CRD1");
        c.setStatus("COMPLETED");
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));
        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReason()).isEqualTo("CARD_COMPLETED");
    }

    @Test
    void activeCard_withReportableTask_canReport() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        c.setCurrentProcessId(5L);
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));

        ProTask t = new ProTask();
        t.setTaskId(33L);
        t.setStatus("PRODUCING");
        t.setWorkstationCode("WS1");
        t.setProcessId(5L); // 无 routeId → 走不过滤兜底
        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(t));
        when(proFeedbackMapper.selectPendingTaskIds(any())).thenReturn(Collections.emptyList());
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 5L)).thenReturn(BigDecimal.TEN);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isTrue();
        assertThat(vo.getReportableTasks()).hasSize(1);
        assertThat(vo.getReportedQualifiedSum()).isEqualByComparingTo("10");
    }

    @Test
    void activeCard_noReportableTask() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));
        when(proTaskMapper.selectProTaskList(any())).thenReturn(Collections.emptyList());

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReason()).isEqualTo("NO_REPORTABLE_TASK");
    }

    /** 路线位置过滤：卡已到 pos2(5)，pos1(6) 的任务属于已过工序 → 排除 */
    @Test
    void activeCard_passedProcess_excluded() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        c.setCurrentProcessId(5L);
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));
        when(routeProcessMapper.selectProRouteProcessByRouteId(ROUTE_ID)).thenReturn(List.of(
                node(6L, 1), node(5L, 2)));

        ProTask passed = task(33L, 6L, "WS1", "PRODUCING"); // 已过工序，PRODUCING 也不应出现
        passed.setRouteId(ROUTE_ID);
        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(passed));
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 5L)).thenReturn(BigDecimal.ZERO);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReportableTasks()).isEmpty();
        assertThat(vo.getReason()).isEqualTo("NO_REPORTABLE_TASK");
    }

    /** 无路线信息兜底：VENDOR 工位任务 → 排除可报、归入外协任务、原因=PROCESS_OUTSOURCED */
    @Test
    void activeCard_taskVendorWorkstation_excluded() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        c.setCurrentProcessId(5L);
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));

        ProTask t = new ProTask();
        t.setTaskId(33L);
        t.setStatus("PRODUCING");
        t.setWorkstationCode("VENDOR"); // 外协工位，无 routeId → 不过滤兜底
        t.setProcessId(5L);
        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(t));
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 5L)).thenReturn(BigDecimal.ZERO);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReportableTasks()).isEmpty();
        assertThat(vo.getOutsourceTasks()).hasSize(1);
        assertThat(vo.getOutsourceTasks().get(0).getTaskId()).isEqualTo(33L);
        assertThat(vo.getReason()).isEqualTo("PROCESS_OUTSOURCED");
    }

    /** 真实场景镜像：分切(204)外协已完成，下一道贴绳(203)外协进行中 → 提示外协、展示两条外协任务 */
    @Test
    void activeCard_outsourceDone_nextOutsourceInProgress() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        c.setCurrentProcessId(204L); // 游标=最近完成的分切
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));
        stubRealRoute();

        // due = {204(当前,续报), 203(下一波)}；印刷(200) 属第三波，不应出现
        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(
                task(570L, 204L, "VENDOR", "COMPLETED"),
                task(571L, 203L, "VENDOR", "PRODUCING"),
                task(572L, 200L, "AUTO", "PRODUCING")));
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 204L)).thenReturn(BigDecimal.ONE);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReason()).isEqualTo("PROCESS_OUTSOURCED");
        assertThat(vo.getOutsourceTasks()).extracting(ProTask::getTaskId).containsExactlyInAnyOrder(570L, 571L);
        assertThat(vo.getReportableTasks()).isEmpty();
    }

    /** 外协全部做完后：游标=203(贴绳已完成) → 下一波印刷(200,厂内) 可报（等值过滤时代的 bug 回归） */
    @Test
    void activeCard_afterOutsource_nextInternal_reportable() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        c.setCurrentProcessId(203L); // 贴绳外协已完成
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));
        stubRealRoute();

        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(
                task(571L, 203L, "VENDOR", "COMPLETED"),
                task(572L, 200L, "AUTO", "PRODUCING")));
        when(proFeedbackMapper.selectPendingTaskIds(any())).thenReturn(Collections.emptyList());
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 203L)).thenReturn(BigDecimal.ONE);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isTrue();
        assertThat(vo.getReportableTasks()).extracting(ProTask::getTaskId).containsExactly(572L);
        assertThat(vo.getOutsourceTasks()).extracting(ProTask::getTaskId).containsExactly(571L);
    }
}
