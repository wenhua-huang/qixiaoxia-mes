package com.ruoyi.system.domain.mes.cal;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 计划班次对象 qxx_cal_shift
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalShift extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 班次ID */
    private Long shiftId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 排班计划ID(关联qxx_cal_plan) */
    @Excel(name = "排班计划ID(关联qxx_cal_plan)")
    private Long planId;

    /** 班次序号(如1-白班,2-夜班) */
    @Excel(name = "班次序号(如1-白班,2-夜班)")
    private Long shiftSeq;

    /** 班次名称(如白班/夜班/中班) */
    @Excel(name = "班次名称(如白班/夜班/中班)")
    private String shiftName;

    /** 班次开始时间(如08:00:00) */
    @JsonFormat(pattern = "HH:mm:ss")
    @Excel(name = "班次开始时间(如08:00:00)", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 班次结束时间(如20:00:00) */
    @JsonFormat(pattern = "HH:mm:ss")
    @Excel(name = "班次结束时间(如20:00:00)", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    public void setShiftId(Long shiftId) 
    {
        this.shiftId = shiftId;
    }

    public Long getShiftId() 
    {
        return shiftId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setPlanId(Long planId) 
    {
        this.planId = planId;
    }

    public Long getPlanId() 
    {
        return planId;
    }

    public void setShiftSeq(Long shiftSeq) 
    {
        this.shiftSeq = shiftSeq;
    }

    public Long getShiftSeq() 
    {
        return shiftSeq;
    }

    public void setShiftName(String shiftName) 
    {
        this.shiftName = shiftName;
    }

    public String getShiftName() 
    {
        return shiftName;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("shiftId", getShiftId())
            .append("factoryId", getFactoryId())
            .append("planId", getPlanId())
            .append("shiftSeq", getShiftSeq())
            .append("shiftName", getShiftName())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
