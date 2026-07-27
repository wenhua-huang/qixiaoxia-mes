package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmProductSalesShipmentService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出库-发运单 Controller（多次发运 + 签收回单）
 * 权限：mes:wm:sales:ship（发运/修改/删除/取消）、mes:wm:sales:receive（签收）
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
@RestController
@RequestMapping("/mes/wm/product_sales_shipment")
public class WmProductSalesShipmentController extends BaseController
{
    @Autowired
    private IWmProductSalesShipmentService shipmentService;

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductSalesShipment entity)
    {
        startPage();
        List<WmProductSalesShipment> list = shipmentService.selectWmProductSalesShipmentList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/bySales/{salesId}")
    public AjaxResult bySales(@PathVariable("salesId") Long salesId)
    {
        return AjaxResult.success(shipmentService.selectShipmentsBySalesId(salesId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/{shipmentId}")
    public AjaxResult getInfo(@PathVariable("shipmentId") Long shipmentId)
    {
        return AjaxResult.success(shipmentService.selectWmProductSalesShipmentByShipmentId(shipmentId));
    }

    /** 新增发运（核心）：勾选装箱、登记物流、回写头表发运汇总 */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:ship')")
    @Log(title = "销售发运登记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductSalesShipment entity)
    {
        return toAjax(shipmentService.createShipment(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:ship')")
    @Log(title = "销售发运修改", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductSalesShipment entity)
    {
        return toAjax(shipmentService.updateWmProductSalesShipment(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:ship')")
    @Log(title = "销售发运删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{shipmentIds}")
    public AjaxResult remove(@PathVariable Long[] shipmentIds)
    {
        return toAjax(shipmentService.deleteWmProductSalesShipmentByShipmentIds(shipmentIds));
    }

    /** 签收：IN_TRANSIT → RECEIVED，写签收时间/人/回单 */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:receive')")
    @Log(title = "销售发运签收", businessType = BusinessType.UPDATE)
    @PutMapping("/receive/{shipmentId}")
    public AjaxResult receive(@PathVariable("shipmentId") Long shipmentId,
                              @RequestBody WmProductSalesShipment info)
    {
        return toAjax(shipmentService.receive(shipmentId, info));
    }

    /** 取消发运（仅待发运可取消） */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:ship')")
    @Log(title = "销售发运取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{shipmentId}")
    public AjaxResult cancel(@PathVariable("shipmentId") Long shipmentId)
    {
        return toAjax(shipmentService.cancel(shipmentId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:export')")
    @Log(title = "销售发运单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductSalesShipment entity)
    {
        List<WmProductSalesShipment> list = shipmentService.selectWmProductSalesShipmentList(entity);
        ExcelUtil<WmProductSalesShipment> util = new ExcelUtil<WmProductSalesShipment>(WmProductSalesShipment.class);
        util.exportExcel(response, list, "销售发运单数据");
    }
}
