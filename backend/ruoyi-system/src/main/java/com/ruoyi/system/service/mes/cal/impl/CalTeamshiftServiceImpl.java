package com.ruoyi.system.service.mes.cal.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.cal.CalendarUtil;
import com.ruoyi.system.domain.mes.cal.CalPlan;
import com.ruoyi.system.domain.mes.cal.CalPlanTeam;
import com.ruoyi.system.domain.mes.cal.CalShift;
import com.ruoyi.system.domain.mes.cal.CalTeamshift;
import com.ruoyi.system.mapper.mes.cal.CalPlanMapper;
import com.ruoyi.system.mapper.mes.cal.CalPlanTeamMapper;
import com.ruoyi.system.mapper.mes.cal.CalShiftMapper;
import com.ruoyi.system.mapper.mes.cal.CalTeamshiftMapper;
import com.ruoyi.system.service.mes.cal.ICalTeamshiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
 * 班组排班明细Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalTeamshiftServiceImpl implements ICalTeamshiftService
{
    @Autowired
    private CalTeamshiftMapper qxxCalTeamshiftMapper;

    @Autowired
    private CalPlanMapper calPlanMapper;

    @Autowired
    private CalPlanTeamMapper calPlanTeamMapper;

    @Autowired
    private CalShiftMapper calShiftMapper;

    /**
     * 查询班组排班明细
     * 
     * @param teamshiftId 班组排班明细主键
     * @return 班组排班明细
     */
    @Override
    public CalTeamshift selectCalTeamshiftByTeamshiftId(Long teamshiftId)
    {
        return qxxCalTeamshiftMapper.selectCalTeamshiftByTeamshiftId(teamshiftId);
    }

    /**
     * 查询班组排班明细列表
     * 
     * @param calTeamshift 班组排班明细
     * @return 班组排班明细
     */
    @Override
    public List<CalTeamshift> selectCalTeamshiftList(CalTeamshift calTeamshift)
    {
        return qxxCalTeamshiftMapper.selectCalTeamshiftList(calTeamshift);
    }

    /**
     * 新增班组排班明细
     * 
     * @param calTeamshift 班组排班明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalTeamshift(CalTeamshift calTeamshift)
    {
        calTeamshift.setCreateTime(DateUtils.getNowDate());
        calTeamshift.setCreateBy(SecurityUtils.getUsername());
        return qxxCalTeamshiftMapper.insertCalTeamshift(calTeamshift);
    }

    /**
     * 修改班组排班明细
     * 
     * @param calTeamshift 班组排班明细
     * @return 结果
     */
    @Override
    public int updateCalTeamshift(CalTeamshift calTeamshift)
    {
        calTeamshift.setUpdateTime(DateUtils.getNowDate());
        calTeamshift.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalTeamshiftMapper.updateCalTeamshift(calTeamshift);
    }

    /**
     * 批量删除班组排班明细
     * 
     * @param teamshiftIds 需要删除的班组排班明细主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamshiftByTeamshiftIds(Long[] teamshiftIds)
    {
        return qxxCalTeamshiftMapper.deleteCalTeamshiftByTeamshiftIds(teamshiftIds);
    }

    /**
     * 删除班组排班明细信息
     * 
     * @param teamshiftId 班组排班明细主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamshiftByTeamshiftId(Long teamshiftId)
    {
        return qxxCalTeamshiftMapper.deleteCalTeamshiftByTeamshiftId(teamshiftId);
    }

    /**
     * 根据排班计划生成排班明细记录
     * 按日期从 startDate 到 endDate 遍历，根据轮班方式将班组分配到各天各班次
     */
    @Override
    @Transactional
    public void genRecords(Long planId) {
        // 1. 查询排班计划
        CalPlan plan = calPlanMapper.selectCalPlanByPlanId(planId);
        if (plan == null) {
            return;
        }

        // 2. 查询该计划关联的班组
        CalPlanTeam teamParam = new CalPlanTeam();
        teamParam.setPlanId(planId);
        List<CalPlanTeam> planTeams = calPlanTeamMapper.selectCalPlanTeamList(teamParam);
        if (planTeams == null || planTeams.isEmpty()) {
            return;
        }

        // 3. 查询该计划的班次（按 shift_seq 排序）
        CalShift shiftParam = new CalShift();
        shiftParam.setPlanId(planId);
        List<CalShift> shifts = calShiftMapper.selectCalShiftList(shiftParam);
        if (shifts == null || shifts.isEmpty()) {
            return;
        }

        String shiftType = plan.getShiftType();
        Date startDate = plan.getStartDate();
        Date endDate = plan.getEndDate();

        // 计算天数
        long dayDiff = CalendarUtil.getDateDiff(startDate, endDate);
        int totalDays = (int) dayDiff + 1;

        // 4. 逐日生成排班明细
        for (int i = 0; i < totalDays; i++) {
            Date theDay = CalendarUtil.addDays(startDate, i);
            // 每天交换班次（shiftIndex 递增实现轮换）
            int shiftIndex = i;

            if ("SINGLE".equals(shiftType)) {
                // 单白班：一个班组 + 第一个班次
                saveTeamShift(theDay, planTeams.get(0), shifts.get(0), plan, 1);
            } else if ("SHIFT_TWO".equals(shiftType)) {
                // 两班倒：两个班组交替上白班和夜班
                CalPlanTeam team1 = planTeams.get(shiftIndex % planTeams.size());
                CalShift shift1 = shifts.get(0);
                CalPlanTeam team2 = planTeams.get((shiftIndex + 1) % planTeams.size());
                CalShift shift2 = shifts.get(1);
                saveTeamShift(theDay, team1, shift1, plan, 1);
                saveTeamShift(theDay, team2, shift2, plan, 2);
            } else if ("SHIFT_THREE".equals(shiftType)) {
                // 三班倒：三个班组轮换
                for (int j = 0; j < shifts.size() && j < planTeams.size(); j++) {
                    int idx = (shiftIndex + j) % planTeams.size();
                    saveTeamShift(theDay, planTeams.get(idx), shifts.get(j), plan, j + 1);
                }
            }
        }
    }

    /**
     * 保存单条排班明细（存在则更新，不存在则新增）
     */
    private void saveTeamShift(Date shiftDate, CalPlanTeam team, CalShift shift, CalPlan plan, int orderNum) {
        // 查询是否已存在
        CalTeamshift query = new CalTeamshift();
        query.setShiftDate(shiftDate);
        query.setTeamId(team.getTeamId());
        query.setPlanId(plan.getPlanId());
        List<CalTeamshift> existingList = qxxCalTeamshiftMapper.selectCalTeamshiftList(query);

        CalTeamshift record;
        if (existingList != null && !existingList.isEmpty()) {
            record = existingList.get(0);
        } else {
            record = new CalTeamshift();
            record.setCreateTime(DateUtils.getNowDate());
            record.setCreateBy(SecurityUtils.getUsername());
        }

        record.setShiftDate(shiftDate);
        record.setTeamId(team.getTeamId());
        record.setTeamCode(team.getTeamCode());
        record.setTeamName(team.getTeamName());
        record.setShiftId(shift.getShiftId());
        record.setShiftName(shift.getShiftName());
        record.setPlanId(plan.getPlanId());
        record.setPlanCode(plan.getPlanCode());
        record.setPlanName(plan.getPlanName());
        record.setOrderNum((long) orderNum);
        record.setUpdateTime(DateUtils.getNowDate());
        record.setUpdateBy(SecurityUtils.getUsername());

        if (existingList != null && !existingList.isEmpty()) {
            qxxCalTeamshiftMapper.updateCalTeamshift(record);
        } else {
            qxxCalTeamshiftMapper.insertCalTeamshift(record);
        }
    }
}
