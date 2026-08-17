package com.ruoyi.system.domain.mes.qc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 出货检验单对象 qxx_qc_oqc
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcOqc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 出货检验单ID */
    private Long oqcId;

    /** 工厂ID */
    private Long factoryId;

    /** 检验单编码 */
    @Excel(name = "检验单编码")
    private String oqcCode;

    /** 检验单名称 */
    @Excel(name = "检验单名称")
    private String oqcName;

    /** 模板ID */
    private Long templateId;

    /** 来源单据ID(qxx_wm_product_sales.sales_id) */
    private Long sourceDocId;

    /** 来源类型 */
    @Excel(name = "来源类型")
    private String sourceDocType;

    /** 来源单据编码 */
    @Excel(name = "来源单据编码")
    private String sourceDocCode;

    /** 来源行ID */
    private Long sourceLineId;

    /** 客户ID */
    private Long clientId;

    /** 客户编码 */
    @Excel(name = "客户编码")
    private String clientCode;

    /** 客户名称 */
    @Excel(name = "客户名称")
    private String clientName;

    /** 批次号 */
    @Excel(name = "批次号")
    private String batchCode;

    /** 物料ID */
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

    /** 发货数量 */
    @Excel(name = "发货数量")
    private BigDecimal quantityOut;

    /** 本次实际检测数量 */
    @Excel(name = "本次实际检测数量")
    private Integer quantityCheck;

    /** 抽检样本量(模板快照) */
    @Excel(name = "抽检样本量")
    private Integer quantityMinCheck;

    /** Ac值(模板快照) */
    @Excel(name = "最大不合格数")
    private Integer quantityMaxUnqualified;

    /** 合格数 */
    @Excel(name = "合格数")
    private Integer quantityQualified;

    /** 不合格数 */
    @Excel(name = "不合格数")
    private Integer quantityUnqualified;

    /** 致命缺陷率阈值(模板快照,%) */
    @Excel(name = "致命缺陷率阈值")
    private BigDecimal crRateLimit;

    /** 严重缺陷率阈值(模板快照,%) */
    @Excel(name = "严重缺陷率阈值")
    private BigDecimal majRateLimit;

    /** 轻微缺陷率阈值(模板快照,%) */
    @Excel(name = "轻微缺陷率阈值")
    private BigDecimal minRateLimit;

    /** 致命缺陷数(判定汇总) */
    @Excel(name = "致命缺陷数")
    private Integer crQuantity;

    /** 严重缺陷数 */
    @Excel(name = "严重缺陷数")
    private Integer majQuantity;

    /** 轻微缺陷数 */
    @Excel(name = "轻微缺陷数")
    private Integer minQuantity;

    /** 致命缺陷率(判定汇总,%) */
    @Excel(name = "致命缺陷率")
    private BigDecimal crRate;

    /** 严重缺陷率(%) */
    @Excel(name = "严重缺陷率")
    private BigDecimal majRate;

    /** 轻微缺陷率(%) */
    @Excel(name = "轻微缺陷率")
    private BigDecimal minRate;

    /** PASS/FAIL/CONCESSION */
    @Excel(name = "判定结果", readConverterExp = "PASS=合格,FAIL=不合格,CONCESSION=让步接收")
    private String checkResult;

    /** 让步理由(CONCESSION必填) */
    @Excel(name = "让步理由")
    private String concessionReason;

    /** 出货日期 */
    @Excel(name = "出货日期")
    private Date outDate;

    /** 检验日期 */
    @Excel(name = "检验日期")
    private Date inspectDate;

    /** 检验员 */
    @Excel(name = "检验员")
    private String inspector;

    /** PENDING/INSPECTING/COMPLETED/CLOSED */
    @Excel(name = "单据状态", readConverterExp = "PENDING=待检验,INSPECTING=检验中,COMPLETED=已完成,CLOSED=已关闭")
    private String status;

    /** 检验单行(非表字段，getInfo 组装返回) */
    private List<QcOrderLine> lines;

    /** 缺陷记录(非表字段，getInfo 组装返回) */
    private List<QcDefectRecord> defectRecords;

    public Long getOqcId()
    {
        return oqcId;
    }

    public void setOqcId(Long oqcId)
    {
        this.oqcId = oqcId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getOqcCode()
    {
        return oqcCode;
    }

    public void setOqcCode(String oqcCode)
    {
        this.oqcCode = oqcCode;
    }

    public String getOqcName()
    {
        return oqcName;
    }

    public void setOqcName(String oqcName)
    {
        this.oqcName = oqcName;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getSourceDocId()
    {
        return sourceDocId;
    }

    public void setSourceDocId(Long sourceDocId)
    {
        this.sourceDocId = sourceDocId;
    }

    public String getSourceDocType()
    {
        return sourceDocType;
    }

    public void setSourceDocType(String sourceDocType)
    {
        this.sourceDocType = sourceDocType;
    }

    public String getSourceDocCode()
    {
        return sourceDocCode;
    }

    public void setSourceDocCode(String sourceDocCode)
    {
        this.sourceDocCode = sourceDocCode;
    }

    public Long getSourceLineId()
    {
        return sourceLineId;
    }

    public void setSourceLineId(Long sourceLineId)
    {
        this.sourceLineId = sourceLineId;
    }

    public Long getClientId()
    {
        return clientId;
    }

    public void setClientId(Long clientId)
    {
        this.clientId = clientId;
    }

    public String getClientCode()
    {
        return clientCode;
    }

    public void setClientCode(String clientCode)
    {
        this.clientCode = clientCode;
    }

    public String getClientName()
    {
        return clientName;
    }

    public void setClientName(String clientName)
    {
        this.clientName = clientName;
    }

    public String getBatchCode()
    {
        return batchCode;
    }

    public void setBatchCode(String batchCode)
    {
        this.batchCode = batchCode;
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

    public BigDecimal getQuantityOut()
    {
        return quantityOut;
    }

    public void setQuantityOut(BigDecimal quantityOut)
    {
        this.quantityOut = quantityOut;
    }

    public Integer getQuantityCheck()
    {
        return quantityCheck;
    }

    public void setQuantityCheck(Integer quantityCheck)
    {
        this.quantityCheck = quantityCheck;
    }

    public Integer getQuantityMinCheck()
    {
        return quantityMinCheck;
    }

    public void setQuantityMinCheck(Integer quantityMinCheck)
    {
        this.quantityMinCheck = quantityMinCheck;
    }

    public Integer getQuantityMaxUnqualified()
    {
        return quantityMaxUnqualified;
    }

    public void setQuantityMaxUnqualified(Integer quantityMaxUnqualified)
    {
        this.quantityMaxUnqualified = quantityMaxUnqualified;
    }

    public Integer getQuantityQualified()
    {
        return quantityQualified;
    }

    public void setQuantityQualified(Integer quantityQualified)
    {
        this.quantityQualified = quantityQualified;
    }

    public Integer getQuantityUnqualified()
    {
        return quantityUnqualified;
    }

    public void setQuantityUnqualified(Integer quantityUnqualified)
    {
        this.quantityUnqualified = quantityUnqualified;
    }

    public BigDecimal getCrRateLimit()
    {
        return crRateLimit;
    }

    public void setCrRateLimit(BigDecimal crRateLimit)
    {
        this.crRateLimit = crRateLimit;
    }

    public BigDecimal getMajRateLimit()
    {
        return majRateLimit;
    }

    public void setMajRateLimit(BigDecimal majRateLimit)
    {
        this.majRateLimit = majRateLimit;
    }

    public BigDecimal getMinRateLimit()
    {
        return minRateLimit;
    }

    public void setMinRateLimit(BigDecimal minRateLimit)
    {
        this.minRateLimit = minRateLimit;
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

    public String getCheckResult()
    {
        return checkResult;
    }

    public void setCheckResult(String checkResult)
    {
        this.checkResult = checkResult;
    }

    public String getConcessionReason()
    {
        return concessionReason;
    }

    public void setConcessionReason(String concessionReason)
    {
        this.concessionReason = concessionReason;
    }

    public Date getOutDate()
    {
        return outDate;
    }

    public void setOutDate(Date outDate)
    {
        this.outDate = outDate;
    }

    public Date getInspectDate()
    {
        return inspectDate;
    }

    public void setInspectDate(Date inspectDate)
    {
        this.inspectDate = inspectDate;
    }

    public String getInspector()
    {
        return inspector;
    }

    public void setInspector(String inspector)
    {
        this.inspector = inspector;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<QcOrderLine> getLines()
    {
        return lines;
    }

    public void setLines(List<QcOrderLine> lines)
    {
        this.lines = lines;
    }

    public List<QcDefectRecord> getDefectRecords()
    {
        return defectRecords;
    }

    public void setDefectRecords(List<QcDefectRecord> defectRecords)
    {
        this.defectRecords = defectRecords;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("oqcId", getOqcId())
            .append("factoryId", getFactoryId())
            .append("oqcCode", getOqcCode())
            .append("oqcName", getOqcName())
            .append("templateId", getTemplateId())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocType", getSourceDocType())
            .append("sourceDocCode", getSourceDocCode())
            .append("sourceLineId", getSourceLineId())
            .append("clientId", getClientId())
            .append("clientCode", getClientCode())
            .append("clientName", getClientName())
            .append("batchCode", getBatchCode())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("specification", getSpecification())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("quantityOut", getQuantityOut())
            .append("quantityCheck", getQuantityCheck())
            .append("quantityMinCheck", getQuantityMinCheck())
            .append("quantityMaxUnqualified", getQuantityMaxUnqualified())
            .append("quantityQualified", getQuantityQualified())
            .append("quantityUnqualified", getQuantityUnqualified())
            .append("crRateLimit", getCrRateLimit())
            .append("majRateLimit", getMajRateLimit())
            .append("minRateLimit", getMinRateLimit())
            .append("crQuantity", getCrQuantity())
            .append("majQuantity", getMajQuantity())
            .append("minQuantity", getMinQuantity())
            .append("crRate", getCrRate())
            .append("majRate", getMajRate())
            .append("minRate", getMinRate())
            .append("checkResult", getCheckResult())
            .append("concessionReason", getConcessionReason())
            .append("outDate", getOutDate())
            .append("inspectDate", getInspectDate())
            .append("inspector", getInspector())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
