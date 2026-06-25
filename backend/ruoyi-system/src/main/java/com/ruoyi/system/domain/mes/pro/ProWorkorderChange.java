package com.ruoyi.system.domain.mes.pro;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单变更记录对象 qxx_pro_workorder_change
 */
public class ProWorkorderChange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long changeId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "变更类型") private String changeType;
    @Excel(name = "变更字段") private String fieldName;
    @Excel(name = "原值") private String oldValue;
    @Excel(name = "新值") private String newValue;
    @Excel(name = "变更原因") private String changeReason;
    @Excel(name = "审批人") private String approver;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;
    @Excel(name = "状态") private String status;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long v) { this.changeId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String v) { this.changeType = v; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String v) { this.fieldName = v; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String v) { this.oldValue = v; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String v) { this.newValue = v; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String v) { this.changeReason = v; }
    public String getApprover() { return approver; }
    public void setApprover(String v) { this.approver = v; }
    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date v) { this.approveTime = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("changeId", getChangeId()).append("workorderId", getWorkorderId())
            .append("changeType", getChangeType()).append("status", getStatus()).toString();
    }
}
