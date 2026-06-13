package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmPackageService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 装箱单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/package")
public class WmPackageController extends BaseController
{
    @Autowired
    private IWmPackageService wmPackageService;

    @PreAuthorize("@ss.hasPermi('mes:wm:package:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmPackage entity)
    {
        startPage();
        List<WmPackage> list = wmPackageService.selectWmPackageList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmPackage> list = wmPackageService.selectWmPackageAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package:export')")
    @Log(title = "装箱单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmPackage entity)
    {
        List<WmPackage> list = wmPackageService.selectWmPackageList(entity);
        ExcelUtil<WmPackage> util = new ExcelUtil<WmPackage>(WmPackage.class);
        util.exportExcel(response, list, "装箱单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package:query')")
    @GetMapping(value = "/{packageId}")
    public AjaxResult getInfo(@PathVariable("packageId") Long packageId)
    {
        return AjaxResult.success(wmPackageService.selectWmPackageByPackageId(packageId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package:add')")
    @Log(title = "装箱单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmPackage entity)
    {
        return toAjax(wmPackageService.insertWmPackage(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package:edit')")
    @Log(title = "装箱单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmPackage entity)
    {
        return toAjax(wmPackageService.updateWmPackage(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:package:remove')")
    @Log(title = "装箱单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{packageIds}")
    public AjaxResult remove(@PathVariable Long[] packageIds)
    {
        return toAjax(wmPackageService.deleteWmPackageByPackageIds(packageIds));
    }
}