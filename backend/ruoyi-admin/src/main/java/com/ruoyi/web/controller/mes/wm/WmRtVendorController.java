package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmRtVendorService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 采购退货单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/rt_vendor")
public class WmRtVendorController extends BaseController
{
    @Autowired
    private IWmRtVendorService wmRtVendorService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtVendor entity)
    {
        startPage();
        List<WmRtVendor> list = wmRtVendorService.selectWmRtVendorList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmRtVendor> list = wmRtVendorService.selectWmRtVendorAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:export')")
    @Log(title = "采购退货单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtVendor entity)
    {
        List<WmRtVendor> list = wmRtVendorService.selectWmRtVendorList(entity);
        ExcelUtil<WmRtVendor> util = new ExcelUtil<WmRtVendor>(WmRtVendor.class);
        util.exportExcel(response, list, "采购退货单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:query')")
    @GetMapping(value = "/{rtId}")
    public AjaxResult getInfo(@PathVariable("rtId") Long rtId)
    {
        return AjaxResult.success(wmRtVendorService.selectWmRtVendorByRtId(rtId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:add')")
    @Log(title = "采购退货单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtVendor entity)
    {
        return toAjax(wmRtVendorService.insertWmRtVendor(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:edit')")
    @Log(title = "采购退货单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtVendor entity)
    {
        return toAjax(wmRtVendorService.updateWmRtVendor(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:remove')")
    @Log(title = "采购退货单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{rtIds}")
    public AjaxResult remove(@PathVariable Long[] rtIds)
    {
        return toAjax(wmRtVendorService.deleteWmRtVendorByRtIds(rtIds));
    }

    /**
     * 确认退货单（DRAFT -> CONFIRMED），执行库存扣减
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:confirm')")
    @Log(title = "退货单确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{rtId}")
    public AjaxResult confirm(@PathVariable("rtId") Long rtId)
    {
        try {
            wmRtVendorService.confirmRtVendor(rtId);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 过账退货单（CONFIRMED -> POSTED），回写采购订单已退货数量
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor:post')")
    @Log(title = "退货单过账", businessType = BusinessType.UPDATE)
    @PutMapping("/post/{rtId}")
    public AjaxResult post(@PathVariable("rtId") Long rtId)
    {
        try {
            wmRtVendorService.postRtVendor(rtId);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}