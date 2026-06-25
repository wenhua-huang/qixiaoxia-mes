package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工艺路线-工序组成对象 qxx_pro_route_process
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public class ProRouteProcess extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 工艺路线ID(关联qxx_pro_route) */
    @Excel(name = "工艺路线ID")
    private Long routeId;

    /** 工序ID(关联qxx_pro_process) */
    @Excel(name = "工序ID")
    private Long processId;

    /** 工序编码 */
    @Excel(name = "工序编码")
    private String processCode;

    /** 工序名称 */
    @Excel(name = "工序名称")
    private String processName;

    /** 工序类型 */
    @Excel(name = "工序类型")
    private String processType;

    /** 工序序号 */
    @Excel(name = "工序序号")
    private Integer orderNum;

    /** 下一道工序ID */
    @Excel(name = "下一道工序ID")
    private Long nextProcessId;

    /** 下一道工序编码 */
    @Excel(name = "下一道工序编码")
    private String nextProcessCode;

    /** 下一道工序名称 */
    @Excel(name = "下一道工序名称")
    private String nextProcessName;

    /** 与下道工序关系:SS-串行,PP-并行,CC-协同 */
    @Excel(name = "与下道工序关系")
    private String linkType;

    /** 默认准备时长(分钟) */
    @Excel(name = "默认准备时长")
    private Integer defaultPreTime;

    /** 默认等待时长(分钟) */
    @Excel(name = "默认等待时长")
    private Integer defaultSufTime;

    /** 甘特图显示颜色 */
    @Excel(name = "甘特图显示颜色")
    private String colorCode;

    /** 是否关键工序(Y-是,N-否) */
    @Excel(name = "是否关键工序")
    private String keyFlag;

    /** 是否检验工序(Y-是,N-否) */
    @Excel(name = "是否检验工序")
    private String isCheck;

    /** 是否外发工序(1-是,0-否) */
    @Excel(name = "是否外发工序")
    private String isOutsource;

    /** 外协厂ID(关联qxx_md_vendor) */
    @Excel(name = "外协厂ID")
    private Long vendorId;

    /** 外协厂编码 */
    @Excel(name = "外协厂编码")
    private String vendorCode;

    /** 外协厂名称 */
    @Excel(name = "外协厂名称")
    private String vendorName;

    /** 外协工厂ID(关联qxx_md_factory) */
    @Excel(name = "外协工厂ID")
    private Long outsourceFactoryId;

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

    public void setRouteId(Long routeId)
    {
        this.routeId = routeId;
    }

    public Long getRouteId()
    {
        return routeId;
    }

    public void setProcessId(Long processId)
    {
        this.processId = processId;
    }

    public Long getProcessId()
    {
        return processId;
    }

    public void setProcessCode(String processCode)
    {
        this.processCode = processCode;
    }

    public String getProcessCode()
    {
        return processCode;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setProcessType(String processType)
    {
        this.processType = processType;
    }

    public String getProcessType()
    {
        return processType;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setNextProcessId(Long nextProcessId)
    {
        this.nextProcessId = nextProcessId;
    }

    public Long getNextProcessId()
    {
        return nextProcessId;
    }

    public void setNextProcessCode(String nextProcessCode)
    {
        this.nextProcessCode = nextProcessCode;
    }

    public String getNextProcessCode()
    {
        return nextProcessCode;
    }

    public void setNextProcessName(String nextProcessName)
    {
        this.nextProcessName = nextProcessName;
    }

    public String getNextProcessName()
    {
        return nextProcessName;
    }

    public void setLinkType(String linkType)
    {
        this.linkType = linkType;
    }

    public String getLinkType()
    {
        return linkType;
    }

    public void setDefaultPreTime(Integer defaultPreTime)
    {
        this.defaultPreTime = defaultPreTime;
    }

    public Integer getDefaultPreTime()
    {
        return defaultPreTime;
    }

    public void setDefaultSufTime(Integer defaultSufTime)
    {
        this.defaultSufTime = defaultSufTime;
    }

    public Integer getDefaultSufTime()
    {
        return defaultSufTime;
    }

    public void setColorCode(String colorCode)
    {
        this.colorCode = colorCode;
    }

    public String getColorCode()
    {
        return colorCode;
    }

    public void setKeyFlag(String keyFlag)
    {
        this.keyFlag = keyFlag;
    }

    public String getKeyFlag()
    {
        return keyFlag;
    }

    public void setIsCheck(String isCheck)
    {
        this.isCheck = isCheck;
    }

    public String getIsCheck()
    {
        return isCheck;
    }

    public void setIsOutsource(String isOutsource)
    {
        this.isOutsource = isOutsource;
    }

    public String getIsOutsource()
    {
        return isOutsource;
    }

    public void setVendorId(Long vendorId)
    {
        this.vendorId = vendorId;
    }

    public Long getVendorId()
    {
        return vendorId;
    }

    public void setVendorCode(String vendorCode)
    {
        this.vendorCode = vendorCode;
    }

    public String getVendorCode()
    {
        return vendorCode;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    public String getVendorName()
    {
        return vendorName;
    }

    public void setOutsourceFactoryId(Long outsourceFactoryId)
    {
        this.outsourceFactoryId = outsourceFactoryId;
    }

    public Long getOutsourceFactoryId()
    {
        return outsourceFactoryId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("routeId", getRouteId())
            .append("processId", getProcessId())
            .append("processCode", getProcessCode())
            .append("processName", getProcessName())
            .append("processType", getProcessType())
            .append("orderNum", getOrderNum())
            .append("nextProcessId", getNextProcessId())
            .append("nextProcessCode", getNextProcessCode())
            .append("nextProcessName", getNextProcessName())
            .append("linkType", getLinkType())
            .append("defaultPreTime", getDefaultPreTime())
            .append("defaultSufTime", getDefaultSufTime())
            .append("colorCode", getColorCode())
            .append("keyFlag", getKeyFlag())
            .append("isCheck", getIsCheck())
            .append("isOutsource", getIsOutsource())
            .append("vendorId", getVendorId())
            .append("vendorCode", getVendorCode())
            .append("vendorName", getVendorName())
            .append("outsourceFactoryId", getOutsourceFactoryId())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
