package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmStorageAreaService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmStorageArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 库位表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/storage_area")
public class WmStorageAreaController extends BaseController
{
    @Autowired
    private IWmStorageAreaService wmStorageAreaService;

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmStorageArea entity)
    {
        startPage();
        List<WmStorageArea> list = wmStorageAreaService.selectWmStorageAreaList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmStorageArea> list = wmStorageAreaService.selectWmStorageAreaAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:export')")
    @Log(title = "库位表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmStorageArea entity)
    {
        List<WmStorageArea> list = wmStorageAreaService.selectWmStorageAreaList(entity);
        ExcelUtil<WmStorageArea> util = new ExcelUtil<WmStorageArea>(WmStorageArea.class);
        util.exportExcel(response, list, "库位表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:query')")
    @GetMapping(value = "/{areaId}")
    public AjaxResult getInfo(@PathVariable("areaId") Long areaId)
    {
        return AjaxResult.success(wmStorageAreaService.selectWmStorageAreaByAreaId(areaId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:add')")
    @Log(title = "库位表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmStorageArea entity)
    {
        return toAjax(wmStorageAreaService.insertWmStorageArea(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:edit')")
    @Log(title = "库位表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmStorageArea entity)
    {
        return toAjax(wmStorageAreaService.updateWmStorageArea(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:storage_area:remove')")
    @Log(title = "库位表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{areaIds}")
    public AjaxResult remove(@PathVariable Long[] areaIds)
    {
        return toAjax(wmStorageAreaService.deleteWmStorageAreaByAreaIds(areaIds));
    }
}