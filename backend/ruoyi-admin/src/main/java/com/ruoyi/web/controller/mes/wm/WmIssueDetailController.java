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
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;
import com.ruoyi.system.service.mes.wm.IWmIssueDetailService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 领料明细Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/issuedetail")
public class WmIssueDetailController extends BaseController
{
    @Autowired
    private IWmIssueDetailService wmIssueDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmIssueDetail e) { startPage(); return getDataTable(wmIssueDetailService.selectWmIssueDetailList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmIssueDetailService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:export')")
    @Log(title = "领料明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmIssueDetail e) {
        List<WmIssueDetail> list = wmIssueDetailService.selectWmIssueDetailList(e);
        ExcelUtil<WmIssueDetail> util = new ExcelUtil<WmIssueDetail>(WmIssueDetail.class);
        util.exportExcel(response, list, "领料明细数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId) { return success(wmIssueDetailService.selectWmIssueDetailByDetailId(detailId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:add')")
    @Log(title = "领料明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmIssueDetail e) { return toAjax(wmIssueDetailService.insertWmIssueDetail(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "领料明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmIssueDetail e) { return toAjax(wmIssueDetailService.updateWmIssueDetail(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:remove')")
    @Log(title = "领料明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds) { return toAjax(wmIssueDetailService.deleteWmIssueDetailByDetailIds(detailIds)); }
}
