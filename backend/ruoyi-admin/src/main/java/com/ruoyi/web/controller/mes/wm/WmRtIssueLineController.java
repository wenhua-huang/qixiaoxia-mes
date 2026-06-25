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
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.service.mes.wm.IWmRtIssueLineService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 退料单行Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/rtissueline")
public class WmRtIssueLineController extends BaseController
{
    @Autowired
    private IWmRtIssueLineService wmRtIssueLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtIssueLine e) { startPage(); return getDataTable(wmRtIssueLineService.selectWmRtIssueLineList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmRtIssueLineService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:export')")
    @Log(title = "退料单行", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtIssueLine e) {
        List<WmRtIssueLine> list = wmRtIssueLineService.selectWmRtIssueLineList(e);
        ExcelUtil<WmRtIssueLine> util = new ExcelUtil<WmRtIssueLine>(WmRtIssueLine.class);
        util.exportExcel(response, list, "退料单行数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId) { return success(wmRtIssueLineService.selectWmRtIssueLineByLineId(lineId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:add')")
    @Log(title = "退料单行", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtIssueLine e) { return toAjax(wmRtIssueLineService.insertWmRtIssueLine(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:edit')")
    @Log(title = "退料单行", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtIssueLine e) { return toAjax(wmRtIssueLineService.updateWmRtIssueLine(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissueline:remove')")
    @Log(title = "退料单行", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds) { return toAjax(wmRtIssueLineService.deleteWmRtIssueLineByLineIds(lineIds)); }
}
