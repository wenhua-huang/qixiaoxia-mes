package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProFeedbackChange;

/**
 * 报工字段变更痕迹Service接口
 *
 * @author qixiaoxia
 */
public interface IProFeedbackChangeService
{
    /**
     * 记录「本次上机数量」变更；old/new 数值相等（含同为 null）时不记录。
     *
     * @param feedbackId 报工记录ID
     * @param fb         报工对象（取 taskId/workorderId 快照）
     * @param oldVal     原值（系统默认值或修改前的值；可为 null）
     * @param newVal     新值（人工填写值；可为 null）
     * @param source     {@link com.ruoyi.system.domain.mes.pro.ProConstants#CHANGE_SOURCE_MANUAL} / SYSTEM
     * @param reason     变更说明
     */
    void recordInputChange(Long feedbackId, ProFeedback fb, BigDecimal oldVal, BigDecimal newVal,
                           String source, String reason);

    /** 按报工记录查痕迹（create_time 倒序） */
    List<ProFeedbackChange> selectByFeedback(Long feedbackId);

    /** 按排产任务查痕迹（create_time 倒序） */
    List<ProFeedbackChange> selectByTask(Long taskId);
}
