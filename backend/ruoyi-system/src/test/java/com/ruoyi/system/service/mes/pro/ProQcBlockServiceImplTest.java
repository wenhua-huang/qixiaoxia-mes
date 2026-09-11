package com.ruoyi.system.service.mes.pro;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.TodoTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.qc.QcBlockInfo;
import com.ruoyi.system.domain.mes.qc.QcBlockRelease;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import com.ruoyi.system.domain.mes.sys.SysTodoList;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.qc.QcBlockReleaseMapper;
import com.ruoyi.system.mapper.mes.qc.QcIpqcMapper;
import com.ruoyi.system.mapper.mes.sys.SysTodoListMapper;
import com.ruoyi.system.service.mes.pro.impl.ProQcBlockServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 跟单质检不合格硬拦服务单元测试（纯 Mockito，不连库/Redis/HTTP）。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("质检不合格硬拦服务")
class ProQcBlockServiceImplTest {

    private static final Long WORKORDER_ID = 100L;
    private static final String WORKORDER_CODE = "WO-100";
    private static final Long ROUTE_ID = 9L;
    private static final Long CHECK_PROCESS_ID = 11L;
    private static final String CHECK_PROCESS_NAME = "车削检验";
    private static final Long TARGET_PROCESS_ID = 22L;
    private static final String TARGET_PROCESS_NAME = "铣削";
    private static final Long TASK_ID = 777L;
    private static final Long CARD_ID = 55L;
    private static final Long IPQC_ID = 9001L;
    private static final String IPQC_CODE = "IPQC20260910001";

    @Mock private ProRouteFlowHelper flow;
    @Mock private QcIpqcMapper qcIpqcMapper;
    @Mock private QcBlockReleaseMapper blockReleaseMapper;
    @Mock private ProTaskMapper proTaskMapper;
    @Mock private SysTodoListMapper sysTodoListMapper;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;

    private ProQcBlockServiceImpl service;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    private ProRouteProcess checkNode() {
        ProRouteProcess n = new ProRouteProcess();
        n.setProcessId(CHECK_PROCESS_ID);
        n.setProcessName(CHECK_PROCESS_NAME);
        n.setOrderNum(1);
        n.setIsCheck("Y");
        return n;
    }

    private QcIpqc ipqc(String result) {
        QcIpqc ipqc = new QcIpqc();
        ipqc.setIpqcId(IPQC_ID);
        ipqc.setIpqcCode(IPQC_CODE);
        ipqc.setWorkorderId(WORKORDER_ID);
        ipqc.setWorkorderCode(WORKORDER_CODE);
        ipqc.setProcessId(CHECK_PROCESS_ID);
        ipqc.setProcessName(CHECK_PROCESS_NAME);
        ipqc.setCheckResult(result);
        ipqc.setStatus("COMPLETED");
        return ipqc;
    }

    private ProTask targetTask(String status) {
        ProTask t = new ProTask();
        t.setTaskId(TASK_ID);
        t.setWorkorderId(WORKORDER_ID);
        t.setWorkorderCode(WORKORDER_CODE);
        t.setRouteId(ROUTE_ID);
        t.setProcessId(TARGET_PROCESS_ID);
        t.setProcessName(TARGET_PROCESS_NAME);
        t.setLeaderId(8L);
        t.setStatus(status);
        return t;
    }

    @BeforeEach
    void setUp() {
        service = new ProQcBlockServiceImpl();
        ReflectionTestUtils.setField(service, "flow", flow);
        ReflectionTestUtils.setField(service, "qcIpqcMapper", qcIpqcMapper);
        ReflectionTestUtils.setField(service, "blockReleaseMapper", blockReleaseMapper);
        ReflectionTestUtils.setField(service, "proTaskMapper", proTaskMapper);
        ReflectionTestUtils.setField(service, "sysTodoListMapper", sysTodoListMapper);
        ReflectionTestUtils.setField(service, "lockTemplate", lockTemplate);

        // 锁直接执行 Runnable；事务模板用 mock 事务管理器
        lenient().doAnswer(inv -> { inv.getArgument(1, Runnable.class).run(); return null; })
                .when(lockTemplate).execute(anyString(), any(Runnable.class));
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service, "txTemplate", new TransactionTemplate(transactionManager));

        securityUtilsMock = org.mockito.Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("1. 无检验前驱节点 → 放行(null)")
    void should_pass_when_no_prev_check_node() {
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.empty());

        QcBlockInfo block = service.findBlock(WORKORDER_ID, ROUTE_ID, TARGET_PROCESS_ID, CARD_ID, TASK_ID);

