package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.vo.DelayItemVO;
import com.ruoyi.system.domain.mes.pro.vo.WorkorderProgressVO;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProProgressMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.service.ISysConfigService;

/**
 * ProProgressServiceImpl 单元测试（全 Mockito，不连 DB）。
 */
@ExtendWith(MockitoExtension.class)
class ProProgressServiceImplTest
{
    @InjectMocks
    private ProProgressServiceImpl service;

    @Mock
    private ProWorkorderMapper workorderMapper;
    @Mock
    private ProTaskMapper taskMapper;
    @Mock
    private ProCardMapper cardMapper;
    @Mock
    private ProCardProcessMapper cardProcessMapper;
    @Mock
    private ProProgressMapper progressMapper;
    @Mock
    private ISysConfigService configService;

    private Date daysAgo(int days)
    {
        return new Date(System.currentTimeMillis() - days * 24L * 3600_000L);
    }

    private Date daysFromNow(int days)
    {
        return new Date(System.currentTimeMillis() + days * 24L * 3600_000L);
    }

    private ProWorkorder overdueWorkorder()
    {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderId(1L);
        wo.setWorkorderCode("WO-001");
        wo.setWorkorderName("测试工单");
        wo.setProductCode("P-001");
        wo.setProductName("产品A");
        wo.setStatus(ProConstants.WORKORDER_STATUS_PRODUCING);
        wo.setQuantity(new BigDecimal("100"));
        wo.setQuantityProduced(new BigDecimal("50"));
        wo.setRequestDate(daysAgo(3));
        return wo;
    }

    private ProTask futureTask()
    {
        ProTask t = new ProTask();
        t.setTaskId(10L);
        t.setTaskCode("T-001");
        t.setTaskName("印刷");
        t.setProcessId(100L);
        t.setProcessCode("PRINT");
        t.setProcessName("印刷");
        t.setStatus(ProConstants.TASK_STATUS_PRODUCING);
        t.setQuantity(new BigDecimal("100"));
        t.setQuantityProduced(new BigDecimal("50"));
        t.setStartTime(daysAgo(1));
        t.setEndTime(daysFromNow(7));
        return t;
    }

    @Test
    @DisplayName("getWorkorderProgress: 已过需求日期的生产中工单返回 DELAY，完成率 50%，任务行 NORMAL")
    void getWorkorderProgress_overdueWorkorder_returnsDelayLevel()
    {
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(overdueWorkorder());
        Map<String, Object> agg = new HashMap<>();
        agg.put("planStart", daysAgo(2));
        agg.put("planEnd", daysFromNow(5));
        agg.put("actualStart", daysAgo(1));
        agg.put("actualEnd", null);
        when(progressMapper.aggregateWorkorderTime(1L)).thenReturn(agg);
        when(taskMapper.selectProTaskList(any())).thenReturn(List.of(futureTask()));
        when(progressMapper.aggregateActualByTaskIds(any())).thenReturn(List.of());
        when(cardMapper.selectProCardList(any())).thenReturn(List.<ProCard>of());
        when(configService.selectConfigByKey(anyString())).thenReturn(null);

        WorkorderProgressVO vo = service.getWorkorderProgress(1L);

        assertThat(vo.getDelayLevel()).isEqualTo(ProConstants.DELAY_OVERDUE);
        assertThat(vo.getCompletionRate()).isEqualTo(50);
        assertThat(vo.getProcesses()).hasSize(1);
        assertThat(vo.getProcesses().get(0).getDelayLevel())
                .isEqualTo(ProConstants.DELAY_NORMAL);
        assertThat(vo.getProcesses().get(0).getPlanStartTime()).isNotNull();
        assertThat(vo.getProcesses().get(0).getPlanEndTime()).isNotNull();
    }

    @Test
    @DisplayName("getWorkorderProgress: 工单不存在抛 ServiceException")
    void getWorkorderProgress_notFound_throws()
    {
        when(workorderMapper.selectProWorkorderByWorkorderId(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> service.getWorkorderProgress(999L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("工单不存在");
    }

    @Test
    @DisplayName("selectDelayList: objectType=TASK 路由到 selectTaskDelays 并计算 WARNING")
    void selectDelayList_task_routesToTaskMapper()
    {
        DelayItemVO item = new DelayItemVO();
        item.setObjectType("TASK");
        item.setObjectId(10L);
        item.setObjectCode("T-001");
        item.setStatus(ProConstants.TASK_STATUS_PRODUCING);
        item.setPlanTime(new Date(System.currentTimeMillis() + 2L * 3600_000L));
        when(progressMapper.selectTaskDelays(any(), any(), any(), any(), any()))
                .thenReturn(List.of(item));
        when(configService.selectConfigByKey(anyString())).thenReturn(null);

        List<DelayItemVO> result = service.selectDelayList(
                "TASK", null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDelayLevel()).isEqualTo(ProConstants.DELAY_WARNING);
        verify(progressMapper).selectTaskDelays(isNull(), isNull(), isNull(), isNull(), anyInt());
        verify(progressMapper, never())
                .selectWorkorderDelays(any(), any(), any(), any());
    }

    @Test
    @DisplayName("selectDelayList: objectType=WORKORDER 路由到 selectWorkorderDelays 并计算 DELAY")
    void selectDelayList_workorder_routesToWorkorderMapper()
    {
        DelayItemVO item = new DelayItemVO();
        item.setObjectType("WORKORDER");
        item.setObjectId(1L);
        item.setObjectCode("WO-001");
        item.setStatus(ProConstants.WORKORDER_STATUS_PRODUCING);
        item.setRequestDate(daysAgo(2));
        when(progressMapper.selectWorkorderDelays(any(), any(), any(), any()))
                .thenReturn(List.of(item));
        when(configService.selectConfigByKey(anyString())).thenReturn(null);

        List<DelayItemVO> result = service.selectDelayList(
                "WORKORDER", null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDelayLevel()).isEqualTo(ProConstants.DELAY_OVERDUE);
        verify(progressMapper).selectWorkorderDelays(isNull(), isNull(), isNull(), anyInt());
        verify(progressMapper, never())
                .selectTaskDelays(any(), any(), any(), any(), any());
    }
}
