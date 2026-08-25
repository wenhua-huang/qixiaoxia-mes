package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;

/**
 * 产能统计行 VO（按车间/工作站/工序/班组维度）
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class ProductivityRowVO
{
    /** 分组 ID（车间/工作站/工序/班组 ID） */
    private Long groupId;
    /** 分组名称 */
    private String groupName;
    /** 工单总数 */
    private Integer workorderCount;
    /** 已完工单数 */
    private Integer completedCount;
    /** 延期单数 */
    private Integer delayedCount;
    /** 完工率（百分比） */
    private Integer completionRate;
    /** 延期率（百分比） */
    private Integer delayRate;
    /** 计划产量 */
    private BigDecimal standardOutput;
    /** 实际产量 */
    private BigDecimal actualOutput;
    /** 标准工时（分钟） */
    private Integer standardMinutes;
    /** 实际工时（分钟） */
    private Integer actualMinutes;
    /** 效率百分比 */
    private Integer efficiencyPercent;
    /** 合格数量 */
    private BigDecimal qualifiedQty;
    /** 报废数量 */
    private BigDecimal scrapQty;
    /** 合格率（百分比） */
    private Integer qualifiedRate;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long v) { this.groupId = v; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String v) { this.groupName = v; }
    public Integer getWorkorderCount() { return workorderCount; }
    public void setWorkorderCount(Integer v) { this.workorderCount = v; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer v) { this.completedCount = v; }
    public Integer getDelayedCount() { return delayedCount; }
    public void setDelayedCount(Integer v) { this.delayedCount = v; }
    public Integer getCompletionRate() { return completionRate; }
    public void setCompletionRate(Integer v) { this.completionRate = v; }
    public Integer getDelayRate() { return delayRate; }
    public void setDelayRate(Integer v) { this.delayRate = v; }
    public BigDecimal getStandardOutput() { return standardOutput; }
    public void setStandardOutput(BigDecimal v) { this.standardOutput = v; }
    public BigDecimal getActualOutput() { return actualOutput; }
    public void setActualOutput(BigDecimal v) { this.actualOutput = v; }
    public Integer getStandardMinutes() { return standardMinutes; }
    public void setStandardMinutes(Integer v) { this.standardMinutes = v; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer v) { this.actualMinutes = v; }
    public Integer getEfficiencyPercent() { return efficiencyPercent; }
    public void setEfficiencyPercent(Integer v) { this.efficiencyPercent = v; }
    public BigDecimal getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(BigDecimal v) { this.qualifiedQty = v; }
    public BigDecimal getScrapQty() { return scrapQty; }
    public void setScrapQty(BigDecimal v) { this.scrapQty = v; }
    public Integer getQualifiedRate() { return qualifiedRate; }
    public void setQualifiedRate(Integer v) { this.qualifiedRate = v; }
}
