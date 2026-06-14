package com.ruoyi.system.domain.mes.tm;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工装夹具类型对象 qxx_tm_tool_type
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class TmToolType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工装夹具类型ID */
    private Long toolTypeId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 类型编码(唯一) */
    @Excel(name = "类型编码(唯一)")
    private String toolTypeCode;

    /** 类型名称 */
    @Excel(name = "类型名称")
    private String toolTypeName;

    /** 是否需要编码(1-需要,0-不需要) */
    @Excel(name = "是否需要编码(1-需要,0-不需要)")
    private String needCodeFlag;

    /** 保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数 */
    @Excel(name = "保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数")
    private String maintenType;

    /** 保养周期(与保养类型配合,如:月+3=每3个月保养一次) */
    @Excel(name = "保养周期(与保养类型配合,如:月+3=每3个月保养一次)")
    private Long maintenCycle;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用(1-是,0-否)")
    private String enableFlag;

    public void setToolTypeId(Long toolTypeId) 
    {
        this.toolTypeId = toolTypeId;
    }

    public Long getToolTypeId() 
    {
        return toolTypeId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setToolTypeCode(String toolTypeCode) 
    {
        this.toolTypeCode = toolTypeCode;
    }

    public String getToolTypeCode() 
    {
        return toolTypeCode;
    }

    public void setToolTypeName(String toolTypeName) 
    {
        this.toolTypeName = toolTypeName;
    }

    public String getToolTypeName() 
    {
        return toolTypeName;
    }

    public void setNeedCodeFlag(String needCodeFlag) 
    {
        this.needCodeFlag = needCodeFlag;
    }

    public String getNeedCodeFlag() 
    {
        return needCodeFlag;
    }

    public void setMaintenType(String maintenType) 
    {
        this.maintenType = maintenType;
    }

    public String getMaintenType() 
    {
        return maintenType;
    }

    public void setMaintenCycle(Long maintenCycle) 
    {
        this.maintenCycle = maintenCycle;
    }

    public Long getMaintenCycle() 
    {
        return maintenCycle;
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
            .append("toolTypeId", getToolTypeId())
            .append("factoryId", getFactoryId())
            .append("toolTypeCode", getToolTypeCode())
            .append("toolTypeName", getToolTypeName())
            .append("needCodeFlag", getNeedCodeFlag())
            .append("maintenType", getMaintenType())
            .append("maintenCycle", getMaintenCycle())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
