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
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.service.mes.pro.IProUserWorkstationService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户工作站Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/userworkstation")
public class ProUserWorkstationController extends BaseController
{
    @Autowired
    private IProUserWorkstationService proUserWorkstationService;

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProUserWorkstation e) { startPage(); return getDataTable(proUserWorkstationService.selectProUserWorkstationList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(proUserWorkstationService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:export')")
    @Log(title = "用户工作站", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProUserWorkstation e) {
        List<ProUserWorkstation> list = proUserWorkstationService.selectProUserWorkstationList(e);
        ExcelUtil<ProUserWorkstation> util = new ExcelUtil<ProUserWorkstation>(ProUserWorkstation.class);
        util.exportExcel(response, list, "用户工作站数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId) { return success(proUserWorkstationService.selectProUserWorkstationByRecordId(recordId)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:add')")
    @Log(title = "用户工作站", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProUserWorkstation e) { return toAjax(proUserWorkstationService.insertProUserWorkstation(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:edit')")
    @Log(title = "用户工作站", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProUserWorkstation e) { return toAjax(proUserWorkstationService.updateProUserWorkstation(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:remove')")
    @Log(title = "用户工作站", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) { return toAjax(proUserWorkstationService.deleteProUserWorkstationByRecordIds(recordIds)); }
}
