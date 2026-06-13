package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmMiscIssueLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmMiscIssueLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 杂项出库单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/misc_issue_line")
public class WmMiscIssueLineController extends BaseController
{
    @Autowired
    private IWmMiscIssueLineService wmMiscIssueLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmMiscIssueLine entity)
    {
        startPage();
        List<WmMiscIssueLine> list = wmMiscIssueLineService.selectWmMiscIssueLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmMiscIssueLine> list = wmMiscIssueLineService.selectWmMiscIssueLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:export')")
    @Log(title = "杂项出库单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmMiscIssueLine entity)
    {
        List<WmMiscIssueLine> list = wmMiscIssueLineService.selectWmMiscIssueLineList(entity);
        ExcelUtil<WmMiscIssueLine> util = new ExcelUtil<WmMiscIssueLine>(WmMiscIssueLine.class);
        util.exportExcel(response, list, "杂项出库单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmMiscIssueLineService.selectWmMiscIssueLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:add')")
    @Log(title = "杂项出库单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmMiscIssueLine entity)
    {
        return toAjax(wmMiscIssueLineService.insertWmMiscIssueLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:edit')")
    @Log(title = "杂项出库单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmMiscIssueLine entity)
    {
        return toAjax(wmMiscIssueLineService.updateWmMiscIssueLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue_line:remove')")
    @Log(title = "杂项出库单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmMiscIssueLineService.deleteWmMiscIssueLineByLineIds(lineIds));
    }
}