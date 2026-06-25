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
import com.ruoyi.system.domain.mes.wm.WmRtIssueDetail;
import com.ruoyi.system.service.mes.wm.IWmRtIssueDetailService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 退料明细Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/rtissuedetail")
public class WmRtIssueDetailController extends BaseController
{
    @Autowired
    private IWmRtIssueDetailService wmRtIssueDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtIssueDetail e) { startPage(); return getDataTable(wmRtIssueDetailService.selectWmRtIssueDetailList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmRtIssueDetailService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:export')")
    @Log(title = "退料明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtIssueDetail e) {
        List<WmRtIssueDetail> list = wmRtIssueDetailService.selectWmRtIssueDetailList(e);
        ExcelUtil<WmRtIssueDetail> util = new ExcelUtil<WmRtIssueDetail>(WmRtIssueDetail.class);
        util.exportExcel(response, list, "退料明细数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId) { return success(wmRtIssueDetailService.selectWmRtIssueDetailByDetailId(detailId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:add')")
    @Log(title = "退料明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtIssueDetail e) { return toAjax(wmRtIssueDetailService.insertWmRtIssueDetail(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:edit')")
    @Log(title = "退料明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtIssueDetail e) { return toAjax(wmRtIssueDetailService.updateWmRtIssueDetail(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissuedetail:remove')")
    @Log(title = "退料明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds) { return toAjax(wmRtIssueDetailService.deleteWmRtIssueDetailByDetailIds(detailIds)); }
}
