package com.ruoyi.system.domain.mes.cal;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 排班计划对象 qxx_cal_plan
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 排班计划ID */
    private Long planId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 排班计划编码(唯一) */
    @Excel(name = "排班计划编码(唯一)")
    private String planCode;

    /** 排班计划名称 */
    @Excel(name = "排班计划名称")
    private String planName;

    /** 日历类型:WEEKLY-周历,MONTHLY-月历,QUARTERLY-季历,YEARLY-年历,CUSTOM-自定义 */
    @Excel(name = "日历类型:WEEKLY-周历,MONTHLY-月历,QUARTERLY-季历,YEARLY-年历,CUSTOM-自定义")
    private String calendarType;

    /** 排班开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "排班开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 排班结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "排班结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 班次类型:TWOSHIFT-两班倒,THREESHIFT-三班倒,DAYONLY-常白班,CUSTOM-自定义 */
    @Excel(name = "班次类型:TWOSHIFT-两班倒,THREESHIFT-三班倒,DAYONLY-常白班,CUSTOM-自定义")
    private String shiftType;

    /** 计划状态:DRAFT-草稿,ACTIVE-生效中,CLOSED-已关闭 */
    @Excel(name = "计划状态:DRAFT-草稿,ACTIVE-生效中,CLOSED-已关闭")
    private String status;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用(1-是,0-否)")
    private String enableFlag;

    public void setPlanId(Long planId) 
    {
        this.planId = planId;
    }

    public Long getPlanId() 
    {
        return planId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
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

    public void setCalendarType(String calendarType) 
    {
        this.calendarType = calendarType;
    }

    public String getCalendarType() 
    {
        return calendarType;
    }

    public void setStartDate(Date startDate) 
    {
        this.startDate = startDate;
    }

    public Date getStartDate() 
    {
        return startDate;
    }

    public void setEndDate(Date endDate) 
    {
        this.endDate = endDate;
    }

    public Date getEndDate() 
    {
        return endDate;
    }

    public void setShiftType(String shiftType) 
    {
        this.shiftType = shiftType;
    }

    public String getShiftType() 
    {
        return shiftType;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
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
            .append("planId", getPlanId())
            .append("factoryId", getFactoryId())
            .append("planCode", getPlanCode())
            .append("planName", getPlanName())
            .append("calendarType", getCalendarType())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("shiftType", getShiftType())
            .append("status", getStatus())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
