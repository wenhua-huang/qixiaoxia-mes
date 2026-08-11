package com.ruoyi.web.controller.mes.pro;

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
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.service.mes.pro.IProFeedbackService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 报工记录Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/feedback")
public class ProFeedbackController extends BaseController
{
    @Autowired
    private IProFeedbackService proFeedbackService;

    @Autowired(required = false)
    private com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator autoCodeGenerator;

    /**
     * 获取工单默认物料消耗（新增报工时预填）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:query')")
    @GetMapping("/consumeDefaults/{workorderId}")
    public AjaxResult consumeDefaults(@PathVariable Long workorderId)
    {
        return success(proFeedbackService.getDefaultConsume(workorderId));
    }

    /**
     * 查询报工记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProFeedback proFeedback)
    {
        startPage();
        List<ProFeedback> list = proFeedbackService.selectProFeedbackList(proFeedback);
        return getDataTable(list);
    }

    /**
     * 查询所有报工记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:list')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<ProFeedback> list = proFeedbackService.selectAll();
        return success(list);
    }

    /**
     * 导出报工记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:export')")
    @Log(title = "报工记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProFeedback proFeedback)
    {
        List<ProFeedback> list = proFeedbackService.selectProFeedbackList(proFeedback);
        ExcelUtil<ProFeedback> util = new ExcelUtil<ProFeedback>(ProFeedback.class);
        util.exportExcel(response, list, "报工记录数据");
    }

    /**
     * 获取报工记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proFeedbackService.selectProFeedbackByRecordId(recordId));
    }

    /**
     * 新增报工记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:add')")
    @Log(title = "报工记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProFeedback proFeedback)
    {
        // 编码由服务端权威生成（AutoCodeGenerator + DB 唯一约束兜底），客户端可传但非必需
        proFeedbackService.checkFeedbackCodeUnique(proFeedback);
        proFeedbackService.insertProFeedback(proFeedback);
        return success(proFeedback);
    }

    /**
     * 修改报工记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:edit')")
    @Log(title = "报工记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProFeedback proFeedback)
    {
        return toAjax(proFeedbackService.updateProFeedback(proFeedback));
    }

    /**
     * 删除报工记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:remove')")
    @Log(title = "报工记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proFeedbackService.deleteProFeedbackByRecordIds(recordIds));
    }

    /** 确认报工：PREPARE → CONFIRMED */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:edit')")
    @Log(title = "报工确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{recordId}")
    public AjaxResult confirm(@PathVariable Long recordId)
    {
        proFeedbackService.confirmFeedback(recordId);
        return success();
    }

    /** 审核报工：CONFIRMED → AUDITED，同时更新任务/工单已生产数量 */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:edit')")
    @Log(title = "报工审核", businessType = BusinessType.UPDATE)
    @PutMapping("/audit/{recordId}")
    public AjaxResult audit(@PathVariable Long recordId)
    {
        proFeedbackService.auditFeedback(recordId);
        return success();
    }

    /** 批量确认报工：PREPARE → CONFIRMED，尽力执行，失败逐条返回。 */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:edit')")
    @Log(title = "报工批量确认", businessType = BusinessType.UPDATE)
    @PutMapping("/batchConfirm")
    public AjaxResult batchConfirm(@RequestBody(required = false) List<Long> recordIds)
    {
        return success(proFeedbackService.batchConfirmFeedback(toIdArray(recordIds)));
    }

    /** 批量审核报工：CONFIRMED → AUDITED，尽力执行，失败逐条返回。 */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:edit')")
    @Log(title = "报工批量审核", businessType = BusinessType.UPDATE)
    @PutMapping("/batchAudit")
    public AjaxResult batchAudit(@RequestBody(required = false) List<Long> recordIds)
    {
        return success(proFeedbackService.batchAuditFeedback(toIdArray(recordIds)));
    }

    private static Long[] toIdArray(List<Long> ids)
    {
        return ids == null ? new Long[0] : ids.toArray(new Long[0]);
    }
}
