package com.ruoyi.web.controller.mes.pro;

import java.util.List;
import java.util.stream.Collectors;
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
import com.ruoyi.system.domain.mes.pro.ProFeedbackConsume;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderBomMapper;
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

    @Autowired
    private ProWorkorderBomMapper proWorkorderBomMapper;

    @Autowired(required = false)
    private com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator autoCodeGenerator;

    /**
     * 获取工单默认物料消耗（新增报工时预填）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:query')")
    @GetMapping("/consumeDefaults/{workorderId}")
    public AjaxResult consumeDefaults(@PathVariable Long workorderId)
    {
        List<ProWorkorderBom> boms = proWorkorderBomMapper.selectProWorkorderBomByWorkorderId(workorderId);
        List<ProFeedbackConsume> list = boms.stream().map(bom -> {
            ProFeedbackConsume c = new ProFeedbackConsume();
            c.setWorkorderId(workorderId);
            c.setItemId(bom.getItemId());
            c.setItemCode(bom.getItemCode());
            c.setItemName(bom.getItemName());
            c.setQuantity(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : bom.getQuantity());
            c.setBatchCode("");
            return c;
        }).collect(Collectors.toList());
        return success(list);
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
        // feedbackCode 为空时自动生成（移动端不传编码，PC端已通过 genSerialCode 生成）
        if (proFeedback.getFeedbackCode() == null || proFeedback.getFeedbackCode().isEmpty()) {
            if (autoCodeGenerator != null) {
                try { proFeedback.setFeedbackCode(autoCodeGenerator.genSerialCode("FEEDBACK_CODE", null)); }
                catch (Exception e) { /* 自动编码失败则由 checkFeedbackCodeUnique 兜底校验 */ }
            }
            if (proFeedback.getFeedbackCode() == null || proFeedback.getFeedbackCode().isEmpty()) {
                proFeedback.setFeedbackCode("FB" + System.currentTimeMillis());
            }
        }
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
        ProFeedback fb = proFeedbackService.selectProFeedbackByRecordId(recordId);
        if (fb == null) return error("报工记录不存在");
        if (!"PREPARE".equals(fb.getStatus())) return error("只有待确认状态的报工才能确认");
        fb.setStatus("CONFIRMED");
        proFeedbackService.updateProFeedback(fb);
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
}
