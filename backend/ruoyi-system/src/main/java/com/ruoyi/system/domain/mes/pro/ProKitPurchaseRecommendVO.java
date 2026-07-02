package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;

/**
 * 采购建议行 — 缺料项对应的采购推荐
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProKitPurchaseRecommendVO
{
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String unitName;
    private BigDecimal shortageQty;
    private BigDecimal recommendedQty;
    private boolean hasPendingPO;
    private String pendingPOInfo;  // 已有在途PO编码/状态

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }

    public BigDecimal getShortageQty() { return shortageQty; }
    public void setShortageQty(BigDecimal v) { this.shortageQty = v; }

    public BigDecimal getRecommendedQty() { return recommendedQty; }
    public void setRecommendedQty(BigDecimal v) { this.recommendedQty = v; }

    public boolean isHasPendingPO() { return hasPendingPO; }
    public void setHasPendingPO(boolean v) { this.hasPendingPO = v; }

    public String getPendingPOInfo() { return pendingPOInfo; }
    public void setPendingPOInfo(String v) { this.pendingPOInfo = v; }
}
