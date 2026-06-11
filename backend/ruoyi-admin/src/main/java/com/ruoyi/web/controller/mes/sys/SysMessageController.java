package com.ruoyi.web.controller.mes.sys;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.sys.ISysMessageService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.sys.SysMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统消息Controller
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@RestController
@RequestMapping("/mes/sys/message")
public class SysMessageController extends BaseController
{
    @Autowired
    private ISysMessageService sysMessageService;

    @PreAuthorize("@ss.hasPermi('mes:sys:message:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysMessage sysMessage)
    {
        startPage();
        List<SysMessage> list = sysMessageService.selectSysMessageList(sysMessage);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:message:export')")
    @Log(title = "系统消息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysMessage sysMessage)
    {
        List<SysMessage> list = sysMessageService.selectSysMessageList(sysMessage);
        ExcelUtil<SysMessage> util = new ExcelUtil<SysMessage>(SysMessage.class);
        util.exportExcel(response, list, "系统消息数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:message:query')")
    @GetMapping(value = "/{messageId}")
    public AjaxResult getInfo(@PathVariable("messageId") Long messageId)
    {
        return AjaxResult.success(sysMessageService.selectSysMessageByMessageId(messageId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:message:add')")
    @Log(title = "系统消息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysMessage sysMessage)
    {
        return toAjax(sysMessageService.insertSysMessage(sysMessage));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:message:remove')")
    @Log(title = "系统消息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{messageIds}")
    public AjaxResult remove(@PathVariable Long[] messageIds)
    {
        return toAjax(sysMessageService.deleteSysMessageByMessageIds(messageIds));
    }

    /**
     * 批量标记已读
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:message:edit')")
    @Log(title = "系统消息", businessType = BusinessType.UPDATE)
    @PutMapping("/read/{messageIds}")
    public AjaxResult markAsRead(@PathVariable Long[] messageIds)
    {
        return toAjax(sysMessageService.markAsRead(messageIds));
    }

    /**
     * 获取当前用户未读消息数
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:message:query')")
    @GetMapping("/unreadCount")
    public AjaxResult unreadCount()
    {
        int count = sysMessageService.selectUnreadCount(SecurityUtils.getUserId());
        return AjaxResult.success(count);
    }
}
