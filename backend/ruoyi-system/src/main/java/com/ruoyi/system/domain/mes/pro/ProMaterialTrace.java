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
    private String unitName;
    private Long workorderId;
    private Long cardId;
    private Long vendorId;
    private Long cardProcessId;
    private Long issueId;
    private Long issueDetailId;
    private Long feedbackId;
    private Long transactionId;
    private Long processId;
    private Date traceTime;

    // ── 展示字段（transient，不入库，由 Mapper JOIN 拼出）──
    /** 源描述，如"采购单 PO-2026-001 · 德欣纸业" */
    private String parentDesc;
    /** 目标描述，如"原料仓 · 箱板纸A级" */
    private String childDesc;
    /** 物料名称（主轴展示用，优先 child 侧回退 parent 侧） */
    private String itemName;
    /** 物料编码 */
    private String itemCode;
    /** 批次号 */
    private String batchCode;

    // ── 查询入参字段（transient，仅用于列表 WHERE 过滤，不入库、不输出）──
    /** 按工单编码模糊查（跨 parent/child 两侧） */
    private String workorderCode;
    /** 按流转卡编码模糊查 */
    private String cardCode;
    /** 按物料名称模糊查 */
    private String queryItemName;
    /** 按物料编码模糊查 */
    private String queryItemCode;
    /** 按批次号模糊查 */
    private String queryBatchCode;

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
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
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
    public String getParentDesc() { return parentDesc; }
    public void setParentDesc(String v) { this.parentDesc = v; }
    public String getChildDesc() { return childDesc; }
    public void setChildDesc(String v) { this.childDesc = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getCardCode() { return cardCode; }
    public void setCardCode(String v) { this.cardCode = v; }
    public String getQueryItemName() { return queryItemName; }
    public void setQueryItemName(String v) { this.queryItemName = v; }
    public String getQueryItemCode() { return queryItemCode; }
    public void setQueryItemCode(String v) { this.queryItemCode = v; }
    public String getQueryBatchCode() { return queryBatchCode; }
    public void setQueryBatchCode(String v) { this.queryBatchCode = v; }

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
