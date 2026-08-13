package com.ruoyi.common.enums;

/**
 * 仓库常量。
 * warehouse_type  — 内容维度（RAW/FINISHED/AUX/LINE/TEMP，纯标签）
 * ownership_type  — 归属维度（PUBLIC/CUSTOMER/SUPPLIER，驱动隔离逻辑）
 */
public final class WmWarehouseConstants {
    // ---- 内容类型 warehouse_type ----
    public static final String TYPE_RAW = "RAW";             // 原料仓
    public static final String TYPE_FINISHED = "FINISHED";   // 成品仓
    public static final String TYPE_AUX = "AUX";             // 辅料仓
    public static final String TYPE_LINE = "LINE";           // 线边库
    public static final String TYPE_TEMP = "TEMP";           // 临时仓

    // ---- 归属类型 ownership_type ----
    public static final String OWNER_PUBLIC = "PUBLIC";      // 公共仓
    public static final String OWNER_CUSTOMER = "CUSTOMER";  // 客户仓
    public static final String OWNER_SUPPLIER = "SUPPLIER";  // 供应商仓

    private WmWarehouseConstants() {}
}
