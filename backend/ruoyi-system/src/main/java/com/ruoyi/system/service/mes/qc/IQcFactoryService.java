package com.ruoyi.system.service.mes.qc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;

/**
 * 检验单生成工厂 — 业务单据保存后按模板绑定生成对应类型待检单
 * （波次 3-5 直接扩展本接口，既有签名不得改动）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcFactoryService
{
    /**
     * 查启用模板绑定：先精确工序(selectEnabledBindExact)，后通用(process_id IS NULL)；
     * 无绑定返回 null = 免检
     */
    QcTemplateProduct resolveTemplate(String qcType, Long itemId, Long processId);

    /**
     * 批量解析模板绑定（gate/生成路径消除逐物料 N+1）：对每个 itemId 先查精确工序绑定，
     * 未命中再回退通用绑定(process_id IS NULL)。等价于对集合内每个元素调用
     * {@link #resolveTemplate}，但至多 2 条 SQL。
     *
     * @return itemId → 绑定；未绑定模板的物料不在 Map 中（免检）
     */
    Map<Long, QcTemplateProduct> resolveTemplates(String qcType, Collection<Long> itemIds, Long processId);

    /**
     * 采购入库单创建后生成 IQC 待检单（幂等：同来源+物料存在未关闭单则跳过；Redis 锁内执行；
     * 多行同物料合并为一张单；生成后回填入库单头 iqc_id/iqc_code 挂点）
     */
    void generateIqcForItemRecpt(WmItemRecpt header, List<WmItemRecptLine> lines);

    /**
     * 外协厂商发货后生成 IQC 待检单（source_doc_type=wm_outsource_order），
     * 幂等 + Redis 锁 + 多物料合并，生成后回填外协单头 iqc_id/iqc_code 挂点；
     * 未绑定 IQC 模板的物料免检跳过。
     */
    void generateIqcForOutsource(WmOutsourceOrder order, List<WmOutsourceRecptLine> lines);

    /**
     * 按模板检测项快照生成检验行（qcType/qcId/检测项五件套/标准值/单位/上下偏差/order_num，
     * 实测值 checkValText 留空待检验员录入）
     */
    List<QcOrderLine> buildLinesFromTemplate(Long templateId, String qcType, Long qcId);

    /** 销售出库过账后生成 OQC 单（Task 12 实现） */
    void generateOqcForProductSales(WmProductSales header, List<WmProductSalesLine> lines);

    /** 成品入库后生成 IPQC 单（Task 14 实现） */
    void generateIpqcForProductRecpt(WmProductRecpt header);

    /**
     * 报工后生成工序检验 IPQC 单（Task 14 实现）
     *
     * @return 生成的检验单编码；null = 该物料/工序未绑定模板未生成
     */
    String generateIpqcForFeedback(ProFeedback feedback);

    /** 退料发出后生成 RQC 单（Task 16 实现） */
    void generateRqcForRtIssue(WmRtIssue header, List<WmRtIssueLine> lines);

    /**
     * 来源单据作废时联动关闭其 PENDING/INSPECTING 检验单（按 source 反查置 CLOSED；
     * COMPLETED 单是质量档案保留不关）
     */
    void closeBySource(String sourceDocType, Long sourceDocId);
}
