package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 物料入库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/item_recpt")
public class WmItemRecptController extends BaseController
{
    @Autowired
    private IWmItemRecptService wmItemRecptService;


    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecpt entity)
    {
        startPage();
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:export')")
    @Log(title = "物料入库单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmItemRecpt entity)
    {
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(entity);
        ExcelUtil<WmItemRecpt> util = new ExcelUtil<WmItemRecpt>(WmItemRecpt.class);
        util.exportExcel(response, list, "物料入库单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmItemRecptService.selectWmItemRecptByRecptId(recptId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:add')")
    @Log(title = "物料入库单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecpt entity)
    {
        return toAjax(wmItemRecptService.insertWmItemRecpt(entity), entity, "新增入库单失败");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:edit')")
    @Log(title = "物料入库单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmItemRecpt entity)
    {
        return toAjax(wmItemRecptService.updateWmItemRecpt(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:remove')")
    @Log(title = "物料入库单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        return toAjax(wmItemRecptService.deleteWmItemRecptByRecptIds(recptIds));
    }

    /**
     * 确认收货 — 更新库存 + 回写PO到货标记（在同一事务中）
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:edit')")
    @Log(title = "物料入库单确认收货", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{recptId}")
    public AjaxResult confirm(@PathVariable Long recptId) {
        try {
            wmItemRecptService.confirmItemRecpt(recptId);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 过账入库（CONFIRMED → POSTED）— 回写PO已收数量
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:edit')")
    @Log(title = "物料入库单过账", businessType = BusinessType.UPDATE)
    @PutMapping("/post/{recptId}")
    public AjaxResult post(@PathVariable Long recptId) {
        try {
            wmItemRecptService.postItemRecpt(recptId);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}