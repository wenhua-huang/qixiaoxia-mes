package com.ruoyi.system.service.mes.cal;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 工作日历计算Service（甘特图排产专用）
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public interface IWorkCalendarService
{
    /**
     * 判断某天是否为工作日
     */
    boolean isWorkingDay(Date date, Long factoryId);

    /**
     * 获取时间范围内的工作时间区间列表
     * @return [{date, startTime, endTime}, ...]
     */
    List<Map<String, Object>> getWorkingRanges(Date from, Date to, Long factoryId);

    /**
     * 工作日历感知的时间推算
     * @param startTime 起始时间
     * @param workSeconds 工作秒数
     * @param factoryId 工厂ID
     * @return 结束时间
     */
    Date calculateEndTime(Date startTime, long workSeconds, Long factoryId);

    /**
     * 获取最近工作日
     */
    Date getNearestWorkingDay(Date date, Long factoryId);

    /**
     * 获取时间范围内的节假日/调休列表
     */
    List<Map<String, String>> getHolidays(Date from, Date to, Long factoryId);

    /**
     * 获取日期范围内每天的工作状态
     * @return [{date, working: true/false, reason}, ...]
     */
    List<Map<String, Object>> getDayStatusList(Date from, Date to, Long factoryId);
}
