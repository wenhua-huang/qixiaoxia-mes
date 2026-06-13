package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;
import com.ruoyi.system.service.mes.wm.IWmBatchService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 物料入库单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/item_recpt_line")
public class WmItemRecptLineController extends BaseController
{
    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IWmBatchService wmBatchService;

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecptLine entity)
    {
        startPage();
        List<WmItemRecptLine> list = wmItemRecptLineService.selectWmItemRecptLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmItemRecptLine> list = wmItemRecptLineService.selectWmItemRecptLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:export')")
    @Log(title = "物料入库单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmItemRecptLine entity)
    {
        List<WmItemRecptLine> list = wmItemRecptLineService.selectWmItemRecptLineList(entity);
        ExcelUtil<WmItemRecptLine> util = new ExcelUtil<WmItemRecptLine>(WmItemRecptLine.class);
        util.exportExcel(response, list, "物料入库单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmItemRecptLineService.selectWmItemRecptLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:add')")
    @Log(title = "物料入库单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecptLine entity)
    {
        autoGenBatch(entity);
        return toAjax(wmItemRecptLineService.insertWmItemRecptLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:edit')")
    @Log(title = "物料入库单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmItemRecptLine entity)
    {
        autoGenBatch(entity);
        return toAjax(wmItemRecptLineService.updateWmItemRecptLine(entity));
    }

    /** 自动生成批次号（来源批次：追溯到供应商） */
    private void autoGenBatch(WmItemRecptLine line) {
        WmItemRecpt header = wmItemRecptService.selectWmItemRecptByRecptId(line.getRecptId());
        if (header == null) return;

        WmBatch batch = new WmBatch();
        batch.setItemId(line.getItemId());
        batch.setItemCode(line.getItemCode());
        batch.setItemName(line.getItemName());
        batch.setSpecification(line.getSpecification());
        batch.setVendorId(header.getVendorId());
        batch.setVendorCode(header.getVendorId() != null ? header.getVendorId().toString() : null);
        batch.setVendorName(header.getVendorName());
        batch.setProduceDate(line.getProduceDate());
        batch.setExpireDate(line.getExpireDate());
        batch.setLotNumber(line.getLotNumber());

        WmBatch wmBatch = wmBatchService.getOrGenerateBatchCode(batch);
        if (wmBatch != null) {
            line.setBatchId(wmBatch.getBatchId());
            line.setBatchCode(wmBatch.getBatchCode());
        }
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt_line:remove')")
    @Log(title = "物料入库单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmItemRecptLineService.deleteWmItemRecptLineByLineIds(lineIds));
    }
}