package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcTemplateService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 质检检验模板Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@RestController
@RequestMapping("/mes/qc/template")
public class QcTemplateController extends BaseController
{
    @Autowired
    private IQcTemplateService qcTemplateService;

    /**
     * 查询质检检验模板列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcTemplate qcTemplate)
    {
        startPage();
        List<QcTemplate> list = qcTemplateService.selectQcTemplateList(qcTemplate);
        return getDataTable(list);
    }

    /**
     * 导出质检检验模板列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:template:export')")
    @Log(title = "质检检验模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcTemplate qcTemplate)
    {
        List<QcTemplate> list = qcTemplateService.selectQcTemplateList(qcTemplate);
        ExcelUtil<QcTemplate> util = new ExcelUtil<QcTemplate>(QcTemplate.class);
        util.exportExcel(response, list, "质检检验模板数据");
    }

    /**
     * 获取质检检验模板详细信息（含检测项行/物料绑定行）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:template:query')")
    @GetMapping(value = "/{templateId}")
    public AjaxResult getInfo(@PathVariable("templateId") Long templateId)
    {
        return AjaxResult.success(qcTemplateService.selectQcTemplateWithRows(templateId));
    }

    /**
     * 新增质检检验模板（头+行级联保存）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:template:add')")
    @Log(title = "质检检验模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcTemplate qcTemplate)
    {
        return toAjax(qcTemplateService.insertQcTemplate(qcTemplate));
    }

    /**
     * 修改质检检验模板（头+行级联全删全插）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:template:edit')")
    @Log(title = "质检检验模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcTemplate qcTemplate)
    {
        return toAjax(qcTemplateService.updateQcTemplate(qcTemplate));
    }

    /**
     * 删除质检检验模板（被检验单引用时禁止删除）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:template:remove')")
    @Log(title = "质检检验模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        return toAjax(qcTemplateService.deleteQcTemplateByTemplateIds(templateIds));
    }
}
