package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.service.mes.wm.IWmIssueLineService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 领料单行Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/issueline")
public class WmIssueLineController extends BaseController
{
    @Autowired
    private IWmIssueLineService wmIssueLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmIssueLine e) { startPage(); return getDataTable(wmIssueLineService.selectWmIssueLineList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmIssueLineService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:export')")
    @Log(title = "领料单行", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmIssueLine e) {
        List<WmIssueLine> list = wmIssueLineService.selectWmIssueLineList(e);
        ExcelUtil<WmIssueLine> util = new ExcelUtil<WmIssueLine>(WmIssueLine.class);
        util.exportExcel(response, list, "领料单行数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping("/listByIssueId/{issueId}")
    public AjaxResult listByIssueId(@PathVariable Long issueId) { WmIssueLine q = new WmIssueLine(); q.setIssueId(issueId); return success(wmIssueLineService.selectWmIssueLineList(q)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId) { return success(wmIssueLineService.selectWmIssueLineByLineId(lineId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:add')")
    @Log(title = "领料单行", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmIssueLine e) { return toAjax(wmIssueLineService.insertWmIssueLine(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "领料单行", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmIssueLine e) { return toAjax(wmIssueLineService.updateWmIssueLine(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:remove')")
    @Log(title = "领料单行", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds) { return toAjax(wmIssueLineService.deleteWmIssueLineByLineIds(lineIds)); }
}
