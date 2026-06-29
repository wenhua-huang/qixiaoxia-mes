package com.ruoyi.system.service.mes.cal.impl;

import java.text.SimpleDateFormat;
import java.util.*;
import com.ruoyi.common.utils.cal.CalendarUtil;
import com.ruoyi.system.domain.mes.cal.*;
import com.ruoyi.system.mapper.mes.cal.*;
import com.ruoyi.system.service.mes.cal.IWorkCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工作日历计算服务实现
 * 基于 qxx_cal_teamshift + qxx_cal_shift + qxx_cal_holiday
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
@Service
public class WorkCalendarServiceImpl implements IWorkCalendarService
{
    @Autowired
    private CalTeamshiftMapper teamshiftMapper;
    @Autowired
    private CalShiftMapper shiftMapper;
    @Autowired
    private CalHolidayMapper holidayMapper;

    private static final int MAX_LOOPS = 1000; // 安全阀
    private final ThreadLocal<Map<String, Boolean>> workingDayCache = ThreadLocal.withInitial(HashMap::new);

    private String cacheKey(Date date, Long factoryId) { return factoryId + "_" + CalendarUtil.getDateStr(date); }

    @Override
    public boolean isWorkingDay(Date date, Long factoryId) {
        String key = cacheKey(date, factoryId);
        Boolean cached = workingDayCache.get().get(key);
        if (cached != null) return cached;

        boolean result;
        // 1. 检查节假日(优先)
        CalHoliday h = new CalHoliday();
        h.setFactoryId(factoryId);
        h.setHolidayDate(date);
        h.setEnableFlag("1");
        List<CalHoliday> holidays = holidayMapper.selectCalHolidayList(h);
        if (!holidays.isEmpty()) {
            result = "WORKDAY".equals(holidays.get(0).getHolidayType());
        } else {
            // 2. 有排班记录 → 工作日
            CalTeamshift q = new CalTeamshift();
            q.setFactoryId(factoryId);
            q.setShiftDate(date);
            if (teamshiftMapper.countByDate(q) > 0) {
                result = true;
            } else {
                // 3. 默认：周一~五工作日，周六日休息
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                result = dow != Calendar.SUNDAY && dow != Calendar.SATURDAY;
            }
        }
        workingDayCache.get().put(key, result);
        return result;
    }

    @Override
    public List<Map<String, Object>> getWorkingRanges(Date from, Date to, Long factoryId) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 查询日期范围内的 teamshift
        CalTeamshift q = new CalTeamshift();
        q.setFactoryId(factoryId);
        Map<String, Object> params = new HashMap<>();
        params.put("beginShiftDate", CalendarUtil.getDateStr(from));
        params.put("endShiftDate", CalendarUtil.getDateStr(to));
        q.setParams(params);

        List<CalTeamshift> list = teamshiftMapper.selectCalTeamshiftList(q);
        Set<Long> seenShiftIds = new HashSet<>();

