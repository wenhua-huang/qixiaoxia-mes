package com.ruoyi.system.domain.mes.cal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 计划班组关联对象 qxx_cal_plan_team
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalPlanTeam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 关联记录ID */
    private Long recordId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 排班计划ID(关联qxx_cal_plan) */
    @Excel(name = "排班计划ID(关联qxx_cal_plan)")
    private Long planId;

    /** 排班计划编码 */
    @Excel(name = "排班计划编码")
    private String planCode;

    /** 排班计划名称 */
    @Excel(name = "排班计划名称")
    private String planName;

    /** 班组ID(关联qxx_cal_team) */
    @Excel(name = "班组ID(关联qxx_cal_team)")
    private Long teamId;

    /** 班组编码 */
    @Excel(name = "班组编码")
    private String teamCode;

    /** 班组名称 */
    @Excel(name = "班组名称")
    private String teamName;

    public void setRecordId(Long recordId) 
    {
        this.recordId = recordId;
    }

    public Long getRecordId() 
    {
        return recordId;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("planId", getPlanId())
            .append("planCode", getPlanCode())
            .append("planName", getPlanName())
            .append("teamId", getTeamId())
            .append("teamCode", getTeamCode())
            .append("teamName", getTeamName())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
