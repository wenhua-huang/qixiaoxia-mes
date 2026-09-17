package com.ruoyi.system.service.mes.pro.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 任务下发闸门单测：机台（厂内必填，外协 VENDOR 豁免）+ 负责人（厂内必填，外协 VENDOR 豁免）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务下发闸门")
class ProTaskDispatchUnitTest {

    @Mock private ProTaskMapper proTaskMapper;
    @Mock private MdWorkstationMapper mdWorkstationMapper;
    @InjectMocks private ProTaskServiceImpl taskService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private ProTask internalTask() {
        ProTask t = new ProTask();
        t.setTaskId(100L);
        t.setTaskCode("WO-1-001");
        t.setStatus(ProConstants.TASK_STATUS_NORMAL);
        t.setWorkstationId(216L);
        t.setWorkstationCode("WST-01");
        return t;
    }

    @Test
    @DisplayName("厂内任务有启用机台但无负责人：拒绝下发")
    void rejectsInternalTaskWithoutLeader() {
        ProTask t = internalTask();
        when(proTaskMapper.selectProTaskByTaskId(100L)).thenReturn(t);
        MdWorkstation ws = new MdWorkstation();
        ws.setEnableFlag("1");
        when(mdWorkstationMapper.selectMdWorkstationByWorkstationId(216L)).thenReturn(ws);

        assertThatThrownBy(() -> taskService.dispatchTask(100L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("负责人");
        verify(proTaskMapper, never()).updateProTask(any());
    }

    @Test
    @DisplayName("外协任务无负责人也可下发（VENDOR 无内部负责人指派入口，豁免）")
    void dispatchesOutsourceTaskWithoutLeader() {
        ProTask t = internalTask();
        t.setWorkstationId(0L);
        t.setWorkstationCode(ProConstants.WS_CODE_VENDOR);
        when(proTaskMapper.selectProTaskByTaskId(100L)).thenReturn(t);

        taskService.dispatchTask(100L);

        assertThat(t.getStatus()).isEqualTo(ProConstants.TASK_STATUS_PRODUCING);
        verify(proTaskMapper).updateProTask(t);
        // 外协不查厂内机台表
        verifyNoInteractions(mdWorkstationMapper);
    }

    @Test
    @DisplayName("外协任务指派负责人后可正常下发到生产中")
    void dispatchesOutsourceTaskWithLeader() {
        ProTask t = internalTask();
        t.setWorkstationId(0L);
        t.setWorkstationCode(ProConstants.WS_CODE_VENDOR);
        t.setLeaderId(1L);
        when(proTaskMapper.selectProTaskByTaskId(100L)).thenReturn(t);

        taskService.dispatchTask(100L);

        assertThat(t.getStatus()).isEqualTo(ProConstants.TASK_STATUS_PRODUCING);
        verify(proTaskMapper).updateProTask(t);
    }

    private ProTask workorderTask(long taskId, String processName, String wsCode, Long leaderId) {
        ProTask t = new ProTask();
        t.setTaskId(taskId);
        t.setTaskCode("WO-1-" + taskId);
        t.setProcessName(processName);
        t.setStatus(ProConstants.TASK_STATUS_NORMAL);
        t.setWorkstationId(ProConstants.WS_CODE_VENDOR.equals(wsCode) ? 0L : 216L);
        t.setWorkstationCode(wsCode);
        t.setLeaderId(leaderId);
        return t;
    }

    private void mockWorkorderTasks(java.util.List<ProTask> tasks) {
        when(proTaskMapper.selectProTaskList(any())).thenReturn(tasks);
    }

    @Test
    @DisplayName("工单开工批量下发：厂内任务有机台但无负责人，整体拒绝且不更新状态")
    void rejectsWorkorderDispatchWithoutLeader() {
        MdWorkstation ws = new MdWorkstation();
        ws.setEnableFlag("1");
        when(mdWorkstationMapper.selectMdWorkstationByWorkstationId(216L)).thenReturn(ws);
        mockWorkorderTasks(java.util.List.of(
                workorderTask(1L, "印刷", "WST-01", null),
                workorderTask(2L, "包装", "WST-02", 1L)));

        assertThatThrownBy(() -> taskService.dispatchByWorkorder(900L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("印刷")
                .hasMessageContaining("负责人");
        verify(proTaskMapper, never())
                .updateStatusByWorkorder(anyLong(), anyList(), anyString());
    }

    @Test
    @DisplayName("工单开工批量下发：仅外协任务且无负责人时放行（外协 VENDOR 豁免负责人）")
    void dispatchesWorkorderWhenOnlyOutsourceWithoutLeader() {
        mockWorkorderTasks(java.util.List.of(
                workorderTask(1L, "覆膜", ProConstants.WS_CODE_VENDOR, null)));

        taskService.dispatchByWorkorder(900L);

        verify(proTaskMapper).updateStatusByWorkorder(
                eq(900L), anyList(), eq(ProConstants.TASK_STATUS_PRODUCING));
        verifyNoInteractions(mdWorkstationMapper);
    }

    @Test
    @DisplayName("工单开工批量下发：厂内缺负责人+外协无负责人时只报错厂内工序，外协放行")
    void rejectsWorkorderDispatchListingOnlyInternalMissingLeader() {
        MdWorkstation ws = new MdWorkstation();
        ws.setEnableFlag("1");
        when(mdWorkstationMapper.selectMdWorkstationByWorkstationId(216L)).thenReturn(ws);
        mockWorkorderTasks(java.util.List.of(
                workorderTask(1L, "印刷", "WST-01", null),
                workorderTask(2L, "覆膜", ProConstants.WS_CODE_VENDOR, null)));

        ServiceException ex = catchThrowableOfType(
                () -> taskService.dispatchByWorkorder(900L), ServiceException.class);
        assertThat(ex.getMessage()).contains("印刷").doesNotContain("覆膜");
        verify(proTaskMapper, never())
                .updateStatusByWorkorder(anyLong(), anyList(), anyString());
    }

    @Test
    @DisplayName("工单开工批量下发：机台与负责人齐全（含外协）才批量置生产中")
    void dispatchesWorkorderWhenMachineAndLeaderReady() {
        MdWorkstation ws = new MdWorkstation();
        ws.setEnableFlag("1");
        when(mdWorkstationMapper.selectMdWorkstationByWorkstationId(216L)).thenReturn(ws);
        mockWorkorderTasks(java.util.List.of(
                workorderTask(1L, "印刷", "WST-01", 1L),
                workorderTask(2L, "覆膜", ProConstants.WS_CODE_VENDOR, 2L)));

        taskService.dispatchByWorkorder(900L);

        verify(proTaskMapper).updateStatusByWorkorder(
                eq(900L), anyList(), eq(ProConstants.TASK_STATUS_PRODUCING));
    }

    @Test
    @DisplayName("工序执行行回填 leaderAssigned：无负责人为 false，齐全为 true")
    void processRowsCarryLeaderAssignedFlag() {
        MdWorkstation ws = new MdWorkstation();
        ws.setEnableFlag("1");
        when(mdWorkstationMapper.selectMdWorkstationByWorkstationId(216L)).thenReturn(ws);
        ProRouteProcess rp = new ProRouteProcess();
        rp.setProcessId(10L);
        rp.setProcessName("印刷");
        ProTask task = workorderTask(1L, "印刷", "WST-01", null);
        task.setProcessId(10L);
        mockWorkorderTasks(java.util.List.of(task));

        java.util.List<java.util.Map<String, Object>> rows =
                taskService.listProcessExecutionRows(900L, java.util.List.of(rp));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("leaderAssigned")).isEqualTo(false);
    }
}
