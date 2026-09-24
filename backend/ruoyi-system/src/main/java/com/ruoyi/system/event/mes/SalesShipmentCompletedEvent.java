package com.ruoyi.system.event.mes;

/**
 * 出库单维度全部发齐事件：wm 域发布，sal 域订单维度复核后 CONFIRMED/PRODUCING→SHIPPED。
 * 非销售订单来源的出库单不发布。不可变。
 */
public class SalesShipmentCompletedEvent {
    private final Long salesId;
    private final Long salesOrderId;
    private final Long factoryId;
    public SalesShipmentCompletedEvent(Long salesId, Long salesOrderId, Long factoryId) {
        this.salesId = salesId; this.salesOrderId = salesOrderId; this.factoryId = factoryId;
    }
    public Long getSalesId() { return salesId; }
    public Long getSalesOrderId() { return salesOrderId; }
    public Long getFactoryId() { return factoryId; }
}
