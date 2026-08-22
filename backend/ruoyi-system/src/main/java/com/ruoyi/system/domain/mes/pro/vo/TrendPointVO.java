package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 生产趋势数据点 VO（按日聚合）
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class TrendPointVO
{
    /** 统计日期 */
    private Date statDate;
    /** 当日新建工单数 */
    private Integer createdCount;
    /** 当日完工任务数 */
    private Integer completedCount;
    /** 当日延期完工任务数 */
    private Integer delayedCount;
    /** 当日合格数量 */
    private BigDecimal qualifiedQty;
    /** 当日实际工时（分钟） */
    private Integer actualMinutes;

    public Date getStatDate() { return statDate; }
    public void setStatDate(Date v) { this.statDate = v; }
    public Integer getCreatedCount() { return createdCount; }
    public void setCreatedCount(Integer v) { this.createdCount = v; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer v) { this.completedCount = v; }
    public Integer getDelayedCount() { return delayedCount; }
    public void setDelayedCount(Integer v) { this.delayedCount = v; }
    public BigDecimal getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(BigDecimal v) { this.qualifiedQty = v; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer v) { this.actualMinutes = v; }
}
