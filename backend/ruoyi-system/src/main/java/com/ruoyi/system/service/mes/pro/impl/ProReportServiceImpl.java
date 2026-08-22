package com.ruoyi.system.service.mes.pro.impl;

import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.vo.ProductivityRowVO;
import com.ruoyi.system.domain.mes.pro.vo.ReportOverviewVO;
import com.ruoyi.system.domain.mes.pro.vo.TaskStatusDistVO;
import com.ruoyi.system.domain.mes.pro.vo.TrendPointVO;
import com.ruoyi.system.mapper.mes.pro.ProReportMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.mes.pro.DelayLevelEvaluator;
import com.ruoyi.system.service.mes.pro.IProReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 生产统计报表 Service 实现
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
@Service
public class ProReportServiceImpl implements IProReportService
{
    @Autowired
    private ProReportMapper reportMapper;

    @Autowired
    private ISysConfigService configService;

    @Override
    public ReportOverviewVO overview(Date begin, Date end, List<Long> workshopIds, List<Long> teamIds)
    {
        Map<String, Object> m = reportMapper.selectOverviewCounts(begin, end, workshopIds, teamIds);
        if (m == null) m = Collections.emptyMap();
        long standard = nvl(reportMapper.selectStandardMinutes(begin, end, workshopIds, teamIds));
        long actual = nvl(reportMapper.selectActualMinutes(begin, end, workshopIds, teamIds));

        ReportOverviewVO vo = new ReportOverviewVO();
        vo.setWorkorderTotal(asInt(m.get("workorderTotal")));
        vo.setWorkorderCompleted(asInt(m.get("workorderCompleted")));
        vo.setWorkorderDelayed(asInt(m.get("workorderDelayed")));
        vo.setWorkorderInProgress(asInt(m.get("workorderInProgress")));
        vo.setStandardMinutes((int) standard);
        vo.setActualMinutes((int) actual);
        vo.setCompletionRate(rate(vo.getWorkorderCompleted(), vo.getWorkorderTotal()));
        vo.setDelayRate(rate(vo.getWorkorderDelayed(), vo.getWorkorderTotal()));
        vo.setEfficiencyPercent(efficiency((int) standard, (int) actual));
        return vo;
    }

    @Override
    public List<ProductivityRowVO> productivity(String groupBy, Date begin, Date end,
            List<Long> workshopIds, List<Long> teamIds)
    {
        String dim = normalizeGroupBy(groupBy);
        List<Map<String, Object>> output = selectOutput(dim, begin, end, workshopIds, teamIds);
        List<Map<String, Object>> hours = selectHours(dim, begin, end, workshopIds, teamIds);
        Map<Long, Map<String, Object>> hoursByKey = hoursByKey(hours);
        List<ProductivityRowVO> result = new ArrayList<>(output.size());
        for (Map<String, Object> out : output)
        {
            Long gid = asLong(out.get("groupId"));
            result.add(toProductivityRow(out, hoursByKey.get(gid)));
        }
        return result;
    }

    @Override
    public List<TrendPointVO> trend(Date begin, Date end, Long workshopId)
    {
        List<Long> wids = workshopId == null ? null : List.of(workshopId);
        Map<String, TrendPointVO> map = new TreeMap<>();
        for (Map<String, Object> r : reportMapper.selectTrendCreated(begin, end, wids))
        {
            trendPoint(map, r).setCreatedCount(asInt(r.get("createdCount")));
        }
        for (Map<String, Object> r : reportMapper.selectTrendCompleted(begin, end, workshopId))
        {
            TrendPointVO p = trendPoint(map, r);
            p.setCompletedCount(asInt(r.get("completedCount")));
            p.setDelayedCount(asInt(r.get("delayedCount")));
            p.setQualifiedQty(asDecimal(r.get("qualifiedQty")));
        }
        for (Map<String, Object> r : reportMapper.selectTrendMinutes(begin, end, workshopId))
        {
            trendPoint(map, r).setActualMinutes(asInt(r.get("actualMinutes")));
        }
        return fillDateGaps(begin, end, map);
    }

