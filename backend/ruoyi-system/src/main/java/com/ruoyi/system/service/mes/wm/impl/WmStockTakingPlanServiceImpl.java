package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmStockTakingPlan;
import com.ruoyi.system.mapper.mes.wm.WmStockTakingPlanMapper;
import com.ruoyi.system.service.mes.wm.IWmStockTakingPlanService;

@Service
public class WmStockTakingPlanServiceImpl implements IWmStockTakingPlanService
{
    @Autowired
    private WmStockTakingPlanMapper wmStockTakingPlanMapper;

    @Override
    public List<WmStockTakingPlan> selectWmStockTakingPlanList(WmStockTakingPlan entity) {
        return wmStockTakingPlanMapper.selectWmStockTakingPlanList(entity);
    }

    @Override
    public List<WmStockTakingPlan> selectWmStockTakingPlanAll() {
        return wmStockTakingPlanMapper.selectWmStockTakingPlanAll();
    }

    @Override
    public WmStockTakingPlan selectWmStockTakingPlanByPlanId(Long planId) {
        return wmStockTakingPlanMapper.selectWmStockTakingPlanByPlanId(planId);
    }

    @Override
    @Transactional
    public int insertWmStockTakingPlan(WmStockTakingPlan entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmStockTakingPlanMapper.insertWmStockTakingPlan(entity);
    }

    @Override
    @Transactional
    public int updateWmStockTakingPlan(WmStockTakingPlan entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmStockTakingPlanMapper.updateWmStockTakingPlan(entity);
    }

    @Override
    @Transactional
    public int deleteWmStockTakingPlanByPlanId(Long planId) {
        return wmStockTakingPlanMapper.deleteWmStockTakingPlanByPlanId(planId);
    }

    @Override
    @Transactional
    public int deleteWmStockTakingPlanByPlanIds(Long[] planIds) {
        return wmStockTakingPlanMapper.deleteWmStockTakingPlanByPlanIds(planIds);
    }
}