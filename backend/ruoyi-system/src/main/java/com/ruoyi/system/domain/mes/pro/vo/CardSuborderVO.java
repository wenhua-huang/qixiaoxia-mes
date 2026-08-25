package com.ruoyi.system.domain.mes.pro.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 工单进度 — 流转卡（子工单）行 VO
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public class CardSuborderVO
{
    private Long cardId;
    private String cardCode;
    private String batchCode;
    private BigDecimal quantityTransfered;
    private Long currentProcessId;
    private String currentProcessName;
    private String status;
    private Date actualStartTime;
    private Date actualEndTime;
    private Integer totalProcessCount;
    private Integer finishedProcessCount;
    /** 完成率 0-100 */
    private Integer completionRate;

    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public String getCardCode() { return cardCode; }
    public void setCardCode(String v) { this.cardCode = v; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }
    public BigDecimal getQuantityTransfered() { return quantityTransfered; }
    public void setQuantityTransfered(BigDecimal v) { this.quantityTransfered = v; }
    public Long getCurrentProcessId() { return currentProcessId; }
    public void setCurrentProcessId(Long v) { this.currentProcessId = v; }
    public String getCurrentProcessName() { return currentProcessName; }
    public void setCurrentProcessName(String v) { this.currentProcessName = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Date getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(Date v) { this.actualStartTime = v; }
    public Date getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(Date v) { this.actualEndTime = v; }
    public Integer getTotalProcessCount() { return totalProcessCount; }
    public void setTotalProcessCount(Integer v) { this.totalProcessCount = v; }
    public Integer getFinishedProcessCount() { return finishedProcessCount; }
    public void setFinishedProcessCount(Integer v) { this.finishedProcessCount = v; }
    public Integer getCompletionRate() { return completionRate; }
    public void setCompletionRate(Integer v) { this.completionRate = v; }
}
