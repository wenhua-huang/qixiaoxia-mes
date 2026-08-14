package com.ruoyi.system.domain.mes.wm.vo;

import java.math.BigDecimal;

/**
 * 按 itemId 聚合各仓库可用量（quantity_available 求和），用于「从销售订单生成」时建议出库仓库与拆行。
 * FIFO 顺序由 SQL 的 ORDER BY MIN(create_time) ASC 保证（早入库的仓优先）。
 *
 * 支持多 itemId 候选查询（工单反查精确制导：一个销售行可能关联多张工单、产出多个变体），
 * itemId/itemCode/itemName 标识每条库存归属的具体物料（SPU 或变体 SKU）。
 *
 * @author qixiaoxia
 * @date 2026-07-22
 */
public class WmStockWarehouseSummary
{
    private Long itemId;
    private String itemCode;
    private String itemName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private BigDecimal quantityAvailable;

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public BigDecimal getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(BigDecimal v) { this.quantityAvailable = v; }
}
