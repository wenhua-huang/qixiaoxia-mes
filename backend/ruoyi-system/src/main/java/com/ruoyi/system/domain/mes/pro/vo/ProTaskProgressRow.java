package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 工单进度批量聚合行：一张工单下的非终态任务（含计划结束时间）。
 * Service 端据此计算当前工序、剩余标准工时、计划完工时间。
 *
 * @author qixiaoxia
 * @date 2026-08-23
 */
public class ProTaskProgressRow
{
    private Long workorderId;
    private String processName;
    private String status;
    private Integer orderNum;
    private BigDecimal quantity;
    private BigDecimal quantityProduced;
    private Integer setupDuration;
    private BigDecimal unitDuration;
    private Date endTime;

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer v) { this.orderNum = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
    public Integer getSetupDuration() { return setupDuration; }
    public void setSetupDuration(Integer v) { this.setupDuration = v; }
    public BigDecimal getUnitDuration() { return unitDuration; }
    public void setUnitDuration(BigDecimal v) { this.unitDuration = v; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date v) { this.endTime = v; }
}
