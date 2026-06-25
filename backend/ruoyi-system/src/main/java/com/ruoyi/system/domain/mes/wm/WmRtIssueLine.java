package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_wm_rt_issue_line 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class WmRtIssueLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long lineId;
    private Long factoryId;
    private Long rtId;
    private String rtCode;
    private Long issueId;
    private Long issueLineId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemSpc;
    private String unitOfMeasure;
    private String unitName;
    private BigDecimal quantityRt;
    private BigDecimal quantityRted;
    private Long batchId;
    private String batchCode;
    private Long warehouseId;
    private Long locationId;
    private Long areaId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getRtId() { return rtId; }
    public void setRtId(Long v) { this.rtId = v; }
    public String getRtCode() { return rtCode; }
    public void setRtCode(String v) { this.rtCode = v; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public Long getIssueLineId() { return issueLineId; }
    public void setIssueLineId(Long v) { this.issueLineId = v; }
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
    public BigDecimal getQuantityRt() { return quantityRt; }
    public void setQuantityRt(BigDecimal v) { this.quantityRt = v; }
    public BigDecimal getQuantityRted() { return quantityRted; }
    public void setQuantityRted(BigDecimal v) { this.quantityRted = v; }
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
            .append("rtId", getRtId())
            .append("rtCode", getRtCode())
            .append("issueId", getIssueId())
            .append("issueLineId", getIssueLineId())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode()).toString();
    }
}
