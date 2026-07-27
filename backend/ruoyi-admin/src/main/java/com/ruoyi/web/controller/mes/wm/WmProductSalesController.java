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
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 权限统一为 mes:wm:sales:*（与 V36 菜单 seed 一致）
 *
 * @author qixiaoxia
 * @date 2026-07-22
 */
@RestController
@RequestMapping("/mes/wm/product_sales")
public class WmProductSalesController extends BaseController
{
    @Autowired
    private IWmProductSalesService wmProductSalesService;

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductSales entity)
    {
        startPage();
        List<WmProductSales> list = wmProductSalesService.selectWmProductSalesList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmProductSales> list = wmProductSalesService.selectWmProductSalesAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:export')")
    @Log(title = "销售出库单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductSales entity)
    {
        List<WmProductSales> list = wmProductSalesService.selectWmProductSalesList(entity);
        ExcelUtil<WmProductSales> util = new ExcelUtil<WmProductSales>(WmProductSales.class);
        util.exportExcel(response, list, "销售出库单数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:query')")
    @GetMapping(value = "/{salesId}")
    public AjaxResult getInfo(@PathVariable("salesId") Long salesId)
    {
        return AjaxResult.success(wmProductSalesService.selectWmProductSalesBySalesId(salesId));
    }

    /** 详情：聚合头+行+明细（出库弹窗用） */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:query')")
    @GetMapping("/detail/{salesId}")
    public AjaxResult getDetail(@PathVariable("salesId") Long salesId)
    {
        return AjaxResult.success(wmProductSalesService.getDetail(salesId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:add')")
    @Log(title = "销售出库单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductSales entity)
    {
        return toAjax(wmProductSalesService.insertWmProductSales(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:edit')")
    @Log(title = "销售出库单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductSales entity)
    {
        return toAjax(wmProductSalesService.updateWmProductSales(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:remove')")
    @Log(title = "销售出库单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{salesIds}")
    public AjaxResult remove(@PathVariable Long[] salesIds)
    {
        return toAjax(wmProductSalesService.deleteWmProductSalesBySalesIds(salesIds));
    }

    // ════════════════════ 业务生命周期端点 ════════════════════

    /** 过账出库：扣减库存，DRAFT/PARTIAL_POSTED → POSTED/PARTIAL_POSTED */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:post')")
    @Log(title = "销售出库过账", businessType = BusinessType.UPDATE)
    @PutMapping("/post/{salesId}")
    public AjaxResult post(@PathVariable("salesId") Long salesId,
                           @RequestBody List<WmProductSalesDetail> details)
    {
        return toAjax(wmProductSalesService.postOut(salesId, details));
    }

    /** 发货：登记物流信息，POSTED/PARTIAL_POSTED → SHIPPED */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:ship')")
    @Log(title = "销售出库发货", businessType = BusinessType.UPDATE)
    @PutMapping("/ship/{salesId}")
    public AjaxResult ship(@PathVariable("salesId") Long salesId,
                           @RequestBody WmProductSales info)
    {
        return toAjax(wmProductSalesService.ship(salesId, info));
    }

    /** 关闭：SHIPPED → CLOSED */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:close')")
    @Log(title = "销售出库关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{salesId}")
    public AjaxResult close(@PathVariable("salesId") Long salesId)
    {
        return toAjax(wmProductSalesService.close(salesId));
    }

    /** 作废：DRAFT/PARTIAL_POSTED → CANCELED */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:cancel')")
    @Log(title = "销售出库作废", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{salesId}")
    public AjaxResult cancel(@PathVariable("salesId") Long salesId)
    {
        return toAjax(wmProductSalesService.cancel(salesId));
    }

    /** 从销售订单生成出库单草稿（返回前端编辑，不落库） */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:add')")
    @GetMapping("/fromSaleOrder/{orderId}")
    public AjaxResult fromSaleOrder(@PathVariable("orderId") Long orderId)
    {
        return AjaxResult.success(wmProductSalesService.buildFromSaleOrder(orderId));
    }
}
