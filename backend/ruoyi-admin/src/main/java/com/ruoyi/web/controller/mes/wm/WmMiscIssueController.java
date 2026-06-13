package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmMiscIssueService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmMiscIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 杂项出库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/misc_issue")
public class WmMiscIssueController extends BaseController
{
    @Autowired
    private IWmMiscIssueService wmMiscIssueService;

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmMiscIssue entity)
    {
        startPage();
        List<WmMiscIssue> list = wmMiscIssueService.selectWmMiscIssueList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmMiscIssue> list = wmMiscIssueService.selectWmMiscIssueAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:export')")
    @Log(title = "杂项出库单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmMiscIssue entity)
    {
        List<WmMiscIssue> list = wmMiscIssueService.selectWmMiscIssueList(entity);
        ExcelUtil<WmMiscIssue> util = new ExcelUtil<WmMiscIssue>(WmMiscIssue.class);
        util.exportExcel(response, list, "杂项出库单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:query')")
    @GetMapping(value = "/{issueId}")
    public AjaxResult getInfo(@PathVariable("issueId") Long issueId)
    {
        return AjaxResult.success(wmMiscIssueService.selectWmMiscIssueByIssueId(issueId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:add')")
    @Log(title = "杂项出库单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmMiscIssue entity)
    {
        return toAjax(wmMiscIssueService.insertWmMiscIssue(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:edit')")
    @Log(title = "杂项出库单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmMiscIssue entity)
    {
        return toAjax(wmMiscIssueService.updateWmMiscIssue(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:misc_issue:remove')")
    @Log(title = "杂项出库单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{issueIds}")
    public AjaxResult remove(@PathVariable Long[] issueIds)
    {
        return toAjax(wmMiscIssueService.deleteWmMiscIssueByIssueIds(issueIds));
    }
}