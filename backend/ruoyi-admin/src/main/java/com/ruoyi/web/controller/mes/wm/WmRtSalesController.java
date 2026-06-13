package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmRtSalesService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmRtSales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售退货单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/rt_sales")
public class WmRtSalesController extends BaseController
{
    @Autowired
    private IWmRtSalesService wmRtSalesService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtSales entity)
    {
        startPage();
        List<WmRtSales> list = wmRtSalesService.selectWmRtSalesList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmRtSales> list = wmRtSalesService.selectWmRtSalesAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:export')")
    @Log(title = "销售退货单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtSales entity)
    {
        List<WmRtSales> list = wmRtSalesService.selectWmRtSalesList(entity);
        ExcelUtil<WmRtSales> util = new ExcelUtil<WmRtSales>(WmRtSales.class);
        util.exportExcel(response, list, "销售退货单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:query')")
    @GetMapping(value = "/{rtId}")
    public AjaxResult getInfo(@PathVariable("rtId") Long rtId)
    {
        return AjaxResult.success(wmRtSalesService.selectWmRtSalesByRtId(rtId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:add')")
    @Log(title = "销售退货单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtSales entity)
    {
        return toAjax(wmRtSalesService.insertWmRtSales(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:edit')")
    @Log(title = "销售退货单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtSales entity)
    {
        return toAjax(wmRtSalesService.updateWmRtSales(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales:remove')")
    @Log(title = "销售退货单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{rtIds}")
    public AjaxResult remove(@PathVariable Long[] rtIds)
    {
        return toAjax(wmRtSalesService.deleteWmRtSalesByRtIds(rtIds));
    }
}