package com.ruoyi.system.domain.mes.qc;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 质检模板-物料绑定行对象 qxx_qc_template_product
 *
 * 说明：本表无 enable_flag/index_type 列，"启用"随头模板 enable_flag，
 * 检验维度从头模板 qc_types 判定（FIND_IN_SET）。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcTemplateProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工厂ID */
    private Long factoryId;

    /** 模板ID */
    private Long templateId;

    /** 物料ID */
    @Excel(name = "物料ID")
    private Long itemId;

    /** 物料编码 */
    @Excel(name = "物料编码")
    private String itemCode;

    /** 物料名称 */
    @Excel(name = "物料名称")
    private String itemName;

    /** 规格型号 */
    @Excel(name = "规格型号")
    private String specification;

    /** 单位 */
    @Excel(name = "单位")
    private String unitOfMeasure;

    /** 工序ID(仅IPQC工序级绑定,可空) */
    @Excel(name = "工序ID")
    private Long processId;

    /** 工序编码 */
    @Excel(name = "工序编码")
    private String processCode;

    /** 工序名称 */
    @Excel(name = "工序名称")
    private String processName;

    /** 抽检样本量 */
    @Excel(name = "抽检样本量")
    private Integer quantityCheck;

    /** 最大不合格数(Ac值,超过整批拒收) */
    @Excel(name = "最大不合格数")
    private Integer quantityUnqualified;

    /** 致命缺陷率阈值(%) */
    @Excel(name = "致命缺陷率阈值")
    private BigDecimal crRate;

    /** 严重缺陷率阈值(%) */
    @Excel(name = "严重缺陷率阈值")
    private BigDecimal majRate;

    /** 轻微缺陷率阈值(%) */
    @Excel(name = "轻微缺陷率阈值")
    private BigDecimal minRate;

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

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public String getItemCode()
    {
        return itemCode;
    }

    public void setItemCode(String itemCode)
    {
        this.itemCode = itemCode;
    }

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }

    public String getSpecification()
    {
        return specification;
    }

    public void setSpecification(String specification)
    {
        this.specification = specification;
    }

    public String getUnitOfMeasure()
    {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure)
    {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Long getProcessId()
    {
        return processId;
    }

    public void setProcessId(Long processId)
    {
        this.processId = processId;
    }

    public String getProcessCode()
    {
        return processCode;
    }

    public void setProcessCode(String processCode)
    {
        this.processCode = processCode;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public Integer getQuantityCheck()
    {
        return quantityCheck;
    }

    public void setQuantityCheck(Integer quantityCheck)
    {
        this.quantityCheck = quantityCheck;
    }

    public Integer getQuantityUnqualified()
    {
        return quantityUnqualified;
    }

    public void setQuantityUnqualified(Integer quantityUnqualified)
    {
        this.quantityUnqualified = quantityUnqualified;
    }

    public BigDecimal getCrRate()
    {
        return crRate;
    }

    public void setCrRate(BigDecimal crRate)
    {
        this.crRate = crRate;
    }

    public BigDecimal getMajRate()
    {
        return majRate;
    }

    public void setMajRate(BigDecimal majRate)
    {
        this.majRate = majRate;
    }

    public BigDecimal getMinRate()
    {
        return minRate;
    }

    public void setMinRate(BigDecimal minRate)
    {
        this.minRate = minRate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("templateId", getTemplateId())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("specification", getSpecification())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("processId", getProcessId())
            .append("processCode", getProcessCode())
            .append("processName", getProcessName())
            .append("quantityCheck", getQuantityCheck())
            .append("quantityUnqualified", getQuantityUnqualified())
            .append("crRate", getCrRate())
            .append("majRate", getMajRate())
            .append("minRate", getMinRate())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
