package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单工序参数值对象 qxx_pro_workorder_param
 */
public class ProWorkorderParam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "路线产品ID") private Long routeProductId;
    @Excel(name = "参数模版ID") private Long templateId;
    @Excel(name = "路线标准值") private String standardValue;
    @Excel(name = "工单调整值") private String adjustedValue;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long v) { this.recordId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Long getRouteProductId() { return routeProductId; }
    public void setRouteProductId(Long v) { this.routeProductId = v; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long v) { this.templateId = v; }
    public String getStandardValue() { return standardValue; }
    public void setStandardValue(String v) { this.standardValue = v; }
    public String getAdjustedValue() { return adjustedValue; }
    public void setAdjustedValue(String v) { this.adjustedValue = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId()).append("workorderId", getWorkorderId())
            .append("templateId", getTemplateId()).toString();
    }
}
