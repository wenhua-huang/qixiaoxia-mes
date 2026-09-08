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
     * 构建单工单甘特图数据（只读场景）。
     * @param autoSchedule 无任务时是否触发自动排产；只读详情页传 false，避免 GET 请求产生写副作用
     * @return { tasks: [...], links: [...] }
     */
    Map<String, Object> buildWorkOrderGantt(Long workorderId, boolean autoSchedule);

    /**
     * 构建工作站维度甘特图（多工单聚合）
     */
    Map<String, Object> buildWorkstationGantt(Long workstationId, String startDate, String endDate);

    /**
     * 构建机台泳道视图：每个启用工作站一行、行内为该机台的任务条；
     * 未指派机台的厂内任务归"⚠待指派机台"行，外协任务归"外协"行。
     * @param startDate 起始日期 yyyy-MM-dd（可空，不过滤）
     * @param endDate   截止日期 yyyy-MM-dd（可空）
     * @param factoryId 工厂ID
     * @return { rows: [ { workstationId, workstationName, workstationCode, tasks: [...] } ] }
     */
    Map<String, Object> buildWorkstationView(String startDate, String endDate, Long factoryId);

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
