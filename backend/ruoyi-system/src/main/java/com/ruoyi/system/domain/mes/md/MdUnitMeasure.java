package com.ruoyi.system.domain.mes.md;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 单位管理对象 qxx_md_unit_measure
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
public class MdUnitMeasure extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 单位ID */
    private Long unitId;

    /** 工厂ID */
    private Long factoryId;

    /** 单位编码 */
    @Excel(name = "单位编码")
    private String unitCode;

    /** 单位名称 */
    @Excel(name = "单位名称")
    private String unitName;

    /** 所属主单位编码 */
    @Excel(name = "所属主单位编码")
    private String primaryUnit;

    /** 与主单位的换算率 */
    @Excel(name = "与主单位的换算率")
    private BigDecimal conversionRate;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getUnitId()
    {
        return unitId;
    }

    public void setUnitId(Long unitId)
    {
        this.unitId = unitId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getUnitCode()
    {
        return unitCode;
    }

    public void setUnitCode(String unitCode)
    {
        this.unitCode = unitCode;
    }

    public String getUnitName()
    {
        return unitName;
    }

    public void setUnitName(String unitName)
    {
        this.unitName = unitName;
    }

    public String getPrimaryUnit()
    {
        return primaryUnit;
    }

    public void setPrimaryUnit(String primaryUnit)
    {
        this.primaryUnit = primaryUnit;
    }

    public BigDecimal getConversionRate()
    {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate)
    {
        this.conversionRate = conversionRate;
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
            .append("unitId", getUnitId())
            .append("factoryId", getFactoryId())
            .append("unitCode", getUnitCode())
            .append("unitName", getUnitName())
            .append("primaryUnit", getPrimaryUnit())
            .append("conversionRate", getConversionRate())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
