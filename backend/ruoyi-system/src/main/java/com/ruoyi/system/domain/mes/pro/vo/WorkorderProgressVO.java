package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 工单进度详情 VO
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class WorkorderProgressVO
{
    private Long workorderId;
    private String workorderCode;
    private String workorderName;
    private String productCode;
    private String productName;
    private String productSpc;
    private String status;
    private BigDecimal quantity;
    private BigDecimal quantityProduced;
    private Date requestDate;
    private Date finishDate;
    private Date planStartTime;
    private Date planEndTime;
    private Date actualStartTime;
    private Date actualEndTime;
    /** 完成率 0-100 */
    private Integer completionRate;
    /** 延期风险等级（ProConstants.DELAY_*） */
    private String delayLevel;
    private List<ProcessProgressRowVO> processes;
    private List<CardSuborderVO> cards;

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductSpc() { return productSpc; }
    public void setProductSpc(String v) { this.productSpc = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public Date getFinishDate() { return finishDate; }
    public void setFinishDate(Date v) { this.finishDate = v; }
    public Date getPlanStartTime() { return planStartTime; }
    public void setPlanStartTime(Date v) { this.planStartTime = v; }
    public Date getPlanEndTime() { return planEndTime; }
    public void setPlanEndTime(Date v) { this.planEndTime = v; }
    public Date getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(Date v) { this.actualStartTime = v; }
    public Date getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(Date v) { this.actualEndTime = v; }
    public Integer getCompletionRate() { return completionRate; }
    public void setCompletionRate(Integer v) { this.completionRate = v; }
    public String getDelayLevel() { return delayLevel; }
    public void setDelayLevel(String v) { this.delayLevel = v; }
    public List<ProcessProgressRowVO> getProcesses() { return processes; }
    public void setProcesses(List<ProcessProgressRowVO> v) { this.processes = v; }
    public List<CardSuborderVO> getCards() { return cards; }
    public void setCards(List<CardSuborderVO> v) { this.cards = v; }
}
