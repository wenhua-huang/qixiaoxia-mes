package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;

/**
 * 外协分切收货请求 DTO（我方在厂商分切完成、子卷送回时确认收货）
 *
 * <p>收货动作：子卷 OUTSOURCED → IN_STOCK（加库存）、母卷 OUTSOURCED → CONSUMED、
 * 建报工 + SLIT 追溯、分切单 → RECEIVED。
 *
 * @author qixiaoxia
 * @date 2026-08-02
 */
public class OutsourceReceiveRequest
{
    /** 子卷入库仓库ID（可选，不填默认回母卷原仓库） */
    private Long receiveWarehouseId;
    private String receiveWarehouseCode;
    private String receiveWarehouseName;

    /** 纸边物料ID（可选，子卷送回时若有纸边一并入库） */
    private Long edgeItemId;
    private String edgeItemCode;
    private String edgeItemName;
    /** 纸边重量(kg) */
    private BigDecimal edgeWeight;

    public Long getReceiveWarehouseId() { return receiveWarehouseId; }
    public void setReceiveWarehouseId(Long v) { this.receiveWarehouseId = v; }
    public String getReceiveWarehouseCode() { return receiveWarehouseCode; }
    public void setReceiveWarehouseCode(String v) { this.receiveWarehouseCode = v; }
    public String getReceiveWarehouseName() { return receiveWarehouseName; }
    public void setReceiveWarehouseName(String v) { this.receiveWarehouseName = v; }
    public Long getEdgeItemId() { return edgeItemId; }
    public void setEdgeItemId(Long v) { this.edgeItemId = v; }
    public String getEdgeItemCode() { return edgeItemCode; }
    public void setEdgeItemCode(String v) { this.edgeItemCode = v; }
    public String getEdgeItemName() { return edgeItemName; }
    public void setEdgeItemName(String v) { this.edgeItemName = v; }
    public BigDecimal getEdgeWeight() { return edgeWeight; }
    public void setEdgeWeight(BigDecimal v) { this.edgeWeight = v; }
}
