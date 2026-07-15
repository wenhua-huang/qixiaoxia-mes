package com.ruoyi.system.domain.mes.sal;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售订单明细行对象 qxx_sal_order_line
 * 用途:一单多产品,每行=一个产品(转工单到行级)。规格字段对齐工单便于1:1回填。
 * 已转量/可转量查询时按 sales_order_line_id 聚合 qxx_pro_workorder 计算,不存计数列。
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public class SalOrderLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long lineId;
    private Long factoryId;
    private Long orderId;

    private Integer lineNo;
    private Long productId;
    private String productCode;

    @Excel(name = "产品名称")
    private String productName;

    private String productSpc;
    private String unitOfMeasure;
    private String unitName;

    @Excel(name = "订单数量")
    private BigDecimal quantity;

    @Excel(name = "单价")
    private BigDecimal unitPrice;

    @Excel(name = "行金额")
    private BigDecimal lineAmount;

    private String spacing;
    private String productSize;
    private String printingReq;
    private String ropeSpec;
    private String packageReq;
    private String shippingReq;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date requestDate;

    /** 已转工单数量(查询时聚合 qxx_pro_workorder,非DB字段) */
    private transient BigDecimal quantityProduced;

    /** 可转数量 = 订单数量 - 已转工单数量(非DB字段) */
    private transient BigDecimal quantityConvertible;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer v) { this.lineNo = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductSpc() { return productSpc; }
    public void setProductSpc(String v) { this.productSpc = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    public BigDecimal getLineAmount() { return lineAmount; }
    public void setLineAmount(BigDecimal v) { this.lineAmount = v; }
    public String getSpacing() { return spacing; }
    public void setSpacing(String v) { this.spacing = v; }
    public String getProductSize() { return productSize; }
    public void setProductSize(String v) { this.productSize = v; }
    public String getPrintingReq() { return printingReq; }
    public void setPrintingReq(String v) { this.printingReq = v; }
    public String getRopeSpec() { return ropeSpec; }
    public void setRopeSpec(String v) { this.ropeSpec = v; }
    public String getPackageReq() { return packageReq; }
    public void setPackageReq(String v) { this.packageReq = v; }
    public String getShippingReq() { return shippingReq; }
    public void setShippingReq(String v) { this.shippingReq = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
    public BigDecimal getQuantityConvertible() { return quantityConvertible; }
    public void setQuantityConvertible(BigDecimal v) { this.quantityConvertible = v; }
}
