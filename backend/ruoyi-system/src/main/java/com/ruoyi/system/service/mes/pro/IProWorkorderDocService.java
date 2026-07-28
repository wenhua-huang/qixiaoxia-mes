package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.mes.pro.ProDocGenerationRequestVO;
import com.ruoyi.system.domain.mes.pro.ProDocGenerationResultVO;
import com.ruoyi.system.domain.mes.pro.ProWorkorderKitDashboardVO;
import com.ruoyi.system.domain.mes.pro.PurOrderWizardLineVO;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;

/**
 * 工单齐套分析 → 一键触发生成采购单/领料单/退料单/入库单 Service 接口
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public interface IProWorkorderDocService
{
    /**
     * 加载齐套看板 — 汇总物料分析、领料/采购/退料/入库四类单据状态
     */
    ProWorkorderKitDashboardVO loadKitDashboard(Long workorderId);

    /**
     * 一键批量生成单据（事务性，同一批次号）
     * 每种类型独立幂等，通过 qxx_pro_doc_generation_log 防重
     */
    ProDocGenerationResultVO generateDocuments(ProDocGenerationRequestVO request);

    /**
     * 报工审核后的自动触发：末工序报工 → 生成入库单 + 退料单。
     * <p>职责边界：仅生成单据，**不更新 workorder** (quantity_produced 更新 / 完工判定
     * 由调用方 auditFeedback 在外层事务完成, 见 {@link #autoCompleteWorkorderIfQualified})。
     *
     * @param feedbackId 报工 ID
     * @param producedAfterDelta 外层事务 addQuantityProduced 后的工单已生产量。本方法走
     *         REQUIRES_NEW 独立事务，MySQL REPEATABLE READ 下读不到外层未提交值，故由调用方
     *         在同事务读出后显式传入，绕开事务隔离问题。传 null 时回退到读取当前 workorder 行。
     */
    List<Map<String, Object>> onFeedbackAudited(Long feedbackId, BigDecimal producedAfterDelta);

    /**
     * 自动完工判定：quantity_produced >= 计划量时，精准条件 UPDATE 置 COMPLETED。
     * 由 auditFeedback 在 addQuantityProduced 之后、同一事务内调用。
     */
    void autoCompleteWorkorderIfQualified(Long workorderId);

    /**
     * 单独生成产品入库单
     */
    WmProductRecpt generateProductReceipt(Long workorderId);

    /**
     * 采购单快捷创建向导 — 返回推荐数据（按供应商分组），不写库
     */
    Map<String, Object> buildPurOrderWizard(Long workorderId);

    /**
     * 采购向导弹窗确认提交 — 按供应商分组生成真实采购单
     */
    Map<String, Object> submitPurOrderWizard(Long workorderId, List<PurOrderWizardLineVO> lines);

    /**
     * 单独生成采购单（针对缺料物料）
     */
    Map<String, Object> generatePurchaseOrder(Long workorderId);

    /**
     * 单独生成退料单
     */
    WmRtIssue generateMaterialReturn(Long workorderId);
}
