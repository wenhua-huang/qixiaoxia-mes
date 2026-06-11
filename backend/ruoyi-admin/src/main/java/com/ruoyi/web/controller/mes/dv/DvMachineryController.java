package com.ruoyi.web.controller.mes.dv;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.dv.IDvMachineryService;
import com.ruoyi.system.service.mes.dv.IDvMachineryTypeService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.dv.DvMachinery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 设备台账Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
@RestController
@RequestMapping("/mes/dv/machinery")
public class DvMachineryController extends BaseController
{
    @Autowired
    private IDvMachineryService dvMachineryService;

    @Autowired
    private IDvMachineryTypeService dvMachineryTypeService;

    @PreAuthorize("@ss.hasPermi('mes:dv:machinery:list')")
    @GetMapping("/list")
    public TableDataInfo list(DvMachinery dvMachinery)
    {
        // 子树过滤：用递归CTE解析设备类型下的所有子孙类型ID，避免FactoryIdInterceptor与CTE冲突
        if (dvMachinery.getMachineryTypeId() != null)
        {
            List<Long> typeIds = dvMachineryTypeService.selectDescendantIds(dvMachinery.getMachineryTypeId());
            dvMachinery.setMachineryTypeIds(typeIds);
            dvMachinery.setMachineryTypeId(null);
        }
        startPage();
        List<DvMachinery> list = dvMachineryService.selectDvMachineryList(dvMachinery);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinery:export')")
    @Log(title = "设备台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DvMachinery dvMachinery)
    {
        List<DvMachinery> list = dvMachineryService.selectDvMachineryList(dvMachinery);
        ExcelUtil<DvMachinery> util = new ExcelUtil<DvMachinery>(DvMachinery.class);
        util.exportExcel(response, list, "设备台账数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinery:query')")
    @GetMapping(value = "/{machineryId}")
    public AjaxResult getInfo(@PathVariable("machineryId") Long machineryId)
    {
        return AjaxResult.success(dvMachineryService.selectDvMachineryById(machineryId));
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinery:add')")
    @Log(title = "设备台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DvMachinery dvMachinery)
    {
        if (!dvMachineryService.checkMachineryCodeUnique(dvMachinery))
        {
            return AjaxResult.error("设备编码已存在！");
        }
        return toAjax(dvMachineryService.insertDvMachinery(dvMachinery));
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinery:edit')")
    @Log(title = "设备台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DvMachinery dvMachinery)
    {
        if (!dvMachineryService.checkMachineryCodeUnique(dvMachinery))
        {
            return AjaxResult.error("设备编码已存在！");
        }
        return toAjax(dvMachineryService.updateDvMachinery(dvMachinery));
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinery:remove')")
    @Log(title = "设备台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{machineryIds}")
    public AjaxResult remove(@PathVariable Long[] machineryIds)
    {
        return toAjax(dvMachineryService.deleteDvMachineryByIds(machineryIds));
    }
}
