package com.ruoyi.system.service.mes.pro;

import java.util.Map;

/**
 * 排产计算Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public interface IScheduleService
{
    /**
     * 对单工单执行自动排产（正排：从今天往后）
     */
    Map<String, Object> scheduleWorkOrder(Long workorderId);

    /**
     * 拖拽移动任务 + 级联更新后继
     */
    Map<String, Object> moveTask(Long taskId, String newStartTime, String newEndTime);
}