    @Override
    public List<TaskStatusDistVO> taskStatus(Date begin, Date end, Long workshopId)
    {
        List<Map<String, Object>> rows = reportMapper.selectTaskStatus(begin, end, workshopId);
        List<TaskStatusDistVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows)
        {
            TaskStatusDistVO vo = new TaskStatusDistVO();
            vo.setStatus((String) r.get("status"));
            vo.setCount(asInt(r.get("cnt")));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> detail(Date begin, Date end, Long workshopId, Long teamId)
    {
        List<Map<String, Object>> rows = reportMapper.selectDetail(begin, end, workshopId, teamId);
        int warnDays = cfgInt(ProConstants.CFG_WARN_DAYS, 2);
        Date now = new Date();
        for (Map<String, Object> row : rows)
        {
            String level = DelayLevelEvaluator.evaluateWorkorder(
                    (Date) row.get("requestDate"), (Date) row.get("finishDate"),
                    (String) row.get("status"), warnDays, now);
            row.put("delayLevel", level);
        }
        return rows;
    }

    // ── productivity 辅助 ──────────────────────────────────────

    private String normalizeGroupBy(String groupBy)
    {
        if (groupBy == null) return "WORKSHOP";
        return switch (groupBy) {
            case "TEAM", "WORKSTATION", "PROCESS" -> groupBy;
            default -> "WORKSHOP";
        };
    }

    private List<Map<String, Object>> selectOutput(String dim, Date begin, Date end,
            List<Long> workshopIds, List<Long> teamIds)
    {
        return switch (dim) {
            case "TEAM" -> reportMapper.selectTeamOutput(begin, end, teamIds);
            case "WORKSTATION" -> reportMapper.selectWorkstationOutput(begin, end, workshopIds);
            case "PROCESS" -> reportMapper.selectProcessOutput(begin, end, workshopIds);
            default -> reportMapper.selectWorkshopOutput(begin, end, workshopIds);
        };
    }

    private List<Map<String, Object>> selectHours(String dim, Date begin, Date end,
            List<Long> workshopIds, List<Long> teamIds)
    {
        return switch (dim) {
            case "TEAM" -> reportMapper.selectTeamHours(begin, end, teamIds);
            case "WORKSTATION" -> reportMapper.selectWorkstationHours(begin, end, workshopIds);
            case "PROCESS" -> reportMapper.selectProcessHours(begin, end, workshopIds);
            default -> reportMapper.selectWorkshopHours(begin, end, workshopIds);
        };
    }

    private Map<Long, Map<String, Object>> hoursByKey(List<Map<String, Object>> hours)
    {
        Map<Long, Map<String, Object>> map = new TreeMap<>();
        for (Map<String, Object> h : hours)
        {
            map.put(asLong(h.get("groupId")), h);
        }
        return map;
    }

    private ProductivityRowVO toProductivityRow(Map<String, Object> out, Map<String, Object> hrs)
    {
        ProductivityRowVO vo = new ProductivityRowVO();
        vo.setGroupId(asLong(out.get("groupId")));
        vo.setGroupName((String) out.get("groupName"));
        int workorderCount = asInt(out.get("workorderCount"));
        int completed = asInt(out.get("completedCount"));
        int delayed = asInt(out.get("delayedCount"));
        int standardMin = asInt(out.get("standardMinutes"));
        int actualMin = hrs == null ? 0 : asInt(hrs.get("actualMinutes"));
        BigDecimal qualified = asDecimal(out.get("qualifiedQty"));
        BigDecimal scrap = asDecimal(out.get("scrapQty"));

        vo.setWorkorderCount(workorderCount);
        vo.setCompletedCount(completed);
        vo.setDelayedCount(delayed);
        vo.setCompletionRate(rate(completed, workorderCount));
        vo.setDelayRate(rate(delayed, workorderCount));
        vo.setStandardOutput(asDecimal(out.get("standardOutput")));
        vo.setActualOutput(asDecimal(out.get("actualOutput")));
        vo.setStandardMinutes(standardMin);
        vo.setActualMinutes(actualMin);
        vo.setEfficiencyPercent(efficiency(standardMin, actualMin));
        vo.setQualifiedQty(qualified);
        vo.setScrapQty(scrap);
        vo.setQualifiedRate(qualifiedRate(qualified, scrap));
        return vo;
    }

    private Integer qualifiedRate(BigDecimal qualified, BigDecimal scrap)
    {
        BigDecimal total = qualified.add(scrap);
        if (total.compareTo(BigDecimal.ZERO) == 0) return 0;
        return qualified.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    // ── trend 辅助 ─────────────────────────────────────────────

    private TrendPointVO trendPoint(Map<String, TrendPointVO> map, Map<String, Object> row)
    {
        String key = dateKey(row.get("statDate"));
        return map.computeIfAbsent(key, k -> {
            TrendPointVO p = new TrendPointVO();
            p.setCreatedCount(0);
            p.setCompletedCount(0);
            p.setDelayedCount(0);
            p.setQualifiedQty(BigDecimal.ZERO);
            p.setActualMinutes(0);
            return p;
        });
    }

    private List<TrendPointVO> fillDateGaps(Date begin, Date end, Map<String, TrendPointVO> map)
    {
        List<TrendPointVO> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(begin);
        truncateToDay(cal);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        while (!cal.getTime().after(endCal.getTime()))
        {
            String key = fmt.format(cal.getTime());
            TrendPointVO p = map.get(key);
            if (p == null)
            {
                p = new TrendPointVO();
                p.setCreatedCount(0);
                p.setCompletedCount(0);
                p.setDelayedCount(0);
                p.setQualifiedQty(BigDecimal.ZERO);
                p.setActualMinutes(0);
            }
            p.setStatDate(java.sql.Date.valueOf(key));
            result.add(p);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return result;
    }

    private void truncateToDay(Calendar cal)
    {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private String dateKey(Object o)
    {
        if (o == null) return null;
        if (o instanceof java.sql.Date) return o.toString();
        if (o instanceof java.util.Date) return new SimpleDateFormat("yyyy-MM-dd").format((Date) o);
        if (o instanceof java.time.LocalDate) return o.toString();
        if (o instanceof java.time.LocalDateTime) return o.toString().substring(0, 10);
        return o.toString();
    }

    // ── 通用辅助 ───────────────────────────────────────────────

    private int asInt(Object o) { return o == null ? 0 : ((Number) o).intValue(); }

    private Long asLong(Object o) { return o == null ? 0L : ((Number) o).longValue(); }

    private BigDecimal asDecimal(Object o)
    {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof java.math.BigInteger) return new BigDecimal((java.math.BigInteger) o);
        if (o instanceof Number) return new BigDecimal(o.toString());
        return BigDecimal.ZERO;
    }

    private long nvl(Long v) { return v == null ? 0L : v; }

    private Integer rate(int part, int total)
    {
        return total == 0 ? 0 : Math.round(part * 100f / total);
    }

    private Integer efficiency(int standard, int actual)
    {
        if (standard == 0 || actual == 0) return null;
        return Math.round(standard * 100f / actual);
    }

    private int cfgInt(String key, int def)
    {
        String v = configService.selectConfigByKey(key);
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
