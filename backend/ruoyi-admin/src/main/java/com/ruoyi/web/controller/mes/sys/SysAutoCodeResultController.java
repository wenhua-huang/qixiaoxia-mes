package com.ruoyi.web.controller.mes.sys;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeResultService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 编码生成记录Controller（只读，不允许手动新增/编辑）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@RestController
@RequestMapping("/mes/sys/autocoderesult")
public class SysAutoCodeResultController extends BaseController
{
    @Autowired
    private ISysAutoCodeResultService sysAutoCodeResultService;

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderesult:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAutoCodeResult sysAutoCodeResult)
    {
        startPage();
        List<SysAutoCodeResult> list = sysAutoCodeResultService.selectSysAutoCodeResultList(sysAutoCodeResult);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderesult:export')")
    @Log(title = "编码生成记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAutoCodeResult sysAutoCodeResult)
    {
        List<SysAutoCodeResult> list = sysAutoCodeResultService.selectSysAutoCodeResultList(sysAutoCodeResult);
        ExcelUtil<SysAutoCodeResult> util = new ExcelUtil<SysAutoCodeResult>(SysAutoCodeResult.class);
        util.exportExcel(response, list, "编码生成记录数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderesult:query')")
    @GetMapping(value = "/{codeId}")
    public AjaxResult getInfo(@PathVariable("codeId") Long codeId)
    {
        return AjaxResult.success(sysAutoCodeResultService.selectSysAutoCodeResultByCodeId(codeId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderesult:remove')")
    @Log(title = "编码生成记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{codeIds}")
    public AjaxResult remove(@PathVariable Long[] codeIds)
    {
        return toAjax(sysAutoCodeResultService.deleteSysAutoCodeResultByCodeIds(codeIds));
    }
}
