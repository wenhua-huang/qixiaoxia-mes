package com.ruoyi.system.domain.mes.pro;

/**
 * 生产模块常量
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public class ProConstants
{
    /** 工序关系类型 */
    public static final String LINK_TYPE_SS = "SS";  // 顺序-必须先后
    public static final String LINK_TYPE_FS = "FS";  // 并行-可重叠

    /** 排产快照状态 */
    public static final String SNAPSHOT_DRAFT = "DRAFT";
    public static final String SNAPSHOT_PUBLISHED = "PUBLISHED";
    public static final String SNAPSHOT_DISCARDED = "DISCARDED";

    /** 任务状态 */
    public static final String TASK_STATUS_NORMAL = "NORMAL";
    public static final String TASK_STATUS_PREPARE = "PREPARE";

    /** 排产默认值 */
    public static final int DEFAULT_DURATION_MINUTES = 60;
    public static final int DEFAULT_SETUP_MINUTES = 0;
    public static final String DEFAULT_COLOR_CODE = "#00AEF3";
    public static final String DEFAULT_UNIT = "个";
    public static final String SYSTEM_USER = "system";

    /** 日历安全阀 */
    public static final int CALENDAR_MAX_LOOPS = 1000;
    public static final int CALENDAR_PROBE_DAYS = 7;
    public static final int NEAREST_WORKING_DAY_MAX = 7;
}
