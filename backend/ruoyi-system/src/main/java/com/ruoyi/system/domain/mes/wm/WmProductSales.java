package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售出库单表对象 qxx_wm_product_sales
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmProductSales extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long salesId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "出库单编码")
    private String salesCode;

    @Excel(name = "出库单名称")
    private String salesName;

    @Excel(name = "客户ID")
    private Long clientId;

    @Excel(name = "客户编码")
    private String clientCode;

    @Excel(name = "客户名称")
    private String clientName;

    @Excel(name = "客户订单号")
    private String clientOrderCode;

    @Excel(name = "业务员")
    private String salesperson;

    @Excel(name = "出货仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "出库日期")
    private Date salesDate;

    @Excel(name = "出库总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "总箱数")
    private Long totalBox;

    @Excel(name = "总体积")
    private BigDecimal totalVolume;

    @Excel(name = "总重量")
    private BigDecimal totalWeight;

    @Excel(name = "物流公司名称")
    private String logisticsCompany;

    @Excel(name = "快递/物流单号")
    private String trackingNo;

    @Excel(name = "物流费用")
    private BigDecimal logisticsFee;

    @Excel(name = "收货详细地址")
    private String shippingAddress;

    @Excel(name = "收货人姓名")
    private String receiverName;

    @Excel(name = "收货人电话")
    private String receiverTel;

    @Excel(name = "是否打托盘")
    private String palletFlag;

    @Excel(name = "箱唛/唛头描述")
    private String boxLabel;

    @Excel(name = "销售类型:FOREIGN-外贸")
    private String salesType;

    @Excel(name = "出货检验单ID")
    private Long oqcId;

    @Excel(name = "出货检验单编码")
    private String oqcCode;

    @Excel(name = "销售订单ID")
    private Long salesOrderId;

    @Excel(name = "销售订单编码")
    private String salesOrderCode;

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

    @Excel(name = "已过账出库量")
    private BigDecimal postedQuantity;

    /** 行列表（详情接口聚合，非DB字段） */
    private java.util.List<WmProductSalesLine> lines;

    /** 明细列表（详情接口聚合，非DB字段） */
    private java.util.List<WmProductSalesDetail> details;

    public Long getSalesId() { return salesId; }
    public void setSalesId(Long v) { this.salesId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getSalesCode() { return salesCode; }
    public void setSalesCode(String v) { this.salesCode = v; }

    public String getSalesName() { return salesName; }
    public void setSalesName(String v) { this.salesName = v; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }

    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { this.clientCode = v; }

    public String getClientName() { return clientName; }
    public void setClientName(String v) { this.clientName = v; }

    public String getClientOrderCode() { return clientOrderCode; }
    public void setClientOrderCode(String v) { this.clientOrderCode = v; }

    public String getSalesperson() { return salesperson; }
    public void setSalesperson(String v) { this.salesperson = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getSalesDate() { return salesDate; }
    public void setSalesDate(Date v) { this.salesDate = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public Long getTotalBox() { return totalBox; }
    public void setTotalBox(Long v) { this.totalBox = v; }

    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal v) { this.totalVolume = v; }

    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal v) { this.totalWeight = v; }

    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String v) { this.logisticsCompany = v; }

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String v) { this.trackingNo = v; }

    public BigDecimal getLogisticsFee() { return logisticsFee; }
    public void setLogisticsFee(BigDecimal v) { this.logisticsFee = v; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String v) { this.shippingAddress = v; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String v) { this.receiverName = v; }

    public String getReceiverTel() { return receiverTel; }
    public void setReceiverTel(String v) { this.receiverTel = v; }

    public String getPalletFlag() { return palletFlag; }
    public void setPalletFlag(String v) { this.palletFlag = v; }

    public String getBoxLabel() { return boxLabel; }
    public void setBoxLabel(String v) { this.boxLabel = v; }

    public String getSalesType() { return salesType; }
    public void setSalesType(String v) { this.salesType = v; }

    public Long getOqcId() { return oqcId; }
    public void setOqcId(Long v) { this.oqcId = v; }

    public String getOqcCode() { return oqcCode; }
    public void setOqcCode(String v) { this.oqcCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Long getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(Long v) { this.salesOrderId = v; }

    public String getSalesOrderCode() { return salesOrderCode; }
    public void setSalesOrderCode(String v) { this.salesOrderCode = v; }

    public BigDecimal getPostedQuantity() { return postedQuantity; }
    public void setPostedQuantity(BigDecimal v) { this.postedQuantity = v; }

    public java.util.List<WmProductSalesLine> getLines() { return lines; }
    public void setLines(java.util.List<WmProductSalesLine> v) { this.lines = v; }

    public java.util.List<WmProductSalesDetail> getDetails() { return details; }
    public void setDetails(java.util.List<WmProductSalesDetail> v) { this.details = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("salesId", getSalesId())
            .append("salesCode", getSalesCode())
            .append("salesName", getSalesName())
            .append("clientCode", getClientCode())
            .toString();
    }
}