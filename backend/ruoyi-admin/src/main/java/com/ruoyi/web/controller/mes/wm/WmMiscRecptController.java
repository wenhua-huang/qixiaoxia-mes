package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmMiscRecptService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmMiscRecpt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 杂项入库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/misc_recpt")
public class WmMiscRecptController extends BaseController
{
    @Autowired
    private IWmMiscRecptService wmMiscRecptService;

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmMiscRecpt entity)
    {
        startPage();
        List<WmMiscRecpt> list = wmMiscRecptService.selectWmMiscRecptList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmMiscRecpt> list = wmMiscRecptService.selectWmMiscRecptAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:export')")
    @Log(title = "杂项入库单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmMiscRecpt entity)
    {
        List<WmMiscRecpt> list = wmMiscRecptService.selectWmMiscRecptList(entity);
        ExcelUtil<WmMiscRecpt> util = new ExcelUtil<WmMiscRecpt>(WmMiscRecpt.class);
        util.exportExcel(response, list, "杂项入库单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmMiscRecptService.selectWmMiscRecptByRecptId(recptId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:add')")
    @Log(title = "杂项入库单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmMiscRecpt entity)
    {
        return toAjax(wmMiscRecptService.insertWmMiscRecpt(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:edit')")
    @Log(title = "杂项入库单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmMiscRecpt entity)
    {
        return toAjax(wmMiscRecptService.updateWmMiscRecpt(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt:remove')")
    @Log(title = "杂项入库单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        return toAjax(wmMiscRecptService.deleteWmMiscRecptByRecptIds(recptIds));
    }
}