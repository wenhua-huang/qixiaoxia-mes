package com.ruoyi.web.controller.mes.sal;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
import com.ruoyi.system.service.mes.sal.ISalOrderService;

/**
 * 销售订单Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
@RestController
@RequestMapping("/mes/sal/order")
public class SalOrderController extends BaseController
{
    @Autowired
    private ISalOrderService salOrderService;

    @PreAuthorize("@ss.hasPermi('mes:sal:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(SalOrder salOrder)
    {
        startPage();
        List<SalOrder> list = salOrderService.selectSalOrderList(salOrder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:list')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        return AjaxResult.success(salOrderService.selectAllConvertible());
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:export')")
    @Log(title = "销售订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SalOrder salOrder)
    {
        List<SalOrder> list = salOrderService.selectSalOrderList(salOrder);
        ExcelUtil<SalOrder> util = new ExcelUtil<SalOrder>(SalOrder.class);
        util.exportExcel(response, list, "销售订单数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:query')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return AjaxResult.success(salOrderService.selectSalOrderByOrderId(orderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:query')")
    @GetMapping("/detail/{orderId}")
    public AjaxResult getDetail(@PathVariable("orderId") Long orderId)
    {
        return AjaxResult.success(salOrderService.getDetail(orderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:add')")
    @Log(title = "销售订单", businessType = BusinessType.INSERT)
    @PostMapping("/createWithLines")
    public AjaxResult createWithLines(@RequestBody SalOrderCreateRequest req)
    {
        return AjaxResult.success(salOrderService.createWithLines(req));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:edit')")
    @Log(title = "销售订单", businessType = BusinessType.UPDATE)
    @PutMapping("/updateWithLines")
    public AjaxResult updateWithLines(@RequestBody SalOrderCreateRequest req)
    {
        return AjaxResult.success(salOrderService.updateWithLines(req));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:edit')")
    @Log(title = "销售订单确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{orderId}")
    public AjaxResult confirm(@PathVariable("orderId") Long orderId)
    {
        return toAjax(salOrderService.confirmOrder(orderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:edit')")
    @Log(title = "销售订单关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{orderId}")
    public AjaxResult close(@PathVariable("orderId") Long orderId)
    {
        return toAjax(salOrderService.closeOrder(orderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:edit')")
    @Log(title = "销售订单取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{orderId}")
    public AjaxResult cancel(@PathVariable("orderId") Long orderId)
    {
        return toAjax(salOrderService.cancelOrder(orderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:workorder')")
    @Log(title = "销售订单转工单", businessType = BusinessType.INSERT)
    @PostMapping("/toWorkorder")
    public AjaxResult toWorkorder(@RequestBody SalOrderToWorkorderRequest req)
    {
        ProWorkorder wo = salOrderService.toWorkorder(req);
        return AjaxResult.success(wo);
    }

    @PreAuthorize("@ss.hasPermi('mes:sal:order:remove')")
    @Log(title = "销售订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(salOrderService.deleteSalOrderByOrderIds(orderIds));
    }
}
