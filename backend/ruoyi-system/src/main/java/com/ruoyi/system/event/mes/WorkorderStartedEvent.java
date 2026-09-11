package com.ruoyi.system.event.mes;

/**
 * 工单开工事件：pro 域发布，sal 域监听后把销售订单 CONFIRMED→PRODUCING。
 * 非销售订单来源的工单不发布。不可变。
 */
public class WorkorderStartedEvent {
    private final Long workorderId;
    private final Long salesOrderLineId;
    private final Long factoryId;
    public WorkorderStartedEvent(Long workorderId, Long salesOrderLineId, Long factoryId) {
        this.workorderId = workorderId;
        this.salesOrderLineId = salesOrderLineId;
        this.factoryId = factoryId;
    }
    public Long getWorkorderId() { return workorderId; }
    public Long getSalesOrderLineId() { return salesOrderLineId; }
    public Long getFactoryId() { return factoryId; }
}
