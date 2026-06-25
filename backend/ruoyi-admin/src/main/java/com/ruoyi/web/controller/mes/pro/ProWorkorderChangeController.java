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
import com.ruoyi.system.domain.mes.pro.ProWorkorderChange;
import com.ruoyi.system.service.mes.pro.IProWorkorderChangeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工单变更记录Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/workorderchange")
public class ProWorkorderChangeController extends BaseController
{
    @Autowired
    private IProWorkorderChangeService proWorkorderChangeService;

    /**
     * 查询工单变更记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProWorkorderChange proWorkorderChange)
    {
        startPage();
        List<ProWorkorderChange> list = proWorkorderChangeService.selectProWorkorderChangeList(proWorkorderChange);
        return getDataTable(list);
    }

    /**
     * 根据工单ID查询变更记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/listByWorkorderId/{workorderId}")
    public AjaxResult listByWorkorderId(@PathVariable("workorderId") Long workorderId)
    {
        List<ProWorkorderChange> list = proWorkorderChangeService.selectProWorkorderChangeByWorkorderId(workorderId);
        return success(list);
    }

    /**
     * 审批变更记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "工单变更审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{changeId}")
    public AjaxResult approve(@PathVariable("changeId") Long changeId)
    {
        return toAjax(proWorkorderChangeService.approve(changeId));
    }

    /**
     * 导出工单变更记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:export')")
    @Log(title = "工单变更记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProWorkorderChange proWorkorderChange)
    {
        List<ProWorkorderChange> list = proWorkorderChangeService.selectProWorkorderChangeList(proWorkorderChange);
        ExcelUtil<ProWorkorderChange> util = new ExcelUtil<ProWorkorderChange>(ProWorkorderChange.class);
        util.exportExcel(response, list, "工单变更记录数据");
    }

    /**
     * 获取工单变更记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping(value = "/{changeId}")
    public AjaxResult getInfo(@PathVariable("changeId") Long changeId)
    {
        return success(proWorkorderChangeService.selectProWorkorderChangeByChangeId(changeId));
    }

    /**
     * 新增工单变更记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "工单变更记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProWorkorderChange proWorkorderChange)
    {
        proWorkorderChangeService.insertProWorkorderChange(proWorkorderChange);
        return success(proWorkorderChange);
    }

    /**
     * 修改工单变更记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "工单变更记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProWorkorderChange proWorkorderChange)
    {
        return toAjax(proWorkorderChangeService.updateProWorkorderChange(proWorkorderChange));
    }

    /**
     * 删除工单变更记录
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @Log(title = "工单变更记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{changeIds}")
    public AjaxResult remove(@PathVariable Long[] changeIds)
    {
        return toAjax(proWorkorderChangeService.deleteProWorkorderChangeByChangeIds(changeIds));
    }
}
