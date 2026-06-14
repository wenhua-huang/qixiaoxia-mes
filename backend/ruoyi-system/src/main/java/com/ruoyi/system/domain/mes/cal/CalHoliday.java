package com.ruoyi.system.domain.mes.cal;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 节假日设置对象 qxx_cal_holiday
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalHoliday extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 节假日设置ID */
    private Long holidayId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date holidayDate;

    /** 节假日名称 */
    @Excel(name = "节假日名称")
    private String holidayName;

    /** 类型:HOLIDAY-节假日,WORKDAY-调休工作日 */
    @Excel(name = "类型:HOLIDAY-节假日,WORKDAY-调休工作日")
    private String holidayType;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用(1-是,0-否)")
    private String enableFlag;

    public void setHolidayId(Long holidayId) 
    {
        this.holidayId = holidayId;
    }

    public Long getHolidayId() 
    {
        return holidayId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setHolidayDate(Date holidayDate) 
    {
        this.holidayDate = holidayDate;
    }

    public Date getHolidayDate() 
    {
        return holidayDate;
    }

    public void setHolidayName(String holidayName) 
    {
        this.holidayName = holidayName;
    }

    public String getHolidayName() 
    {
        return holidayName;
    }

    public void setHolidayType(String holidayType) 
    {
        this.holidayType = holidayType;
    }

    public String getHolidayType() 
    {
        return holidayType;
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
            .append("holidayId", getHolidayId())
            .append("factoryId", getFactoryId())
            .append("holidayDate", getHolidayDate())
            .append("holidayName", getHolidayName())
            .append("holidayType", getHolidayType())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