        assertThat(block).isNull();
        verify(qcIpqcMapper, never()).selectLatestCompletedByProcess(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("2. 最新判定单 PASS → 放行")
    void should_pass_when_ipqc_pass() {
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(WORKORDER_ID, CHECK_PROCESS_ID, CARD_ID))
                .thenReturn(ipqc("PASS"));

        assertThat(service.findBlock(WORKORDER_ID, ROUTE_ID, TARGET_PROCESS_ID, CARD_ID, TASK_ID)).isNull();
        verify(blockReleaseMapper, never()).existsByIpqcAndTask(anyLong(), anyLong());
    }

    @Test
    @DisplayName("3. 最新判定单 CONCESSION 让步接收 → 放行")
    void should_pass_when_concession() {
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(WORKORDER_ID, CHECK_PROCESS_ID, CARD_ID))
                .thenReturn(ipqc("CONCESSION"));

        assertThat(service.findBlock(WORKORDER_ID, ROUTE_ID, TARGET_PROCESS_ID, CARD_ID, TASK_ID)).isNull();
    }

    @Test
    @DisplayName("4. FAIL 且无放行记录 → 返回阻塞信息")
    void should_block_when_fail_without_release() {
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(WORKORDER_ID, CHECK_PROCESS_ID, CARD_ID))
                .thenReturn(ipqc("FAIL"));
        when(blockReleaseMapper.existsByIpqcAndTask(IPQC_ID, TASK_ID)).thenReturn(false);

        QcBlockInfo block = service.findBlock(WORKORDER_ID, ROUTE_ID, TARGET_PROCESS_ID, CARD_ID, TASK_ID);

        assertThat(block).isNotNull();
        assertThat(block.getIpqcId()).isEqualTo(IPQC_ID);
        assertThat(block.getIpqcCode()).isEqualTo(IPQC_CODE);
        assertThat(block.getCheckProcessName()).isEqualTo(CHECK_PROCESS_NAME);
        assertThat(block.getReason()).contains(IPQC_CODE).contains(CHECK_PROCESS_NAME);
    }

    @Test
    @DisplayName("5. FAIL 但已有放行记录 → 放行")
    void should_pass_when_fail_but_released() {
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(WORKORDER_ID, CHECK_PROCESS_ID, CARD_ID))
                .thenReturn(ipqc("FAIL"));
        when(blockReleaseMapper.existsByIpqcAndTask(IPQC_ID, TASK_ID)).thenReturn(true);

        assertThat(service.findBlock(WORKORDER_ID, ROUTE_ID, TARGET_PROCESS_ID, CARD_ID, TASK_ID)).isNull();
    }

    @Test
    @DisplayName("6. 无已判定(COMPLETED)检验单 → 放行（只拦已判不合格）")
    void should_pass_when_no_completed_ipqc() {
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(WORKORDER_ID, CHECK_PROCESS_ID, CARD_ID))
                .thenReturn(null);

        assertThat(service.findBlock(WORKORDER_ID, ROUTE_ID, TARGET_PROCESS_ID, CARD_ID, TASK_ID)).isNull();
    }

    @Test
    @DisplayName("7a. release 成功：插入放行记录(键正确)并关闭拦截待办")
    void should_release_when_blocked() {
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(targetTask("PRODUCING"));
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(eq(WORKORDER_ID), eq(CHECK_PROCESS_ID), any()))
                .thenReturn(ipqc("FAIL"));
        when(blockReleaseMapper.existsByIpqcAndTask(IPQC_ID, TASK_ID)).thenReturn(false);

        service.release(TASK_ID, " 客户急要，先放行这批 ");

        ArgumentCaptor<QcBlockRelease> rc = ArgumentCaptor.forClass(QcBlockRelease.class);
        verify(blockReleaseMapper).insertQcBlockRelease(rc.capture());
        QcBlockRelease saved = rc.getValue();
        assertThat(saved.getIpqcId()).isEqualTo(IPQC_ID);
        assertThat(saved.getIpqcCode()).isEqualTo(IPQC_CODE);
        assertThat(saved.getTargetTaskId()).isEqualTo(TASK_ID);
        assertThat(saved.getTargetProcessId()).isEqualTo(TARGET_PROCESS_ID);
        assertThat(saved.getReleaseReason()).isEqualTo("客户急要，先放行这批");
        assertThat(saved.getApproverId()).isEqualTo(1L);
        assertThat(saved.getApproverName()).contains("admin");
        assertThat(saved.getApproveTime()).isNotNull();

        verify(sysTodoListMapper).completePendingByDocAndCode(
                eq("IPQC"), eq(IPQC_ID),
                eq(IPQC_CODE + "::TASK:" + TASK_ID),
                any(), eq("人工放行：客户急要，先放行这批"), eq("admin"));
    }

    @Test
    @DisplayName("7b. release 无阻塞时抛「当前任务无需放行」")
    void should_throw_when_release_not_needed() {
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(targetTask("PRODUCING"));
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(eq(WORKORDER_ID), eq(CHECK_PROCESS_ID), any()))
                .thenReturn(ipqc("PASS"));

        assertThatThrownBy(() -> service.release(TASK_ID, "理由"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("当前任务无需放行");
        verify(blockReleaseMapper, never()).insertQcBlockRelease(any());
    }

    @Test
    @DisplayName("7c. release 理由为空或过短 → 抛「请填写放行理由」")
    void should_throw_when_reason_blank() {
        assertThatThrownBy(() -> service.release(TASK_ID, "  "))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("请填写放行理由");
        assertThatThrownBy(() -> service.release(TASK_ID, "x"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("请填写放行理由");
        verify(proTaskMapper, never()).selectProTaskByTaskId(anyLong());
    }

    @Test
    @DisplayName("8. assertReportable 阻塞时抛异常且文案含检验单号")
    void should_assert_reportable_throw_with_ipqc_code() {
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType("INTERNAL");
        fb.setWorkorderId(WORKORDER_ID);
        fb.setRouteId(ROUTE_ID);
        fb.setProcessId(TARGET_PROCESS_ID);
        fb.setCardId(CARD_ID);
        fb.setTaskId(TASK_ID);
        when(flow.prevCheckNode(ROUTE_ID, TARGET_PROCESS_ID)).thenReturn(Optional.of(checkNode()));
        when(qcIpqcMapper.selectLatestCompletedByProcess(WORKORDER_ID, CHECK_PROCESS_ID, CARD_ID))
                .thenReturn(ipqc("FAIL"));
        when(blockReleaseMapper.existsByIpqcAndTask(IPQC_ID, TASK_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.assertReportable(fb))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(IPQC_CODE);
    }

    @Test
    @DisplayName("8b. 外协报工(非 INTERNAL)不受质检门控")
    void should_skip_non_internal_feedback() {
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType("OUTSOURCE_RECPT");
        service.assertReportable(fb);
        verifyNoInteractionsForGate();
    }

    private void verifyNoInteractionsForGate() {
        verify(flow, never()).prevCheckNode(anyLong(), anyLong());
    }

    @Test
    @DisplayName("9. onIpqcFailed：下一波非终态任务逐建待办，重复触发幂等不再插入")
    void should_create_todo_once_when_on_ipqc_failed_twice() {
        QcIpqc failed = ipqc("FAIL");
        failed.setRouteId(ROUTE_ID);
        ProRouteProcess targetNode = new ProRouteProcess();
        targetNode.setProcessId(TARGET_PROCESS_ID);
        targetNode.setProcessName(TARGET_PROCESS_NAME);
        targetNode.setOrderNum(2);
        when(flow.nextWave(ROUTE_ID, CHECK_PROCESS_ID)).thenReturn(List.of(targetNode));
        when(proTaskMapper.selectProTaskList(any(ProTask.class)))
                .thenReturn(List.of(targetTask("PRODUCING"), targetTask("COMPLETED")));
        // 第一次：无 PENDING 待办 → 插入；第二次：已有 PENDING → 跳过
        when(sysTodoListMapper.selectPendingByDocAndCode(eq("IPQC"), eq(IPQC_ID),
                eq(IPQC_CODE + "::TASK:" + TASK_ID)))
                .thenReturn(null)
                .thenReturn(new SysTodoList());

        List<String> first = service.onIpqcFailed(failed);
        List<String> second = service.onIpqcFailed(failed);

        assertThat(first).containsExactly(TARGET_PROCESS_NAME);
        assertThat(second).containsExactly(TARGET_PROCESS_NAME);
        // 终态任务不建待办；两次触发只插入一条
        verify(sysTodoListMapper, times(1)).insertSysTodoList(any(SysTodoList.class));

        ArgumentCaptor<SysTodoList> tc = ArgumentCaptor.forClass(SysTodoList.class);
        verify(sysTodoListMapper).insertSysTodoList(tc.capture());
        SysTodoList todo = tc.getValue();
        assertThat(todo.getTodoType()).isEqualTo(TodoTypeEnum.PRO_QC_BLOCK.getCode());
        assertThat(todo.getSourceDocType()).isEqualTo("IPQC");
        assertThat(todo.getSourceDocId()).isEqualTo(IPQC_ID);
        assertThat(todo.getSourceDocCode()).isEqualTo(IPQC_CODE + "::TASK:" + TASK_ID);
        assertThat(todo.getUserId()).isEqualTo(8L);
        assertThat(todo.getStatus()).isEqualTo("PENDING");
        assertThat(todo.getTodoTitle()).contains(WORKORDER_CODE).contains(TARGET_PROCESS_NAME);
    }

    @Test
    @DisplayName("9b. onIpqcFailed：任务无负责人时待办按 派工人→判定人 兜底，user_id 永不为 null")
    void should_fallback_todo_assignee_when_leader_missing() {
        QcIpqc failed = ipqc("FAIL");
        failed.setRouteId(ROUTE_ID);
        ProRouteProcess targetNode = new ProRouteProcess();
        targetNode.setProcessId(TARGET_PROCESS_ID);
        targetNode.setProcessName(TARGET_PROCESS_NAME);
        targetNode.setOrderNum(2);
        when(flow.nextWave(ROUTE_ID, CHECK_PROCESS_ID)).thenReturn(List.of(targetNode));
        ProTask workerTask = targetTask("PRODUCING");
        workerTask.setLeaderId(null);
        workerTask.setWorkerId(9L);
        ProTask nakedTask = targetTask("NORMAL");
        nakedTask.setTaskId(TASK_ID + 1);
        nakedTask.setLeaderId(null);
        nakedTask.setWorkerId(null);
        when(proTaskMapper.selectProTaskList(any(ProTask.class)))
                .thenReturn(List.of(workerTask, nakedTask));
        when(sysTodoListMapper.selectPendingByDocAndCode(eq("IPQC"), eq(IPQC_ID), anyString()))
                .thenReturn(null);

        service.onIpqcFailed(failed);

        ArgumentCaptor<SysTodoList> tc = ArgumentCaptor.forClass(SysTodoList.class);
        verify(sysTodoListMapper, times(2)).insertSysTodoList(tc.capture());
        assertThat(tc.getAllValues()).extracting(SysTodoList::getUserId).containsExactly(9L, 1L);
    }

    @Test
    @DisplayName("9c. onIpqcFailed：App 手工单无 taskId/routeId 时按工单+检验工序任务反查路线")
    void should_resolve_route_via_workorder_when_ipqc_has_no_task() {
        QcIpqc failed = ipqc("FAIL");   // routeId/taskId 均为 null，仅 workorderId+processId
        ProRouteProcess targetNode = new ProRouteProcess();
        targetNode.setProcessId(TARGET_PROCESS_ID);
        targetNode.setProcessName(TARGET_PROCESS_NAME);
        targetNode.setOrderNum(2);
        ProTask checkTask = targetTask("PRODUCING");
        checkTask.setProcessId(CHECK_PROCESS_ID);  // 同工单同检验工序的任务携带 routeId
        when(proTaskMapper.selectProTaskList(any(ProTask.class)))
                .thenReturn(List.of(checkTask), List.of(checkTask));
        when(flow.nextWave(ROUTE_ID, CHECK_PROCESS_ID)).thenReturn(List.of(targetNode));
        when(sysTodoListMapper.selectPendingByDocAndCode(eq("IPQC"), eq(IPQC_ID), anyString()))
                .thenReturn(null);

        List<String> names = service.onIpqcFailed(failed);

        assertThat(names).containsExactly(TARGET_PROCESS_NAME);
        verify(sysTodoListMapper).insertSysTodoList(any(SysTodoList.class));
    }

    @Test
    @DisplayName("10. onIpqcFailed：下一波无任务时仍返回工序名（供检验页提示）")
    void should_return_process_names_even_without_tasks() {
        QcIpqc failed = ipqc("FAIL");
        failed.setRouteId(ROUTE_ID);
        ProRouteProcess targetNode = new ProRouteProcess();
        targetNode.setProcessId(TARGET_PROCESS_ID);
        targetNode.setProcessName(TARGET_PROCESS_NAME);
        targetNode.setOrderNum(2);
        when(flow.nextWave(ROUTE_ID, CHECK_PROCESS_ID)).thenReturn(List.of(targetNode));
        when(proTaskMapper.selectProTaskList(any(ProTask.class))).thenReturn(Collections.emptyList());

        List<String> names = service.onIpqcFailed(failed);

        assertThat(names).containsExactly(TARGET_PROCESS_NAME);
        verify(sysTodoListMapper, never()).insertSysTodoList(any());
    }
}
