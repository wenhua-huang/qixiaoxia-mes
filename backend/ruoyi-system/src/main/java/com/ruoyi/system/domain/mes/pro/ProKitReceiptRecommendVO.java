package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;

/**
 * 产品入库建议 — 已产出合格品可入库信息
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProKitReceiptRecommendVO
{
    private boolean recommended;
    private BigDecimal producedQty;
    private BigDecimal qualifiedQty;
    private String lastProcessName;
    private Long lastFeedbackId;
    private boolean alreadyHasReceipt;
    private String existingReceiptCode;

    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean v) { this.recommended = v; }

    public BigDecimal getProducedQty() { return producedQty; }
    public void setProducedQty(BigDecimal v) { this.producedQty = v; }

    public BigDecimal getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(BigDecimal v) { this.qualifiedQty = v; }

    public String getLastProcessName() { return lastProcessName; }
    public void setLastProcessName(String v) { this.lastProcessName = v; }

    public Long getLastFeedbackId() { return lastFeedbackId; }
    public void setLastFeedbackId(Long v) { this.lastFeedbackId = v; }

    public boolean isAlreadyHasReceipt() { return alreadyHasReceipt; }
    public void setAlreadyHasReceipt(boolean v) { this.alreadyHasReceipt = v; }

    public String getExistingReceiptCode() { return existingReceiptCode; }
    public void setExistingReceiptCode(String v) { this.existingReceiptCode = v; }
}
