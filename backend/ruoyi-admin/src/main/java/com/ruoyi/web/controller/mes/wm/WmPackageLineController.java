package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmPackageLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmPackageLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 装箱明细表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/package_line")
public class WmPackageLineController extends BaseController
{
    @Autowired
    private IWmPackageLineService wmPackageLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmPackageLine entity)
    {
        startPage();
        List<WmPackageLine> list = wmPackageLineService.selectWmPackageLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmPackageLine> list = wmPackageLineService.selectWmPackageLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:export')")
    @Log(title = "装箱明细表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmPackageLine entity)
    {
        List<WmPackageLine> list = wmPackageLineService.selectWmPackageLineList(entity);
        ExcelUtil<WmPackageLine> util = new ExcelUtil<WmPackageLine>(WmPackageLine.class);
        util.exportExcel(response, list, "装箱明细表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmPackageLineService.selectWmPackageLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:add')")
    @Log(title = "装箱明细表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmPackageLine entity)
    {
        return toAjax(wmPackageLineService.insertWmPackageLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:edit')")
    @Log(title = "装箱明细表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmPackageLine entity)
    {
        return toAjax(wmPackageLineService.updateWmPackageLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package_line:remove')")
    @Log(title = "装箱明细表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmPackageLineService.deleteWmPackageLineByLineIds(lineIds));
    }
}