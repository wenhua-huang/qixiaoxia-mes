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
}
