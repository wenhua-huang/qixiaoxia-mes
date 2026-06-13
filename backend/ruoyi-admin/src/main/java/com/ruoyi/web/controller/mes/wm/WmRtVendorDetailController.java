package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmRtVendorDetailService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmRtVendorDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 采购退货单明细表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/rt_vendor_detail")
public class WmRtVendorDetailController extends BaseController
{
    @Autowired
    private IWmRtVendorDetailService wmRtVendorDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtVendorDetail entity)
    {
        startPage();
        List<WmRtVendorDetail> list = wmRtVendorDetailService.selectWmRtVendorDetailList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmRtVendorDetail> list = wmRtVendorDetailService.selectWmRtVendorDetailAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:export')")
    @Log(title = "采购退货单明细表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtVendorDetail entity)
    {
        List<WmRtVendorDetail> list = wmRtVendorDetailService.selectWmRtVendorDetailList(entity);
        ExcelUtil<WmRtVendorDetail> util = new ExcelUtil<WmRtVendorDetail>(WmRtVendorDetail.class);
        util.exportExcel(response, list, "采购退货单明细表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId)
    {
        return AjaxResult.success(wmRtVendorDetailService.selectWmRtVendorDetailByDetailId(detailId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:add')")
    @Log(title = "采购退货单明细表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtVendorDetail entity)
    {
        return toAjax(wmRtVendorDetailService.insertWmRtVendorDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:edit')")
    @Log(title = "采购退货单明细表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtVendorDetail entity)
    {
        return toAjax(wmRtVendorDetailService.updateWmRtVendorDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_detail:remove')")
    @Log(title = "采购退货单明细表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds)
    {
        return toAjax(wmRtVendorDetailService.deleteWmRtVendorDetailByDetailIds(detailIds));
    }
}