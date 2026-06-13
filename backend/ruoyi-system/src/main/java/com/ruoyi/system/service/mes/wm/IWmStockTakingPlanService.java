package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmStockTakingPlan;

public interface IWmStockTakingPlanService
{
    public List<WmStockTakingPlan> selectWmStockTakingPlanList(WmStockTakingPlan entity);
    public List<WmStockTakingPlan> selectWmStockTakingPlanAll();
    public WmStockTakingPlan selectWmStockTakingPlanByPlanId(Long planId);
    public int insertWmStockTakingPlan(WmStockTakingPlan entity);
    public int updateWmStockTakingPlan(WmStockTakingPlan entity);
    public int deleteWmStockTakingPlanByPlanId(Long planId);
    public int deleteWmStockTakingPlanByPlanIds(Long[] planIds);
}