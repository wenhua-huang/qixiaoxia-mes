package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalPlanMapper;
import com.ruoyi.system.domain.mes.cal.CalPlan;
import com.ruoyi.system.service.mes.cal.ICalPlanService;

/**
 * 排班计划Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalPlanServiceImpl implements ICalPlanService 
{
    @Autowired
    private CalPlanMapper qxxCalPlanMapper;

    /**
     * 查询排班计划
     * 
     * @param planId 排班计划主键
     * @return 排班计划
     */
    @Override
    public CalPlan selectCalPlanByPlanId(Long planId)
    {
        return qxxCalPlanMapper.selectCalPlanByPlanId(planId);
    }

    /**
     * 查询排班计划列表
     * 
     * @param calPlan 排班计划
     * @return 排班计划
     */
    @Override
    public List<CalPlan> selectCalPlanList(CalPlan calPlan)
    {
        return qxxCalPlanMapper.selectCalPlanList(calPlan);
    }

    /**
     * 新增排班计划
     * 
     * @param calPlan 排班计划
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalPlan(CalPlan calPlan)
    {
        calPlan.setCreateTime(DateUtils.getNowDate());
        calPlan.setCreateBy(SecurityUtils.getUsername());
        return qxxCalPlanMapper.insertCalPlan(calPlan);
    }

    /**
     * 修改排班计划
     * 
     * @param calPlan 排班计划
     * @return 结果
     */
    @Override
    public int updateCalPlan(CalPlan calPlan)
    {
        calPlan.setUpdateTime(DateUtils.getNowDate());
        calPlan.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalPlanMapper.updateCalPlan(calPlan);
    }

    /**
     * 批量删除排班计划
     * 
     * @param planIds 需要删除的排班计划主键
     * @return 结果
     */
    @Override
    public int deleteCalPlanByPlanIds(Long[] planIds)
    {
        return qxxCalPlanMapper.deleteCalPlanByPlanIds(planIds);
    }

    /**
     * 删除排班计划信息
     * 
     * @param planId 排班计划主键
     * @return 结果
     */
    @Override
    public int deleteCalPlanByPlanId(Long planId)
    {
        return qxxCalPlanMapper.deleteCalPlanByPlanId(planId);
    }
}
