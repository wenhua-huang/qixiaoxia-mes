package com.ruoyi.web.controller.mes.sys;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.sys.ISysAttachmentService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.sys.SysAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 通用附件Controller
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@RestController
@RequestMapping("/mes/sys/attachment")
public class SysAttachmentController extends BaseController
{
    @Autowired
    private ISysAttachmentService sysAttachmentService;

    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAttachment sysAttachment)
    {
        startPage();
        List<SysAttachment> list = sysAttachmentService.selectSysAttachmentList(sysAttachment);
        return getDataTable(list);
    }

    /**
     * 按关联单据查询附件列表
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:list')")
    @GetMapping("/listBySource")
    public AjaxResult listBySource(@RequestParam Long sourceDocId, @RequestParam String sourceDocType)
    {
        List<SysAttachment> list = sysAttachmentService.selectSysAttachmentBySource(sourceDocId, sourceDocType);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:export')")
    @Log(title = "通用附件", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAttachment sysAttachment)
    {
        List<SysAttachment> list = sysAttachmentService.selectSysAttachmentList(sysAttachment);
        ExcelUtil<SysAttachment> util = new ExcelUtil<SysAttachment>(SysAttachment.class);
        util.exportExcel(response, list, "附件数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:query')")
    @GetMapping(value = "/{attachmentId}")
    public AjaxResult getInfo(@PathVariable("attachmentId") Long attachmentId)
    {
        return AjaxResult.success(sysAttachmentService.selectSysAttachmentByAttachmentId(attachmentId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:add')")
    @Log(title = "通用附件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysAttachment sysAttachment)
    {
        return toAjax(sysAttachmentService.insertSysAttachment(sysAttachment));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:edit')")
    @Log(title = "通用附件", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAttachment sysAttachment)
    {
        return toAjax(sysAttachmentService.updateSysAttachment(sysAttachment));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:attachment:remove')")
    @Log(title = "通用附件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attachmentIds}")
    public AjaxResult remove(@PathVariable Long[] attachmentIds)
    {
        return toAjax(sysAttachmentService.deleteSysAttachmentByAttachmentIds(attachmentIds));
    }
}
