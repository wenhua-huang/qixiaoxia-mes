package com.ruoyi.system.domain.mes.wm.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 可退批次聚合 VO（从采购订单生成退货单向导用）
 * 按 (item_id, warehouse_id, batch_id) 聚合某 PO 的已收货入库，
 * 已退量从 wm_transaction(ITEM_RTV) 按同维度反查。
 *
 * @author qixiaoxia
 * @date 2026-07-18
 */
public class ReturnableBatchVO
{
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unitOfMeasure;
    private String unitName;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Long batchId;
    private String batchCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expireDate;

    /** 来源入库单 */
    private Long recptId;
    private String recptCode;

    /** 采购订单行ID(精确回写退货量,可空-历史数据) */
    private Long purOrderLineId;

    /** 总入库量(聚合) */
    private BigDecimal quantityRecptTotal;

    /** 已退量(从 transaction 反查) */
    private BigDecimal quantityReturned;

    /** 可退量 = quantityRecptTotal - quantityReturned */
    private BigDecimal quantityReturnable;

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
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }
    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date v) { this.expireDate = v; }
    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }
    public String getRecptCode() { return recptCode; }
    public void setRecptCode(String v) { this.recptCode = v; }
    public Long getPurOrderLineId() { return purOrderLineId; }
    public void setPurOrderLineId(Long v) { this.purOrderLineId = v; }
    public BigDecimal getQuantityRecptTotal() { return quantityRecptTotal; }
    public void setQuantityRecptTotal(BigDecimal v) { this.quantityRecptTotal = v; }
    public BigDecimal getQuantityReturned() { return quantityReturned; }
    public void setQuantityReturned(BigDecimal v) { this.quantityReturned = v; }
    public BigDecimal getQuantityReturnable() { return quantityReturnable; }
    public void setQuantityReturnable(BigDecimal v) { this.quantityReturnable = v; }
}
