package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 报工记录对象 qxx_pro_feedback
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProFeedback extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "报工类型") private String feedbackType;
    @Excel(name = "报工编码") private String feedbackCode;
    @Excel(name = "工位ID") private Long workstationId;
    @Excel(name = "工位编码") private String workstationCode;
    @Excel(name = "工位名称") private String workstationName;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "工单编码") private String workorderCode;
    @Excel(name = "工单名称") private String workorderName;
    @Excel(name = "路线ID") private Long routeId;
    @Excel(name = "路线编码") private String routeCode;
    @Excel(name = "工序ID") private Long processId;
    @Excel(name = "工序编码") private String processCode;
    @Excel(name = "工序名称") private String processName;
    @Excel(name = "任务ID") private Long taskId;
    @Excel(name = "任务编码") private String taskCode;
    @Excel(name = "物料ID") private Long itemId;
    @Excel(name = "物料编码") private String itemCode;
    @Excel(name = "物料名称") private String itemName;
    @Excel(name = "主单位编码") private String unitOfMeasure;
    @Excel(name = "主单位名称") private String unitName;
    @Excel(name = "规格型号") private String specification;
    @Excel(name = "供应商ID") private Long vendorId;
    @Excel(name = "供应商编码") private String vendorCode;
    @Excel(name = "供应商名称") private String vendorName;
    @Excel(name = "外协工厂ID") private Long outsourceFactoryId;
    @Excel(name = "外协发料ID") private Long outsourceIssueId;
    @Excel(name = "外协发料编码") private String outsourceIssueCode;
    @Excel(name = "外协收货ID") private Long outsourceRecptId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "有效期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expireDate;
    @Excel(name = "批次号") private String lotNumber;
    @Excel(name = "数量") private BigDecimal quantity;
    @Excel(name = "报工数量") private BigDecimal quantityFeedback;
    @Excel(name = "合格数量") private BigDecimal quantityQualified;
    @Excel(name = "不合格数量") private BigDecimal quantityUnqualified;
    @Excel(name = "待检数量") private BigDecimal quantityUncheck;
    @Excel(name = "人工报废数量") private BigDecimal quantityLaborScrap;
    @Excel(name = "物料报废数量") private BigDecimal quantityMaterialScrap;
    @Excel(name = "其他报废数量") private BigDecimal quantityOtherScrap;
    @Excel(name = "报工人") private String userName;
    @Excel(name = "报工人昵称") private String nickName;
    @Excel(name = "报工渠道") private String feedbackChannel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "报工时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date feedbackTime;
    @Excel(name = "录入人") private String recordUser;
    @Excel(name = "录入人昵称") private String recordNick;
    @Excel(name = "状态") private String status;

    /** 物料消耗列表（transient，由Service层手动填充） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ProFeedbackConsume> consumeList;

    /** 报工参数列表（transient，由Service层手动填充，含偏差判定结果） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ProFeedbackParam> paramList;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long v) { this.recordId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String v) { this.feedbackType = v; }
    public String getFeedbackCode() { return feedbackCode; }
    public void setFeedbackCode(String v) { this.feedbackCode = v; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }
    public String getWorkstationCode() { return workstationCode; }
    public void setWorkstationCode(String v) { this.workstationCode = v; }
    public String getWorkstationName() { return workstationName; }
    public void setWorkstationName(String v) { this.workstationName = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String v) { this.routeCode = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String v) { this.taskCode = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public Long getOutsourceFactoryId() { return outsourceFactoryId; }
    public void setOutsourceFactoryId(Long v) { this.outsourceFactoryId = v; }
    public Long getOutsourceIssueId() { return outsourceIssueId; }
    public void setOutsourceIssueId(Long v) { this.outsourceIssueId = v; }
    public String getOutsourceIssueCode() { return outsourceIssueCode; }
    public void setOutsourceIssueCode(String v) { this.outsourceIssueCode = v; }
    public Long getOutsourceRecptId() { return outsourceRecptId; }
    public void setOutsourceRecptId(Long v) { this.outsourceRecptId = v; }
    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date v) { this.expireDate = v; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String v) { this.lotNumber = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityFeedback() { return quantityFeedback; }
    public void setQuantityFeedback(BigDecimal v) { this.quantityFeedback = v; }
    public BigDecimal getQuantityQualified() { return quantityQualified; }
    public void setQuantityQualified(BigDecimal v) { this.quantityQualified = v; }
    public BigDecimal getQuantityUnqualified() { return quantityUnqualified; }
    public void setQuantityUnqualified(BigDecimal v) { this.quantityUnqualified = v; }
    public BigDecimal getQuantityUncheck() { return quantityUncheck; }
    public void setQuantityUncheck(BigDecimal v) { this.quantityUncheck = v; }
    public BigDecimal getQuantityLaborScrap() { return quantityLaborScrap; }
    public void setQuantityLaborScrap(BigDecimal v) { this.quantityLaborScrap = v; }
    public BigDecimal getQuantityMaterialScrap() { return quantityMaterialScrap; }
    public void setQuantityMaterialScrap(BigDecimal v) { this.quantityMaterialScrap = v; }
    public BigDecimal getQuantityOtherScrap() { return quantityOtherScrap; }
    public void setQuantityOtherScrap(BigDecimal v) { this.quantityOtherScrap = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getNickName() { return nickName; }
    public void setNickName(String v) { this.nickName = v; }
    public String getFeedbackChannel() { return feedbackChannel; }
    public void setFeedbackChannel(String v) { this.feedbackChannel = v; }
    public Date getFeedbackTime() { return feedbackTime; }
    public void setFeedbackTime(Date v) { this.feedbackTime = v; }
    public String getRecordUser() { return recordUser; }
    public void setRecordUser(String v) { this.recordUser = v; }
    public String getRecordNick() { return recordNick; }
    public void setRecordNick(String v) { this.recordNick = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public List<ProFeedbackConsume> getConsumeList() { return consumeList; }
    public void setConsumeList(List<ProFeedbackConsume> v) { this.consumeList = v; }
    public List<ProFeedbackParam> getParamList() { return paramList; }
    public void setParamList(List<ProFeedbackParam> v) { this.paramList = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId()).append("feedbackCode", getFeedbackCode())
            .append("feedbackType", getFeedbackType()).append("workorderId", getWorkorderId())
            .append("quantity", getQuantity()).append("status", getStatus())
            .append("remark", getRemark()).append("createBy", getCreateBy())
            .append("createTime", getCreateTime()).append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime()).toString();
    }
}
