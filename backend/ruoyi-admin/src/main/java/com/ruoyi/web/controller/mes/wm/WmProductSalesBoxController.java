package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import com.ruoyi.system.service.mes.wm.IWmProductSalesBoxService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出库-装箱明细 Controller
 * 权限：mes:wm:sales:edit（装箱增删改）、mes:wm:sales:list（查询）
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
@RestController
@RequestMapping("/mes/wm/product_sales_box")
public class WmProductSalesBoxController extends BaseController
{
    @Autowired
    private IWmProductSalesBoxService boxService;

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/list")
    public AjaxResult list(WmProductSalesBox entity)
    {
        return AjaxResult.success(boxService.selectWmProductSalesBoxList(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/bySales/{salesId}")
    public AjaxResult bySales(@PathVariable("salesId") Long salesId)
    {
        return AjaxResult.success(boxService.selectBoxesBySalesId(salesId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:list')")
    @GetMapping("/{boxId}")
    public AjaxResult getInfo(@PathVariable("boxId") Long boxId)
    {
        return AjaxResult.success(boxService.selectWmProductSalesBoxByBoxId(boxId));
    }

    /** 新增装箱（自动算体积、自动箱号 BOX-NNN） */
    @PreAuthorize("@ss.hasPermi('mes:wm:sales:edit')")
    @Log(title = "销售装箱新增", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductSalesBox entity)
    {
        return toAjax(boxService.insertWmProductSalesBox(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:edit')")
    @Log(title = "销售装箱修改", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductSalesBox entity)
    {
        return toAjax(boxService.updateWmProductSalesBox(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:sales:edit')")
    @Log(title = "销售装箱删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{boxIds}")
    public AjaxResult remove(@PathVariable Long[] boxIds)
    {
        return toAjax(boxService.deleteWmProductSalesBoxByBoxIds(boxIds));
    }
}
