package com.ruoyi.common.enums;

/**
 * 外协单状态枚举
 *
 * 生命周期：DRAFT → ISSUED → VENDOR_RCVD → PROCESSING → FINISHED → SHIPPED → RECEIVED → CLOSED
 *
 * @author qixiaoxia
 */
public enum OutsourceStatus {

    DRAFT("DRAFT", "草稿"),
    ISSUED("ISSUED", "已发料"),
    VENDOR_RCVD("VENDOR_RCVD", "厂商已签收"),
    PROCESSING("PROCESSING", "加工中"),
    FINISHED("FINISHED", "已完工"),
    SHIPPED("SHIPPED", "已发货"),
    RECEIVED("RECEIVED", "已收货"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String info;

    OutsourceStatus(String code, String info) {
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
    public static OutsourceStatus fromCode(String code) {
        for (OutsourceStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
