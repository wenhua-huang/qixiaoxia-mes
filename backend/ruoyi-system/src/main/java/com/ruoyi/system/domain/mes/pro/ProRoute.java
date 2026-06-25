package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工艺路线对象 qxx_pro_route
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public class ProRoute extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工艺路线ID */
    private Long routeId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 工艺路线编码(唯一) */
    @Excel(name = "工艺路线编码")
    private String routeCode;

    /** 工艺路线名称 */
    @Excel(name = "工艺路线名称")
    private String routeName;

    /** 工艺路线描述 */
    @Excel(name = "工艺路线描述")
    private String routeDesc;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用")
    private String enableFlag;

    public void setRouteId(Long routeId)
    {
        this.routeId = routeId;
    }

    public Long getRouteId()
    {
        return routeId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setRouteCode(String routeCode)
    {
        this.routeCode = routeCode;
    }

    public String getRouteCode()
    {
        return routeCode;
    }

    public void setRouteName(String routeName)
    {
        this.routeName = routeName;
    }

    public String getRouteName()
    {
        return routeName;
    }

    public void setRouteDesc(String routeDesc)
    {
        this.routeDesc = routeDesc;
    }

    public String getRouteDesc()
    {
        return routeDesc;
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
            .append("routeId", getRouteId())
            .append("factoryId", getFactoryId())
            .append("routeCode", getRouteCode())
            .append("routeName", getRouteName())
            .append("routeDesc", getRouteDesc())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
