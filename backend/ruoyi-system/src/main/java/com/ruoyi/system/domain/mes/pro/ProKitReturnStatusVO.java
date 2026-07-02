package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;

/**
 * 退料单状态信息
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProKitReturnStatusVO
{
    private Long rtId;
    private String rtCode;
    private String rtName;
    private String issueCode;
    private String status;
    private int lineCount;
    private BigDecimal totalQuantity;

    public Long getRtId() { return rtId; }
    public void setRtId(Long v) { this.rtId = v; }

    public String getRtCode() { return rtCode; }
    public void setRtCode(String v) { this.rtCode = v; }

    public String getRtName() { return rtName; }
    public void setRtName(String v) { this.rtName = v; }

    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String v) { this.issueCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int v) { this.lineCount = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }
}
