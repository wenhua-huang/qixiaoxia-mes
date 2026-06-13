package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmStockTakingPlanService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmStockTakingPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 盘点方案表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/stock_taking_plan")
public class WmStockTakingPlanController extends BaseController
{
    @Autowired
    private IWmStockTakingPlanService wmStockTakingPlanService;

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmStockTakingPlan entity)
    {
        startPage();
        List<WmStockTakingPlan> list = wmStockTakingPlanService.selectWmStockTakingPlanList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmStockTakingPlan> list = wmStockTakingPlanService.selectWmStockTakingPlanAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:export')")
    @Log(title = "盘点方案表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmStockTakingPlan entity)
    {
        List<WmStockTakingPlan> list = wmStockTakingPlanService.selectWmStockTakingPlanList(entity);
        ExcelUtil<WmStockTakingPlan> util = new ExcelUtil<WmStockTakingPlan>(WmStockTakingPlan.class);
        util.exportExcel(response, list, "盘点方案表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:query')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") Long planId)
    {
        return AjaxResult.success(wmStockTakingPlanService.selectWmStockTakingPlanByPlanId(planId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:add')")
    @Log(title = "盘点方案表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmStockTakingPlan entity)
    {
        return toAjax(wmStockTakingPlanService.insertWmStockTakingPlan(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:edit')")
    @Log(title = "盘点方案表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmStockTakingPlan entity)
    {
        return toAjax(wmStockTakingPlanService.updateWmStockTakingPlan(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking_plan:remove')")
    @Log(title = "盘点方案表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(wmStockTakingPlanService.deleteWmStockTakingPlanByPlanIds(planIds));
    }
}