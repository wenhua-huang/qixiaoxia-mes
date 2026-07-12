package com.ruoyi.system.domain.mes.wm.tx;

import java.math.BigDecimal;

/**
 * 产品入库事务Bean
 *
 * @author qixiaoxia
 * @date 2026-07-11
 */
public class ProductRecptTxBean implements TxBean {

    private String sourceDocType;
    private Long sourceDocId;
    private String sourceDocCode;
    private Long sourceDocLineId;

    private Long materialStockId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unitOfMeasure;
    private String unitName;

    private BigDecimal transactionQuantity;

    private Long batchId;
    private String batchCode;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long locationId;
    private Long areaId;

    // getters & setters
    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String v) { this.sourceDocType = v; }
    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long v) { this.sourceDocId = v; }
    public String getSourceDocCode() { return sourceDocCode; }
    public void setSourceDocCode(String v) { this.sourceDocCode = v; }
    public Long getSourceDocLineId() { return sourceDocLineId; }
    public void setSourceDocLineId(Long v) { this.sourceDocLineId = v; }
    public Long getMaterialStockId() { return materialStockId; }
    public void setMaterialStockId(Long v) { this.materialStockId = v; }
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
    public BigDecimal getTransactionQuantity() { return transactionQuantity; }
    public void setTransactionQuantity(BigDecimal v) { this.transactionQuantity = v; }
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
}
