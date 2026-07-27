package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工序参数模版定义对象 qxx_pro_param_template
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public class ProParamTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模版ID */
    private Long templateId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 工序ID(关联qxx_pro_process) */
    @Excel(name = "工序ID")
    private Long processId;

    /** 参数编码(英文标识,同一工序下唯一) */
    @Excel(name = "参数编码")
    private String paramCode;

    /** 参数名称(中文显示名) */
    @Excel(name = "参数名称")
    private String paramName;

    /** 参数分组:MACHINE-设备参数,PROCESS-工艺参数,MATERIAL-材料参数,QUALITY-质量参数,PRODUCT-产品规格参数 */
    @Excel(name = "参数分组")
    private String paramGroup;

    /** 参数值类型:INT,DECIMAL,VARCHAR,ENUM,DATE,BOOLEAN */
    @Excel(name = "参数值类型")
    private String paramType;

    /** 单位(如mm,μm,℃,m/min,mm²,kg) */
    @Excel(name = "单位")
    private String unit;

    /** 枚举可选值(JSON数组,param_type=ENUM时必填) */
    @Excel(name = "枚举可选值")
    private String enumValues;

    /** 默认值 */
    @Excel(name = "默认值")
    private String defaultValue;

    /** 最小值(超出时触发偏差预警) */
    @Excel(name = "最小值")
    private java.math.BigDecimal minValue;

    /** 最大值(超出时触发偏差预警) */
    @Excel(name = "最大值")
    private java.math.BigDecimal maxValue;

    /** 排序号(同一工序下参数显示顺序) */
    @Excel(name = "排序号")
    private Integer sortOrder;

    /** 是否必填(Y-是,N-否) */
    @Excel(name = "是否必填")
    private String isRequired;

    /** 报工时是否显示(Y-是=操作工可见可填,N-否=仅工艺设计时可见) */
    @Excel(name = "报工时是否显示")
    private String isReportVisible;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用")
    private String enableFlag;

    /** 标准图样(多个逗号分隔,存/common/upload返回的相对路径) */
    private String imageUrl;

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setProcessId(Long processId)
    {
        this.processId = processId;
    }

    public Long getProcessId()
    {
        return processId;
    }

    public void setParamCode(String paramCode)
    {
        this.paramCode = paramCode;
    }

    public String getParamCode()
    {
        return paramCode;
    }

    public void setParamName(String paramName)
    {
        this.paramName = paramName;
    }

    public String getParamName()
    {
        return paramName;
    }

    public void setParamGroup(String paramGroup)
    {
        this.paramGroup = paramGroup;
    }

    public String getParamGroup()
    {
        return paramGroup;
    }

    public void setParamType(String paramType)
    {
        this.paramType = paramType;
    }

    public String getParamType()
    {
        return paramType;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setEnumValues(String enumValues)
    {
        this.enumValues = enumValues;
    }

    public String getEnumValues()
    {
        return enumValues;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setMinValue(java.math.BigDecimal minValue) { this.minValue = minValue; }
    public java.math.BigDecimal getMinValue() { return minValue; }
    public void setMaxValue(java.math.BigDecimal maxValue) { this.maxValue = maxValue; }
    public java.math.BigDecimal getMaxValue() { return maxValue; }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setIsRequired(String isRequired)
    {
        this.isRequired = isRequired;
    }

    public String getIsRequired()
    {
        return isRequired;
    }

    public void setIsReportVisible(String isReportVisible)
    {
        this.isReportVisible = isReportVisible;
    }

    public String getIsReportVisible()
    {
        return isReportVisible;
    }

    public void setEnableFlag(String enableFlag)
    {
        this.enableFlag = enableFlag;
    }

    public String getEnableFlag()
    {
        return enableFlag;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("templateId", getTemplateId())
            .append("factoryId", getFactoryId())
            .append("processId", getProcessId())
            .append("paramCode", getParamCode())
            .append("paramName", getParamName())
            .append("paramGroup", getParamGroup())
            .append("paramType", getParamType())
            .append("unit", getUnit())
            .append("enumValues", getEnumValues())
            .append("defaultValue", getDefaultValue())
            .append("minValue", getMinValue())
            .append("maxValue", getMaxValue())
            .append("sortOrder", getSortOrder())
            .append("isRequired", getIsRequired())
            .append("isReportVisible", getIsReportVisible())
            .append("enableFlag", getEnableFlag())
            .append("imageUrl", getImageUrl())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
