package com.ruoyi.system.domain.mes.qc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 检验缺陷记录对象 qxx_qc_defect_record（IQC/IPQC/OQC/RQC 共用，多态 qc_type+qc_id）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcDefectRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工厂ID */
    private Long factoryId;

    /** IQC/IPQC/OQC/RQC */
    private String qcType;

    /** 检验单ID */
    private Long qcId;

    /** 检验单行ID */
    private Long lineId;

    /** 缺陷字典ID */
    private Long defectId;

    /** 缺陷编码 */
    private String defectCode;

    /** 缺陷描述 */
    private String defectName;

    /** CRITICAL/MAJOR/MINOR */
    private String defectLevel;

    /** 缺陷数量(不合格样品数) */
    private Integer defectQuantity;

    /** 处置方法 */
    private String processMethod;

    /** 缺陷图片URL */
    private String defectImage;

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

    public Long getLineId()
    {
        return lineId;
    }

    public void setLineId(Long lineId)
    {
        this.lineId = lineId;
    }

    public Long getDefectId()
    {
        return defectId;
    }

    public void setDefectId(Long defectId)
    {
        this.defectId = defectId;
    }

    public String getDefectCode()
    {
        return defectCode;
    }

    public void setDefectCode(String defectCode)
    {
        this.defectCode = defectCode;
    }

    public String getDefectName()
    {
        return defectName;
    }

    public void setDefectName(String defectName)
    {
        this.defectName = defectName;
    }

    public String getDefectLevel()
    {
        return defectLevel;
    }

    public void setDefectLevel(String defectLevel)
    {
        this.defectLevel = defectLevel;
    }

    public Integer getDefectQuantity()
    {
        return defectQuantity;
    }

    public void setDefectQuantity(Integer defectQuantity)
    {
        this.defectQuantity = defectQuantity;
    }

    public String getProcessMethod()
    {
        return processMethod;
    }

    public void setProcessMethod(String processMethod)
    {
        this.processMethod = processMethod;
    }

    public String getDefectImage()
    {
        return defectImage;
    }

    public void setDefectImage(String defectImage)
    {
        this.defectImage = defectImage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("qcType", getQcType())
            .append("qcId", getQcId())
            .append("lineId", getLineId())
            .append("defectId", getDefectId())
            .append("defectCode", getDefectCode())
            .append("defectName", getDefectName())
            .append("defectLevel", getDefectLevel())
            .append("defectQuantity", getDefectQuantity())
            .append("processMethod", getProcessMethod())
            .append("defectImage", getDefectImage())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
