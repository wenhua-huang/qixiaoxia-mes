package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmItemRecptDetailService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmItemRecptDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 物料入库单明细表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/item_recpt_detail")
public class WmItemRecptDetailController extends BaseController
{
    @Autowired
    private IWmItemRecptDetailService wmItemRecptDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecptDetail entity)
    {
        startPage();
        List<WmItemRecptDetail> list = wmItemRecptDetailService.selectWmItemRecptDetailList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmItemRecptDetail> list = wmItemRecptDetailService.selectWmItemRecptDetailAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:export')")
    @Log(title = "物料入库单明细表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmItemRecptDetail entity)
    {
        List<WmItemRecptDetail> list = wmItemRecptDetailService.selectWmItemRecptDetailList(entity);
        ExcelUtil<WmItemRecptDetail> util = new ExcelUtil<WmItemRecptDetail>(WmItemRecptDetail.class);
        util.exportExcel(response, list, "物料入库单明细表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId)
    {
        return AjaxResult.success(wmItemRecptDetailService.selectWmItemRecptDetailByDetailId(detailId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:add')")
    @Log(title = "物料入库单明细表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecptDetail entity)
    {
        return toAjax(wmItemRecptDetailService.insertWmItemRecptDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:edit')")
    @Log(title = "物料入库单明细表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmItemRecptDetail entity)
    {
        return toAjax(wmItemRecptDetailService.updateWmItemRecptDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_detail:remove')")
    @Log(title = "物料入库单明细表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds)
    {
        return toAjax(wmItemRecptDetailService.deleteWmItemRecptDetailByDetailIds(detailIds));
    }
}