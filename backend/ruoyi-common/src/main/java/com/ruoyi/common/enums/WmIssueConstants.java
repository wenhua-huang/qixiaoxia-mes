package com.ruoyi.common.enums;

/**
 * 生产领料单常量 — 状态值、事务类型、领料类型、文档来源类型
 *
 * <p>领料单完整生命周期状态机（标准制造 8 态）：
 * <pre>
 *   DRAFT(草稿) ──提交──▶ PENDING(待审核) ──审核──▶ APPROVED(已下达)
 *        │                       │                       │
 *        │                  审核退回▼                预占▼
 *        ◀───────────────────  (回 DRAFT)            ALLOCATED(已预占)
 *        │                                               │ 发料(可分批)
 *        └──────────作废──────────────────────▶    PARTIAL_ISSUED / ISSUED
 *                                                        │ 收料确认/关闭
 *                                                   CLOSED(已关闭)
 *
 *   CANCELED(已作废) 为终态，可从 DRAFT/PENDING/APPROVED/ALLOCATED/PARTIAL_ISSUED 流入
 * </pre>
 *
 * <p>对应字典：sys_dict_type = 'mes_wm_issue_status'（见 V44 迁移种子）
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
public final class WmIssueConstants
{
    private WmIssueConstants() {}

    // ==================== 领料单状态 ====================
    /** 草稿：制单中，可编辑删除 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 待审核：已提交，等待计划员/仓管主管审核 */
    public static final String STATUS_PENDING = "PENDING";
    /** 已下达：审核通过，仓库可见，未预占库存 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 已预占：已扣减 quantity_available（可用量） */
    public static final String STATUS_ALLOCATED = "ALLOCATED";
    /** 部分发料：分批发料中，已扣减部分 quantity_onhand */
    public static final String STATUS_PARTIAL_ISSUED = "PARTIAL_ISSUED";
    /** 已发料：全量出库完成，quantity_onhand 全部扣减 */
    public static final String STATUS_ISSUED = "ISSUED";
    /** 已关闭：产线收料确认/手工关闭（终态） */
    public static final String STATUS_CLOSED = "CLOSED";
    /** 已作废（终态），ALLOCATED 态作废需先恢复 available */
    public static final String STATUS_CANCELED = "CANCELED";

    /** 可编辑的状态（仅这些状态允许修改/删除） */
    public static final String[] EDITABLE_STATUSES = {STATUS_DRAFT, STATUS_PENDING};

    /** 终态（不可再流转） */
    public static final String[] TERMINAL_STATUSES = {STATUS_CLOSED, STATUS_CANCELED};

    // ==================== 库存事务类型（qxx_wm_transaction.transaction_type） ====================
    /** 预占：领料确认/预占时扣减可用库存（负数） */
    public static final String TX_ALLOCATE = "ALLOCATE";
    /** 释放预占：恢复可用库存（正数） */
    public static final String TX_RELEASE = "RELEASE";
    /** 领料出库：扣减现有库存（负数） */
    public static final String TX_ISSUE_OUT = "ISSUE_OUT";
    /** 退料入库：增加现有库存（正数） */
    public static final String TX_RETURN_IN = "RETURN_IN";
    /** 入库：采购/生产入库（正数） */
    public static final String TX_RECEIPT = "RECEIPT";
    /** 调拨：仓库间调拨 */
    public static final String TX_TRANSFER = "TRANSFER";
    /** 调整：盘点调整 */
    public static final String TX_ADJUST = "ADJUST";
    /** 拆分：库存拆分 */
    public static final String TX_SPLIT = "SPLIT";

    // ==================== 领料类型 ====================
    /** 生产领料（工单驱动） */
    public static final String TYPE_PRODUCE = "PRODUCE";
    /** 杂项领料（非生产性） */
    public static final String TYPE_MISC = "MISC";

    // ==================== 文档来源类型（qxx_wm_transaction.source_doc_type） ====================
    /** 来源：领料单 */
    public static final String SOURCE_ISSUE = "ISSUE";
    /** 来源：退料单 */
    public static final String SOURCE_RTISSUE = "RTISSUE";

    // ==================== 自动编码规则 ====================
    /** 领料单编码规则（sys_auto_code_rule.rule_code） */
    public static final String CODE_RULE_ISSUE = "ISSUE_CODE";

    // ==================== 库存质量状态（WmMaterialStock.qualityStatus） ====================
    /** 正常（领料默认只发正常库存） */
    public static final String QUALITY_NORMAL = "NORMAL";

    // ==================== 辅助方法 ====================

    /** 判断状态是否可编辑（修改/删除） */
    public static boolean isEditable(String status)
    {
        if (status == null) return false;
        for (String s : EDITABLE_STATUSES)
        {
            if (s.equals(status)) return true;
        }
        return false;
    }

    /** 判断状态是否为终态 */
    public static boolean isTerminal(String status)
    {
        if (status == null) return false;
        for (String s : TERMINAL_STATUSES)
        {
            if (s.equals(status)) return true;
        }
        return false;
    }
}
