package com.ruoyi.system.domain.mes.qc;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 质检检验模板对象 qxx_qc_template（头表，检测项行/物料绑定行级联保存）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long templateId;

    /** 工厂ID */
    private Long factoryId;

    /** 模板编码 */
    @Excel(name = "模板编码")
    private String templateCode;

    /** 模板名称 */
    @Excel(name = "模板名称")
    private String templateName;

    /** 适用检验种类(多选逗号分隔,如IQC,RQC) */
    @Excel(name = "适用检验种类")
    private String qcTypes;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    /** 检测项行（qxx_qc_template_index，级联全删全插） */
    private List<QcTemplateIndex> indexRows;

    /** 物料绑定行（qxx_qc_template_product，级联全删全插+启用唯一校验） */
    private List<QcTemplateProduct> productRows;

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getTemplateCode()
    {
        return templateCode;
    }

    public void setTemplateCode(String templateCode)
    {
        this.templateCode = templateCode;
    }

    public String getTemplateName()
    {
        return templateName;
    }

    public void setTemplateName(String templateName)
    {
        this.templateName = templateName;
    }

    public String getQcTypes()
    {
        return qcTypes;
    }

    public void setQcTypes(String qcTypes)
    {
        this.qcTypes = qcTypes;
    }

    public String getEnableFlag()
    {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag)
    {
        this.enableFlag = enableFlag;
    }

    public List<QcTemplateIndex> getIndexRows()
    {
        return indexRows;
    }

    public void setIndexRows(List<QcTemplateIndex> indexRows)
    {
        this.indexRows = indexRows;
    }

    public List<QcTemplateProduct> getProductRows()
    {
        return productRows;
    }

    public void setProductRows(List<QcTemplateProduct> productRows)
    {
        this.productRows = productRows;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("templateId", getTemplateId())
            .append("factoryId", getFactoryId())
            .append("templateCode", getTemplateCode())
            .append("templateName", getTemplateName())
            .append("qcTypes", getQcTypes())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
