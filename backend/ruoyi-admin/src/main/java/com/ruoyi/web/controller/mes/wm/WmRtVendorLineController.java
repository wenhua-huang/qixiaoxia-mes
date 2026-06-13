package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmRtVendorLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmRtVendorLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 采购退货单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/rt_vendor_line")
public class WmRtVendorLineController extends BaseController
{
    @Autowired
    private IWmRtVendorLineService wmRtVendorLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtVendorLine entity)
    {
        startPage();
        List<WmRtVendorLine> list = wmRtVendorLineService.selectWmRtVendorLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmRtVendorLine> list = wmRtVendorLineService.selectWmRtVendorLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:export')")
    @Log(title = "采购退货单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtVendorLine entity)
    {
        List<WmRtVendorLine> list = wmRtVendorLineService.selectWmRtVendorLineList(entity);
        ExcelUtil<WmRtVendorLine> util = new ExcelUtil<WmRtVendorLine>(WmRtVendorLine.class);
        util.exportExcel(response, list, "采购退货单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmRtVendorLineService.selectWmRtVendorLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:add')")
    @Log(title = "采购退货单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtVendorLine entity)
    {
        return toAjax(wmRtVendorLineService.insertWmRtVendorLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:edit')")
    @Log(title = "采购退货单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtVendorLine entity)
    {
        return toAjax(wmRtVendorLineService.updateWmRtVendorLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_vendor_line:remove')")
    @Log(title = "采购退货单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmRtVendorLineService.deleteWmRtVendorLineByLineIds(lineIds));
    }
}