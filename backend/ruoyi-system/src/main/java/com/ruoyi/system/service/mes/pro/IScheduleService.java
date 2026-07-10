package com.ruoyi.system.service.mes.pro;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.mes.md.MdWorkstation;

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

    /**
     * 按工序匹配候选工作站：优先 process_id 精确，无则 process_type 兜底
     * （覆盖 process_id 为空、仅登记 process_type 的工作站）。仅启用工作站。
     */
    List<MdWorkstation> matchCandidates(Long processId, String processType, Long factoryId);
}
