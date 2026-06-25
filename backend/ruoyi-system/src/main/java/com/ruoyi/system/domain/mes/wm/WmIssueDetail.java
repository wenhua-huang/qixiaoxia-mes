package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_wm_issue_detail 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class WmIssueDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long detailId;
    private Long factoryId;
    private Long lineId;
    private Long issueId;
    private String issueCode;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private Long batchId;
    private String batchCode;
    private Long warehouseId;
    private Long locationId;
    private Long areaId;
    private Long materialStockId;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private String unitName;
    private String unit2;
    private String unit2Name;
    private BigDecimal conversionRate;
    private BigDecimal quantity2;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long v) { this.detailId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String v) { this.issueCode = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long v) { this.areaId = v; }
    public Long getMaterialStockId() { return materialStockId; }
    public void setMaterialStockId(Long v) { this.materialStockId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
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
    public BigDecimal getQuantity2() { return quantity2; }
    public void setQuantity2(BigDecimal v) { this.quantity2 = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("detailId", getDetailId())
            .append("factoryId", getFactoryId())
            .append("lineId", getLineId())
            .append("issueId", getIssueId())
            .append("issueCode", getIssueCode())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName()).toString();
    }
}
