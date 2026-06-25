package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_wm_issue_line 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class WmIssueLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long lineId;
    private Long factoryId;
    private Long issueId;
    private String issueCode;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemSpc;
    private String unitOfMeasure;
    private String unitName;
    private BigDecimal quantityIssue;
    private BigDecimal quantityIssued;
    private String unit2;
    private String unit2Name;
    private BigDecimal conversionRate;
    private BigDecimal quantityIssue2;
    private BigDecimal quantityIssued2;
    private Long processId;
    private String processCode;
    private String processName;
    private Long batchId;
    private String batchCode;
    private Long warehouseId;
    private Long locationId;
    private Long areaId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
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
    public String getItemSpc() { return itemSpc; }
    public void setItemSpc(String v) { this.itemSpc = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public BigDecimal getQuantityIssue() { return quantityIssue; }
    public void setQuantityIssue(BigDecimal v) { this.quantityIssue = v; }
    public BigDecimal getQuantityIssued() { return quantityIssued; }
    public void setQuantityIssued(BigDecimal v) { this.quantityIssued = v; }
    public String getUnit2() { return unit2; }
    public void setUnit2(String v) { this.unit2 = v; }
    public String getUnit2Name() { return unit2Name; }
    public void setUnit2Name(String v) { this.unit2Name = v; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal v) { this.conversionRate = v; }
    public BigDecimal getQuantityIssue2() { return quantityIssue2; }
    public void setQuantityIssue2(BigDecimal v) { this.quantityIssue2 = v; }
    public BigDecimal getQuantityIssued2() { return quantityIssued2; }
    public void setQuantityIssued2(BigDecimal v) { this.quantityIssued2 = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("lineId", getLineId())
            .append("factoryId", getFactoryId())
            .append("issueId", getIssueId())
            .append("issueCode", getIssueCode())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("itemSpc", getItemSpc()).toString();
    }
}
