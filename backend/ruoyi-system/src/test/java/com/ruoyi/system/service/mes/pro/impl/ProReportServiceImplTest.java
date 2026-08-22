package com.ruoyi.system.service.mes.pro.impl;

import com.ruoyi.system.domain.mes.pro.vo.ProductivityRowVO;
import com.ruoyi.system.domain.mes.pro.vo.ReportOverviewVO;
import com.ruoyi.system.domain.mes.pro.vo.TaskStatusDistVO;
import com.ruoyi.system.mapper.mes.pro.ProReportMapper;
import com.ruoyi.system.service.ISysConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProReportServiceImpl 单元测试（纯 Mockito，不连 DB）。
 */
@ExtendWith(MockitoExtension.class)
class ProReportServiceImplTest
{
    @InjectMocks
    private ProReportServiceImpl service;

    @Mock
    private ProReportMapper reportMapper;

    @Mock
    private ISysConfigService configService;

    private Map<String, Object> overviewCounts(int total, int completed, int delayed, int inProgress)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("workorderTotal", (long) total);
        m.put("workorderCompleted", (long) completed);
        m.put("workorderDelayed", (long) delayed);
        m.put("workorderInProgress", (long) inProgress);
        return m;
    }

    @Test
    @DisplayName("overview: 效率 = 标准工时*100/实际工时，完工率/延期率正确")
    void overview_efficiencyPercent_standardOverActual()
    {
        when(reportMapper.selectOverviewCounts(any(), any(), any(), any()))
                .thenReturn(overviewCounts(10, 5, 2, 3));
        when(reportMapper.selectStandardMinutes(any(), any(), any(), any())).thenReturn(600L);
        when(reportMapper.selectActualMinutes(any(), any(), any(), any())).thenReturn(300L);

        ReportOverviewVO vo = service.overview(new Date(), new Date(), null, null);

        assertThat(vo.getWorkorderTotal()).isEqualTo(10);
        assertThat(vo.getWorkorderCompleted()).isEqualTo(5);
        assertThat(vo.getCompletionRate()).isEqualTo(50);
        assertThat(vo.getDelayRate()).isEqualTo(20);
        assertThat(vo.getStandardMinutes()).isEqualTo(600);
        assertThat(vo.getActualMinutes()).isEqualTo(300);
        assertThat(vo.getEfficiencyPercent()).isEqualTo(200);
    }

    @Test
    @DisplayName("overview: 实际工时为 0 时效率为 null")
    void overview_zeroActualMinutes_efficiencyNull()
    {
        when(reportMapper.selectOverviewCounts(any(), any(), any(), any()))
                .thenReturn(overviewCounts(10, 5, 2, 3));
        when(reportMapper.selectStandardMinutes(any(), any(), any(), any())).thenReturn(600L);
        when(reportMapper.selectActualMinutes(any(), any(), any(), any())).thenReturn(0L);

        ReportOverviewVO vo = service.overview(new Date(), new Date(), null, null);

        assertThat(vo.getEfficiencyPercent()).isNull();
        assertThat(vo.getCompletionRate()).isEqualTo(50);
    }

    @Test
    @DisplayName("productivity: groupBy=TEAM 路由到班组 mapper 并合并工时")
    void productivity_team_routesToTeamMapper()
    {
        when(reportMapper.selectTeamOutput(any(), any(), any()))
                .thenReturn(List.of(teamOutputRow()));
        when(reportMapper.selectTeamHours(any(), any(), any()))
                .thenReturn(List.of(Map.of("groupId", 7L, "actualMinutes", 120L)));

        List<ProductivityRowVO> rows = service.productivity("TEAM", new Date(), new Date(), null, null);

        assertThat(rows).hasSize(1);
        ProductivityRowVO row = rows.get(0);
        assertThat(row.getGroupId()).isEqualTo(7L);
        assertThat(row.getGroupName()).isEqualTo("甲班");
        assertThat(row.getActualMinutes()).isEqualTo(120);
        assertThat(row.getWorkorderCount()).isEqualTo(3);
        verify(reportMapper).selectTeamOutput(any(), any(), isNull());
        verify(reportMapper, never()).selectWorkshopOutput(any(), any(), any());
    }

    @Test
    @DisplayName("productivity: 未知 groupBy 默认按车间")
    void productivity_unknownGroupBy_defaultsWorkshop()
    {
        when(reportMapper.selectWorkshopOutput(any(), any(), any())).thenReturn(List.of());
        when(reportMapper.selectWorkshopHours(any(), any(), any())).thenReturn(List.of());

        List<ProductivityRowVO> rows = service.productivity("UNKNOWN", new Date(), new Date(), null, null);

        assertThat(rows).isEmpty();
        verify(reportMapper).selectWorkshopOutput(any(), any(), any());
        verify(reportMapper, never()).selectTeamOutput(any(), any(), any());
    }

    @Test
    @DisplayName("taskStatus: 正确映射状态与计数")
    void taskStatus_mapsRows()
    {
        when(reportMapper.selectTaskStatus(any(), any(), any()))
                .thenReturn(List.of(Map.of("status", "PRODUCING", "cnt", 4L)));

        List<TaskStatusDistVO> rows = service.taskStatus(new Date(), new Date(), null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo("PRODUCING");
        assertThat(rows.get(0).getCount()).isEqualTo(4);
    }

    private Map<String, Object> teamOutputRow()
    {
        Map<String, Object> m = new HashMap<>();
        m.put("groupId", 7L);
        m.put("groupName", "甲班");
        m.put("workorderCount", 3L);
        m.put("completedCount", 0L);
        m.put("delayedCount", 0L);
        m.put("standardOutput", BigDecimal.ZERO);
        m.put("actualOutput", BigDecimal.ZERO);
        m.put("standardMinutes", 0L);
        m.put("qualifiedQty", BigDecimal.ZERO);
        m.put("scrapQty", BigDecimal.ZERO);
        return m;
    }
}
