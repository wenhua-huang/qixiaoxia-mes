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
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.service.mes.wm.IWmRtIssueService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 生产退料单Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/rtissue")
public class WmRtIssueController extends BaseController
{
    @Autowired
    private IWmRtIssueService wmRtIssueService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtIssue e) { startPage(); return getDataTable(wmRtIssueService.selectWmRtIssueList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmRtIssueService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:export')")
    @Log(title = "生产退料单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtIssue e) {
        List<WmRtIssue> list = wmRtIssueService.selectWmRtIssueList(e);
        ExcelUtil<WmRtIssue> util = new ExcelUtil<WmRtIssue>(WmRtIssue.class);
        util.exportExcel(response, list, "生产退料单数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:query')")
    @GetMapping(value = "/{rtId}")
    public AjaxResult getInfo(@PathVariable("rtId") Long rtId) { return success(wmRtIssueService.selectWmRtIssueByRtId(rtId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:add')")
    @Log(title = "生产退料单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtIssue e) { return toAjax(wmRtIssueService.insertWmRtIssue(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:edit')")
    @Log(title = "生产退料单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtIssue e) { return toAjax(wmRtIssueService.updateWmRtIssue(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:remove')")
    @Log(title = "生产退料单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{rtIds}")
    public AjaxResult remove(@PathVariable Long[] rtIds) { return toAjax(wmRtIssueService.deleteWmRtIssueByRtIds(rtIds)); }

    /** 从领料单生成退料单（落库式，全量退，需已存在领料单） */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:add')")
    @Log(title = "生产退料单", businessType = BusinessType.INSERT)
    @PostMapping("/createFromIssue/{issueId}")
    public AjaxResult createFromIssue(@PathVariable Long issueId) {
        Long rtId = wmRtIssueService.createFromIssue(issueId);
        return success(rtId);
    }

    /**
     * 从领料单生成退料单草稿（不落库，差额退料）。
     * 读领料明细 + 工单报工消耗，组装差额退料草稿返回前端编辑，落库走 POST /mes/wm/rtissue。
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:add')")
    @GetMapping("/buildFromIssue/{issueId}")
    public AjaxResult buildFromIssue(@PathVariable Long issueId)
    { return AjaxResult.success(wmRtIssueService.buildFromIssue(issueId)); }

    /**
     * 退料领料单选择弹窗：分页查 ISSUED 领料单，每行带预算可退量（已发料−工单级消耗）。
     * 复用领料单查询条件（status/workorderName/issueCode 等）。同工单的消耗只算一次（消除 N+1）。
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:add')")
    @GetMapping("/returnablePreview")
    public TableDataInfo returnablePreview(WmIssueHeader query)
    { startPage(); return getDataTable(wmRtIssueService.returnablePreview(query)); }

    /** 执行退库：加库存 + 写追溯 + 状态改为POSTED */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:edit')")
    @Log(title = "生产退料单", businessType = BusinessType.UPDATE)
    @PutMapping("/execute/{rtId}")
    public AjaxResult execute(@PathVariable Long rtId)
    { return toAjax(wmRtIssueService.executeReturn(rtId)); }
}