        for (CalTeamshift ts : list) {
            // 跳过节假日
            if (!isWorkingDay(ts.getShiftDate(), factoryId)) continue;

            // 获取 shift start/end time
            CalShift shift = null;
            if (!seenShiftIds.contains(ts.getShiftId())) {
                shift = shiftMapper.selectCalShiftByShiftId(ts.getShiftId());
                seenShiftIds.add(ts.getShiftId());
            }
            if (shift == null) continue;

            Map<String, Object> range = new LinkedHashMap<>();
            range.put("date", ts.getShiftDate());
            range.put("startTime", shift.getStartTime());
            range.put("endTime", shift.getEndTime());
            result.add(range);
        }
        return result;
    }

    @Override
    public Date calculateEndTime(Date startTime, long workSeconds, Long factoryId) {
        if (workSeconds <= 0) return startTime;

        // 快速检测：前 7 天无日历数据 → 只考虑周末
        boolean hasData = false;
        for (int i = 0; i < 7; i++) {
            Date d = CalendarUtil.addDays(startTime, i);
            if (getOneDayRange(d, factoryId) != null) { hasData = true; break; }
        }
        if (!hasData) {
            // 逐天推进，跳过周末
            long remaining = workSeconds;
            Date cur = startTime;
            int loops = 0;
            while (remaining > 0 && loops < 1000) {
                Calendar cal = Calendar.getInstance(); cal.setTime(cur);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                if (dow == Calendar.SATURDAY) { cur = CalendarUtil.addDays(cur, 2); continue; }
                if (dow == Calendar.SUNDAY) { cur = CalendarUtil.addDays(cur, 1); continue; }
                long secsToday = remaining > 86400 ? 86400 : remaining; // 一天最多24h
                remaining -= secsToday;
                if (remaining <= 0) return new Date(cur.getTime() + secsToday * 1000);
                cur = CalendarUtil.addDays(cur, 1);
                loops++;
            }
            return new Date(startTime.getTime() + workSeconds * 1000);
        }

        Date currentTime = startTime;
        long remainingMs = workSeconds * 1000;
        Date currentDay = zeroTime(currentTime);
        int loops = 0;

        while (remainingMs > 0 && loops < MAX_LOOPS) {
            if (!isWorkingDay(currentDay, factoryId)) {
                currentDay = CalendarUtil.addDays(currentDay, 1);
                currentTime = currentDay;
                loops++;
                continue;
            }

            Map<String, Object> range = getOneDayRange(currentDay, factoryId);
            if (range == null) {
                currentDay = CalendarUtil.addDays(currentDay, 1);
                currentTime = currentDay;
                loops++;
                continue;
            }

            Date rangeStart = toFullDate(currentDay, (Date) range.get("startTime"));
            Date rangeEnd = toFullDate(currentDay, (Date) range.get("endTime"));
            if (rangeEnd.before(rangeStart) || rangeEnd.equals(rangeStart)) {
                rangeEnd = CalendarUtil.addDays(rangeEnd, 1);
            }
            if (currentTime.after(rangeStart)) { rangeStart = currentTime; }
            if (!rangeStart.before(rangeEnd)) {
                currentDay = CalendarUtil.addDays(currentDay, 1);
                currentTime = currentDay;
                loops++;
                continue;
            }

            long rangeMs = rangeEnd.getTime() - rangeStart.getTime();
            if (remainingMs > rangeMs) {
                remainingMs -= rangeMs;
                currentTime = rangeEnd;
            } else {
                return new Date(rangeStart.getTime() + remainingMs);
            }
            currentDay = CalendarUtil.addDays(currentDay, 1);
            currentTime = currentDay;
            loops++;
        }
        return new Date(startTime.getTime() + workSeconds * 1000);
    }

    private Boolean hasAnyCalendarCache = null;
    private boolean hasAnyCalendar(Long factoryId) {
        if (hasAnyCalendarCache != null) return hasAnyCalendarCache;
        CalTeamshift q = new CalTeamshift();
        q.setFactoryId(factoryId);
        // 只查一条判断有无数据
        hasAnyCalendarCache = !teamshiftMapper.selectCalTeamshiftList(q).isEmpty();
        return hasAnyCalendarCache;
    }

    private static final long MAX_DAY_SPAN = 90L * 86400000L; // 最大90天

    @Override
    public List<Map<String, Object>> getDayStatusList(Date from, Date to, Long factoryId) {
        Date end = to;
        if (to.getTime() - from.getTime() > MAX_DAY_SPAN) {
            end = new Date(from.getTime() + MAX_DAY_SPAN);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        // 预加载节假日
        CalHoliday hq = new CalHoliday(); hq.setFactoryId(factoryId); hq.setEnableFlag("1");
        List<CalHoliday> holidays = holidayMapper.selectCalHolidayList(hq);
        Map<String, String> holidayMap = new HashMap<>();
        for (CalHoliday h : holidays) holidayMap.put(CalendarUtil.getDateStr(h.getHolidayDate()), h.getHolidayType());

        Date d = from;
        while (!d.after(end)) {
            String ds = CalendarUtil.getDateStr(d);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", ds);

            String ht = holidayMap.get(ds);
            if ("HOLIDAY".equals(ht)) {
                item.put("working", false); item.put("reason", "节假日");
            } else if ("WORKDAY".equals(ht)) {
                item.put("working", true); item.put("reason", "调休上班");
            } else if (isWorkingDay(d, factoryId)) {
                item.put("working", true); item.put("reason", "排班日");
            } else {
                item.put("working", false); item.put("reason", "未排班");
            }
            result.add(item);
            d = CalendarUtil.addDays(d, 1);
        }
        return result;
    }

    @Override
    public List<Map<String, String>> getHolidays(Date from, Date to, Long factoryId) {
        List<Map<String, String>> result = new ArrayList<>();
        CalHoliday q = new CalHoliday();
        q.setFactoryId(factoryId);
        q.setEnableFlag("1");
        List<CalHoliday> list = holidayMapper.selectCalHolidayList(q);
        for (CalHoliday h : list) {
            if (h.getHolidayDate().before(from) || h.getHolidayDate().after(to)) continue;
            Map<String, String> item = new LinkedHashMap<>();
            item.put("date", CalendarUtil.getDateStr(h.getHolidayDate()));
            item.put("name", h.getHolidayName());
            item.put("type", h.getHolidayType());
            result.add(item);
        }
        return result;
    }

    @Override
    public Date getNearestWorkingDay(Date date, Long factoryId) {
        // 无日历数据时默认当天即为工作日(仅跳过周末)
        if (!isWorkingDay(date, factoryId)) {
            Date d = CalendarUtil.addDays(date, 1);
            int loops = 0;
            while (!isWorkingDay(d, factoryId) && loops < 7) {
                d = CalendarUtil.addDays(d, 1);
                loops++;
            }
            return d;
        }
        return date;
    }

    // ---- helpers ----

    private Date zeroTime(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private final ThreadLocal<Map<String, Map<String, Object>>> rangeCache = ThreadLocal.withInitial(HashMap::new);

    private Map<String, Object> getOneDayRange(Date date, Long factoryId) {
        String key = cacheKey(date, factoryId);
        Map<String, Object> cached = rangeCache.get().get(key);
        if (cached != null) return cached.isEmpty() ? null : cached;

        CalTeamshift q = new CalTeamshift();
        q.setFactoryId(factoryId);
        q.setShiftDate(date);
        List<CalTeamshift> list = teamshiftMapper.selectCalTeamshiftList(q);
        if (list.isEmpty()) {
            rangeCache.get().put(key, Collections.emptyMap()); // 缓存空结果
            return null;
        }

        CalTeamshift ts = list.get(0);
        CalShift shift = shiftMapper.selectCalShiftByShiftId(ts.getShiftId());
        if (shift == null) {
            rangeCache.get().put(key, Collections.emptyMap());
            return null;
        }

        Map<String, Object> range = new LinkedHashMap<>();
        range.put("startTime", shift.getStartTime());
        range.put("endTime", shift.getEndTime());
        rangeCache.get().put(key, range);
        return range;
    }

    private Date toFullDate(Date day, Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(day);
        // CalShift.startTime/endTime 是 java.util.Date (java.sql.Time)，只取 HH:mm:ss 部分
        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(time);
        // 如果年份是1970，说明是纯Time字段，直接取时分秒
        if (timeCal.get(Calendar.YEAR) == 1970) {
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        } else {
            // 完整datetime，直接使用时间部分
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        }
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
