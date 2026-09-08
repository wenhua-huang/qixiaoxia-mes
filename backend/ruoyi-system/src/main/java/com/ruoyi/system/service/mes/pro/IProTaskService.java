package com.ruoyi.system.service.mes.pro;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;

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

    /**
     * 查可报工任务（厂内 PRODUCING + 工单 PREPARE/PRODUCING），供移动端待报工列表。
     */
    public List<ProTask> selectReportableTaskList(ProTask proTask);
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
     * 开工排产检查：逐道工序给出执行方式明细。
     * 每行含 processId/processCode/processName/orderNum、execType（OUTSOURCE 外协 / INHOUSE 厂内 / UNSCHEDULED 未排产）、
     * execTypeName、resourceName（外协厂商名 或 厂内机台名）、assigned（外协恒为 true；厂内仅指派有效机台为 true）。
     * 外协工序用外厂机器，不校验厂内机台；厂内工序 assigned=false 即"待指派机台"，开工前必须先指派。
     */
    public List<Map<String, Object>> listProcessExecutionRows(Long workorderId, List<ProRouteProcess> routeProcesses);

    /**
     * 工单开工级联：把该工单下所有 NORMAL/PREPARE 任务自动下发为 PRODUCING
     */
    public void dispatchByWorkorder(Long workorderId);

    /**
     * 取消工单级联：把该工单下所有非终态任务自动置为 CANCEL
     */
    public void cancelByWorkorder(Long workorderId);
}
