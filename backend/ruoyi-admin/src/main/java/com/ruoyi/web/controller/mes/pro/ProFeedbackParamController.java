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
import com.ruoyi.system.domain.mes.pro.ProFeedbackParam;
import com.ruoyi.system.service.mes.pro.IProFeedbackParamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 报工实际参数值Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/feedbackparam")
public class ProFeedbackParamController extends BaseController
{
    @Autowired
    private IProFeedbackParamService proFeedbackParamService;

    /**
     * 查询报工实际参数值列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProFeedbackParam proFeedbackParam)
    {
        startPage();
        List<ProFeedbackParam> list = proFeedbackParamService.selectProFeedbackParamList(proFeedbackParam);
        return getDataTable(list);
    }

    /**
     * 根据报工ID查询参数列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:query')")
    @GetMapping("/listByFeedbackId/{feedbackId}")
    public AjaxResult listByFeedbackId(@PathVariable("feedbackId") Long feedbackId)
    {
        List<ProFeedbackParam> list = proFeedbackParamService.selectProFeedbackParamByFeedbackId(feedbackId);
        return success(list);
    }

    /**
     * 导出报工实际参数值列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:export')")
    @Log(title = "报工实际参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProFeedbackParam proFeedbackParam)
    {
        List<ProFeedbackParam> list = proFeedbackParamService.selectProFeedbackParamList(proFeedbackParam);
        ExcelUtil<ProFeedbackParam> util = new ExcelUtil<ProFeedbackParam>(ProFeedbackParam.class);
        util.exportExcel(response, list, "报工实际参数数据");
    }

    /**
     * 获取报工实际参数值详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proFeedbackParamService.selectProFeedbackParamByRecordId(recordId));
    }

    /**
     * 新增报工实际参数值
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:add')")
    @Log(title = "报工实际参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProFeedbackParam proFeedbackParam)
    {
        proFeedbackParamService.insertProFeedbackParam(proFeedbackParam);
        return success(proFeedbackParam);
    }

    /**
     * 修改报工实际参数值
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:edit')")
    @Log(title = "报工实际参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProFeedbackParam proFeedbackParam)
    {
        return toAjax(proFeedbackParamService.updateProFeedbackParam(proFeedbackParam));
    }

    /**
     * 删除报工实际参数值
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:feedbackparam:remove')")
    @Log(title = "报工实际参数", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proFeedbackParamService.deleteProFeedbackParamByRecordIds(recordIds));
    }
}
