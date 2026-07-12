package com.ruoyi.system.domain.mes.pro;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 上下工会话记录对象 qxx_pro_workrecord
 * <p>
 * 会话模式：上工 INSERT 一条(status=ACTIVE)，下工 UPDATE 同一条(status=CLOSED + clock_out_time + work_duration)
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProWorkrecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long factoryId;

    /** 操作人员 */
    private Long userId;
    private String userName;
    private String nickName;

    /** 工作站（上工必填，一人一岗） */
    private Long workstationId;
    private String workstationCode;
    private String workstationName;

    /** 可选关联（工位为主，任务/工单可选） */
    private Long workorderId;
    private String workorderCode;
    private Long taskId;
    private String taskCode;
    private String processName;

    /** 会话时间 */
    @Excel(name = "上工时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date clockInTime;
    @Excel(name = "下工时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date clockOutTime;
    /** 工作时长(分钟) */
    @Excel(name = "工作时长(分钟)")
    private Integer workDuration;
    @Excel(name = "状态", readConverterExp = "ACTIVE=在岗,CLOSED=已下工")
    private String status;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long v) { this.recordId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getNickName() { return nickName; }
    public void setNickName(String v) { this.nickName = v; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }
    public String getWorkstationCode() { return workstationCode; }
    public void setWorkstationCode(String v) { this.workstationCode = v; }
    public String getWorkstationName() { return workstationName; }
    public void setWorkstationName(String v) { this.workstationName = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String v) { this.taskCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public Date getClockInTime() { return clockInTime; }
    public void setClockInTime(Date v) { this.clockInTime = v; }
    public Date getClockOutTime() { return clockOutTime; }
    public void setClockOutTime(Date v) { this.clockOutTime = v; }
    public Integer getWorkDuration() { return workDuration; }
    public void setWorkDuration(Integer v) { this.workDuration = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("nickName", getNickName())
            .append("workstationId", getWorkstationId())
            .append("workstationName", getWorkstationName())
            .append("clockInTime", getClockInTime())
            .append("clockOutTime", getClockOutTime())
            .append("workDuration", getWorkDuration())
            .append("status", getStatus())
            .toString();
    }
}
