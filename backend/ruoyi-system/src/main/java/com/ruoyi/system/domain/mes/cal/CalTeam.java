package com.ruoyi.system.domain.mes.cal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 班组对象 qxx_cal_team
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalTeam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 班组ID */
    private Long teamId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 班组编码(唯一) */
    @Excel(name = "班组编码(唯一)")
    private String teamCode;

    /** 班组名称 */
    @Excel(name = "班组名称")
    private String teamName;

    /** 班组类型:DAY-白班,NIGHT-夜班,MIDDLE-中班,ROTATION-轮班 */
    @Excel(name = "班组类型:DAY-白班,NIGHT-夜班,MIDDLE-中班,ROTATION-轮班")
    private String teamType;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用(1-是,0-否)")
    private String enableFlag;

    public void setTeamId(Long teamId) 
    {
        this.teamId = teamId;
    }

    public Long getTeamId() 
    {
        return teamId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
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

    public void setTeamType(String teamType) 
    {
        this.teamType = teamType;
    }

    public String getTeamType() 
    {
        return teamType;
    }

    public void setEnableFlag(String enableFlag) 
    {
        this.enableFlag = enableFlag;
    }

    public String getEnableFlag() 
    {
        return enableFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("teamId", getTeamId())
            .append("factoryId", getFactoryId())
            .append("teamCode", getTeamCode())
            .append("teamName", getTeamName())
            .append("teamType", getTeamType())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
