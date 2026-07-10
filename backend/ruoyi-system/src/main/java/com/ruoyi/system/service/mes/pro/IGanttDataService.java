package com.ruoyi.system.service.mes.pro;

import java.util.Map;
import java.util.List;

/**
 * 甘特图数据组装Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public interface IGanttDataService
{
    /**
     * 构建单工单甘特图数据
     * @return { tasks: [...], links: [...] }
     */
    Map<String, Object> buildWorkOrderGantt(Long workorderId);

    /**
     * 构建工作站维度甘特图（多工单聚合）
     */
    Map<String, Object> buildWorkstationGantt(Long workstationId, String startDate, String endDate);

    /**
     * 查询某工序的可用工作站：按 process_id / process_type 匹配候选，并标记给定时段是否空闲。
     * @param processId     工序ID（必填）
     * @param processType   工序类型（可选，process_id 无匹配时按类型兜底）
     * @param startTime     计划开始时间（可选，为空则不判空闲）
     * @param endTime       计划结束时间
     * @param excludeTaskId 排除自身任务（编辑场景）
     * @param factoryId     工厂ID
     */
    List<Map<String, Object>> availableWorkstations(Long processId, String processType,
                                                    java.util.Date startTime, java.util.Date endTime,
                                                    Long excludeTaskId, Long factoryId);
}
