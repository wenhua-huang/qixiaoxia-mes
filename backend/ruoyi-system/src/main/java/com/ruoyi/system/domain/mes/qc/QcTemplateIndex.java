package com.ruoyi.system.domain.mes.qc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 质检模板-检测项行对象 qxx_qc_template_index
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcTemplateIndex extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工厂ID */
    private Long factoryId;

    /** 模板ID */
    private Long templateId;

    /** 检测项ID */
    @Excel(name = "检测项ID")
    private Long indexId;

    /** 检测项编码 */
    @Excel(name = "检测项编码")
    private String indexCode;

    /** 检测项名称 */
    @Excel(name = "检测项名称")
    private String indexName;

    /** 检测项类型 */
    @Excel(name = "检测项类型", readConverterExp = "IQC=来料检验,IPQC=过程检验,OQC=出货检验,RQC=退料检验")
    private String indexType;

    /** 检测工具 */
    @Excel(name = "检测工具")
    private String qcTool;

    /** 值类型:NUMBER/TEXT/DICT/FILE/COUNT */
    @Excel(name = "值类型", readConverterExp = "NUMBER=数值,TEXT=文本,DICT=字典,FILE=文件,COUNT=计数")
    private String qcResultType;

    /** 检测方法/要求 */
    @Excel(name = "检测方法")
    private String checkMethod;

    /** 标准值(数值型) */
    @Excel(name = "标准值")
    private BigDecimal standerVal;

    /** 单位 */
    @Excel(name = "单位")
    private String unitOfMeasure;

    /** 允许下偏差(实测<标准+下偏差=不合格) */
    @Excel(name = "允许下偏差")
    private BigDecimal thresholdMin;

    /** 允许上偏差(实测>标准+上偏差=不合格) */
    @Excel(name = "允许上偏差")
    private BigDecimal thresholdMax;

    /** 排序 */
    @Excel(name = "排序")
    private Integer orderNum;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getIndexId()
    {
        return indexId;
    }

    public void setIndexId(Long indexId)
    {
        this.indexId = indexId;
    }

    public String getIndexCode()
    {
        return indexCode;
    }

    public void setIndexCode(String indexCode)
    {
        this.indexCode = indexCode;
    }

    public String getIndexName()
    {
        return indexName;
    }

    public void setIndexName(String indexName)
    {
        this.indexName = indexName;
    }

    public String getIndexType()
    {
        return indexType;
    }

    public void setIndexType(String indexType)
    {
        this.indexType = indexType;
    }

    public String getQcTool()
    {
        return qcTool;
    }

    public void setQcTool(String qcTool)
    {
        this.qcTool = qcTool;
    }

    public String getQcResultType()
    {
        return qcResultType;
    }

    public void setQcResultType(String qcResultType)
    {
        this.qcResultType = qcResultType;
    }

    public String getCheckMethod()
    {
        return checkMethod;
    }

    public void setCheckMethod(String checkMethod)
    {
        this.checkMethod = checkMethod;
    }

    public BigDecimal getStanderVal()
    {
        return standerVal;
    }

    public void setStanderVal(BigDecimal standerVal)
    {
        this.standerVal = standerVal;
    }

    public String getUnitOfMeasure()
    {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure)
    {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getThresholdMin()
    {
        return thresholdMin;
    }

    public void setThresholdMin(BigDecimal thresholdMin)
    {
        this.thresholdMin = thresholdMin;
    }

    public BigDecimal getThresholdMax()
    {
        return thresholdMax;
    }

    public void setThresholdMax(BigDecimal thresholdMax)
    {
        this.thresholdMax = thresholdMax;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("templateId", getTemplateId())
            .append("indexId", getIndexId())
            .append("indexCode", getIndexCode())
            .append("indexName", getIndexName())
            .append("indexType", getIndexType())
            .append("qcTool", getQcTool())
            .append("qcResultType", getQcResultType())
            .append("checkMethod", getCheckMethod())
            .append("standerVal", getStanderVal())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("thresholdMin", getThresholdMin())
            .append("thresholdMax", getThresholdMax())
            .append("orderNum", getOrderNum())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
