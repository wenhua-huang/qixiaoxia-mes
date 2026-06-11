package com.ruoyi.web.controller.mes.sys;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.sys.ISysAutoCodePartService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 编码生成规则组成Controller
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@RestController
@RequestMapping("/mes/sys/autocodepart")
public class SysAutoCodePartController extends BaseController
{
    @Autowired
    private ISysAutoCodePartService sysAutoCodePartService;

    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAutoCodePart sysAutoCodePart)
    {
        startPage();
        List<SysAutoCodePart> list = sysAutoCodePartService.selectSysAutoCodePartList(sysAutoCodePart);
        return getDataTable(list);
    }

    /**
     * 按规则ID查询分段列表（不分页，供生成编码使用）
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:list')")
    @GetMapping("/listByRuleId/{ruleId}")
    public AjaxResult listByRuleId(@PathVariable("ruleId") Long ruleId)
    {
        List<SysAutoCodePart> list = sysAutoCodePartService.selectSysAutoCodePartByRuleId(ruleId);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:export')")
    @Log(title = "编码分段", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAutoCodePart sysAutoCodePart)
    {
        List<SysAutoCodePart> list = sysAutoCodePartService.selectSysAutoCodePartList(sysAutoCodePart);
        ExcelUtil<SysAutoCodePart> util = new ExcelUtil<SysAutoCodePart>(SysAutoCodePart.class);
        util.exportExcel(response, list, "编码分段数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:query')")
    @GetMapping(value = "/{partId}")
    public AjaxResult getInfo(@PathVariable("partId") Long partId)
    {
        return AjaxResult.success(sysAutoCodePartService.selectSysAutoCodePartByPartId(partId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:add')")
    @Log(title = "编码分段", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysAutoCodePart sysAutoCodePart)
    {
        return toAjax(sysAutoCodePartService.insertSysAutoCodePart(sysAutoCodePart));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:edit')")
    @Log(title = "编码分段", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAutoCodePart sysAutoCodePart)
    {
        return toAjax(sysAutoCodePartService.updateSysAutoCodePart(sysAutoCodePart));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocodepart:remove')")
    @Log(title = "编码分段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{partIds}")
    public AjaxResult remove(@PathVariable Long[] partIds)
    {
        return toAjax(sysAutoCodePartService.deleteSysAutoCodePartByPartIds(partIds));
    }
}
