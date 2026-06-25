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
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.service.mes.wm.IWmIssueHeaderService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 生产领料单Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/issueheader")
public class WmIssueHeaderController extends BaseController
{
    @Autowired
    private IWmIssueHeaderService wmIssueHeaderService;

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmIssueHeader e) { startPage(); return getDataTable(wmIssueHeaderService.selectWmIssueHeaderList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmIssueHeaderService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:export')")
    @Log(title = "生产领料单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmIssueHeader e) {
        List<WmIssueHeader> list = wmIssueHeaderService.selectWmIssueHeaderList(e);
        ExcelUtil<WmIssueHeader> util = new ExcelUtil<WmIssueHeader>(WmIssueHeader.class);
        util.exportExcel(response, list, "生产领料单数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping(value = "/{issueId}")
    public AjaxResult getInfo(@PathVariable("issueId") Long issueId) { return success(wmIssueHeaderService.selectWmIssueHeaderByIssueId(issueId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:add')")
    @Log(title = "生产领料单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmIssueHeader e) { wmIssueHeaderService.insertWmIssueHeader(e); return success(e); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "生产领料单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmIssueHeader e) { return toAjax(wmIssueHeaderService.updateWmIssueHeader(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:remove')")
    @Log(title = "生产领料单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{issueIds}")
    public AjaxResult remove(@PathVariable Long[] issueIds) { return toAjax(wmIssueHeaderService.deleteWmIssueHeaderByIssueIds(issueIds)); }

    /** 根据工单BOM自动生成领料行 */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "生产领料单", businessType = BusinessType.UPDATE)
    @PutMapping("/loadBom/{issueId}/{workorderId}")
    public AjaxResult loadBom(@PathVariable Long issueId, @PathVariable Long workorderId)
    { return toAjax(wmIssueHeaderService.loadBomLines(issueId, workorderId)); }

    /** 执行出库：扣库存 + 写追溯 + 状态改为POSTED */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "生产领料单", businessType = BusinessType.UPDATE)
    @PutMapping("/execute/{issueId}")
    public AjaxResult execute(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.executeIssue(issueId)); }
}
