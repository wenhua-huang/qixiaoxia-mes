package com.ruoyi.system.domain.mes.qc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 过程检验单对象 qxx_qc_ipqc
 *
 * <p>两类来源：①报工确认触发的工序检（source=pro_card_process，ipqc_type=LAST_CHECK）；
 * ②成品入库触发的完工检（source=wm_product_recpt，ipqc_type=LAST_CHECK）；
 * ③手工创建的首检/巡检/抽检/完工检（qc-ipqc:add 端点）。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
public class QcIpqc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 过程检验单ID */
    private Long ipqcId;

    /** 工厂ID */
    private Long factoryId;

    /** 检验单编码 */
    @Excel(name = "检验单编码")
    private String ipqcCode;

    /** 检验单名称 */
    @Excel(name = "检验单名称")
    private String ipqcName;

    /** FIRST_CHECK/TOUR_CHECK/LAST_CHECK/SPOT_CHECK */
    @Excel(name = "检验类型", readConverterExp = "FIRST_CHECK=首检,TOUR_CHECK=巡检,LAST_CHECK=完工检,SPOT_CHECK=抽检")
    private String ipqcType;

    /** 模板ID */
    private Long templateId;

    /** 来源单据ID(pro_card_process.record_id 或 qxx_wm_product_recpt.recpt_id) */
    private Long sourceDocId;

    /** 来源类型:pro_card_process/wm_product_recpt */
    @Excel(name = "来源类型")
    private String sourceDocType;

    /** 来源单据编码 */
    @Excel(name = "来源单据编码")
    private String sourceDocCode;

    /** 来源行ID */
    private Long sourceLineId;

    /** 工单ID */
    private Long workorderId;

    /** 工单编码 */
    @Excel(name = "工单编码")
    private String workorderCode;

    /** 工单名称 */
    @Excel(name = "工单名称")
    private String workorderName;

    /** 流转卡ID */
    private Long cardId;

    /** 流转卡编码 */
    @Excel(name = "流转卡编码")
    private String cardCode;

    /** 任务ID */
    private Long taskId;

    /** 任务编码 */
    @Excel(name = "任务编码")
    private String taskCode;

    /** 工序ID */
    private Long processId;

    /** 工序编码 */
    @Excel(name = "工序编码")
    private String processCode;

    /** 工序名称 */
    @Excel(name = "工序名称")
    private String processName;

    /** 工位ID(可空) */
    private Long workstationId;

    /** 工位编码 */
    @Excel(name = "工位编码")
    private String workstationCode;

    /** 工位名称 */
    @Excel(name = "工位名称")
    private String workstationName;

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

    public Long getIpqcId()
    {
        return ipqcId;
    }

    public void setIpqcId(Long ipqcId)
    {
        this.ipqcId = ipqcId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getIpqcCode()
    {
        return ipqcCode;
    }

    public void setIpqcCode(String ipqcCode)
    {
        this.ipqcCode = ipqcCode;
    }

    public String getIpqcName()
    {
        return ipqcName;
    }

    public void setIpqcName(String ipqcName)
    {
        this.ipqcName = ipqcName;
    }

    public String getIpqcType()
    {
        return ipqcType;
    }

    public void setIpqcType(String ipqcType)
    {
        this.ipqcType = ipqcType;
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

    public Long getWorkorderId()
    {
        return workorderId;
    }

    public void setWorkorderId(Long workorderId)
    {
        this.workorderId = workorderId;
    }

    public String getWorkorderCode()
    {
        return workorderCode;
    }

    public void setWorkorderCode(String workorderCode)
    {
        this.workorderCode = workorderCode;
    }

    public String getWorkorderName()
    {
        return workorderName;
    }

    public void setWorkorderName(String workorderName)
    {
        this.workorderName = workorderName;
    }

    public Long getCardId()
    {
        return cardId;
    }

    public void setCardId(Long cardId)
    {
        this.cardId = cardId;
    }

    public String getCardCode()
    {
        return cardCode;
    }

    public void setCardCode(String cardCode)
    {
        this.cardCode = cardCode;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getTaskCode()
    {
        return taskCode;
    }

    public void setTaskCode(String taskCode)
    {
        this.taskCode = taskCode;
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

    public Long getWorkstationId()
    {
        return workstationId;
    }

    public void setWorkstationId(Long workstationId)
    {
        this.workstationId = workstationId;
    }

    public String getWorkstationCode()
    {
        return workstationCode;
    }

    public void setWorkstationCode(String workstationCode)
    {
        this.workstationCode = workstationCode;
    }

    public String getWorkstationName()
    {
        return workstationName;
    }

    public void setWorkstationName(String workstationName)
    {
        this.workstationName = workstationName;
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
            .append("ipqcId", getIpqcId())
            .append("factoryId", getFactoryId())
            .append("ipqcCode", getIpqcCode())
            .append("ipqcName", getIpqcName())
            .append("ipqcType", getIpqcType())
            .append("templateId", getTemplateId())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocType", getSourceDocType())
            .append("sourceDocCode", getSourceDocCode())
            .append("sourceLineId", getSourceLineId())
            .append("workorderId", getWorkorderId())
            .append("workorderCode", getWorkorderCode())
            .append("workorderName", getWorkorderName())
            .append("cardId", getCardId())
            .append("cardCode", getCardCode())
            .append("taskId", getTaskId())
            .append("taskCode", getTaskCode())
            .append("processId", getProcessId())
            .append("processCode", getProcessCode())
            .append("processName", getProcessName())
            .append("workstationId", getWorkstationId())
            .append("workstationCode", getWorkstationCode())
            .append("workstationName", getWorkstationName())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("specification", getSpecification())
            .append("unitOfMeasure", getUnitOfMeasure())
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
