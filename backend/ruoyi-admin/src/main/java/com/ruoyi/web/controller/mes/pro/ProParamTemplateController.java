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
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;
import com.ruoyi.system.service.mes.pro.IProParamTemplateService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工序参数模版Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/paramtemplate")
public class ProParamTemplateController extends BaseController
{
    @Autowired
    private IProParamTemplateService proParamTemplateService;

    /**
     * 查询工序参数模版列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProParamTemplate proParamTemplate)
    {
        startPage();
        List<ProParamTemplate> list = proParamTemplateService.selectProParamTemplateList(proParamTemplate);
        return getDataTable(list);
    }

    /**
     * 根据工序ID查询参数模版
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:query')")
    @GetMapping("/listByProcessId/{processId}")
    public AjaxResult listByProcessId(@PathVariable("processId") Long processId)
    {
        List<ProParamTemplate> list = proParamTemplateService.selectProParamTemplateByProcessId(processId);
        return success(list);
    }

    /**
     * 获取工序参数模版详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:query')")
    @GetMapping(value = "/{templateId}")
    public AjaxResult getInfo(@PathVariable("templateId") Long templateId)
    {
        return success(proParamTemplateService.selectProParamTemplateByTemplateId(templateId));
    }

    /**
     * 新增工序参数模版
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:add')")
    @Log(title = "工序参数模版", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProParamTemplate proParamTemplate)
    {
        proParamTemplateService.checkParamCodeUnique(proParamTemplate);
        proParamTemplateService.insertProParamTemplate(proParamTemplate);
        return success(proParamTemplate);
    }

    /**
     * 修改工序参数模版
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:edit')")
    @Log(title = "工序参数模版", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProParamTemplate proParamTemplate)
    {
        proParamTemplateService.checkParamCodeUnique(proParamTemplate);
        return toAjax(proParamTemplateService.updateProParamTemplate(proParamTemplate));
    }

    /**
     * 删除工序参数模版
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:remove')")
    @Log(title = "工序参数模版", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        return toAjax(proParamTemplateService.deleteProParamTemplateByTemplateIds(templateIds));
    }
}
