package com.ruoyi.web.controller.mes.pur;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderDetailVO;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 采购订单头Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/pur/order")
public class PurOrderController extends BaseController
{
    @Autowired
    private IPurOrderService purOrderService;

    @Autowired
    private IPurOrderLineService purOrderLineService;

    /**
     * 查询采购订单头列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(PurOrder purOrder)
    {
        startPage();
        List<PurOrderVO> list = purOrderService.selectPurOrderList(purOrder);
        return getDataTable(list);
    }

    /**
     * 导出采购订单头列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:export')")
    @Log(title = "采购订单头", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PurOrder purOrder)
    {
        List<PurOrderVO> list = purOrderService.selectPurOrderList(purOrder);
        ExcelUtil<PurOrderVO> util = new ExcelUtil<PurOrderVO>(PurOrderVO.class);
        util.exportExcel(response, list, "采购订单头数据");
    }

    /**
     * 获取采购订单头详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        PurOrder order = purOrderService.selectPurOrderByOrderId(orderId);
        PurOrderVO orderVO = (PurOrderVO) order;  // MyBatis 通过 PurOrderVOResult 返回 PurOrderVO 实例
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        return success(new PurOrderDetailVO(orderVO, lines));
    }

    /**
     * 新增采购订单头
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:add')")
    @Log(title = "采购订单头", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PurOrder purOrder)
    {
        int rows = purOrderService.insertPurOrder(purOrder);
        return toAjax(rows, purOrder, "新增采购订单失败");
    }

    /**
     * 修改采购订单头
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:edit')")
    @Log(title = "采购订单头", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PurOrder purOrder)
    {
        return toAjax(purOrderService.updatePurOrder(purOrder));
    }

    /**
     * 删除采购订单头
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:remove')")
    @Log(title = "采购订单头", businessType = BusinessType.DELETE)
	@DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(purOrderService.deletePurOrderByOrderIds(orderIds));
    }

    /**
     * 审批采购订单（DRAFT → APPROVED）
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:approve')")
    @Log(title = "采购订单审批", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/approve")
    public AjaxResult approve(@PathVariable Long orderId)
    {
        try {
            purOrderService.approvePurOrder(orderId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 下达采购订单（APPROVED → ORDERED）
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:order')")
    @Log(title = "采购订单下达", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/order")
    public AjaxResult order(@PathVariable Long orderId)
    {
        try {
            purOrderService.orderPurOrder(orderId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 关闭采购订单（RECEIVED → CLOSED）
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order:close')")
    @Log(title = "采购订单关闭", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/close")
    public AjaxResult close(@PathVariable Long orderId)
    {
        try {
            purOrderService.closePurOrder(orderId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }
}
