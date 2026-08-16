package com.ruoyi.system.domain.mes.qc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 质检缺陷字典对象 qxx_qc_defect
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcDefect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 缺陷ID */
    private Long defectId;

    /** 工厂ID */
    private Long factoryId;

    /** 缺陷编码 */
    @Excel(name = "缺陷编码")
    private String defectCode;

    /** 缺陷描述 */
    @Excel(name = "缺陷描述")
    private String defectName;

    /** 适用检验类型(IQC/IPQC/OQC/RQC) */
    @Excel(name = "适用检验类型", readConverterExp = "IQC=来料检验,IPQC=过程检验,OQC=出货检验,RQC=退料检验")
    private String indexType;

    /** 等级:CRITICAL/MAJOR/MINOR */
    @Excel(name = "等级", readConverterExp = "CRITICAL=致命缺陷,MAJOR=严重缺陷,MINOR=轻微缺陷")
    private String defectLevel;

    /** 处置方法(返工/让步接收/退货/报废) */
    @Excel(name = "处置方法")
    private String processMethod;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getDefectId()
    {
        return defectId;
    }

    public void setDefectId(Long defectId)
    {
        this.defectId = defectId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
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

    public String getIndexType()
    {
        return indexType;
    }

    public void setIndexType(String indexType)
    {
        this.indexType = indexType;
    }

    public String getDefectLevel()
    {
        return defectLevel;
    }

    public void setDefectLevel(String defectLevel)
    {
        this.defectLevel = defectLevel;
    }

    public String getProcessMethod()
    {
        return processMethod;
    }

    public void setProcessMethod(String processMethod)
    {
        this.processMethod = processMethod;
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
            .append("defectId", getDefectId())
            .append("factoryId", getFactoryId())
            .append("defectCode", getDefectCode())
            .append("defectName", getDefectName())
            .append("indexType", getIndexType())
            .append("defectLevel", getDefectLevel())
            .append("processMethod", getProcessMethod())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
