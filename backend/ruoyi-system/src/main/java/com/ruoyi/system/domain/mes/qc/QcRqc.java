package com.ruoyi.system.domain.mes.qc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 退料检验单对象 qxx_qc_rqc
 *
 * <p>来源：生产退料单（wm_rt_issue）执行退库前自动生成；一期仅 PROD_RETURN（生产退料），
 * PURCHASE_RETURN/QC_REJECT 为预留子类型。判定合格后由退料单核验放行并退回仓库。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
public class QcRqc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 退料检验单ID */
    private Long rqcId;

    /** 工厂ID */
    private Long factoryId;

    /** 检验单编码 */
    @Excel(name = "检验单编码")
    private String rqcCode;

    /** 检验单名称 */
    @Excel(name = "检验单名称")
    private String rqcName;

    /** PROD_RETURN/PURCHASE_RETURN/QC_REJECT */
    @Excel(name = "退料类型", readConverterExp = "PROD_RETURN=生产退料,PURCHASE_RETURN=采购退货,QC_REJECT=质检退货")
    private String rqcType;

    /** 模板ID */
    private Long templateId;

    /** 来源单据ID(qxx_wm_rt_issue.rt_id) */
    private Long sourceDocId;

    /** 来源类型 */
    @Excel(name = "来源类型")
    private String sourceDocType;

    /** 来源单据编码 */
    @Excel(name = "来源单据编码")
    private String sourceDocCode;

    /** 来源行ID */
    private Long sourceLineId;

    /** 工单ID(生产退料) */
    private Long workorderId;

    /** 工单编码 */
    @Excel(name = "工单编码")
    private String workorderCode;

    /** 供应商ID(采购退货) */
    private Long vendorId;

    /** 供应商编码 */
    @Excel(name = "供应商编码")
    private String vendorCode;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String vendorName;

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

    /** 批次号 */
    @Excel(name = "批次号")
    private String batchCode;

    /** 实际检测数量 */
    @Excel(name = "实际检测数量")
    private BigDecimal quantityCheck;

    /** 抽检样本量(模板快照) */
    @Excel(name = "抽检样本量")
    private Integer quantityMinCheck;

    /** Ac值(模板快照) */
    @Excel(name = "最大不合格数")
    private Integer quantityMaxUnqualified;

    /** 合格数 */
    @Excel(name = "合格数")
    private BigDecimal quantityQualified;

    /** 不合格数 */
    @Excel(name = "不合格数")
    private BigDecimal quantityUnqualified;

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

    /** 退料原因 */
    @Excel(name = "退料原因")
    private String returnReason;

    /** 责任归属:SUPPLIER/PRODUCTION/STORAGE/OTHER */
    @Excel(name = "责任归属", readConverterExp = "SUPPLIER=供应商,PRODUCTION=生产,STORAGE=仓储,OTHER=其他")
    private String responsibility;

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

    public Long getRqcId() { return rqcId; }
    public void setRqcId(Long rqcId) { this.rqcId = rqcId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public String getRqcCode() { return rqcCode; }
    public void setRqcCode(String rqcCode) { this.rqcCode = rqcCode; }

    public String getRqcName() { return rqcName; }
    public void setRqcName(String rqcName) { this.rqcName = rqcName; }

    public String getRqcType() { return rqcType; }
    public void setRqcType(String rqcType) { this.rqcType = rqcType; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long sourceDocId) { this.sourceDocId = sourceDocId; }

    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String sourceDocType) { this.sourceDocType = sourceDocType; }

    public String getSourceDocCode() { return sourceDocCode; }
    public void setSourceDocCode(String sourceDocCode) { this.sourceDocCode = sourceDocCode; }

    public Long getSourceLineId() { return sourceLineId; }
    public void setSourceLineId(Long sourceLineId) { this.sourceLineId = sourceLineId; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long workorderId) { this.workorderId = workorderId; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String workorderCode) { this.workorderCode = workorderCode; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public BigDecimal getQuantityCheck() { return quantityCheck; }
    public void setQuantityCheck(BigDecimal quantityCheck) { this.quantityCheck = quantityCheck; }

    public Integer getQuantityMinCheck() { return quantityMinCheck; }
    public void setQuantityMinCheck(Integer quantityMinCheck) { this.quantityMinCheck = quantityMinCheck; }

    public Integer getQuantityMaxUnqualified() { return quantityMaxUnqualified; }
    public void setQuantityMaxUnqualified(Integer quantityMaxUnqualified) { this.quantityMaxUnqualified = quantityMaxUnqualified; }

    public BigDecimal getQuantityQualified() { return quantityQualified; }
    public void setQuantityQualified(BigDecimal quantityQualified) { this.quantityQualified = quantityQualified; }

    public BigDecimal getQuantityUnqualified() { return quantityUnqualified; }
    public void setQuantityUnqualified(BigDecimal quantityUnqualified) { this.quantityUnqualified = quantityUnqualified; }

    public BigDecimal getCrRateLimit() { return crRateLimit; }
    public void setCrRateLimit(BigDecimal crRateLimit) { this.crRateLimit = crRateLimit; }

    public BigDecimal getMajRateLimit() { return majRateLimit; }
    public void setMajRateLimit(BigDecimal majRateLimit) { this.majRateLimit = majRateLimit; }

    public BigDecimal getMinRateLimit() { return minRateLimit; }
    public void setMinRateLimit(BigDecimal minRateLimit) { this.minRateLimit = minRateLimit; }

    public Integer getCrQuantity() { return crQuantity; }
    public void setCrQuantity(Integer crQuantity) { this.crQuantity = crQuantity; }

    public Integer getMajQuantity() { return majQuantity; }
    public void setMajQuantity(Integer majQuantity) { this.majQuantity = majQuantity; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public BigDecimal getCrRate() { return crRate; }
    public void setCrRate(BigDecimal crRate) { this.crRate = crRate; }

    public BigDecimal getMajRate() { return majRate; }
    public void setMajRate(BigDecimal majRate) { this.majRate = majRate; }

    public BigDecimal getMinRate() { return minRate; }
    public void setMinRate(BigDecimal minRate) { this.minRate = minRate; }

    public String getCheckResult() { return checkResult; }
    public void setCheckResult(String checkResult) { this.checkResult = checkResult; }

    public String getConcessionReason() { return concessionReason; }
    public void setConcessionReason(String concessionReason) { this.concessionReason = concessionReason; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public String getResponsibility() { return responsibility; }
    public void setResponsibility(String responsibility) { this.responsibility = responsibility; }

    public Date getInspectDate() { return inspectDate; }
    public void setInspectDate(Date inspectDate) { this.inspectDate = inspectDate; }

    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<QcOrderLine> getLines() { return lines; }
    public void setLines(List<QcOrderLine> lines) { this.lines = lines; }

    public List<QcDefectRecord> getDefectRecords() { return defectRecords; }
    public void setDefectRecords(List<QcDefectRecord> defectRecords) { this.defectRecords = defectRecords; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("rqcId", getRqcId())
            .append("factoryId", getFactoryId())
            .append("rqcCode", getRqcCode())
            .append("rqcName", getRqcName())
            .append("rqcType", getRqcType())
            .append("templateId", getTemplateId())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocType", getSourceDocType())
            .append("sourceDocCode", getSourceDocCode())
            .append("sourceLineId", getSourceLineId())
            .append("workorderId", getWorkorderId())
            .append("workorderCode", getWorkorderCode())
            .append("vendorId", getVendorId())
            .append("vendorCode", getVendorCode())
            .append("vendorName", getVendorName())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("specification", getSpecification())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("batchCode", getBatchCode())
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
            .append("returnReason", getReturnReason())
            .append("responsibility", getResponsibility())
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
