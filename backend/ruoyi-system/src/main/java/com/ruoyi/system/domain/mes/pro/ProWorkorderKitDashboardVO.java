package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工单齐套看板 VO — 汇总物料分析、领料/采购/退料/入库四类单据状态
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProWorkorderKitDashboardVO
{
    // ---- 工单上下文 ----
    private Long workorderId;
    private String workorderCode;
    private String workorderName;
    private String workorderStatus;
    private BigDecimal planQuantity;
    private String productName;
    private String productCode;
    private String requestDate;

    // ---- 1. 物料齐套分析 ----
    private List<Map<String, Object>> materialRows;      // checkMaterialReadiness 结果
    private int sufficientCount;
    private int shortageCount;
    private boolean allSufficient;

    // ---- 2. 采购单建议 ----
    private List<ProKitPurchaseRecommendVO> purchaseRecommends;
    private boolean hasPurchaseRecommend;

    // ---- 3. 生产领料单状态 ----
    private List<ProKitIssueStatusVO> issueStatuses;
    private int issueDraftCount;
    private int issueConfirmedCount;
    private int issuePostedCount;
    private boolean hasIssueDocs;

    // ---- 4. 生产退料单状态 ----
    private List<ProKitReturnStatusVO> returnStatuses;
    private boolean hasPendingReturns;
    private boolean returnReady;

    // ---- 5. 产品入库单建议 ----
    private ProKitReceiptRecommendVO receiptRecommend;
    private boolean receiptReady;

    // ====== getters & setters ======

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }

    public String getWorkorderStatus() { return workorderStatus; }
    public void setWorkorderStatus(String v) { this.workorderStatus = v; }

    public BigDecimal getPlanQuantity() { return planQuantity; }
    public void setPlanQuantity(BigDecimal v) { this.planQuantity = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }

    public String getRequestDate() { return requestDate; }
    public void setRequestDate(String v) { this.requestDate = v; }

    public List<Map<String, Object>> getMaterialRows() { return materialRows; }
    public void setMaterialRows(List<Map<String, Object>> v) { this.materialRows = v; }

    public int getSufficientCount() { return sufficientCount; }
    public void setSufficientCount(int v) { this.sufficientCount = v; }

    public int getShortageCount() { return shortageCount; }
    public void setShortageCount(int v) { this.shortageCount = v; }

    public boolean isAllSufficient() { return allSufficient; }
    public void setAllSufficient(boolean v) { this.allSufficient = v; }

    public List<ProKitPurchaseRecommendVO> getPurchaseRecommends() { return purchaseRecommends; }
    public void setPurchaseRecommends(List<ProKitPurchaseRecommendVO> v) { this.purchaseRecommends = v; }

    public boolean isHasPurchaseRecommend() { return hasPurchaseRecommend; }
    public void setHasPurchaseRecommend(boolean v) { this.hasPurchaseRecommend = v; }

    public List<ProKitIssueStatusVO> getIssueStatuses() { return issueStatuses; }
    public void setIssueStatuses(List<ProKitIssueStatusVO> v) { this.issueStatuses = v; }

    public int getIssueDraftCount() { return issueDraftCount; }
    public void setIssueDraftCount(int v) { this.issueDraftCount = v; }

    public int getIssueConfirmedCount() { return issueConfirmedCount; }
    public void setIssueConfirmedCount(int v) { this.issueConfirmedCount = v; }

    public int getIssuePostedCount() { return issuePostedCount; }
    public void setIssuePostedCount(int v) { this.issuePostedCount = v; }

    public boolean isHasIssueDocs() { return hasIssueDocs; }
    public void setHasIssueDocs(boolean v) { this.hasIssueDocs = v; }

    public List<ProKitReturnStatusVO> getReturnStatuses() { return returnStatuses; }
    public void setReturnStatuses(List<ProKitReturnStatusVO> v) { this.returnStatuses = v; }

    public boolean isHasPendingReturns() { return hasPendingReturns; }
    public void setHasPendingReturns(boolean v) { this.hasPendingReturns = v; }

    public boolean isReturnReady() { return returnReady; }
    public void setReturnReady(boolean v) { this.returnReady = v; }

    public ProKitReceiptRecommendVO getReceiptRecommend() { return receiptRecommend; }
    public void setReceiptRecommend(ProKitReceiptRecommendVO v) { this.receiptRecommend = v; }

    public boolean isReceiptReady() { return receiptReady; }
    public void setReceiptReady(boolean v) { this.receiptReady = v; }
}
