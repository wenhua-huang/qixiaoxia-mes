package com.ruoyi.system.domain.mes.qc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 检验单行对象 qxx_qc_order_line（IQC/IPQC/OQC/RQC 四类单据共用，多态 qc_type+qc_id）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcOrderLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 行ID */
    private Long lineId;

    /** 工厂ID */
    private Long factoryId;

    /** 检验单类型:IQC/IPQC/OQC/RQC */
    private String qcType;

    /** 检验单ID(多态) */
    private Long qcId;

    /** 检测项ID */
    private Long indexId;

    /** 检测项编码 */
    private String indexCode;

    /** 检测项名称 */
    private String indexName;

    /** 检测项类型 */
    private String indexType;

    /** 检测工具 */
    private String qcTool;

    /** 值类型 */
    private String qcResultType;

    /** 检测方法 */
    private String checkMethod;

    /** 标准值(快照) */
    private BigDecimal standerVal;

    /** 单位 */
    private String unitOfMeasure;

    /** 允许下偏差(快照) */
    private BigDecimal thresholdMin;

    /** 允许上偏差(快照) */
    private BigDecimal thresholdMax;

    /** 实测值(数值型存数字文本/文本型存内容/字典型存dict_value) */
    private String checkValText;

    /** 致命缺陷数(该检测项) */
    private Integer crQuantity;

    /** 严重缺陷数 */
    private Integer majQuantity;

    /** 轻微缺陷数 */
    private Integer minQuantity;

    /** 行结果:PASS/FAIL */
    private String lineResult;

    /** 排序 */
    private Integer orderNum;

    public Long getLineId()
    {
        return lineId;
    }

    public void setLineId(Long lineId)
    {
        this.lineId = lineId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getQcType()
    {
        return qcType;
    }

    public void setQcType(String qcType)
    {
        this.qcType = qcType;
    }

    public Long getQcId()
    {
        return qcId;
    }

    public void setQcId(Long qcId)
    {
        this.qcId = qcId;
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

    public String getCheckValText()
    {
        return checkValText;
    }

    public void setCheckValText(String checkValText)
    {
        this.checkValText = checkValText;
    }

    public Integer getCrQuantity()
    {
        return crQuantity;
    }

    public void setCrQuantity(Integer crQuantity)
    {
        this.crQuantity = crQuantity;
    }

    public Integer getMajQuantity()
    {
        return majQuantity;
    }

    public void setMajQuantity(Integer majQuantity)
    {
        this.majQuantity = majQuantity;
    }

    public Integer getMinQuantity()
    {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity)
    {
        this.minQuantity = minQuantity;
    }

    public String getLineResult()
    {
        return lineResult;
    }

    public void setLineResult(String lineResult)
    {
        this.lineResult = lineResult;
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
            .append("lineId", getLineId())
            .append("factoryId", getFactoryId())
            .append("qcType", getQcType())
            .append("qcId", getQcId())
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
            .append("checkValText", getCheckValText())
            .append("crQuantity", getCrQuantity())
            .append("majQuantity", getMajQuantity())
            .append("minQuantity", getMinQuantity())
            .append("lineResult", getLineResult())
            .append("orderNum", getOrderNum())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
