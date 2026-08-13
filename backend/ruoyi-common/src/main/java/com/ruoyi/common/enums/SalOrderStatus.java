package com.ruoyi.common.enums;

/**
 * 销售订单状态枚举
 *
 * <p>审核状态机（方案 A：审核 = 生效）：
 * <pre>
 *   PREPARE(待提交) ──提交──▶ PENDING(待审核) ──审核通过──▶ CONFIRMED(已确认) ──▶ CLOSED(已关闭)
 *        │                      │                     =可转工单
 *        │                   驳回(必填意见)
 *        ◀─────────────────────┘  (回退 PREPARE)
 *
 *   CANCEL(已取消) 可由 PREPARE/PENDING/CONFIRMED 流入（终态）
 * </pre>
 *
 * <p>对应字典：sys_dict_type = 'mes_sal_order_status'（见 V124 迁移种子）
 *
 * @author qixiaoxia
 * @date 2026-08-13
 */
public enum SalOrderStatus {

    PREPARE("PREPARE", "待提交"),
    PENDING("PENDING", "待审核"),
    CONFIRMED("CONFIRMED", "已确认"),
    CLOSED("CLOSED", "已关闭"),
    CANCEL("CANCEL", "已取消");

    private final String code;
    private final String info;

    SalOrderStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    /** 判断是否为给定状态 */
    public boolean is(String status) {
        return this.code.equals(status);
    }

    /** 从 code 转换为枚举，未匹配返回 null */
    public static SalOrderStatus fromCode(String code) {
        if (code == null) return null;
        for (SalOrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
