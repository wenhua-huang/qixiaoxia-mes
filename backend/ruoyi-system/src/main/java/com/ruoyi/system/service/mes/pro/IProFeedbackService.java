package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProFeedback;

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
}
