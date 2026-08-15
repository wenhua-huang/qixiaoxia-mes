package com.ruoyi.system.service.mes.pro;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.system.domain.mes.pro.CardScanResultVO;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
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
 * <p>覆盖 ProCardServiceImpl.scanForReport 各分支：卡不存在 / 卡已完成 / ACTIVE 且有可报任务 / ACTIVE 无可报任务。
 *
 * <p>注：ProCardServiceImpl 现有依赖为 proCardMapper / lockTemplate(RedisLockTemplate) /
 * transactionManager / autoCodeGenerator，新增 proTaskMapper / proFeedbackMapper / proFeedbackService。
 * scanForReport 不触发事务模板与 Redis 锁，故无需在 setUp 中手动注入 txTemplate。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
class ProCardScanServiceUnitTest {

    @Mock private ProCardMapper proCardMapper;
    @Mock private ProTaskMapper proTaskMapper;
    @Mock private ProFeedbackMapper proFeedbackMapper;
    @Mock private IProFeedbackService proFeedbackService;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private AutoCodeGenerator autoCodeGenerator;

    @InjectMocks private ProCardServiceImpl service;

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
        t.setProcessId(5L);
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

    /** §6.2：卡在当前工序 5，但任务 processId=6（不同工序）→ 排除，无可报任务 */
    @Test
    void activeCard_taskDifferentProcess_excluded() {
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
        t.setProcessId(6L); // 与卡当前工序不一致
        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(t));
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 5L)).thenReturn(BigDecimal.ZERO);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReportableTasks()).isEmpty();
        assertThat(vo.getReason()).isEqualTo("NO_REPORTABLE_TASK");
    }

    /** §6.2：任务工位为外协(VENDOR) → 排除可报、归入外协任务、原因=PROCESS_OUTSOURCED */
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
        t.setWorkstationCode("VENDOR"); // 外协工位
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

    /** 真实数据场景：当前工序的外协任务已 COMPLETED（厂商做完等收货）→ 仍标记为外协工序 */
    @Test
    void activeCard_outsourceTaskCompleted_stillFlagged() {
        ProCard c = new ProCard();
        c.setCardId(1L);
        c.setCardCode("CRD1");
        c.setStatus("ACTIVE");
        c.setWorkorderId(10L);
        c.setCurrentProcessId(204L);
        when(proCardMapper.selectProCardList(any())).thenReturn(List.of(c));

        ProTask vendorDone = new ProTask();
        vendorDone.setTaskId(570L);
        vendorDone.setStatus("COMPLETED");
        vendorDone.setWorkstationCode("VENDOR");
        vendorDone.setProcessId(204L);
        ProTask otherProcess = new ProTask();
        otherProcess.setTaskId(572L);
        otherProcess.setStatus("PRODUCING");
        otherProcess.setWorkstationCode("AUTO");
        otherProcess.setProcessId(200L); // 其它工序的厂内任务，不应出现
        when(proTaskMapper.selectProTaskList(any())).thenReturn(List.of(vendorDone, otherProcess));
        when(proFeedbackService.getDefaultConsume(10L)).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(1L, 204L)).thenReturn(BigDecimal.ZERO);

        CardScanResultVO vo = service.scanForReport("CRD1");
        assertThat(vo.isCanReport()).isFalse();
        assertThat(vo.getReason()).isEqualTo("PROCESS_OUTSOURCED");
        assertThat(vo.getOutsourceTasks()).extracting(ProTask::getTaskId).containsExactly(570L);
    }
}
