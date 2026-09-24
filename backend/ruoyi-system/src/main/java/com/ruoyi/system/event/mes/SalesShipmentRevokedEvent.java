package com.ruoyi.system.event.mes;

/**
 * 发运单冲销（删除 IN_TRANSIT 发运单、箱回滚 PACKED）事件：wm 域发布，
 * sal 域订单维度复核箱量，已不发齐时 SHIPPED→PRODUCING/CONFIRMED 对称降级。
 * 非销售订单来源的出库单不发布。不可变。
 */
public class SalesShipmentRevokedEvent {
    private final Long salesId;
    private final Long salesOrderId;
    private final Long factoryId;
    public SalesShipmentRevokedEvent(Long salesId, Long salesOrderId, Long factoryId) {
        this.salesId = salesId; this.salesOrderId = salesOrderId; this.factoryId = factoryId;
    }
    public Long getSalesId() { return salesId; }
    public Long getSalesOrderId() { return salesOrderId; }
    public Long getFactoryId() { return factoryId; }
}
