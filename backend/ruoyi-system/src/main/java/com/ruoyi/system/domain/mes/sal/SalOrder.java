package com.ruoyi.system.domain.mes.sal;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售订单对象 qxx_sal_order
 * 用途:承接客户订单(客户->圣享),记产品/数量/交期/商务条款,转工单的上游
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public class SalOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long factoryId;

    @Excel(name = "销售订单号")
    private String orderCode;

    @Excel(name = "订单名称")
    private String orderName;

    @Excel(name = "订单类型", readConverterExp = "NEW=新单,REPEAT=返单")
    private String orderType;

    @Excel(name = "客户编码")
    private String clientCode;

    @Excel(name = "客户名称")
    private String clientName;

    private Long clientId;
    private String clientNick;

    @Excel(name = "客户PO号")
    private String clientOrderCode;

    @Excel(name = "业务员")
    private String salesperson;

    @Excel(name = "业务线", readConverterExp = "DOMESTIC=内贸,FOREIGN=外贸,SPOT=现货")
    private String businessLine;

    @Excel(name = "是否有样品", readConverterExp = "Y=是,N=否")
    private String sampleFlag;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "订单日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date orderDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "需求交期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date requestDate;

    @Excel(name = "订单总金额")
    private BigDecimal totalAmount;

    @Excel(name = "付款方式")
    private String paymentMethod;

    @Excel(name = "状态", readConverterExp = "PREPARE=待确认,CONFIRMED=已确认,CLOSED=已关闭,CANCEL=已取消")
    private String status;

    /** 明细行列表(详情接口返回时填充,非DB字段) */
    private transient List<SalOrderLine> lines;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String v) { this.orderCode = v; }
    public String getOrderName() { return orderName; }
    public void setOrderName(String v) { this.orderName = v; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String v) { this.orderType = v; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { this.clientCode = v; }
    public String getClientName() { return clientName; }
    public void setClientName(String v) { this.clientName = v; }
    public String getClientNick() { return clientNick; }
    public void setClientNick(String v) { this.clientNick = v; }
    public String getClientOrderCode() { return clientOrderCode; }
    public void setClientOrderCode(String v) { this.clientOrderCode = v; }
    public String getSalesperson() { return salesperson; }
    public void setSalesperson(String v) { this.salesperson = v; }
    public String getBusinessLine() { return businessLine; }
    public void setBusinessLine(String v) { this.businessLine = v; }
    public String getSampleFlag() { return sampleFlag; }
    public void setSampleFlag(String v) { this.sampleFlag = v; }
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date v) { this.orderDate = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public List<SalOrderLine> getLines() { return lines; }
    public void setLines(List<SalOrderLine> lines) { this.lines = lines; }
}
