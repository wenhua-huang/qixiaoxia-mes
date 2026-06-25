package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_pro_card_process 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProCardProcess extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long factoryId;
    private Long cardId;
    private String cardCode;
    private Integer seqNum;
    private Long processId;
    private String processCode;
    private String processName;
    private String processType;
    private Long taskId;
    private String taskCode;
    private Date inputTime;
    private Date outputTime;
    private BigDecimal quantityInput;
    private BigDecimal quantityOutput;
    private BigDecimal quantityUnqualified;
    private Long workstationId;
    private String workstationCode;
    private String workstationName;
    private Long userId;
    private String userName;
    private String nickName;
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private Long outsourceFactoryId;
    private Long ipqcId;
    private String ipqcCode;
    private Long feedbackId;
    private Long issueDetailId;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long v) { this.recordId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public String getCardCode() { return cardCode; }
    public void setCardCode(String v) { this.cardCode = v; }
    public Integer getSeqNum() { return seqNum; }
    public void setSeqNum(Integer v) { this.seqNum = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public String getProcessType() { return processType; }
    public void setProcessType(String v) { this.processType = v; }
    public Date getInputTime() { return inputTime; }
    public void setInputTime(Date v) { this.inputTime = v; }
    public Date getOutputTime() { return outputTime; }
    public void setOutputTime(Date v) { this.outputTime = v; }
    public BigDecimal getQuantityInput() { return quantityInput; }
    public void setQuantityInput(BigDecimal v) { this.quantityInput = v; }
    public BigDecimal getQuantityOutput() { return quantityOutput; }
    public void setQuantityOutput(BigDecimal v) { this.quantityOutput = v; }
    public BigDecimal getQuantityUnqualified() { return quantityUnqualified; }
    public void setQuantityUnqualified(BigDecimal v) { this.quantityUnqualified = v; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }
    public String getWorkstationCode() { return workstationCode; }
    public void setWorkstationCode(String v) { this.workstationCode = v; }
    public String getWorkstationName() { return workstationName; }
    public void setWorkstationName(String v) { this.workstationName = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getNickName() { return nickName; }
    public void setNickName(String v) { this.nickName = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public Long getOutsourceFactoryId() { return outsourceFactoryId; }
    public void setOutsourceFactoryId(Long v) { this.outsourceFactoryId = v; }
    public Long getIpqcId() { return ipqcId; }
    public void setIpqcId(Long v) { this.ipqcId = v; }
    public String getIpqcCode() { return ipqcCode; }
    public void setIpqcCode(String v) { this.ipqcCode = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public Long getIssueDetailId() { return issueDetailId; }
    public void setIssueDetailId(Long v) { this.issueDetailId = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("cardId", getCardId())
            .append("cardCode", getCardCode())
            .append("seqNum", getSeqNum())
            .append("processId", getProcessId())
            .append("processCode", getProcessCode())
            .append("processName", getProcessName()).toString();
    }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
}
