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
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;
import com.ruoyi.system.service.mes.pro.IProWorkorderParamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工单工序参数值Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/workorderparam")
public class ProWorkorderParamController extends BaseController
{
    @Autowired
    private IProWorkorderParamService proWorkorderParamService;

    /**
     * 查询工单工序参数值列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProWorkorderParam proWorkorderParam)
    {
        startPage();
        List<ProWorkorderParam> list = proWorkorderParamService.selectProWorkorderParamList(proWorkorderParam);
        return getDataTable(list);
    }

    /**
     * 根据工单ID查询工序参数列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/listByWorkorderId/{workorderId}")
    public AjaxResult listByWorkorderId(@PathVariable("workorderId") Long workorderId)
    {
        List<ProWorkorderParam> list = proWorkorderParamService.selectProWorkorderParamByWorkorderId(workorderId);
        return success(list);
    }

    /**
     * 导出工单工序参数值列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:export')")
    @Log(title = "工单工序参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProWorkorderParam proWorkorderParam)
    {
        List<ProWorkorderParam> list = proWorkorderParamService.selectProWorkorderParamList(proWorkorderParam);
        ExcelUtil<ProWorkorderParam> util = new ExcelUtil<ProWorkorderParam>(ProWorkorderParam.class);
        util.exportExcel(response, list, "工单工序参数数据");
    }

    /**
     * 获取工单工序参数值详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proWorkorderParamService.selectProWorkorderParamByRecordId(recordId));
    }

    /**
     * 新增工单工序参数值
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "工单工序参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProWorkorderParam proWorkorderParam)
    {
        proWorkorderParamService.insertProWorkorderParam(proWorkorderParam);
        return success(proWorkorderParam);
    }

    /**
     * 修改工单工序参数值
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "工单工序参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProWorkorderParam proWorkorderParam)
    {
        return toAjax(proWorkorderParamService.updateProWorkorderParam(proWorkorderParam));
    }

    /**
     * 删除工单工序参数值
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @Log(title = "工单工序参数", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proWorkorderParamService.deleteProWorkorderParamByRecordIds(recordIds));
    }
}
