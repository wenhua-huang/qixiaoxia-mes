package com.ruoyi.web.controller.mes.dv;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.dv.IDvMachineryTypeService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.dv.DvMachineryType;
import com.ruoyi.system.service.mes.dv.IDvMachineryService;
import com.ruoyi.system.domain.mes.dv.DvMachinery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 设备类型Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
@RestController
@RequestMapping("/mes/dv/machinerytype")
public class DvMachineryTypeController extends BaseController
{
    @Autowired
    private IDvMachineryTypeService dvMachineryTypeService;

    @Autowired
    private IDvMachineryService dvMachineryService;

    /** 查询列表（树形表格用，返回全量数据不做分页） */
    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:list')")
    @GetMapping("/list")
    public AjaxResult list(DvMachineryType dvMachineryType)
    {
        List<DvMachineryType> list = dvMachineryTypeService.selectDvMachineryTypeList(dvMachineryType);
        return AjaxResult.success(list);
    }

    /** 前端树形下拉 */
    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:query')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect()
    {
        DvMachineryType dvMachineryType = new DvMachineryType();
        dvMachineryType.setEnableFlag("1");
        List<DvMachineryType> list = dvMachineryTypeService.selectDvMachineryTypeList(dvMachineryType);
        List<TreeSelect> trees = dvMachineryTypeService.buildMachineryTypeTreeSelect(list);
        return AjaxResult.success(trees);
    }

    /** 编辑时查询排除自身及子孙的列表 */
    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:query')")
    @GetMapping("/list/exclude/{machineryTypeId}")
    public AjaxResult listExcludeChild(@PathVariable Long machineryTypeId)
    {
        List<DvMachineryType> list = dvMachineryTypeService.selectListExcludeChild(machineryTypeId);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:export')")
    @Log(title = "设备类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DvMachineryType dvMachineryType)
    {
        List<DvMachineryType> list = dvMachineryTypeService.selectDvMachineryTypeList(dvMachineryType);
        ExcelUtil<DvMachineryType> util = new ExcelUtil<DvMachineryType>(DvMachineryType.class);
        util.exportExcel(response, list, "设备类型数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:query')")
    @GetMapping(value = "/{machineryTypeId}")
    public AjaxResult getInfo(@PathVariable("machineryTypeId") Long machineryTypeId)
    {
        return AjaxResult.success(dvMachineryTypeService.selectDvMachineryTypeById(machineryTypeId));
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:add')")
    @Log(title = "设备类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DvMachineryType dvMachineryType)
    {
        if (!dvMachineryTypeService.checkMachineryTypeCodeUnique(dvMachineryType))
        {
            return AjaxResult.error("设备类型编码已存在！");
        }
        return toAjax(dvMachineryTypeService.insertDvMachineryType(dvMachineryType));
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:edit')")
    @Log(title = "设备类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DvMachineryType dvMachineryType)
    {
        if (!dvMachineryTypeService.checkMachineryTypeCodeUnique(dvMachineryType))
        {
            return AjaxResult.error("设备类型编码已存在！");
        }
        if (!dvMachineryTypeService.canSetParentType(dvMachineryType.getMachineryTypeId(), dvMachineryType.getParentTypeId()))
        {
            return AjaxResult.error("父类型不能选择自身或子孙节点！");
        }
        return toAjax(dvMachineryTypeService.updateDvMachineryType(dvMachineryType));
    }

    @PreAuthorize("@ss.hasPermi('mes:dv:machinerytype:remove')")
    @Log(title = "设备类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{machineryTypeIds}")
    public AjaxResult remove(@PathVariable Long[] machineryTypeIds)
    {
        // 检查每个类型（含子孙类型）下是否有设备，如果有则不允许删除
        for (Long typeId : machineryTypeIds)
        {
            // 检查是否有子类型
            List<DvMachineryType> childTypes = dvMachineryTypeService.selectDvMachineryTypeByParentId(typeId);
            if (!childTypes.isEmpty())
            {
                DvMachineryType type = dvMachineryTypeService.selectDvMachineryTypeById(typeId);
                return AjaxResult.error("设备类型【" + (type != null ? type.getMachineryTypeName() : typeId) + "】下存在子类型，请先删除子类型！");
            }
            // 递归查询该类型及所有子孙类型ID，确保子树设备也被检查
            DvMachinery param = new DvMachinery();
            // 递归查询该类型及所有子孙类型ID，确保子树设备也被检查
            List<Long> allTypeIds = dvMachineryTypeService.selectDescendantIds(typeId);
            param.setMachineryTypeIds(allTypeIds);
            List<DvMachinery> children = dvMachineryService.selectDvMachineryList(param);
            if (!children.isEmpty())
            {
                DvMachineryType type = dvMachineryTypeService.selectDvMachineryTypeById(typeId);
                return AjaxResult.error("设备类型【" + (type != null ? type.getMachineryTypeName() : typeId) + "】及其子类型下已配置了设备，不能删除！");
            }
        }
        return toAjax(dvMachineryTypeService.deleteDvMachineryTypeByIds(machineryTypeIds));
    }
}
