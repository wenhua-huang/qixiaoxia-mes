package com.ruoyi.system.domain.mes.cal;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 班组排班明细对象 qxx_cal_teamshift
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalTeamshift extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 排班明细ID */
    private Long teamshiftId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 排班日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "排班日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date shiftDate;

    /** 班组ID(关联qxx_cal_team) */
    @Excel(name = "班组ID(关联qxx_cal_team)")
    private Long teamId;

    /** 班组编码 */
    @Excel(name = "班组编码")
    private String teamCode;

    /** 班组名称 */
    @Excel(name = "班组名称")
    private String teamName;

    /** 班次ID(关联qxx_cal_shift) */
    @Excel(name = "班次ID(关联qxx_cal_shift)")
    private Long shiftId;

    /** 班次名称 */
    @Excel(name = "班次名称")
    private String shiftName;

    /** 排班计划ID(关联qxx_cal_plan) */
    @Excel(name = "排班计划ID(关联qxx_cal_plan)")
    private Long planId;

    /** 排班计划编码 */
    @Excel(name = "排班计划编码")
    private String planCode;

    /** 排班计划名称 */
    @Excel(name = "排班计划名称")
    private String planName;

    public void setTeamshiftId(Long teamshiftId) 
    {
        this.teamshiftId = teamshiftId;
    }

    public Long getTeamshiftId() 
    {
        return teamshiftId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setShiftDate(Date shiftDate) 
    {
        this.shiftDate = shiftDate;
    }

    public Date getShiftDate() 
    {
        return shiftDate;
    }

    public void setTeamId(Long teamId) 
    {
        this.teamId = teamId;
    }

    public Long getTeamId() 
    {
        return teamId;
    }

    public void setTeamCode(String teamCode) 
    {
        this.teamCode = teamCode;
    }

    public String getTeamCode() 
    {
        return teamCode;
    }

    public void setTeamName(String teamName) 
    {
        this.teamName = teamName;
    }

    public String getTeamName() 
    {
        return teamName;
    }

    public void setShiftId(Long shiftId) 
    {
        this.shiftId = shiftId;
    }

    public Long getShiftId() 
    {
        return shiftId;
    }

    public void setShiftName(String shiftName) 
    {
        this.shiftName = shiftName;
    }

    public String getShiftName() 
    {
        return shiftName;
    }

    public void setPlanId(Long planId) 
    {
        this.planId = planId;
    }

    public Long getPlanId() 
    {
        return planId;
    }

    public void setPlanCode(String planCode) 
    {
        this.planCode = planCode;
    }

    public String getPlanCode() 
    {
        return planCode;
    }

    public void setPlanName(String planName) 
    {
        this.planName = planName;
    }

    public String getPlanName() 
    {
        return planName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("teamshiftId", getTeamshiftId())
            .append("factoryId", getFactoryId())
            .append("shiftDate", getShiftDate())
            .append("teamId", getTeamId())
            .append("teamCode", getTeamCode())
            .append("teamName", getTeamName())
            .append("shiftId", getShiftId())
            .append("shiftName", getShiftName())
            .append("planId", getPlanId())
            .append("planCode", getPlanCode())
            .append("planName", getPlanName())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
