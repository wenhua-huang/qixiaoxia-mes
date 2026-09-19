package com.ruoyi.common.enums;

/**
 * 销售订单状态枚举（主线四态 + 链外作废）
 * <pre>
 *   CONFIRMED(已确认) ──任一工单开工──▶ PRODUCING(生产中) ──全部明细发齐──▶ SHIPPED(已出货) ──人工结单──▶ CLOSED(已结单)
 *        │                                   │
 *        └──────────── 取消 ─────────────────┘
 *                       ▼
 *                 CANCEL(已取消，链外终态)
 * </pre>
 * 工序任务报工不改变订单状态，只驱动进度百分比。
 * 对应字典：sys_dict_type = 'mes_sal_order_status'（V124 建，V151 收敛）
 *
 * @author qixiaoxia
 * @date 2026-08-13
 */
public enum SalOrderStatus {

    CONFIRMED("CONFIRMED", "已确认"),
    PRODUCING("PRODUCING", "生产中"),
    SHIPPED("SHIPPED", "已出货"),
    CLOSED("CLOSED", "已结单"),
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
