package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;

/**
 * 领料单状态信息
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProKitIssueStatusVO
{
    private Long issueId;
    private String issueCode;
    private String issueName;
    private String processName;
    private String status;
    private int lineCount;
    private BigDecimal totalQuantity;
    private Long taskId;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }

    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String v) { this.issueCode = v; }

    public String getIssueName() { return issueName; }
    public void setIssueName(String v) { this.issueName = v; }

    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int v) { this.lineCount = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
}
