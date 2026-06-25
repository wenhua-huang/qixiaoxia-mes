package com.ruoyi.web.controller.mes.pro;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.service.mes.pro.IProMaterialTraceService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 物料追溯Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/materialtrace")
public class ProMaterialTraceController extends BaseController
{
    @Autowired
    private IProMaterialTraceService proMaterialTraceService;

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProMaterialTrace e) { startPage(); return getDataTable(proMaterialTraceService.selectProMaterialTraceList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(proMaterialTraceService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:export')")
    @Log(title = "物料追溯", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProMaterialTrace e) {
        List<ProMaterialTrace> list = proMaterialTraceService.selectProMaterialTraceList(e);
        ExcelUtil<ProMaterialTrace> util = new ExcelUtil<ProMaterialTrace>(ProMaterialTrace.class);
        util.exportExcel(response, list, "物料追溯数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:query')")
    @GetMapping(value = "/{traceId}")
    public AjaxResult getInfo(@PathVariable("traceId") Long traceId) { return success(proMaterialTraceService.selectProMaterialTraceByTraceId(traceId)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:add')")
    @Log(title = "物料追溯", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProMaterialTrace e) { return toAjax(proMaterialTraceService.insertProMaterialTrace(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:edit')")
    @Log(title = "物料追溯", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProMaterialTrace e) { return toAjax(proMaterialTraceService.updateProMaterialTrace(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:remove')")
    @Log(title = "物料追溯", businessType = BusinessType.DELETE)
    @DeleteMapping("/{traceIds}")
    public AjaxResult remove(@PathVariable Long[] traceIds) { return toAjax(proMaterialTraceService.deleteProMaterialTraceByTraceIds(traceIds)); }

    /** 正向追溯：查来源的所有去向 */
    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:query')")
    @GetMapping("/forward")
    public AjaxResult forward(@RequestParam String parentType, @RequestParam Long parentId)
    { return success(proMaterialTraceService.traceForward(parentType, parentId)); }

    /** 反向追溯：查去向的所有来源 */
    @PreAuthorize("@ss.hasPermi('mes:pro:materialtrace:query')")
    @GetMapping("/backward")
    public AjaxResult backward(@RequestParam String childType, @RequestParam Long childId)
    { return success(proMaterialTraceService.traceBackward(childType, childId)); }
}
