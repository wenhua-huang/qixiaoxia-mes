package com.ruoyi.common.enums;

/**
 * 仓库类型常量。warehouse_type 取值。
 */
public final class WmWarehouseConstants {
    public static final String TYPE_RAW = "RAW";             // 原料仓
    public static final String TYPE_FINISHED = "FINISHED";   // 成品仓
    public static final String TYPE_AUX = "AUX";             // 辅料仓
    public static final String TYPE_LINE = "LINE";           // 线边库
    public static final String TYPE_TEMP = "TEMP";           // 临时仓
    public static final String TYPE_CUSTOMER = "CUSTOMER";   // 客户仓(归属客户)
    public static final String TYPE_SUPPLIER = "SUPPLIER";   // 供应商仓(归属供应商)

    private WmWarehouseConstants() {}
}
