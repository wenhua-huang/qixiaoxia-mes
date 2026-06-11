package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工厂定义对象 qxx_md_factory
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdFactory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工厂ID */
    private Long factoryId;

    /** 工厂编码 */
    @Excel(name = "工厂编码")
    private String factoryCode;

    /** 工厂全称 */
    @Excel(name = "工厂全称")
    private String factoryName;

    /** 工厂简称 */
    @Excel(name = "工厂简称")
    private String shortName;

    /** 工厂地址 */
    @Excel(name = "工厂地址")
    private String address;

    /** 工厂负责人 */
    @Excel(name = "工厂负责人")
    private String contact;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getFactoryCode()
    {
        return factoryCode;
    }

    public void setFactoryCode(String factoryCode)
    {
        this.factoryCode = factoryCode;
    }

    public String getFactoryName()
    {
        return factoryName;
    }

    public void setFactoryName(String factoryName)
    {
        this.factoryName = factoryName;
    }

    public String getShortName()
    {
        return shortName;
    }

    public void setShortName(String shortName)
    {
        this.shortName = shortName;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getContact()
    {
        return contact;
    }

    public void setContact(String contact)
    {
        this.contact = contact;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
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
            .append("factoryId", getFactoryId())
            .append("factoryCode", getFactoryCode())
            .append("factoryName", getFactoryName())
            .append("shortName", getShortName())
            .append("address", getAddress())
            .append("contact", getContact())
            .append("phone", getPhone())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
