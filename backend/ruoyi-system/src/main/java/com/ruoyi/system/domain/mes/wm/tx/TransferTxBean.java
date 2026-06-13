package com.ruoyi.system.domain.mes.wm.tx;

import java.math.BigDecimal;

/**
 * 调拨转移事务Bean（源仓库出库 + 目标仓库入库）
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class TransferTxBean {
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
    // 来源仓库
    private Long fromWarehouseId;
    private String fromWarehouseCode;
    private String fromWarehouseName;
    private Long fromLocationId;
    private Long fromAreaId;
    // 目标仓库
    private Long toWarehouseId;
    private String toWarehouseCode;
    private String toWarehouseName;
    private Long toLocationId;
    private Long toAreaId;
    // 供应商（调拨供应商库存时必须传入，否则默认 0）
    private Long vendorId;

    public String getSourceDocType() { return sourceDocType; } public void setSourceDocType(String v) { this.sourceDocType = v; }
    public Long getSourceDocId() { return sourceDocId; } public void setSourceDocId(Long v) { this.sourceDocId = v; }
    public String getSourceDocCode() { return sourceDocCode; } public void setSourceDocCode(String v) { this.sourceDocCode = v; }
    public Long getSourceDocLineId() { return sourceDocLineId; } public void setSourceDocLineId(Long v) { this.sourceDocLineId = v; }
    public Long getMaterialStockId() { return materialStockId; } public void setMaterialStockId(Long v) { this.materialStockId = v; }
    public Long getItemId() { return itemId; } public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; } public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; } public void setItemName(String v) { this.itemName = v; }
    public String getSpecification() { return specification; } public void setSpecification(String v) { this.specification = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; } public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; } public void setUnitName(String v) { this.unitName = v; }
    public BigDecimal getTransactionQuantity() { return transactionQuantity; } public void setTransactionQuantity(BigDecimal v) { this.transactionQuantity = v; }
    public Long getBatchId() { return batchId; } public void setBatchId(Long v) { this.batchId = v; }
    public String getBatchCode() { return batchCode; } public void setBatchCode(String v) { this.batchCode = v; }
    public Long getFromWarehouseId() { return fromWarehouseId; } public void setFromWarehouseId(Long v) { this.fromWarehouseId = v; }
    public String getFromWarehouseCode() { return fromWarehouseCode; } public void setFromWarehouseCode(String v) { this.fromWarehouseCode = v; }
    public String getFromWarehouseName() { return fromWarehouseName; } public void setFromWarehouseName(String v) { this.fromWarehouseName = v; }
    public Long getFromLocationId() { return fromLocationId; } public void setFromLocationId(Long v) { this.fromLocationId = v; }
    public Long getFromAreaId() { return fromAreaId; } public void setFromAreaId(Long v) { this.fromAreaId = v; }
    public Long getToWarehouseId() { return toWarehouseId; } public void setToWarehouseId(Long v) { this.toWarehouseId = v; }
    public String getToWarehouseCode() { return toWarehouseCode; } public void setToWarehouseCode(String v) { this.toWarehouseCode = v; }
    public String getToWarehouseName() { return toWarehouseName; } public void setToWarehouseName(String v) { this.toWarehouseName = v; }
    public Long getToLocationId() { return toLocationId; } public void setToLocationId(Long v) { this.toLocationId = v; }
    public Long getToAreaId() { return toAreaId; } public void setToAreaId(Long v) { this.toAreaId = v; }
    public Long getVendorId() { return vendorId; } public void setVendorId(Long v) { this.vendorId = v; }
}
