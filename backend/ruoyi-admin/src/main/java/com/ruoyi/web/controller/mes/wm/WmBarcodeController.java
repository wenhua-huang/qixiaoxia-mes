package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmBarcodeService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmBarcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 条码清单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/barcode")
public class WmBarcodeController extends BaseController
{
    @Autowired
    private IWmBarcodeService wmBarcodeService;

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmBarcode entity)
    {
        startPage();
        List<WmBarcode> list = wmBarcodeService.selectWmBarcodeList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmBarcode> list = wmBarcodeService.selectWmBarcodeAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:export')")
    @Log(title = "条码清单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmBarcode entity)
    {
        List<WmBarcode> list = wmBarcodeService.selectWmBarcodeList(entity);
        ExcelUtil<WmBarcode> util = new ExcelUtil<WmBarcode>(WmBarcode.class);
        util.exportExcel(response, list, "条码清单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:query')")
    @GetMapping(value = "/{barcodeId}")
    public AjaxResult getInfo(@PathVariable("barcodeId") Long barcodeId)
    {
        return AjaxResult.success(wmBarcodeService.selectWmBarcodeByBarcodeId(barcodeId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:add')")
    @Log(title = "条码清单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmBarcode entity)
    {
        return toAjax(wmBarcodeService.insertWmBarcode(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:edit')")
    @Log(title = "条码清单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmBarcode entity)
    {
        return toAjax(wmBarcodeService.updateWmBarcode(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:barcode:remove')")
    @Log(title = "条码清单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{barcodeIds}")
    public AjaxResult remove(@PathVariable Long[] barcodeIds)
    {
        return toAjax(wmBarcodeService.deleteWmBarcodeByBarcodeIds(barcodeIds));
    }
}