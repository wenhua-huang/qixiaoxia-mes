package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_pro_material_trace 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProMaterialTrace extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long traceId;
    private Long factoryId;
    private String traceType;
    private String parentType;
    private Long parentId;
    private String childType;
    private Long childId;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private Long workorderId;
    private Long cardId;
    private Long cardProcessId;
    private Long issueId;
    private Long issueDetailId;
    private Long feedbackId;
    private Long transactionId;
    private Long processId;
    private Date traceTime;

    public Long getTraceId() { return traceId; }
    public void setTraceId(Long v) { this.traceId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getTraceType() { return traceType; }
    public void setTraceType(String v) { this.traceType = v; }
    public String getParentType() { return parentType; }
    public void setParentType(String v) { this.parentType = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
    public String getChildType() { return childType; }
    public void setChildType(String v) { this.childType = v; }
    public Long getChildId() { return childId; }
    public void setChildId(Long v) { this.childId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public Long getCardProcessId() { return cardProcessId; }
    public void setCardProcessId(Long v) { this.cardProcessId = v; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public Long getIssueDetailId() { return issueDetailId; }
    public void setIssueDetailId(Long v) { this.issueDetailId = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long v) { this.transactionId = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public Date getTraceTime() { return traceTime; }
    public void setTraceTime(Date v) { this.traceTime = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("traceId", getTraceId())
            .append("factoryId", getFactoryId())
            .append("traceType", getTraceType())
            .append("parentType", getParentType())
            .append("parentId", getParentId())
            .append("childType", getChildType())
            .append("childId", getChildId())
            .append("quantity", getQuantity()).toString();
    }
}
