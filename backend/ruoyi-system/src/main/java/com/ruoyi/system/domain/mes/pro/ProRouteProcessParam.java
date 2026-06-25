package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工艺路线工序参数对象 qxx_pro_route_process_param
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public class ProRouteProcessParam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 路线产品ID(关联qxx_pro_route_product) */
    @Excel(name = "路线产品ID")
    private Long routeProductId;

    /** 工序ID(关联qxx_pro_process) */
    @Excel(name = "工序ID")
    private Long processId;

    /** 参数模版ID(关联qxx_pro_param_template) */
    @Excel(name = "参数模版ID")
    private Long templateId;

    /** 参数值 */
    @Excel(name = "参数值")
    private String paramValue;

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

    public void setRouteProductId(Long routeProductId)
    {
        this.routeProductId = routeProductId;
    }

    public Long getRouteProductId()
    {
        return routeProductId;
    }

    public void setProcessId(Long processId)
    {
        this.processId = processId;
    }

    public Long getProcessId()
    {
        return processId;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setParamValue(String paramValue)
    {
        this.paramValue = paramValue;
    }

    public String getParamValue()
    {
        return paramValue;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("routeProductId", getRouteProductId())
            .append("processId", getProcessId())
            .append("templateId", getTemplateId())
            .append("paramValue", getParamValue())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
