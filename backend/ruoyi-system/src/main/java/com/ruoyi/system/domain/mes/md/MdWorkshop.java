package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 车间管理对象 qxx_md_workshop
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdWorkshop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 车间ID */
    private Long workshopId;

    /** 工厂ID */
    private Long factoryId;

    /** 车间编码 */
    @Excel(name = "车间编码")
    private String workshopCode;

    /** 车间名称 */
    @Excel(name = "车间名称")
    private String workshopName;

    /** 车间位置/地址 */
    @Excel(name = "车间地址")
    private String address;

    /** 车间负责人 */
    @Excel(name = "车间负责人")
    private String manager;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getWorkshopId()
    {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId)
    {
        this.workshopId = workshopId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getWorkshopCode()
    {
        return workshopCode;
    }

    public void setWorkshopCode(String workshopCode)
    {
        this.workshopCode = workshopCode;
    }

    public String getWorkshopName()
    {
        return workshopName;
    }

    public void setWorkshopName(String workshopName)
    {
        this.workshopName = workshopName;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getManager()
    {
        return manager;
    }

    public void setManager(String manager)
    {
        this.manager = manager;
    }

    public String getEnableFlag()
    {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag)
    {
        this.enableFlag = enableFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("workshopId", getWorkshopId())
            .append("factoryId", getFactoryId())
            .append("workshopCode", getWorkshopCode())
            .append("workshopName", getWorkshopName())
            .append("address", getAddress())
            .append("manager", getManager())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
