package com.ruoyi.system.service.mes.qc;

/** 质检模块公共常量 */
public class QcConstants {
    // 自动编码规则 key（与 V136 一致）
    public static final String CODE_RULE_IQC = "QC_IQC_CODE";
    public static final String CODE_RULE_IPQC = "QC_IPQC_CODE";
    public static final String CODE_RULE_OQC = "QC_OQC_CODE";
    public static final String CODE_RULE_RQC = "QC_RQC_CODE";
    // 检验业务类型
    public static final String TYPE_IQC = "IQC";
    public static final String TYPE_IPQC = "IPQC";
    public static final String TYPE_OQC = "OQC";
    public static final String TYPE_RQC = "RQC";
    // 单据状态
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_INSPECTING = "INSPECTING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CLOSED = "CLOSED";
    // 判定结果
    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_FAIL = "FAIL";
    public static final String RESULT_CONCESSION = "CONCESSION";
    // 行结果
    public static final String LINE_PASS = "PASS";
    public static final String LINE_FAIL = "FAIL";
    // 检测值类型
    public static final String RESULT_TYPE_NUMBER = "NUMBER";
    public static final String RESULT_TYPE_TEXT = "TEXT";
    public static final String RESULT_TYPE_DICT = "DICT";
    public static final String RESULT_TYPE_FILE = "FILE";
    public static final String RESULT_TYPE_COUNT = "COUNT";
    // 缺陷等级
    public static final String DEFECT_CRITICAL = "CRITICAL";
    public static final String DEFECT_MAJOR = "MAJOR";
    public static final String DEFECT_MINOR = "MINOR";
    // IPQC 检验类型
    public static final String IPQC_FIRST = "FIRST_CHECK";
    public static final String IPQC_TOUR = "TOUR_CHECK";
    public static final String IPQC_LAST = "LAST_CHECK";
    public static final String IPQC_SPOT = "SPOT_CHECK";
    // RQC 退料类型
    public static final String RQC_TYPE_PROD_RETURN = "PROD_RETURN";
    public static final String RQC_TYPE_PURCHASE_RETURN = "PURCHASE_RETURN";
    public static final String RQC_TYPE_QC_REJECT = "QC_REJECT";
    // RQC 责任归属
    public static final String RQC_RESP_SUPPLIER = "SUPPLIER";
    public static final String RQC_RESP_PRODUCTION = "PRODUCTION";
    public static final String RQC_RESP_STORAGE = "STORAGE";
    public static final String RQC_RESP_OTHER = "OTHER";
    // 来源单据类型（gate/factory 反查用）
    public static final String SOURCE_ITEM_RECPT = "wm_item_recpt";
    public static final String SOURCE_PRODUCT_RECPT = "wm_product_recpt";
    public static final String SOURCE_PRODUCT_SALES = "wm_product_sales";
    public static final String SOURCE_CARD_PROCESS = "pro_card_process";
    public static final String SOURCE_RT_ISSUE = "wm_rt_issue";
    public static final String SOURCE_OUTSOURCE_ORDER = "wm_outsource_order";
    // 判定锁 key 前缀
    public static final String LOCK_JUDGE = "qc:judge:";
    public static final String LOCK_GENERATE = "qc:generate:";
    // QC_CHECK 待办：优先级/状态/处理结果（类型用 TodoTypeEnum.QC_CHECK，与 sys_todo_list 字典口径一致）
    public static final String TODO_PRIORITY_NORMAL = "NORMAL";
    public static final String TODO_STATUS_PENDING = "PENDING";
    public static final String TODO_STATUS_COMPLETED = "COMPLETED";
    public static final String TODO_RESULT_PASS = "检验合格";
    public static final String TODO_RESULT_FAIL = "检验不合格";
    public static final String TODO_RESULT_CONCESSION = "让步接收";
}
