package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmStockTakingService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmStockTaking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 盘点任务表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/stock_taking")
public class WmStockTakingController extends BaseController
{
    @Autowired
    private IWmStockTakingService wmStockTakingService;

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmStockTaking entity)
    {
        startPage();
        List<WmStockTaking> list = wmStockTakingService.selectWmStockTakingList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmStockTaking> list = wmStockTakingService.selectWmStockTakingAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:export')")
    @Log(title = "盘点任务表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmStockTaking entity)
    {
        List<WmStockTaking> list = wmStockTakingService.selectWmStockTakingList(entity);
        ExcelUtil<WmStockTaking> util = new ExcelUtil<WmStockTaking>(WmStockTaking.class);
        util.exportExcel(response, list, "盘点任务表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:query')")
    @GetMapping(value = "/{takingId}")
    public AjaxResult getInfo(@PathVariable("takingId") Long takingId)
    {
        return AjaxResult.success(wmStockTakingService.selectWmStockTakingByTakingId(takingId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:add')")
    @Log(title = "盘点任务表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmStockTaking entity)
    {
        return toAjax(wmStockTakingService.insertWmStockTaking(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:edit')")
    @Log(title = "盘点任务表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmStockTaking entity)
    {
        return toAjax(wmStockTakingService.updateWmStockTaking(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:stock_taking:remove')")
    @Log(title = "盘点任务表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{takingIds}")
    public AjaxResult remove(@PathVariable Long[] takingIds)
    {
        return toAjax(wmStockTakingService.deleteWmStockTakingByTakingIds(takingIds));
    }
}