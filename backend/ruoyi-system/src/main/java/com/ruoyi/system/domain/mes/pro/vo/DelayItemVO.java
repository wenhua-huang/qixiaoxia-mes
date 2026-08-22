package com.ruoyi.system.domain.mes.pro.vo;

import java.util.Date;

/**
 * 延期预警列表项 VO（工单/任务通用）
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class DelayItemVO
{
    /** WORKORDER / TASK */
    private String objectType;
    private Long objectId;
    private String objectCode;
    private String objectName;
    private String productName;
    private Long workshopId;
    private String workshopName;
    private Long teamId;
    private String teamName;
    private String workstationName;
    /** 任务：计划结束时间；工单为空 */
    private Date planTime;
    /** 工单：需求日期；任务为关联工单需求日期 */
    private Date requestDate;
    /** 实际完工时间 */
    private Date actualEndTime;
    private Integer overdueDays;
    private Integer progressPercent;
    private String status;
    /** 延期风险等级（Java 端填充，非 resultMap 映射） */
    private String delayLevel;

    public String getObjectType() { return objectType; }
    public void setObjectType(String v) { this.objectType = v; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long v) { this.objectId = v; }
    public String getObjectCode() { return objectCode; }
    public void setObjectCode(String v) { this.objectCode = v; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String v) { this.objectName = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long v) { this.workshopId = v; }
    public String getWorkshopName() { return workshopName; }
    public void setWorkshopName(String v) { this.workshopName = v; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long v) { this.teamId = v; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String v) { this.teamName = v; }
    public String getWorkstationName() { return workstationName; }
    public void setWorkstationName(String v) { this.workstationName = v; }
    public Date getPlanTime() { return planTime; }
    public void setPlanTime(Date v) { this.planTime = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public Date getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(Date v) { this.actualEndTime = v; }
    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer v) { this.overdueDays = v; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer v) { this.progressPercent = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getDelayLevel() { return delayLevel; }
    public void setDelayLevel(String v) { this.delayLevel = v; }
}
