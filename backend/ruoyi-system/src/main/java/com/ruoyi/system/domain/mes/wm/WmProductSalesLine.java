package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售出库单行表对象 qxx_wm_product_sales_line
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmProductSalesLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long lineId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "出库单ID")
    private Long salesId;

    @Excel(name = "销售订单行ID")
    private Long salesOrderLineId;

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

    @Excel(name = "出库数量")
    private BigDecimal quantitySales;

    @Excel(name = "已过账出库量")
    private BigDecimal quantityPosted;

    @Excel(name = "出库箱数")
    private Long quantityBox;

    @Excel(name = "装箱规格")
    private String boxSpec;

    @Excel(name = "箱长")
    private BigDecimal boxLength;

    @Excel(name = "箱宽")
    private BigDecimal boxWidth;

    @Excel(name = "箱高")
    private BigDecimal boxHeight;

    @Excel(name = "体积")
    private BigDecimal volume;

    @Excel(name = "重量")
    private BigDecimal weight;

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

    @Excel(name = "本仓可用量")
    private BigDecimal availableQty;

    @Excel(name = "库区ID")
    private Long locationId;

    @Excel(name = "库位ID")
    private Long areaId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getSalesId() { return salesId; }
    public void setSalesId(Long v) { this.salesId = v; }

    public Long getSalesOrderLineId() { return salesOrderLineId; }
    public void setSalesOrderLineId(Long v) { this.salesOrderLineId = v; }

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

    public BigDecimal getQuantitySales() { return quantitySales; }
    public void setQuantitySales(BigDecimal v) { this.quantitySales = v; }

    public BigDecimal getQuantityPosted() { return quantityPosted; }
    public void setQuantityPosted(BigDecimal v) { this.quantityPosted = v; }

    public Long getQuantityBox() { return quantityBox; }
    public void setQuantityBox(Long v) { this.quantityBox = v; }

    public String getBoxSpec() { return boxSpec; }
    public void setBoxSpec(String v) { this.boxSpec = v; }

    public BigDecimal getBoxLength() { return boxLength; }
    public void setBoxLength(BigDecimal v) { this.boxLength = v; }

    public BigDecimal getBoxWidth() { return boxWidth; }
    public void setBoxWidth(BigDecimal v) { this.boxWidth = v; }

    public BigDecimal getBoxHeight() { return boxHeight; }
    public void setBoxHeight(BigDecimal v) { this.boxHeight = v; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal v) { this.volume = v; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal v) { this.weight = v; }

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

    public BigDecimal getAvailableQty() { return availableQty; }
    public void setAvailableQty(BigDecimal v) { this.availableQty = v; }

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