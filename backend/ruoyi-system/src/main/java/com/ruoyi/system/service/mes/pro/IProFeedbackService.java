package com.ruoyi.system.service.mes.pro;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProFeedbackConsume;

/**
 * 报工记录Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProFeedbackService
{
    /**
     * 查询报工记录
     *
     * @param recordId 报工记录主键
     * @return 报工记录
     */
    public ProFeedback selectProFeedbackByRecordId(Long recordId);

    /**
     * 查询报工记录列表
     *
     * @param proFeedback 报工记录
     * @return 报工记录集合
     */
    public List<ProFeedback> selectProFeedbackList(ProFeedback proFeedback);

    /**
     * 查询所有报工记录
     *
     * @return 报工记录集合
     */
    public List<ProFeedback> selectAll();

    /**
     * 检查报工编码唯一性
     *
     * @param proFeedback 报工记录
     * @return 结果
     */
    public boolean checkFeedbackCodeUnique(ProFeedback proFeedback);

    /**
     * 新增报工记录
     *
     * @param proFeedback 报工记录
     * @return 结果
     */
    public int insertProFeedback(ProFeedback proFeedback);

    /**
     * 修改报工记录
     *
     * @param proFeedback 报工记录
     * @return 结果
     */
    public int updateProFeedback(ProFeedback proFeedback);

    /**
     * 批量删除报工记录
     *
     * @param recordIds 需要删除的报工记录主键集合
     * @return 结果
     */
    public int deleteProFeedbackByRecordIds(Long[] recordIds);

    /**
     * 删除报工记录信息
     *
     * @param recordId 报工记录主键
     * @return 结果
     */
    public int deleteProFeedbackByRecordId(Long recordId);

    /** 审核报工：CONFIRMED→AUDITED，同时增量更新任务和工单已生产数量 */
    public void auditFeedback(Long recordId);

    /**
     * 确认报工：PREPARE → CONFIRMED（下沉自 Controller，供批量复用）。
     * 确认成功后触发 IPQC 工序检待检单生成（弱拦截，失败仅告警不回滚确认）。
     *
     * @return 生成的 IPQC 检验单编码；null = 未生成（非检验工序/未绑模板/已有活动单）
     */
    public String confirmFeedback(Long recordId);

    /**
     * 批量确认报工：逐条调用 confirmFeedback，尽力执行，失败逐条收集。
     * @return total/successCount/failedCount/failures[{recordId,feedbackCode,workorderName,reason}]
     */
    public java.util.Map<String, Object> batchConfirmFeedback(Long[] recordIds);

    /**
     * 批量审核报工：逐条调用 auditFeedback，尽力执行，失败逐条收集。
     * @return total/successCount/failedCount/failures[{recordId,feedbackCode,workorderName,reason}]
     */
    public java.util.Map<String, Object> batchAuditFeedback(Long[] recordIds);

    /**
     * 批量统计每个 task 的待审核(PREPARE)报工数。
     * @param taskIds 任务ID集合
     * @return taskId → 待审核报工数（无记录的 taskId 不在 map 中）
     */
    public Map<Long, Integer> countPendingByTaskIds(Collection<Long> taskIds);

    /**
     * 获取工单默认物料消耗（新增报工时预填）。
     * 从工单BOM构建，batchCode 反查该工单最近领料的真实批次（与 insertProFeedback 自动填充逻辑一致）。
     */
    public List<ProFeedbackConsume> getDefaultConsume(Long workorderId);
}
