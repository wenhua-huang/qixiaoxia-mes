package com.ruoyi.common.enums;

/**
 * 销售订单类型枚举（字典 mes_sal_order_type）
 *
 * <p>五条业务线由 订单类型 + 是否外发 + 是否包装 三个开关组合区分，
 * 不走五套流程：
 * <ul>
 *   <li>STANDARD 标品：柔印标品常规订单</li>
 *   <li>SMALL_BATCH 小批量：小批量订单</li>
 *   <li>GIFT 礼品：礼品盒订单（含包装）</li>
 *   <li>PLATE 制版：完稿制版线订单</li>
 *   <li>STOCK 备货订单：无客户备货生产，走同一审核与转工单链路</li>
 * </ul>
 * 外发不是独立类型，而是订单头 outsource_flag=Y，可与任一类型组合。
 *
 * @author qixiaoxia
 * @date 2026-09-10
 */
public enum SalOrderType {

    STANDARD("STANDARD", "标品"),
    SMALL_BATCH("SMALL_BATCH", "小批量"),
    GIFT("GIFT", "礼品"),
    STOCK("STOCK", "备货订单"),
    PLATE("PLATE", "制版");

    private final String code;
    private final String info;

    SalOrderType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    /** 判断是否为给定类型 */
    public boolean is(String code) { return this.code.equals(code); }

    /** 从 code 转换为枚举，未匹配返回 null */
    public static SalOrderType fromCode(String code) {
        if (code == null) return null;
        for (SalOrderType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
