package com.ruoyi.system.domain.mes.pro.vo;

/**
 * 生产统计总览 VO（KPI 指标）
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class ReportOverviewVO
{
    /** 工单总数 */
    private Integer workorderTotal;
    /** 已完工单数 */
    private Integer workorderCompleted;
    /** 完工率（百分比，0-100） */
    private Integer completionRate;
    /** 延期单数 */
    private Integer workorderDelayed;
    /** 延期率（百分比，0-100） */
    private Integer delayRate;
    /** 生产中单数 */
    private Integer workorderInProgress;
    /** 标准工时（分钟） */
    private Integer standardMinutes;
    /** 实际工时（分钟） */
    private Integer actualMinutes;
    /** 效率百分比（标准/实际*100，无实际工时为 null） */
    private Integer efficiencyPercent;

    public Integer getWorkorderTotal() { return workorderTotal; }
    public void setWorkorderTotal(Integer v) { this.workorderTotal = v; }
    public Integer getWorkorderCompleted() { return workorderCompleted; }
    public void setWorkorderCompleted(Integer v) { this.workorderCompleted = v; }
    public Integer getCompletionRate() { return completionRate; }
    public void setCompletionRate(Integer v) { this.completionRate = v; }
    public Integer getWorkorderDelayed() { return workorderDelayed; }
    public void setWorkorderDelayed(Integer v) { this.workorderDelayed = v; }
    public Integer getDelayRate() { return delayRate; }
    public void setDelayRate(Integer v) { this.delayRate = v; }
    public Integer getWorkorderInProgress() { return workorderInProgress; }
    public void setWorkorderInProgress(Integer v) { this.workorderInProgress = v; }
    public Integer getStandardMinutes() { return standardMinutes; }
    public void setStandardMinutes(Integer v) { this.standardMinutes = v; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer v) { this.actualMinutes = v; }
    public Integer getEfficiencyPercent() { return efficiencyPercent; }
    public void setEfficiencyPercent(Integer v) { this.efficiencyPercent = v; }
}
