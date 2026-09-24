package com.ruoyi.system.domain.mes.sal.vo;

/**
 * 订单生产进度聚合行（任务口径实时计算，非持久化）
 *
 * @author qixiaoxia
 */
public class SalOrderProgressRow
{
    private Long orderId;

    /** 0-100，SUM(任务已报数)/SUM(任务计划数)，SQL 内 LEAST 封顶 */
    private Integer progressPercent;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
}
