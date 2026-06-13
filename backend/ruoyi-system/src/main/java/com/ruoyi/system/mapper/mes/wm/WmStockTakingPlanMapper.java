package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmStockTakingPlan;

public interface WmStockTakingPlanMapper
{
    public List<WmStockTakingPlan> selectWmStockTakingPlanList(WmStockTakingPlan entity);
    public List<WmStockTakingPlan> selectWmStockTakingPlanAll();
    public WmStockTakingPlan selectWmStockTakingPlanByPlanId(Long planId);
    public int insertWmStockTakingPlan(WmStockTakingPlan entity);
    public int updateWmStockTakingPlan(WmStockTakingPlan entity);
    public int deleteWmStockTakingPlanByPlanId(Long planId);
    public int deleteWmStockTakingPlanByPlanIds(Long[] planIds);
}