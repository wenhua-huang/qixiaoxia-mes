package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 采购退货单行表对象 qxx_wm_rt_vendor_line
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmRtVendorLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long lineId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "退货单ID")
    private Long rtId;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "规格型号")
    private String specification;

    @Excel(name = "单位编码")
    private String unitOfMeasure;

    @Excel(name = "单位名称")
    private String unitName;

    @Excel(name = "退货数量")
    private BigDecimal quantityRt;

    @Excel(name = "批次ID")
    private Long batchId;

    @Excel(name = "批次编码")
    private String batchCode;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "库区ID")
    private Long locationId;

    @Excel(name = "库位ID")
    private Long areaId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getRtId() { return rtId; }
    public void setRtId(Long v) { this.rtId = v; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }

    public BigDecimal getQuantityRt() { return quantityRt; }
    public void setQuantityRt(BigDecimal v) { this.quantityRt = v; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long v) { this.areaId = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("lineId", getLineId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("unitName", getUnitName())
            .toString();
    }
}