package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料入库单行表对象 qxx_wm_item_recpt_line
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmItemRecptLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long lineId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "入库单ID")
    private Long recptId;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "规格型号")
    private String specification;

    @Excel(name = "主单位编码")
    private String unitOfMeasure;

    @Excel(name = "主单位名称")
    private String unitName;

    @Excel(name = "辅助单位编码")
    private String unit2;

    @Excel(name = "辅助单位名称")
    private String unit2Name;

    @Excel(name = "换算率")
    private BigDecimal conversionRate;

    @Excel(name = "入库数量")
    private BigDecimal quantityRecpt;

    @Excel(name = "入库数量")
    private BigDecimal quantityRecpt2;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期至")
    private Date expireDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "生产日期")
    private Date produceDate;

    @Excel(name = "生产批号")
    private String lotNumber;

    @Excel(name = "到货通知行ID")
    private Long noticeLineId;

    /** 采购订单行ID(回写退货量精确匹配) */
    private Long purOrderLineId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }

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

    public String getUnit2() { return unit2; }
    public void setUnit2(String v) { this.unit2 = v; }

    public String getUnit2Name() { return unit2Name; }
    public void setUnit2Name(String v) { this.unit2Name = v; }

    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal v) { this.conversionRate = v; }

    public BigDecimal getQuantityRecpt() { return quantityRecpt; }
    public void setQuantityRecpt(BigDecimal v) { this.quantityRecpt = v; }

    public BigDecimal getQuantityRecpt2() { return quantityRecpt2; }
    public void setQuantityRecpt2(BigDecimal v) { this.quantityRecpt2 = v; }

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

    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date v) { this.expireDate = v; }

    public Date getProduceDate() { return produceDate; }
    public void setProduceDate(Date v) { this.produceDate = v; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String v) { this.lotNumber = v; }

    public Long getNoticeLineId() { return noticeLineId; }
    public void setNoticeLineId(Long v) { this.noticeLineId = v; }

    public Long getPurOrderLineId() { return purOrderLineId; }
    public void setPurOrderLineId(Long v) { this.purOrderLineId = v; }

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