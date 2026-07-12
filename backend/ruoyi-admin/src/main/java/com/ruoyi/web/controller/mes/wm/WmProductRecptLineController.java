package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import com.ruoyi.system.service.mes.wm.IWmProductRecptLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 产品入库单行Controller
 *
 * @author qixiaoxia
 * @date 2026-07-11
 */
@RestController
@RequestMapping("/mes/wm/product_recpt_line")
public class WmProductRecptLineController extends BaseController
{
    @Autowired
    private IWmProductRecptLineService wmProductRecptLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductRecptLine entity)
    {
        startPage();
        List<WmProductRecptLine> list = wmProductRecptLineService.selectWmProductRecptLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmProductRecptLineService.selectWmProductRecptLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:add')")
    @Log(title = "产品入库单行", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductRecptLine entity)
    {
        wmProductRecptLineService.insertWmProductRecptLine(entity);
        return AjaxResult.success(entity);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:edit')")
    @Log(title = "产品入库单行", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductRecptLine entity)
    {
        return toAjax(wmProductRecptLineService.updateWmProductRecptLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:remove')")
    @Log(title = "产品入库单行", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmProductRecptLineService.deleteWmProductRecptLineByLineIds(lineIds));
    }
}
