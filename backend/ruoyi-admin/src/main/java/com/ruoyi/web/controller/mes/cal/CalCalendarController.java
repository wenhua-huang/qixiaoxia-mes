package com.ruoyi.web.controller.mes.cal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.cal.CalendarUtil;
import com.ruoyi.system.domain.mes.cal.CalCalendar;
import com.ruoyi.system.domain.mes.cal.CalPlan;
import com.ruoyi.system.domain.mes.cal.CalTeamMember;
import com.ruoyi.system.domain.mes.cal.CalTeamshift;
import com.ruoyi.system.service.mes.cal.ICalPlanService;
import com.ruoyi.system.service.mes.cal.ICalTeamMemberService;
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

    @Autowired
    private ICalPlanService calPlanService;

    @Autowired
    private ICalTeamMemberService calTeamMemberService;

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
        String queryType = calCalendar.getQueryType();

        // Pre-compute matching plan IDs for TYPE query mode
        List<Long> matchingPlanIds = null;
        if ("TYPE".equals(queryType) && calCalendar.getCalendarType() != null) {
            CalPlan planParam = new CalPlan();
            planParam.setCalendarType(calCalendar.getCalendarType());
            List<CalPlan> matchingPlans = calPlanService.selectCalPlanList(planParam);
            if (matchingPlans != null && !matchingPlans.isEmpty()) {
                matchingPlanIds = matchingPlans.stream()
                        .map(CalPlan::getPlanId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
            }
        }

        // Pre-compute team ID for USER query mode
        Long userTeamId = null;
        if ("USER".equals(queryType) && calCalendar.getUserId() != null) {
            CalTeamMember memberParam = new CalTeamMember();
            memberParam.setUserId(calCalendar.getUserId());
            List<CalTeamMember> members = calTeamMemberService.selectCalTeamMemberList(memberParam);
            if (members != null && !members.isEmpty()) {
                userTeamId = members.get(0).getTeamId();
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        // For each day, query teamshifts
        for (CalCalendar cal : calendars) {
            CalTeamshift param = new CalTeamshift();
            try {
                Date shiftDay = sdf.parse(cal.getTheDay());
                param.setShiftDate(shiftDay);
            } catch (Exception e) {
                continue;
            }

            if ("TEAM".equals(queryType) && calCalendar.getTeamId() != null) {
                // Filter by team
                param.setTeamId(calCalendar.getTeamId());
            } else if ("TYPE".equals(queryType) && calCalendar.getCalendarType() != null) {
                // Filter by calendar type via plan relationship
                if (matchingPlanIds != null && !matchingPlanIds.isEmpty()) {
                    List<CalTeamshift> allShifts = calTeamshiftService.selectCalTeamshiftList(param);
                    List<CalTeamshift> filteredShifts = new ArrayList<>();
                    if (allShifts != null) {
                        for (CalTeamshift ts : allShifts) {
                            if (ts.getPlanId() != null && matchingPlanIds.contains(ts.getPlanId())) {
                                filteredShifts.add(ts);
                            }
                        }
                    }
                    cal.setTeamShifts(filteredShifts);
                    if (!filteredShifts.isEmpty()) {
                        cal.setShiftType(filteredShifts.get(0).getPlanCode());
                    }
                    continue;
                }
                // No matching plans → empty teamshifts for this day
                cal.setTeamShifts(new ArrayList<>());
                continue;
            } else if ("USER".equals(queryType) && calCalendar.getUserId() != null) {
                // Filter by user's team
                if (userTeamId != null) {
                    param.setTeamId(userTeamId);
                }
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
