package com.ruoyi.common.enums;

/**
 * 生产异常单状态：OPEN 待处理 → PROCESSING 处理中 → CLOSED 已关闭；
 * OPEN 可直接 VOID 已作废（挂错对象作废重开，E1），作废为终态且不参与完工硬拦。
 *
 * <p>对应字典：mes_pro_exception_status（见 V157/V158 迁移种子）
 *
 * @author qixiaoxia
 */
public enum ProExceptionStatus {

    OPEN("OPEN", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    CLOSED("CLOSED", "已关闭"),
    VOID("VOID", "已作废");

    private final String code;
    private final String info;

    ProExceptionStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    public boolean is(String status) {
        return this.code.equals(status);
    }

    public static ProExceptionStatus fromCode(String code) {
        if (code == null) return null;
        for (ProExceptionStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
