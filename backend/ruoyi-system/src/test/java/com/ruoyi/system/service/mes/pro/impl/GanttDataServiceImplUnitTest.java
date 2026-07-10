package com.ruoyi.system.service.mes.pro.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.service.mes.pro.IScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GanttDataServiceImpl.availableWorkstations 单元测试（全 Mock，不连 DB）。
 */
@ExtendWith(MockitoExtension.class)
class GanttDataServiceImplUnitTest
{
    @InjectMocks
    private GanttDataServiceImpl service;
    @Mock
    private ProTaskMapper proTaskMapper;
    @Mock
    private IScheduleService scheduleService;

    private MdWorkstation ws(long id, String name) {
        MdWorkstation w = new MdWorkstation();
        w.setWorkstationId(id);
        w.setWorkstationCode("WS" + id);
        w.setWorkstationName(name);
        return w;
    }

    @Test
    @DisplayName("availableWorkstations: 给定时段时标记 idle，并将空闲工作站排到前面")
    void should_mark_idle_and_sort_when_time_given() {
        MdWorkstation ws1 = ws(201, "印刷机1");
        MdWorkstation ws2 = ws(202, "印刷机2");
        when(scheduleService.matchCandidates(eq(100L), eq("PRINT"), eq(1L)))
            .thenReturn(List.of(ws1, ws2)); // ws1 在前
        when(proTaskMapper.countConflict(eq(201L), any(), any(), eq(1L), any())).thenReturn(1); // 占用
        when(proTaskMapper.countConflict(eq(202L), any(), any(), eq(1L), any())).thenReturn(0); // 空闲

        Date start = new Date(1700000000000L);
        Date end = new Date(start.getTime() + 600_000L);
        List<Map<String, Object>> result = service.availableWorkstations(100L, "PRINT", start, end, null, 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("idle")).isEqualTo(true);   // 空闲 ws2 排到第一
        assertThat(result.get(0).get("workstationId")).isEqualTo(202L);
        assertThat(result.get(1).get("idle")).isEqualTo(false);
    }

    @Test
    @DisplayName("availableWorkstations: 未给定时段时 idle 为 null（不标注）且不查冲突")
    void should_idle_null_when_no_time() {
        when(scheduleService.matchCandidates(eq(100L), eq("PRINT"), eq(1L)))
            .thenReturn(List.of(ws(201, "印刷机1")));

        List<Map<String, Object>> result = service.availableWorkstations(100L, "PRINT", null, null, null, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("idle")).isNull();
        verifyNoInteractions(proTaskMapper);
    }
}
