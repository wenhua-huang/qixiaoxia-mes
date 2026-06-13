package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmBatchService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 批次记录表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/batch")
public class WmBatchController extends BaseController
{
    @Autowired
    private IWmBatchService wmBatchService;

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmBatch entity)
    {
        startPage();
        List<WmBatch> list = wmBatchService.selectWmBatchList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmBatch> list = wmBatchService.selectWmBatchAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:export')")
    @Log(title = "批次记录表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmBatch entity)
    {
        List<WmBatch> list = wmBatchService.selectWmBatchList(entity);
        ExcelUtil<WmBatch> util = new ExcelUtil<WmBatch>(WmBatch.class);
        util.exportExcel(response, list, "批次记录表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:query')")
    @GetMapping(value = "/{batchId}")
    public AjaxResult getInfo(@PathVariable("batchId") Long batchId)
    {
        return AjaxResult.success(wmBatchService.selectWmBatchByBatchId(batchId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:add')")
    @Log(title = "批次记录表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmBatch entity)
    {
        return toAjax(wmBatchService.insertWmBatch(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:edit')")
    @Log(title = "批次记录表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmBatch entity)
    {
        return toAjax(wmBatchService.updateWmBatch(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:batch:remove')")
    @Log(title = "批次记录表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{batchIds}")
    public AjaxResult remove(@PathVariable Long[] batchIds)
    {
        return toAjax(wmBatchService.deleteWmBatchByBatchIds(batchIds));
    }
}