package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;

/**
 * 工单工序步骤点（列表"完成率"步骤圈用）。
 * 一道工序一个点；{@code done}=true 表示该工序已完工，
 * {@code quantity}/{@code quantityProduced} 用于按比例填充圆圈。
 *
 * @author qixiaoxia
 * @date 2026-08-23
 */
public class ProTaskProgressStep
{
    private Long workorderId;
    private Integer orderNum;
    private String processName;
    /** 该工序是否已完工（其下任务全部 COMPLETED 或已报满） */
    private Boolean done;
    /** 该工序计划数量（其下非取消任务合计） */
    private BigDecimal quantity;
    /** 该工序已生产数量（其下非取消任务合计） */
    private BigDecimal quantityProduced;

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer v) { this.orderNum = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public Boolean getDone() { return done; }
    public void setDone(Boolean v) { this.done = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
}
