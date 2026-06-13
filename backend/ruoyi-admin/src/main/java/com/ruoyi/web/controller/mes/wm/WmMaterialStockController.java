package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmMaterialStockService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 库存记录表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/material_stock")
public class WmMaterialStockController extends BaseController
{
    @Autowired
    private IWmMaterialStockService wmMaterialStockService;

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmMaterialStock entity)
    {
        startPage();
        List<WmMaterialStock> list = wmMaterialStockService.selectWmMaterialStockList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmMaterialStock> list = wmMaterialStockService.selectWmMaterialStockAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:export')")
    @Log(title = "库存记录表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmMaterialStock entity)
    {
        List<WmMaterialStock> list = wmMaterialStockService.selectWmMaterialStockList(entity);
        ExcelUtil<WmMaterialStock> util = new ExcelUtil<WmMaterialStock>(WmMaterialStock.class);
        util.exportExcel(response, list, "库存记录表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:query')")
    @GetMapping(value = "/{materialStockId}")
    public AjaxResult getInfo(@PathVariable("materialStockId") Long materialStockId)
    {
        return AjaxResult.success(wmMaterialStockService.selectWmMaterialStockByMaterialStockId(materialStockId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:add')")
    @Log(title = "库存记录表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmMaterialStock entity)
    {
        return toAjax(wmMaterialStockService.insertWmMaterialStock(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:edit')")
    @Log(title = "库存记录表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmMaterialStock entity)
    {
        return toAjax(wmMaterialStockService.updateWmMaterialStock(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:material_stock:remove')")
    @Log(title = "库存记录表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{materialStockIds}")
    public AjaxResult remove(@PathVariable Long[] materialStockIds)
    {
        return toAjax(wmMaterialStockService.deleteWmMaterialStockByMaterialStockIds(materialStockIds));
    }
}