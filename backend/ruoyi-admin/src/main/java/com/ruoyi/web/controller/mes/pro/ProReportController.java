package com.ruoyi.web.controller.mes.pro;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.mes.pro.IProReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 生产统计报表 Controller
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
@RestController
@RequestMapping("/mes/pro/report")
public class ProReportController extends BaseController
{
    @Autowired
    private IProReportService reportService;

    /**
     * 总览 KPI：工单计数 + 标准/实际工时 + 效率。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required = false) Date beginTime,
                               @RequestParam(required = false) Date endTime,
                               @RequestParam(required = false) List<Long> workshopIds,
                               @RequestParam(required = false) List<Long> teamIds)
    {
        Date[] range = defaultRange(beginTime, endTime);
        return success(reportService.overview(range[0], range[1], workshopIds, teamIds));
    }

    /**
     * 产能/工时/效率统计，按维度分组（WORKSHOP/WORKSTATION/PROCESS/TEAM）。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/productivity")
    public AjaxResult productivity(@RequestParam(defaultValue = "WORKSHOP") String groupBy,
                                   @RequestParam(required = false) Date beginTime,
                                   @RequestParam(required = false) Date endTime,
                                   @RequestParam(required = false) List<Long> workshopIds,
                                   @RequestParam(required = false) List<Long> teamIds)
    {
        Date[] range = defaultRange(beginTime, endTime);
        return success(reportService.productivity(groupBy, range[0], range[1], workshopIds, teamIds));
    }

    /**
     * 按日趋势：新建/完工/延期/合格数/实际工时。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam(required = false) Date beginTime,
                            @RequestParam(required = false) Date endTime,
                            @RequestParam(required = false) Long workshopId)
    {
        Date[] range = defaultRange(beginTime, endTime);
        return success(reportService.trend(range[0], range[1], workshopId));
    }

    /**
     * 任务状态分布。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/taskStatus")
    public AjaxResult taskStatus(@RequestParam(required = false) Date beginTime,
                                 @RequestParam(required = false) Date endTime,
                                 @RequestParam(required = false) Long workshopId)
    {
        Date[] range = defaultRange(beginTime, endTime);
        return success(reportService.taskStatus(range[0], range[1], workshopId));
    }

    /**
     * 报表明细（工单粒度，分页）。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/detail")
    public TableDataInfo detail(@RequestParam(required = false) Date beginTime,
                                @RequestParam(required = false) Date endTime,
                                @RequestParam(required = false) Long workshopId,
                                @RequestParam(required = false) Long teamId)
    {
        Date[] range = defaultRange(beginTime, endTime);
        startPage();
        return getDataTable(reportService.detail(range[0], range[1], workshopId, teamId));
    }

    /**
     * 默认本月 1 日 00:00 到 now。
     * 用户传的结束日（YYYY-MM-DD 解析为当天 00:00）补齐到 23:59:59.999，
     * 否则 BETWEEN 会漏掉结束日当天 00:00 之后的数据。
     */
    private Date[] defaultRange(Date begin, Date end)
    {
        if (begin != null && end != null) return new Date[]{begin, endOfDay(end)};
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return new Date[]{c.getTime(), new Date()};
    }

    private Date endOfDay(Date date)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }
}
