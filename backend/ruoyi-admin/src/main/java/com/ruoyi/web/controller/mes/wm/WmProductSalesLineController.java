package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmProductSalesLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出库单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/product_sales_line")
public class WmProductSalesLineController extends BaseController
{
    @Autowired
    private IWmProductSalesLineService wmProductSalesLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductSalesLine entity)
    {
        startPage();
        List<WmProductSalesLine> list = wmProductSalesLineService.selectWmProductSalesLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmProductSalesLine> list = wmProductSalesLineService.selectWmProductSalesLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:export')")
    @Log(title = "销售出库单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductSalesLine entity)
    {
        List<WmProductSalesLine> list = wmProductSalesLineService.selectWmProductSalesLineList(entity);
        ExcelUtil<WmProductSalesLine> util = new ExcelUtil<WmProductSalesLine>(WmProductSalesLine.class);
        util.exportExcel(response, list, "销售出库单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmProductSalesLineService.selectWmProductSalesLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:add')")
    @Log(title = "销售出库单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductSalesLine entity)
    {
        return toAjax(wmProductSalesLineService.insertWmProductSalesLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:edit')")
    @Log(title = "销售出库单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductSalesLine entity)
    {
        return toAjax(wmProductSalesLineService.updateWmProductSalesLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_line:remove')")
    @Log(title = "销售出库单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmProductSalesLineService.deleteWmProductSalesLineByLineIds(lineIds));
    }
}