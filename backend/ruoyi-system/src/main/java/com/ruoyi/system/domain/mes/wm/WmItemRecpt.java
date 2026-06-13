package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料入库单表对象 qxx_wm_item_recpt
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmItemRecpt extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long recptId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "入库单编码")
    private String recptCode;

    @Excel(name = "入库单名称")
    private String recptName;

    @Excel(name = "采购订单ID")
    private Long purOrderId;

    @Excel(name = "采购订单编码")
    private String purOrderCode;

    @Excel(name = "供应商ID")
    private Long vendorId;

    @Excel(name = "供应商编码")
    private String vendorCode;

    @Excel(name = "供应商名称")
    private String vendorName;

    @Excel(name = "入库仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "入库日期")
    private Date recptDate;

    @Excel(name = "入库类型:PURCHASE-采购入库")
    private String recptType;

    @Excel(name = "入库总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "质检单ID")
    private Long iqcId;

    @Excel(name = "质检单编码")
    private String iqcCode;

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getRecptCode() { return recptCode; }
    public void setRecptCode(String v) { this.recptCode = v; }

    public String getRecptName() { return recptName; }
    public void setRecptName(String v) { this.recptName = v; }

    public Long getPurOrderId() { return purOrderId; }
    public void setPurOrderId(Long v) { this.purOrderId = v; }

    public String getPurOrderCode() { return purOrderCode; }
    public void setPurOrderCode(String v) { this.purOrderCode = v; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getRecptDate() { return recptDate; }
    public void setRecptDate(Date v) { this.recptDate = v; }

    public String getRecptType() { return recptType; }
    public void setRecptType(String v) { this.recptType = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public Long getIqcId() { return iqcId; }
    public void setIqcId(Long v) { this.iqcId = v; }

    public String getIqcCode() { return iqcCode; }
    public void setIqcCode(String v) { this.iqcCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recptId", getRecptId())
            .append("recptCode", getRecptCode())
            .append("recptName", getRecptName())
            .append("purOrderCode", getPurOrderCode())
            .toString();
    }
}