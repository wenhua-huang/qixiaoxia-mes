package com.ruoyi.web.controller.mes.pro;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.pro.ProException;
import com.ruoyi.system.service.mes.pro.IProExceptionBlockService;
import com.ruoyi.system.service.mes.pro.IProExceptionService;

/**
 * 生产异常单 Controller（E1–E6 + D2）。
 *
 * <p>单据由手机端（E6）与 PC 工序任务页（E1）上报产生，无 remove；只允许补全/选出口/关闭/作废。
 *
 * @author qixiaoxia
 */
@RestController
@RequestMapping("/mes/pro/exception")
public class ProExceptionController extends BaseController
{
    @Autowired
    private IProExceptionService proExceptionService;

    @Autowired
    private IProExceptionBlockService proExceptionBlockService;

    /** 异常台账（D2） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProException query)
    {
        startPage();
        List<ProException> list = proExceptionService.selectProExceptionList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:exception:export')")
    @Log(title = "生产异常单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProException query)
    {
        List<ProException> list = proExceptionService.selectProExceptionList(query);
        ExcelUtil<ProException> util = new ExcelUtil<>(ProException.class);
        util.exportExcel(response, list, "生产异常单数据");
    }

    /** 手机上报页只读关联对象（工单/工序上下文） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:add')")
    @GetMapping("/reportContext/{taskId}")
    public AjaxResult reportContext(@PathVariable Long taskId)
    {
        return success(proExceptionService.reportContext(taskId));
    }

    /** 工单未关闭异常明细（工单详情红色警示区，E5） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:query')")
    @GetMapping("/openByWorkorder/{workorderId}")
    public AjaxResult openByWorkorder(@PathVariable Long workorderId)
    {
        return success(proExceptionBlockService.listOpenByWorkorder(workorderId));
    }

    /** 工单列表未关闭异常角标批量查询，单次 ≤100 */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:list')")
    @PostMapping("/openState")
    public AjaxResult openState(@RequestBody List<Long> workorderIds)
    {
        return success(proExceptionBlockService.openState(workorderIds));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:exception:query')")
    @GetMapping(value = "/{exceptionId}")
    public AjaxResult getInfo(@PathVariable Long exceptionId)
    {
        return success(proExceptionService.selectProExceptionByExceptionId(exceptionId));
    }

    /** 手机端上报异常（E6） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:add')")
    @Log(title = "生产异常单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProException exception)
    {
        return success(proExceptionService.reportException(exception));
    }

    /** PC 补全异常单（E1/E2：定责 + 四类专属字段） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:edit')")
    @Log(title = "生产异常单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProException exception)
    {
        return toAjax(proExceptionService.updateProException(exception));
    }

    /** 选出口处理（E3 七选一） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:handle')")
    @Log(title = "生产异常单处理", businessType = BusinessType.UPDATE)
    @PutMapping("/resolve/{exceptionId}")
    public AjaxResult resolve(@PathVariable Long exceptionId, @RequestBody ProException input)
    {
        return success(proExceptionService.resolveException(exceptionId, input));
    }

    /** 回流类异常手动收口（处理中 → 已关闭） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:handle')")
    @Log(title = "生产异常单关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{exceptionId}")
    public AjaxResult close(@PathVariable Long exceptionId, @RequestBody ProException input)
    {
        String conclusion = input == null ? null : input.getConclusion();
        return toAjax(proExceptionService.closeException(exceptionId, conclusion));
    }

    /** 作废异常单（E1：挂错对象作废重开，仅待处理可作废，必填原因） */
    @PreAuthorize("@ss.hasPermi('mes:pro:exception:handle')")
    @Log(title = "生产异常单作废", businessType = BusinessType.UPDATE)
    @PutMapping("/void/{exceptionId}")
    public AjaxResult voidException(@PathVariable Long exceptionId, @RequestBody ProException input)
    {
        String reason = input == null ? null : input.getConclusion();
        return toAjax(proExceptionService.voidException(exceptionId, reason));
    }
}
