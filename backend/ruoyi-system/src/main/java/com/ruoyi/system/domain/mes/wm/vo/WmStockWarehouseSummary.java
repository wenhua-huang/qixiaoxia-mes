package com.ruoyi.system.domain.mes.wm.vo;

import java.math.BigDecimal;

/**
 * 按 itemId 聚合各仓库可用量（quantity_available 求和），用于「从销售订单生成」时建议出库仓库与拆行。
 * FIFO 顺序由 SQL 的 ORDER BY MIN(create_time) ASC 保证（早入库的仓优先）。
 *
 * @author qixiaoxia
 * @date 2026-07-22
 */
public class WmStockWarehouseSummary
{
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private BigDecimal quantityAvailable;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public BigDecimal getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(BigDecimal v) { this.quantityAvailable = v; }
}
