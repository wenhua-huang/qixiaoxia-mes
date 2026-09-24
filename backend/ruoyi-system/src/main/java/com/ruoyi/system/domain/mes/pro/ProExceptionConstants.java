package com.ruoyi.system.domain.mes.pro;

/**
 * 生产异常模块常量
 *
 * @author qixiaoxia
 */
public class ProExceptionConstants
{
    /** 异常单自动编码规则 code（见 V157：EX+yyyyMMdd+'-'+3位日流水） */
    public static final String RULE_CODE = "PRO_EXCEPTION_CODE";

    /** 异常单写操作分布式锁前缀（拼接 exceptionId）：补全/选出口/关闭/作废共用，防状态机并发覆盖 */
    public static final String LOCK_PREFIX = "pro:exception:lock:";

    /** 开返工/补做任务序号锁前缀（拼接 originTaskId）：跨异常单同原任务取 -En 序号互斥 */
    public static final String LOCK_TASK_PREFIX = "pro:exception:task:";

    /** 关联对象类型：工序任务（一期唯一，预留二期销售订单） */
    public static final String TARGET_TYPE_TASK = "TASK";

    /** 处理产生单据类型 */
    public static final String DOC_TYPE_TASK = "TASK";
    public static final String DOC_TYPE_PUR_ORDER = "PUR_ORDER";

    /** 异常任务任务名后缀 */
    public static final String TASK_SUFFIX_REWORK = "（返工）";
    public static final String TASK_SUFFIX_REMAKE = "（补做）";

    /** 异常任务编号连接符：原任务编号-E1 */
    public static final String TASK_CODE_EXCEPTION_SEP = "-E";

    /** 现场照片上限（PC/手机端九宫格一致） */
    public static final int MAX_SCENE_IMAGES = 9;

    /** scene_images 列长度上限（V159 varchar(2000)） */
    public static final int SCENE_IMAGES_MAX_LEN = 2000;

    /** 批量查询工单数上限（消 N+1 接口） */
    public static final int BATCH_STATE_MAX = 100;

    /** 完工硬拦文案中列举异常单号的最大条数 */
    public static final int BLOCK_CODE_SAMPLE = 3;

    /** 是/否 */
    public static final String YES = "Y";
    public static final String NO = "N";

    /** 补料采购单币种 */
    public static final String PO_CURRENCY_CNY = "CNY";

    /** 补料采购单供应商占位 id（0 不对应真实供应商，DRAFT 下发前必须补供应商） */
    public static final Long VENDOR_PENDING_ID = 0L;

    /** 作废原因写入处理结论时的前缀（台账/导出可辨识） */
    public static final String VOID_CONCLUSION_PREFIX = "【作废】";

    /** 关闭异常单时处理结论的最少字数 */
    public static final int MIN_CLOSE_CONCLUSION_LEN = 2;

    /** conclusion 列长度上限（qxx_pro_exception.conclusion varchar(1000)），所有写结论入口服务端兜底 */
    public static final int MAX_CONCLUSION_LEN = 1000;

    /** 作废原因字数上限：conclusion 落库为【作废】前缀 + 原因，需给前缀留位 */
    public static final int MAX_VOID_REASON_LEN = MAX_CONCLUSION_LEN - VOID_CONCLUSION_PREFIX.length();

    private ProExceptionConstants() {}
}
