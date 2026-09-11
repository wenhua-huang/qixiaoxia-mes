package com.ruoyi.system.domain.mes.sal.vo;

/**
 * 订单派生工单数聚合行（排除已取消工单，非持久化）
 *
 * @author qixiaoxia
 */
public class SalOrderWorkorderCountRow
{
    private Long orderId;

    private Integer workorderCount;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getWorkorderCount() { return workorderCount; }
    public void setWorkorderCount(Integer workorderCount) { this.workorderCount = workorderCount; }
}
