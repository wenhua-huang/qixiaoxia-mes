package com.ruoyi.system.domain.mes.pro.vo;

/**
 * 任务状态分布 VO
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class TaskStatusDistVO
{
    /** 任务状态（NORMAL/PREPARE/PRODUCING/PAUSED/COMPLETED/CANCEL） */
    private String status;
    /** 该状态任务数 */
    private Integer count;

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Integer getCount() { return count; }
    public void setCount(Integer v) { this.count = v; }
}
