package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 生产异常单对象 qxx_pro_exception
 *
 * <p>四类异常（质量数量/缺料/进度延迟/客户退货）同表，专属字段为可空列；
 * 一期关联对象恒为工序任务（target_type=TASK）。
 *
 * @author qixiaoxia
 */
public class ProException extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long exceptionId;

    /** 工厂ID */
    private Long factoryId;

    /** 异常单号 EX20260902-014 */
    @Excel(name = "异常单号")
    private String exceptionCode;

    /** 异常类型 QUALITY/MATERIAL/DELAY/RETURN */
    @Excel(name = "异常类型", dictType = "mes_pro_exception_type")
    private String exceptionType;

    /** 状态 OPEN/PROCESSING/CLOSED */
    @Excel(name = "状态", dictType = "mes_pro_exception_status")
    private String status;

    /** 责任方 FACTORY/SUPPLIER/CUSTOMER/PENDING */
    @Excel(name = "责任方", dictType = "mes_pro_exception_party")
    private String responsibleParty;

    // ---- 关联快照 ----
    private Long workorderId;
    @Excel(name = "工单号")
    private String workorderCode;
    private String workorderName;
    private Long taskId;
    @Excel(name = "任务编号")
    private String taskCode;
    private Long processId;
    private String processCode;
    @Excel(name = "工序")
    private String processName;
    /** 关联对象类型（预留，一期恒 TASK） */
    private String targetType;
    private Long targetId;
    private String targetCode;

    // ---- 上报信息 ----
    private Long reporterId;
    @Excel(name = "上报人")
    private String reporterName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发生时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date occurTime;

    /** 影响数量 */
    @Excel(name = "影响数量")
    private BigDecimal impactQuantity;

    /** 异常情况说明 */
    @Excel(name = "异常说明")
    private String description;

    /** 现场照片（MinIO URL 逗号串） */
    private String sceneImages;

    // ---- 质量数量异常专属 ----
    /** 质量小类 BROKEN 做坏了 / SHORT 做少了 */
    private String qualitySubclass;
    /** 可使用数量 */
    private BigDecimal usableQuantity;
    /** 是否需要返工 Y/N */
    private String needRework;

    // ---- 缺料异常专属 ----
    private Long itemId;
    private String itemCode;
    private String itemName;
    private BigDecimal shortageQuantity;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expectedArrivalDate;

    // ---- 进度延迟异常专属 ----
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date originalPlanTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date newExpectedTime;
    private String delayReason;

    // ---- 客户退货异常专属 ----
    private BigDecimal returnQuantity;
    private String returnReason;
    /** 客户是否接受返工 Y/N */
    private String customerAcceptRework;

    // ---- 处理与关闭 ----
    /** 处理结论 */
    @Excel(name = "处理结论")
    private String conclusion;

    /** 出口动作 REWORK/REMAKE/PURCHASE/RESCHEDULE/SCRAP/CONCESSION/REFUND */
    @Excel(name = "出口动作", dictType = "mes_pro_exception_resolve")
    private String resolveType;

    private String resolveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resolveTime;

    /** 处理产生单据类型 TASK/PUR_ORDER */
    private String targetDocType;
    private Long targetDocId;
    @Excel(name = "处理单据号")
    private String targetDocCode;

    private BigDecimal scrapQuantity;
    private String closeBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "关闭时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    /** 选出口弹窗输入的返工/补做任务数量（非持久化，仅 resolve 请求体使用） */
    private transient BigDecimal resolveQuantity;

    /** 处理单据当前状态（非持久化，详情按 targetDocType 聚合任务/采购单状态回填，E4 验收用） */
    private transient String targetDocStatus;

    public Long getExceptionId() { return exceptionId; }
    public void setExceptionId(Long v) { this.exceptionId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getExceptionCode() { return exceptionCode; }
    public void setExceptionCode(String v) { this.exceptionCode = v; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String v) { this.exceptionType = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getResponsibleParty() { return responsibleParty; }
    public void setResponsibleParty(String v) { this.responsibleParty = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String v) { this.taskCode = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long v) { this.targetId = v; }
    public String getTargetCode() { return targetCode; }
    public void setTargetCode(String v) { this.targetCode = v; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long v) { this.reporterId = v; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String v) { this.reporterName = v; }
    public Date getOccurTime() { return occurTime; }
    public void setOccurTime(Date v) { this.occurTime = v; }
    public BigDecimal getImpactQuantity() { return impactQuantity; }
    public void setImpactQuantity(BigDecimal v) { this.impactQuantity = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getSceneImages() { return sceneImages; }
    public void setSceneImages(String v) { this.sceneImages = v; }
    public String getQualitySubclass() { return qualitySubclass; }
    public void setQualitySubclass(String v) { this.qualitySubclass = v; }
    public BigDecimal getUsableQuantity() { return usableQuantity; }
    public void setUsableQuantity(BigDecimal v) { this.usableQuantity = v; }
    public String getNeedRework() { return needRework; }
    public void setNeedRework(String v) { this.needRework = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public BigDecimal getShortageQuantity() { return shortageQuantity; }
    public void setShortageQuantity(BigDecimal v) { this.shortageQuantity = v; }
    public Date getExpectedArrivalDate() { return expectedArrivalDate; }
    public void setExpectedArrivalDate(Date v) { this.expectedArrivalDate = v; }
    public Date getOriginalPlanTime() { return originalPlanTime; }
    public void setOriginalPlanTime(Date v) { this.originalPlanTime = v; }
    public Date getNewExpectedTime() { return newExpectedTime; }
    public void setNewExpectedTime(Date v) { this.newExpectedTime = v; }
    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String v) { this.delayReason = v; }
    public BigDecimal getReturnQuantity() { return returnQuantity; }
    public void setReturnQuantity(BigDecimal v) { this.returnQuantity = v; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String v) { this.returnReason = v; }
    public String getCustomerAcceptRework() { return customerAcceptRework; }
    public void setCustomerAcceptRework(String v) { this.customerAcceptRework = v; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String v) { this.conclusion = v; }
    public String getResolveType() { return resolveType; }
    public void setResolveType(String v) { this.resolveType = v; }
    public String getResolveBy() { return resolveBy; }
    public void setResolveBy(String v) { this.resolveBy = v; }
    public Date getResolveTime() { return resolveTime; }
    public void setResolveTime(Date v) { this.resolveTime = v; }
    public String getTargetDocType() { return targetDocType; }
    public void setTargetDocType(String v) { this.targetDocType = v; }
    public Long getTargetDocId() { return targetDocId; }
    public void setTargetDocId(Long v) { this.targetDocId = v; }
    public String getTargetDocCode() { return targetDocCode; }
    public void setTargetDocCode(String v) { this.targetDocCode = v; }
    public BigDecimal getScrapQuantity() { return scrapQuantity; }
    public void setScrapQuantity(BigDecimal v) { this.scrapQuantity = v; }
    public String getCloseBy() { return closeBy; }
    public void setCloseBy(String v) { this.closeBy = v; }
    public Date getCloseTime() { return closeTime; }
    public void setCloseTime(Date v) { this.closeTime = v; }
    public BigDecimal getResolveQuantity() { return resolveQuantity; }
    public void setResolveQuantity(BigDecimal v) { this.resolveQuantity = v; }
    public String getTargetDocStatus() { return targetDocStatus; }
    public void setTargetDocStatus(String v) { this.targetDocStatus = v; }
}
