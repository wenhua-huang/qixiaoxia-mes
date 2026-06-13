package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmTransferLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmTransferLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 调拨转移单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/transfer_line")
public class WmTransferLineController extends BaseController
{
    @Autowired
    private IWmTransferLineService wmTransferLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmTransferLine entity)
    {
        startPage();
        List<WmTransferLine> list = wmTransferLineService.selectWmTransferLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmTransferLine> list = wmTransferLineService.selectWmTransferLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:export')")
    @Log(title = "调拨转移单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmTransferLine entity)
    {
        List<WmTransferLine> list = wmTransferLineService.selectWmTransferLineList(entity);
        ExcelUtil<WmTransferLine> util = new ExcelUtil<WmTransferLine>(WmTransferLine.class);
        util.exportExcel(response, list, "调拨转移单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmTransferLineService.selectWmTransferLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:add')")
    @Log(title = "调拨转移单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmTransferLine entity)
    {
        return toAjax(wmTransferLineService.insertWmTransferLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:edit')")
    @Log(title = "调拨转移单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmTransferLine entity)
    {
        return toAjax(wmTransferLineService.updateWmTransferLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_line:remove')")
    @Log(title = "调拨转移单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmTransferLineService.deleteWmTransferLineByLineIds(lineIds));
    }
}