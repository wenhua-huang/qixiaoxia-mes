package com.ruoyi.system.domain.mes.qc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 质检检测项对象 qxx_qc_index
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcIndex extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 检测项ID */
    private Long indexId;

    /** 工厂ID */
    private Long factoryId;

    /** 检测项编码 */
    @Excel(name = "检测项编码")
    private String indexCode;

    /** 检测项名称 */
    @Excel(name = "检测项名称")
    private String indexName;

    /** 检测项类型(IQC/IPQC/OQC/RQC) */
    @Excel(name = "检测项类型", readConverterExp = "IQC=来料检验,IPQC=过程检验,OQC=出货检验,RQC=退料检验")
    private String indexType;

    /** 检测工具 */
    @Excel(name = "检测工具")
    private String qcTool;

    /** 值类型:NUMBER/TEXT/DICT/FILE/COUNT */
    @Excel(name = "值类型", readConverterExp = "NUMBER=数值,TEXT=文本,DICT=字典,FILE=文件,COUNT=计数")
    private String qcResultType;

    /** 字典型关联的sys_dict_type(DICT型必填) */
    private String dictType;

    /** 值属性(如长度mm/色差ΔE) */
    @Excel(name = "值属性")
    private String qcResultSpc;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getIndexId()
    {
        return indexId;
    }

    public void setIndexId(Long indexId)
    {
        this.indexId = indexId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
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

    public String getDictType()
    {
        return dictType;
    }

    public void setDictType(String dictType)
    {
        this.dictType = dictType;
    }

    public String getQcResultSpc()
    {
        return qcResultSpc;
    }

    public void setQcResultSpc(String qcResultSpc)
    {
        this.qcResultSpc = qcResultSpc;
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
            .append("indexId", getIndexId())
            .append("factoryId", getFactoryId())
            .append("indexCode", getIndexCode())
            .append("indexName", getIndexName())
            .append("indexType", getIndexType())
            .append("qcTool", getQcTool())
            .append("qcResultType", getQcResultType())
            .append("dictType", getDictType())
            .append("qcResultSpc", getQcResultSpc())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
