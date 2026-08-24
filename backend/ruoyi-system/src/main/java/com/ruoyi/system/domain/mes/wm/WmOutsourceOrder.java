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
    /** 外协场景：供应商对应系统工厂ID（外协 8 表冗余字段，便于跨工厂查询） */
    private Long outsourceFactoryId;
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
    /** 多状态查询（IN 条件），与 status 互斥使用；非持久字段 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> statusList;
    private Long feedbackId;
    /** 首条来料检验单ID（source_doc_type=wm_outsource_order），多物料多张单时仅首张回填 */
    private Long iqcId;
    private String iqcCode;

    // 汇总
    private BigDecimal issueTotalQty;
    private BigDecimal recptTotalQty;

    // 操作
    private String operator;
    private Date issueTime;
    private Date vendorReceiveTime;
    private String vendorReceiver;
    private Date finishTime;
    private String finishBy;
    private Date shipTime;
    private String shipBy;
    private Date receiveTime;

    /** 发料行（创建/详情时用） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<WmOutsourceIssueLine> issueLines;

    /** 收货行（录结果/收货/详情时用） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<WmOutsourceRecptLine> recptLines;

    /** 来料检验状态汇总（列表计算列：PASSED/CONCESSION/PENDING/FAILED/NONE），非持久字段 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String qcStatus;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getOutsourceFactoryId() { return outsourceFactoryId; }
    public void setOutsourceFactoryId(Long v) { this.outsourceFactoryId = v; }
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
    public List<String> getStatusList() { return statusList; }
    public void setStatusList(List<String> v) { this.statusList = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public Long getIqcId() { return iqcId; }
    public void setIqcId(Long v) { this.iqcId = v; }
    public String getIqcCode() { return iqcCode; }
    public void setIqcCode(String v) { this.iqcCode = v; }
    public BigDecimal getIssueTotalQty() { return issueTotalQty; }
    public void setIssueTotalQty(BigDecimal v) { this.issueTotalQty = v; }
    public BigDecimal getRecptTotalQty() { return recptTotalQty; }
    public void setRecptTotalQty(BigDecimal v) { this.recptTotalQty = v; }
    public String getOperator() { return operator; }
    public void setOperator(String v) { this.operator = v; }
    public Date getIssueTime() { return issueTime; }
    public void setIssueTime(Date v) { this.issueTime = v; }
    public Date getVendorReceiveTime() { return vendorReceiveTime; }
    public void setVendorReceiveTime(Date v) { this.vendorReceiveTime = v; }
    public String getVendorReceiver() { return vendorReceiver; }
    public void setVendorReceiver(String v) { this.vendorReceiver = v; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date v) { this.finishTime = v; }
    public String getFinishBy() { return finishBy; }
    public void setFinishBy(String v) { this.finishBy = v; }
    public Date getShipTime() { return shipTime; }
    public void setShipTime(Date v) { this.shipTime = v; }
    public String getShipBy() { return shipBy; }
    public void setShipBy(String v) { this.shipBy = v; }
    public Date getReceiveTime() { return receiveTime; }
    public void setReceiveTime(Date v) { this.receiveTime = v; }
    public List<WmOutsourceIssueLine> getIssueLines() { return issueLines; }
    public void setIssueLines(List<WmOutsourceIssueLine> v) { this.issueLines = v; }
    public List<WmOutsourceRecptLine> getRecptLines() { return recptLines; }
    public void setRecptLines(List<WmOutsourceRecptLine> v) { this.recptLines = v; }
    public String getQcStatus() { return qcStatus; }
    public void setQcStatus(String v) { this.qcStatus = v; }
}
