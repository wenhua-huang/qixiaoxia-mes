package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 外协发料行 qxx_wm_outsource_issue_line
 * @author qixiaoxia
 */
public class WmOutsourceIssueLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long lineId;
    private Long factoryId;
    private Long orderId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unitOfMeasure;
    private String unitName;
    private BigDecimal quantity;
    private Long batchId;
    private String batchCode;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    /** 来源对象类型：ROLL-纸卷, STOCK-库存记录 */
    private String sourceRefType;
    /** 来源对象ID（如 roll_id） */
    private Long sourceRefId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }
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
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
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
    public String getSourceRefType() { return sourceRefType; }
    public void setSourceRefType(String v) { this.sourceRefType = v; }
    public Long getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(Long v) { this.sourceRefId = v; }
}
