package com.ruoyi.common.enums;

/**
 * 生产异常类型（线框图四类，同表专属字段）。
 *
 * <p>对应字典：mes_pro_exception_type（见 V157 迁移种子）
 *
 * @author qixiaoxia
 */
public enum ProExceptionType {

    QUALITY("QUALITY", "质量数量"),
    MATERIAL("MATERIAL", "缺料"),
    DELAY("DELAY", "进度延迟"),
    RETURN("RETURN", "客户退货");

    private final String code;
    private final String info;

    ProExceptionType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    public boolean is(String type) {
        return this.code.equals(type);
    }

    public static ProExceptionType fromCode(String code) {
        if (code == null) return null;
        for (ProExceptionType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
