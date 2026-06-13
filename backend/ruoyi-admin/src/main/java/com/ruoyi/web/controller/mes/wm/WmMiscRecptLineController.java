package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmMiscRecptLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmMiscRecptLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 杂项入库单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/misc_recpt_line")
public class WmMiscRecptLineController extends BaseController
{
    @Autowired
    private IWmMiscRecptLineService wmMiscRecptLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmMiscRecptLine entity)
    {
        startPage();
        List<WmMiscRecptLine> list = wmMiscRecptLineService.selectWmMiscRecptLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmMiscRecptLine> list = wmMiscRecptLineService.selectWmMiscRecptLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:export')")
    @Log(title = "杂项入库单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmMiscRecptLine entity)
    {
        List<WmMiscRecptLine> list = wmMiscRecptLineService.selectWmMiscRecptLineList(entity);
        ExcelUtil<WmMiscRecptLine> util = new ExcelUtil<WmMiscRecptLine>(WmMiscRecptLine.class);
        util.exportExcel(response, list, "杂项入库单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmMiscRecptLineService.selectWmMiscRecptLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:add')")
    @Log(title = "杂项入库单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmMiscRecptLine entity)
    {
        return toAjax(wmMiscRecptLineService.insertWmMiscRecptLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:edit')")
    @Log(title = "杂项入库单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmMiscRecptLine entity)
    {
        return toAjax(wmMiscRecptLineService.updateWmMiscRecptLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_recpt_line:remove')")
    @Log(title = "杂项入库单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmMiscRecptLineService.deleteWmMiscRecptLineByLineIds(lineIds));
    }
}