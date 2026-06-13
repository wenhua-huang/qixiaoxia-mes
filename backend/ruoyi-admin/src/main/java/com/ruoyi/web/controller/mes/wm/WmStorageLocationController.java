package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmStorageLocationService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 库区表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/storage_location")
public class WmStorageLocationController extends BaseController
{
    @Autowired
    private IWmStorageLocationService wmStorageLocationService;

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmStorageLocation entity)
    {
        startPage();
        List<WmStorageLocation> list = wmStorageLocationService.selectWmStorageLocationList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmStorageLocation> list = wmStorageLocationService.selectWmStorageLocationAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:export')")
    @Log(title = "库区表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmStorageLocation entity)
    {
        List<WmStorageLocation> list = wmStorageLocationService.selectWmStorageLocationList(entity);
        ExcelUtil<WmStorageLocation> util = new ExcelUtil<WmStorageLocation>(WmStorageLocation.class);
        util.exportExcel(response, list, "库区表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:query')")
    @GetMapping(value = "/{locationId}")
    public AjaxResult getInfo(@PathVariable("locationId") Long locationId)
    {
        return AjaxResult.success(wmStorageLocationService.selectWmStorageLocationByLocationId(locationId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:add')")
    @Log(title = "库区表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmStorageLocation entity)
    {
        return toAjax(wmStorageLocationService.insertWmStorageLocation(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:edit')")
    @Log(title = "库区表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmStorageLocation entity)
    {
        return toAjax(wmStorageLocationService.updateWmStorageLocation(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_location:remove')")
    @Log(title = "库区表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{locationIds}")
    public AjaxResult remove(@PathVariable Long[] locationIds)
    {
        return toAjax(wmStorageLocationService.deleteWmStorageLocationByLocationIds(locationIds));
    }
}