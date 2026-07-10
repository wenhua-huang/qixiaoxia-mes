package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.service.mes.cal.IWorkCalendarService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScheduleServiceImpl 选站逻辑单元测试（全 Mock，不连 DB）。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplUnitTest
{
    @InjectMocks
    private ScheduleServiceImpl service;
    @Mock
    private ProWorkorderMapper workorderMapper;
    @Mock
    private ProRouteProductMapper routeProductMapper;
    @Mock
    private ProRouteProcessMapper routeProcessMapper;
    @Mock
    private ProTaskMapper taskMapper;
    @Mock
    private IWorkCalendarService calendarService;
    @Mock
    private MdWorkstationMapper workstationMapper;

    private MdWorkstation ws(long id, String code, String name) {
        MdWorkstation w = new MdWorkstation();
        w.setWorkstationId(id);
        w.setWorkstationCode(code);
        w.setWorkstationName(name);
        return w;
    }

    @Test
    @DisplayName("matchCandidates: process_id 精确命中时直接返回，不再查 process_type")
    void should_match_by_process_id_first() {
        when(workstationMapper.selectMdWorkstationList(any(MdWorkstation.class)))
            .thenReturn(List.of(ws(201, "WS1", "印刷机1")));

        List<MdWorkstation> result = service.matchCandidates(100L, "PRINT", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkstationId()).isEqualTo(201L);
        verify(workstationMapper, times(1)).selectMdWorkstationList(any(MdWorkstation.class));
    }

    @Test
    @DisplayName("matchCandidates: process_id 无匹配时退回 process_type")
    void should_fallback_to_process_type_when_process_id_empty() {
        when(workstationMapper.selectMdWorkstationList(any(MdWorkstation.class)))
            .thenReturn(new ArrayList<>(), List.of(ws(202, "WS2", "印刷机2")));

        List<MdWorkstation> result = service.matchCandidates(100L, "PRINT", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkstationId()).isEqualTo(202L);
        verify(workstationMapper, times(2)).selectMdWorkstationList(any(MdWorkstation.class));
    }

    @Test
    @DisplayName("matchCandidates: process_id 与 process_type 均无匹配返回空列表")
    void should_return_empty_when_no_match() {
        when(workstationMapper.selectMdWorkstationList(any(MdWorkstation.class)))
            .thenReturn(new ArrayList<>());

        List<MdWorkstation> result = service.matchCandidates(100L, null, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("scheduleWorkOrder: 无任务工序自动建任务时，在候选中选时段空闲的工作站")
    void should_pick_idle_workstation_when_schedule() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderId(1L);
        wo.setFactoryId(1L);
        wo.setRouteProductId(1L);
        wo.setWorkorderCode("WO1");
        wo.setWorkorderName("工单1");
        wo.setProductId(10L);
        wo.setProductCode("P1");
        wo.setProductName("产品1");
        wo.setQuantity(BigDecimal.TEN);
        wo.setUnitOfMeasure("个");
        wo.setUnitName("个");
        ProRouteProduct rp = new ProRouteProduct();
        rp.setRouteId(5L);
        ProRouteProcess rproc = new ProRouteProcess();
        rproc.setProcessId(100L);
        rproc.setProcessName("印刷");
        rproc.setOrderNum(1);
        rproc.setProcessType("PRINT");

        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(wo);
        when(routeProductMapper.selectProRouteProductByRecordId(1L)).thenReturn(rp);
        when(routeProcessMapper.selectProRouteProcessList(any())).thenReturn(new ArrayList<>(List.of(rproc)));
        when(taskMapper.selectProTaskList(any())).thenReturn(new ArrayList<>());
        MdWorkstation ws1 = ws(201, "WS1", "印刷机1");
        ws1.setCapacity(60);
        MdWorkstation ws2 = ws(202, "WS2", "印刷机2");
        ws2.setCapacity(60);
        when(workstationMapper.selectMdWorkstationList(any(MdWorkstation.class)))
            .thenReturn(List.of(ws1, ws2));
        when(taskMapper.insertProTask(any(ProTask.class))).thenAnswer(inv -> {
            ((ProTask) inv.getArgument(0)).setTaskId(999L);
            return 1;
        });
        Date start = new Date(1700000000000L);
        Date end = new Date(start.getTime() + 600_000L);
        when(calendarService.getNearestWorkingDay(any(), anyLong())).thenReturn(start);
        when(calendarService.calculateEndTime(any(), anyLong(), anyLong())).thenReturn(end);
        // ws1 时段被占用、ws2 空闲
        when(taskMapper.countConflict(eq(201L), any(), any(), eq(1L), eq(999L))).thenReturn(1);
        when(taskMapper.countConflict(eq(202L), any(), any(), eq(1L), eq(999L))).thenReturn(0);

        Map<String, Object> result = service.scheduleWorkOrder(1L);

        assertThat(result).doesNotContainKey("error");
        ArgumentCaptor<ProTask> captor = ArgumentCaptor.forClass(ProTask.class);
        verify(taskMapper).updateProTask(captor.capture());
        assertThat(captor.getValue().getWorkstationId()).isEqualTo(202L); // 选了空闲的 ws2
    }
}
