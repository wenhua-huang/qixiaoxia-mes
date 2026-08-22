package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 工单进度 — 工序任务行 VO
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class ProcessProgressRowVO
{
    private Long taskId;
    private String taskCode;
    private Long processId;
    private String processCode;
    private String processName;
    private Long workstationId;
    private String workstationName;
    private String colorCode;
    private String status;
    private BigDecimal quantity;
    private BigDecimal quantityProduced;
    private BigDecimal quantityQualified;
    private BigDecimal quantityUnqualified;
    private Date planStartTime;
    private Date planEndTime;
    private Date actualStartTime;
    private Date actualEndTime;
    /** 完成率 0-100 */
    private Integer completionRate;
    /** 延期风险等级 */
    private String delayLevel;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String v) { this.taskCode = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }
    public String getWorkstationName() { return workstationName; }
    public void setWorkstationName(String v) { this.workstationName = v; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String v) { this.colorCode = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
    public BigDecimal getQuantityQualified() { return quantityQualified; }
    public void setQuantityQualified(BigDecimal v) { this.quantityQualified = v; }
    public BigDecimal getQuantityUnqualified() { return quantityUnqualified; }
    public void setQuantityUnqualified(BigDecimal v) { this.quantityUnqualified = v; }
    public Date getPlanStartTime() { return planStartTime; }
    public void setPlanStartTime(Date v) { this.planStartTime = v; }
    public Date getPlanEndTime() { return planEndTime; }
    public void setPlanEndTime(Date v) { this.planEndTime = v; }
    public Date getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(Date v) { this.actualStartTime = v; }
    public Date getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(Date v) { this.actualEndTime = v; }
    public Integer getCompletionRate() { return completionRate; }
    public void setCompletionRate(Integer v) { this.completionRate = v; }
    public String getDelayLevel() { return delayLevel; }
    public void setDelayLevel(String v) { this.delayLevel = v; }
}
