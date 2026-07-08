package com.ruoyi.common.enums;

/**
 * 采购订单状态枚举
 *
 * @author qixiaoxia
 * @date 2026-07-08
 */
public enum PurOrderStatus {

    DRAFT("DRAFT", "草稿"),
    APPROVED("APPROVED", "已审批"),
    ORDERED("ORDERED", "已下单"),
    RECEIVING("RECEIVING", "收货中"),
    RECEIVED("RECEIVED", "已收货"),
    CLOSED("CLOSED", "已关闭"),
    CANCEL("CANCEL", "已取消");

    private final String code;
    private final String info;

    PurOrderStatus(String code, String info) {
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
    public static PurOrderStatus fromCode(String code) {
        for (PurOrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
