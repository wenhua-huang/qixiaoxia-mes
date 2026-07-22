package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 生产工单对象 qxx_pro_workorder
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public class ProWorkorder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long workorderId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "工单编码") private String workorderCode;
    @Excel(name = "工单名称") private String workorderName;
    @Excel(name = "工单类型") private String workorderType;
    @Excel(name = "来源类型") private String orderSource;
    @Excel(name = "来源单据编码") private String sourceCode;
    @Excel(name = "销售订单行ID") private Long salesOrderLineId;
    @Excel(name = "产品ID") private Long productId;
    @Excel(name = "产品编码") private String productCode;
    @Excel(name = "产品名称") private String productName;
    @Excel(name = "产品规格") private String productSpc;
    @Excel(name = "路线产品ID") private Long routeProductId;
    @Excel(name = "主单位编码") private String unitOfMeasure;
    @Excel(name = "主单位名称") private String unitName;
    @Excel(name = "计划生产数量") private BigDecimal quantity;
    @Excel(name = "已生产数量") private BigDecimal quantityProduced;
    @Excel(name = "调整数量") private BigDecimal quantityChanged;
    @Excel(name = "已排产数量") private BigDecimal quantityScheduled;
    @Excel(name = "客户ID") private Long clientId;
    @Excel(name = "客户编码") private String clientCode;
    @Excel(name = "客户名称") private String clientName;
    @Excel(name = "客户订单号") private String clientOrderCode;
    @Excel(name = "产品尺寸") private String productSize;
    @Excel(name = "印刷要求") private String printingReq;
    @Excel(name = "绳料规格") private String ropeSpec;
    @Excel(name = "包装要求") private String packageReq;
    @Excel(name = "发货要求") private String shippingReq;
    @Excel(name = "订单类型") private String orderType;
    @Excel(name = "外协供应商ID") private Long vendorId;
    @Excel(name = "外协供应商编码") private String vendorCode;
    @Excel(name = "外协供应商名称") private String vendorName;
    @Excel(name = "外协工厂ID") private Long outsourceFactoryId;
    @Excel(name = "批次号") private String batchCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "需求日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date requestDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishDate;
    @Excel(name = "工单状态") private String status;

    /** SKU变体创建标志（非DB字段，仅API传输用） */
    private Boolean createSkuVariant;
    /** SKU变体编码（createSkuVariant=true时必填） */
    private String skuCode;
    /** SKU变体名称（createSkuVariant=true时必填） */
    private String skuName;

    /** 状态多值查询（非DB字段，status IN 过滤用） */
    private List<String> statusList;
    /** 已生产量下限（非DB字段，quantity_produced >= 过滤用） */
    private BigDecimal quantityProducedMin;
    /** 已入库量（非DB字段，由 Mapper 子查询填充，前端可读） */
    private BigDecimal quantityRecpt;

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public String getWorkorderType() { return workorderType; }
    public void setWorkorderType(String v) { this.workorderType = v; }
    public String getOrderSource() { return orderSource; }
    public void setOrderSource(String v) { this.orderSource = v; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String v) { this.sourceCode = v; }
    public Long getSalesOrderLineId() { return salesOrderLineId; }
    public void setSalesOrderLineId(Long v) { this.salesOrderLineId = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductSpc() { return productSpc; }
    public void setProductSpc(String v) { this.productSpc = v; }
    public Long getRouteProductId() { return routeProductId; }
    public void setRouteProductId(Long v) { this.routeProductId = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
    public BigDecimal getQuantityChanged() { return quantityChanged; }
    public void setQuantityChanged(BigDecimal v) { this.quantityChanged = v; }
    public BigDecimal getQuantityScheduled() { return quantityScheduled; }
    public void setQuantityScheduled(BigDecimal v) { this.quantityScheduled = v; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { this.clientCode = v; }
    public String getClientName() { return clientName; }
    public void setClientName(String v) { this.clientName = v; }
    public String getClientOrderCode() { return clientOrderCode; }
    public void setClientOrderCode(String v) { this.clientOrderCode = v; }
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
    public String getOrderType() { return orderType; }
    public void setOrderType(String v) { this.orderType = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public Long getOutsourceFactoryId() { return outsourceFactoryId; }
    public void setOutsourceFactoryId(Long v) { this.outsourceFactoryId = v; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public Date getCancelDate() { return cancelDate; }
    public void setCancelDate(Date v) { this.cancelDate = v; }
    public Date getFinishDate() { return finishDate; }
    public void setFinishDate(Date v) { this.finishDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Boolean getCreateSkuVariant() { return createSkuVariant; }
    public void setCreateSkuVariant(Boolean v) { this.createSkuVariant = v; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String v) { this.skuCode = v; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String v) { this.skuName = v; }
    public List<String> getStatusList() { return statusList; }
    public void setStatusList(List<String> v) { this.statusList = v; }
    public BigDecimal getQuantityProducedMin() { return quantityProducedMin; }
    public void setQuantityProducedMin(BigDecimal v) { this.quantityProducedMin = v; }
    public BigDecimal getQuantityRecpt() { return quantityRecpt; }
    public void setQuantityRecpt(BigDecimal v) { this.quantityRecpt = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("workorderId", getWorkorderId()).append("workorderCode", getWorkorderCode())
            .append("workorderName", getWorkorderName()).append("productId", getProductId())
            .append("quantity", getQuantity()).append("status", getStatus())
            .append("remark", getRemark()).append("createBy", getCreateBy())
            .append("createTime", getCreateTime()).append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime()).toString();
    }
}
