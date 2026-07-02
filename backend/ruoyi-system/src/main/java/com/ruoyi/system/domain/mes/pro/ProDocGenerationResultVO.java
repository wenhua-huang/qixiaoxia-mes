package com.ruoyi.system.domain.mes.pro;

import java.util.List;
import java.util.Map;

/**
 * 批量生成单据结果 VO
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProDocGenerationResultVO
{
    private String generationBatch;
    private List<Map<String, Object>> issues;     // 生成的领料单
    private List<Map<String, Object>> purOrders;  // 生成的采购单
    private List<Map<String, Object>> returns;    // 生成的退料单
    private List<Map<String, Object>> receipts;   // 生成的入库单
    private String message;                       // 汇总描述

    public String getGenerationBatch() { return generationBatch; }
    public void setGenerationBatch(String v) { this.generationBatch = v; }

    public List<Map<String, Object>> getIssues() { return issues; }
    public void setIssues(List<Map<String, Object>> v) { this.issues = v; }

    public List<Map<String, Object>> getPurOrders() { return purOrders; }
    public void setPurOrders(List<Map<String, Object>> v) { this.purOrders = v; }

    public List<Map<String, Object>> getReturns() { return returns; }
    public void setReturns(List<Map<String, Object>> v) { this.returns = v; }

    public List<Map<String, Object>> getReceipts() { return receipts; }
    public void setReceipts(List<Map<String, Object>> v) { this.receipts = v; }

    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
}
