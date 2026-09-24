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

    /** 选出口分布式锁前缀（拼接 exceptionId） */
    public static final String LOCK_RESOLVE_PREFIX = "pro:exception:resolve:";

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

    /** 批量查询工单数上限（消 N+1 接口） */
    public static final int BATCH_STATE_MAX = 100;

    /** 完工硬拦文案中列举异常单号的最大条数 */
    public static final int BLOCK_CODE_SAMPLE = 3;

    /** 是/否 */
    public static final String YES = "Y";
    public static final String NO = "N";

    /** 补料采购单币种 */
    public static final String PO_CURRENCY_CNY = "CNY";

    /** 作废原因写入处理结论时的前缀（台账/导出可辨识） */
    public static final String VOID_CONCLUSION_PREFIX = "【作废】";

    private ProExceptionConstants() {}
}
