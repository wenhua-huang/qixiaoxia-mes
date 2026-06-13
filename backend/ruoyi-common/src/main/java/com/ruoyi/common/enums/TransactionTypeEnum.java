package com.ruoyi.common.enums;

/**
 * 库存事务类型枚举
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
public enum TransactionTypeEnum {

    ITEM_RECPT("ITEM_RECPT", "物料入库"),
    MISC_RECPT("MISC_RECPT", "杂项入库"),
    MISC_ISSUE("MISC_ISSUE", "杂项出库"),
    ITEM_RTV("ITEM_RTV", "供应商退货"),
    PRODUCT_SALES("PRODUCT_SALES", "销售出库"),
    PRODUCT_RT("PRODUCT_RT", "销售退货"),
    TRANS_OUT("TRANS_OUT", "调拨出库"),
    TRANS_IN("TRANS_IN", "调拨入库");

    private final String code;
    private final String info;

    TransactionTypeEnum(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }
}
