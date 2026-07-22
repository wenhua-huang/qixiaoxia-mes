package com.ruoyi.common.enums;

/**
 * 销售出库单常量 — 状态值、事务类型、文档来源类型
 *
 * <p>销售出库单生命周期状态机（无预占模型，过账即扣减库存）：
 * <pre>
 *   DRAFT(草稿) ──post(全量)──▶ POSTED(已过账) ──ship──▶ SHIPPED(已发货) ──close──▶ CLOSED(已关闭)
 *      │
 *      └──post(部分)──▶ PARTIAL_POSTED(部分过账) ──post──▶ { PARTIAL_POSTED | POSTED }
 *                            │
 *   DRAFT / PARTIAL_POSTED ──cancel──▶ CANCELED(已作废)
 *
 *   说明：
 *   - 无预占，过账(post)时一次性扣减 quantity_onhand
 *   - 部分过账后可继续过账，全部出完转 POSTED
 *   - 作废仅允许 DRAFT/PARTIAL_POSTED；POSTED 已全量扣库存需走销售退货(rt_sales)回库
 * </pre>
 *
 * <p>对应字典：sys_dict_type = 'mes_wm_sales_status'（见 V80 迁移种子）
 *
 * @author qixiaoxia
 * @date 2026-07-22
 */
public final class WmProductSalesConstants
{
    private WmProductSalesConstants() {}

    // ==================== 销售出库单状态 ====================
    /** 草稿：制单中，可编辑删除 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 部分过账：分批出库中，已扣减部分 quantity_onhand */
    public static final String STATUS_PARTIAL_POSTED = "PARTIAL_POSTED";
    /** 已过账：全量出库完成，quantity_onhand 全部扣减 */
    public static final String STATUS_POSTED = "POSTED";
    /** 已发货：已登记物流发货信息 */
    public static final String STATUS_SHIPPED = "SHIPPED";
    /** 已关闭（终态） */
    public static final String STATUS_CLOSED = "CLOSED";
    /** 已作废（终态），PARTIAL_POSTED 作废需回滚已扣库存 */
    public static final String STATUS_CANCELED = "CANCELED";

    /** 可编辑的状态（仅这些状态允许修改/删除） */
    public static final String[] EDITABLE_STATUSES = {STATUS_DRAFT};

    /** 可过账的状态（允许执行出库扣减） */
    public static final String[] POSTABLE_STATUSES = {STATUS_DRAFT, STATUS_PARTIAL_POSTED};

    /** 可发货的状态 */
    public static final String[] SHIPPABLE_STATUSES = {STATUS_POSTED, STATUS_PARTIAL_POSTED};

    /** 终态（不可再流转） */
    public static final String[] TERMINAL_STATUSES = {STATUS_CLOSED, STATUS_CANCELED};

    // ==================== 库存事务类型（qxx_wm_transaction.transaction_type） ====================
    /** 销售出库：扣减现有库存（负数），复用 TransactionTypeEnum.PRODUCT_SALES */
    public static final String TX_SALES_OUT = "PRODUCT_SALES";

    // ==================== 文档来源类型（qxx_wm_transaction.source_doc_type） ====================
    /** 来源：销售出库单 */
    public static final String SOURCE_SALES_OUT = "SALES_OUT";

    // ==================== 自动编码规则（sys_auto_code_rule.rule_code） ====================
    /** 销售出库单编码规则 */
    public static final String CODE_RULE_SALES = "SALES_NO";

    // ==================== 库存质量状态（WmMaterialStock.qualityStatus） ====================
    /** 正常（销售出库默认只发正常库存） */
    public static final String QUALITY_NORMAL = "NORMAL";

    // ==================== 辅助方法 ====================

    /** 判断状态是否可编辑（修改/删除） */
    public static boolean isEditable(String status)
    {
        return contains(EDITABLE_STATUSES, status);
    }

    /** 判断状态是否可过账（执行出库扣减） */
    public static boolean isPostable(String status)
    {
        return contains(POSTABLE_STATUSES, status);
    }

    /** 判断状态是否可发货 */
    public static boolean isShippable(String status)
    {
        return contains(SHIPPABLE_STATUSES, status);
    }

    /** 判断状态是否为终态 */
    public static boolean isTerminal(String status)
    {
        return contains(TERMINAL_STATUSES, status);
    }

    private static boolean contains(String[] arr, String status)
    {
        if (status == null) return false;
        for (String s : arr)
        {
            if (s.equals(status)) return true;
        }
        return false;
    }
}
