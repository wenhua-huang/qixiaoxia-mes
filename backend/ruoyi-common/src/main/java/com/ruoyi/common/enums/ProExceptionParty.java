package com.ruoyi.common.enums;

/**
 * 生产异常责任方。上报后默认 PENDING（待定），PC 补全时首次选定后不可修改。
 *
 * <p>对应字典：mes_pro_exception_party（见 V157 迁移种子）
 *
 * @author qixiaoxia
 */
public enum ProExceptionParty {

    FACTORY("FACTORY", "本厂工序"),
    SUPPLIER("SUPPLIER", "供应商"),
    CUSTOMER("CUSTOMER", "客户"),
    PENDING("PENDING", "待定");

    private final String code;
    private final String info;

    ProExceptionParty(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    public boolean is(String party) {
        return this.code.equals(party);
    }

    public static ProExceptionParty fromCode(String code) {
        if (code == null) return null;
        for (ProExceptionParty p : values()) {
            if (p.code.equals(code)) return p;
        }
        return null;
    }
}
