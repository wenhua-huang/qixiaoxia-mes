package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 报工实际参数值对象 qxx_pro_feedback_param
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProFeedbackParam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "报工ID") private Long feedbackId;
    @Excel(name = "工单参数ID") private Long workorderParamId;
    @Excel(name = "参数模版ID") private Long templateId;
    @Excel(name = "实际值") private String actualValue;
    @Excel(name = "是否偏差") private String isDeviation;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long v) { this.recordId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public Long getWorkorderParamId() { return workorderParamId; }
    public void setWorkorderParamId(Long v) { this.workorderParamId = v; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long v) { this.templateId = v; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String v) { this.actualValue = v; }
    public String getIsDeviation() { return isDeviation; }
    public void setIsDeviation(String v) { this.isDeviation = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId()).append("feedbackId", getFeedbackId())
            .append("templateId", getTemplateId()).append("isDeviation", getIsDeviation())
            .toString();
    }
}
