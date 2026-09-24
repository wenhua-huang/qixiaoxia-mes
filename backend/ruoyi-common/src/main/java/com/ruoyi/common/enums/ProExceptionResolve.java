package com.ruoyi.common.enums;

/**
 * 生产异常出口动作（线框图 7 选 1）。
 *
 * <p>回流类（异常单置处理中，待单据/任务完成后手动关闭）：REWORK 返工、REMAKE 补做、
 * PURCHASE 补料采购、RESCHEDULE 顺延改期。
 * <p>终结类（选出口即关闭）：SCRAP 报废、CONCESSION 让步接收、REFUND 退款结单。
 *
 * <p>对应字典：mes_pro_exception_resolve（见 V157 迁移种子）
 *
 * @author qixiaoxia
 */
public enum ProExceptionResolve {

    REWORK("REWORK", "开返工任务", true),
    REMAKE("REMAKE", "开补做任务", true),
    PURCHASE("PURCHASE", "开补料采购", true),
    RESCHEDULE("RESCHEDULE", "顺延改期", true),
    SCRAP("SCRAP", "报废", false),
    CONCESSION("CONCESSION", "让步接收", false),
    REFUND("REFUND", "退款结单", false);

    private final String code;
    private final String info;
    /** true=回流（处理中，待手动关闭）；false=终结（直接关闭） */
    private final boolean flowback;

    ProExceptionResolve(String code, String info, boolean flowback) {
        this.code = code;
        this.info = info;
        this.flowback = flowback;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }
    public boolean isFlowback() { return flowback; }
    public boolean isTerminal() { return !flowback; }

    public boolean is(String resolveType) {
        return this.code.equals(resolveType);
    }

    public static ProExceptionResolve fromCode(String code) {
        if (code == null) return null;
        for (ProExceptionResolve r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }
}
