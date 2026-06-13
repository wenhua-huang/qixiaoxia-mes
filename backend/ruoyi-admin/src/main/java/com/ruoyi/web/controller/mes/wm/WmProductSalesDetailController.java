package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmProductSalesDetailService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出库单明细表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/product_sales_detail")
public class WmProductSalesDetailController extends BaseController
{
    @Autowired
    private IWmProductSalesDetailService wmProductSalesDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductSalesDetail entity)
    {
        startPage();
        List<WmProductSalesDetail> list = wmProductSalesDetailService.selectWmProductSalesDetailList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmProductSalesDetail> list = wmProductSalesDetailService.selectWmProductSalesDetailAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:export')")
    @Log(title = "销售出库单明细表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductSalesDetail entity)
    {
        List<WmProductSalesDetail> list = wmProductSalesDetailService.selectWmProductSalesDetailList(entity);
        ExcelUtil<WmProductSalesDetail> util = new ExcelUtil<WmProductSalesDetail>(WmProductSalesDetail.class);
        util.exportExcel(response, list, "销售出库单明细表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId)
    {
        return AjaxResult.success(wmProductSalesDetailService.selectWmProductSalesDetailByDetailId(detailId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:add')")
    @Log(title = "销售出库单明细表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductSalesDetail entity)
    {
        return toAjax(wmProductSalesDetailService.insertWmProductSalesDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:edit')")
    @Log(title = "销售出库单明细表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductSalesDetail entity)
    {
        return toAjax(wmProductSalesDetailService.updateWmProductSalesDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_sales_detail:remove')")
    @Log(title = "销售出库单明细表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds)
    {
        return toAjax(wmProductSalesDetailService.deleteWmProductSalesDetailByDetailIds(detailIds));
    }
}