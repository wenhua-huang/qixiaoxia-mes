package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmProductSalesService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/product_sales")
public class WmProductSalesController extends BaseController
{
    @Autowired
    private IWmProductSalesService wmProductSalesService;

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductSales entity)
    {
        startPage();
        List<WmProductSales> list = wmProductSalesService.selectWmProductSalesList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmProductSales> list = wmProductSalesService.selectWmProductSalesAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:export')")
    @Log(title = "销售出库单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductSales entity)
    {
        List<WmProductSales> list = wmProductSalesService.selectWmProductSalesList(entity);
        ExcelUtil<WmProductSales> util = new ExcelUtil<WmProductSales>(WmProductSales.class);
        util.exportExcel(response, list, "销售出库单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:query')")
    @GetMapping(value = "/{salesId}")
    public AjaxResult getInfo(@PathVariable("salesId") Long salesId)
    {
        return AjaxResult.success(wmProductSalesService.selectWmProductSalesBySalesId(salesId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:add')")
    @Log(title = "销售出库单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductSales entity)
    {
        return toAjax(wmProductSalesService.insertWmProductSales(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:edit')")
    @Log(title = "销售出库单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductSales entity)
    {
        return toAjax(wmProductSalesService.updateWmProductSales(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales:remove')")
    @Log(title = "销售出库单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{salesIds}")
    public AjaxResult remove(@PathVariable Long[] salesIds)
    {
        return toAjax(wmProductSalesService.deleteWmProductSalesBySalesIds(salesIds));
    }
}