package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 仓库表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/warehouse")
public class WmWarehouseController extends BaseController
{
    @Autowired
    private IWmWarehouseService wmWarehouseService;

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmWarehouse entity)
    {
        startPage();
        List<WmWarehouse> list = wmWarehouseService.selectWmWarehouseList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmWarehouse> list = wmWarehouseService.selectWmWarehouseAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:export')")
    @Log(title = "仓库表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmWarehouse entity)
    {
        List<WmWarehouse> list = wmWarehouseService.selectWmWarehouseList(entity);
        ExcelUtil<WmWarehouse> util = new ExcelUtil<WmWarehouse>(WmWarehouse.class);
        util.exportExcel(response, list, "仓库表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:query')")
    @GetMapping(value = "/{warehouseId}")
    public AjaxResult getInfo(@PathVariable("warehouseId") Long warehouseId)
    {
        return AjaxResult.success(wmWarehouseService.selectWmWarehouseByWarehouseId(warehouseId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:add')")
    @Log(title = "仓库表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmWarehouse entity)
    {
        return toAjax(wmWarehouseService.insertWmWarehouse(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:edit')")
    @Log(title = "仓库表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmWarehouse entity)
    {
        return toAjax(wmWarehouseService.updateWmWarehouse(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:warehouse:remove')")
    @Log(title = "仓库表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{warehouseIds}")
    public AjaxResult remove(@PathVariable Long[] warehouseIds)
    {
        return toAjax(wmWarehouseService.deleteWmWarehouseByWarehouseIds(warehouseIds));
    }
}