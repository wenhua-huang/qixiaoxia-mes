package com.ruoyi.system.domain.mes.pro;

/**
 * 批量生成单据请求 VO
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProDocGenerationRequestVO
{
    private Long workorderId;
    private boolean generateIssue;    // 生成生产领料单
    private boolean generatePurOrder; // 生成采购单
    private boolean generateReturn;   // 生成退料单
    private boolean generateReceipt;  // 生成产品入库单

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public boolean isGenerateIssue() { return generateIssue; }
    public void setGenerateIssue(boolean v) { this.generateIssue = v; }

    public boolean isGeneratePurOrder() { return generatePurOrder; }
    public void setGeneratePurOrder(boolean v) { this.generatePurOrder = v; }

    public boolean isGenerateReturn() { return generateReturn; }
    public void setGenerateReturn(boolean v) { this.generateReturn = v; }

    public boolean isGenerateReceipt() { return generateReceipt; }
    public void setGenerateReceipt(boolean v) { this.generateReceipt = v; }
}
