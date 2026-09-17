package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 报工字段变更痕迹对象 qxx_pro_feedback_change
 *
 * <p>记录「本次上机数量」系统默认值与人工填写值不一致，以及报工编辑时的前后差异。
 *
 * @author qixiaoxia
 */
public class ProFeedbackChange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long changeId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "报工记录ID") private Long feedbackId;
    @Excel(name = "任务ID") private Long taskId;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "变更字段") private String fieldName;
    @Excel(name = "原值") private String oldValue;
    @Excel(name = "新值") private String newValue;
    @Excel(name = "来源") private String changeSource;
    @Excel(name = "变更说明") private String changeReason;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long v) { this.changeId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String v) { this.fieldName = v; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String v) { this.oldValue = v; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String v) { this.newValue = v; }
    public String getChangeSource() { return changeSource; }
    public void setChangeSource(String v) { this.changeSource = v; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String v) { this.changeReason = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("changeId", getChangeId()).append("feedbackId", getFeedbackId())
            .append("fieldName", getFieldName()).append("oldValue", getOldValue())
            .append("newValue", getNewValue()).append("changeSource", getChangeSource()).toString();
    }
}
