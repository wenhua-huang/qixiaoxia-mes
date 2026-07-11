package com.ruoyi.system.service.mes.pro;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.mes.pro.ProTask;

/**
 * 生产任务/排产Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProTaskService
{
    public ProTask selectProTaskByTaskId(Long taskId);
    public List<ProTask> selectProTaskList(ProTask proTask);
    public List<ProTask> selectAll();
    public int insertProTask(ProTask proTask);
    public int updateProTask(ProTask proTask);
    public int deleteProTaskByTaskIds(Long[] taskIds);
    public int deleteProTaskByTaskId(Long taskId);

    /**
     * 按工序汇总生产进度（用于工单详情页显示各工序完成情况）
     * @param workorderId 工单ID
     * @return 每道工序的排产数量、已生产数量、合格/不合格数量
     */
    public List<Map<String, Object>> selectProcessProgressByWorkorder(Long workorderId);

    /**
     * 下发单个任务：NORMAL/PREPARE → PRODUCING
     */
    public void dispatchTask(Long taskId);

    /**
     * 完成单个任务：PRODUCING → COMPLETED。
     * 若完成的是末工序任务，额外检查工单是否产够→自动完工。
     */
    public void completeTask(Long taskId);

    /**
     * 取消单个任务：非终态（非 COMPLETED/CANCEL）→ CANCEL
     */
    public void cancelTask(Long taskId);

    /**
     * 工单开工级联：把该工单下所有 NORMAL/PREPARE 任务自动下发为 PRODUCING
     */
    public void dispatchByWorkorder(Long workorderId);

    /**
     * 取消工单级联：把该工单下所有非终态任务自动置为 CANCEL
     */
    public void cancelByWorkorder(Long workorderId);
}
