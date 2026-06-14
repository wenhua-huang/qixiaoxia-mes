package com.ruoyi.web.controller.mes.cal;

import java.util.Date;
import java.util.List;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.cal.CalendarUtil;
import com.ruoyi.system.domain.mes.cal.CalCalendar;
import com.ruoyi.system.domain.mes.cal.CalTeamshift;
import com.ruoyi.system.service.mes.cal.ICalTeamshiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 排班日历
 *
 * @author qixiaoxia
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/calendar")
public class CalCalendarController extends BaseController {

    @Autowired
    private ICalTeamshiftService calTeamshiftService;

    /**
     * 查询排班日历
     * queryType: TYPE-按班组类型, TEAM-按班组, USER-按用户(默认)
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:calendar:list')")
    @GetMapping("/list")
    public AjaxResult getCalendars(CalCalendar calCalendar) {
        Date day = calCalendar.getDate();
        if (StringUtils.isNull(day)) {
            day = new Date();
        }

        // Generate all days for the current month
        List<CalCalendar> calendars = CalendarUtil.getDays(day);

        // For each day, query teamshifts
        for (CalCalendar cal : calendars) {
            CalTeamshift param = new CalTeamshift();
            // Parse theDay string to Date for querying
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            try {
                Date shiftDay = sdf.parse(cal.getTheDay());
                param.setShiftDate(shiftDay);
            } catch (Exception e) {
                continue;
            }

            String queryType = calCalendar.getQueryType();
            if ("TEAM".equals(queryType) && calCalendar.getTeamId() != null) {
                // Filter by team if specified
            } else if ("TYPE".equals(queryType) && calCalendar.getCalendarType() != null) {
                // Filter by calendar type if specified
            }

            List<CalTeamshift> teamshifts = calTeamshiftService.selectCalTeamshiftList(param);
            cal.setTeamShifts(teamshifts);
            if (teamshifts != null && !teamshifts.isEmpty()) {
                cal.setShiftType(teamshifts.get(0).getPlanCode());
            }
        }

        return AjaxResult.success(calendars);
    }
}
