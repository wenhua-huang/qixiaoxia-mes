package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 通用外协订单头 qxx_wm_outsource_order
 *
 * 统一管理外协全生命周期（发料→厂商录结果→收货），分切/印刷等业务通过 sourceType 标识。
 * 关联子表：issueLines（发料行）、recptLines（收货行）。
 *
 * @author qixiaoxia
 */
public class WmOutsourceOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long factoryId;
    private String orderCode;

    // 关联
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private Long workorderId;
    private String workorderCode;
    private Long cardId;
    private Long routeId;
    private Long processId;
    private String processCode;
    private String processName;

    // 来源业务
    private String sourceType;
    private Long sourceRefId;

    // 状态
    private String status;
    private Long feedbackId;

    // 汇总
    private BigDecimal issueTotalQty;
    private BigDecimal recptTotalQty;

    // 操作
    private String operator;
    private Date issueTime;
    private Date receiveTime;

    /** 发料行（创建/详情时用） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<WmOutsourceIssueLine> issueLines;

    /** 收货行（录结果/收货/详情时用） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<WmOutsourceRecptLine> recptLines;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String v) { this.orderCode = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public Long getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(Long v) { this.sourceRefId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public BigDecimal getIssueTotalQty() { return issueTotalQty; }
    public void setIssueTotalQty(BigDecimal v) { this.issueTotalQty = v; }
    public BigDecimal getRecptTotalQty() { return recptTotalQty; }
    public void setRecptTotalQty(BigDecimal v) { this.recptTotalQty = v; }
    public String getOperator() { return operator; }
    public void setOperator(String v) { this.operator = v; }
    public Date getIssueTime() { return issueTime; }
    public void setIssueTime(Date v) { this.issueTime = v; }
    public Date getReceiveTime() { return receiveTime; }
    public void setReceiveTime(Date v) { this.receiveTime = v; }
    public List<WmOutsourceIssueLine> getIssueLines() { return issueLines; }
    public void setIssueLines(List<WmOutsourceIssueLine> v) { this.issueLines = v; }
    public List<WmOutsourceRecptLine> getRecptLines() { return recptLines; }
    public void setRecptLines(List<WmOutsourceRecptLine> v) { this.recptLines = v; }
}
