package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProFeedbackParam;

/**
 * 报工实际参数值Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProFeedbackParamService
{
    /**
     * 查询报工实际参数值
     *
     * @param recordId 报工实际参数值主键
     * @return 报工实际参数值
     */
    public ProFeedbackParam selectProFeedbackParamByRecordId(Long recordId);

    /**
     * 查询报工实际参数值列表
     *
     * @param proFeedbackParam 报工实际参数值
     * @return 报工实际参数值集合
     */
    public List<ProFeedbackParam> selectProFeedbackParamList(ProFeedbackParam proFeedbackParam);

    /**
     * 根据报工ID查询参数列表
     *
     * @param feedbackId 报工ID
     * @return 报工实际参数值集合
     */
    public List<ProFeedbackParam> selectProFeedbackParamByFeedbackId(Long feedbackId);

    /**
     * 新增报工实际参数值
     *
     * @param proFeedbackParam 报工实际参数值
     * @return 结果
     */
    public int insertProFeedbackParam(ProFeedbackParam proFeedbackParam);

    /**
     * 修改报工实际参数值
     *
     * @param proFeedbackParam 报工实际参数值
     * @return 结果
     */
    public int updateProFeedbackParam(ProFeedbackParam proFeedbackParam);

    /**
     * 批量删除报工实际参数值
     *
     * @param recordIds 需要删除的报工实际参数值主键集合
     * @return 结果
     */
    public int deleteProFeedbackParamByRecordIds(Long[] recordIds);

    /**
     * 删除报工实际参数值信息
     *
     * @param recordId 报工实际参数值主键
     * @return 结果
     */
    public int deleteProFeedbackParamByRecordId(Long recordId);

    /**
     * 根据报工ID删除参数
     *
     * @param feedbackId 报工ID
     * @return 结果
     */
    public int deleteProFeedbackParamByFeedbackId(Long feedbackId);
}
