package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdItemTypeService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 物料产品分类Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/itemtype")
public class MdItemTypeController extends BaseController
{
    @Autowired
    private IMdItemTypeService mdItemTypeService;

    /**
     * 查询分类列表（树形表格用，返回全量数据不做分页）
     */
    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:list')")
    @GetMapping("/list")
    public AjaxResult list(MdItemType mdItemType)
    {
        List<MdItemType> list = mdItemTypeService.selectMdItemTypeList(mdItemType);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:query')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect()
    {
        MdItemType mdItemType = new MdItemType();
        mdItemType.setEnableFlag("1");
        List<MdItemType> list = mdItemTypeService.selectMdItemTypeList(mdItemType);
        List<TreeSelect> trees = mdItemTypeService.buildItemTypeTreeSelect(list);
        return AjaxResult.success(trees);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:query')")
    @GetMapping("/list/exclude/{itemTypeId}")
    public AjaxResult listExcludeChild(@PathVariable Long itemTypeId)
    {
        List<MdItemType> list = mdItemTypeService.selectMdItemTypeListExcludeChild(itemTypeId);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:export')")
    @Log(title = "物料产品分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdItemType mdItemType)
    {
        List<MdItemType> list = mdItemTypeService.selectMdItemTypeList(mdItemType);
        ExcelUtil<MdItemType> util = new ExcelUtil<MdItemType>(MdItemType.class);
        util.exportExcel(response, list, "物料分类数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:query')")
    @GetMapping(value = "/{itemTypeId}")
    public AjaxResult getInfo(@PathVariable("itemTypeId") Long itemTypeId)
    {
        return AjaxResult.success(mdItemTypeService.selectMdItemTypeById(itemTypeId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:add')")
    @Log(title = "物料产品分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdItemType mdItemType)
    {
        if (!mdItemTypeService.checkItemTypeCodeUnique(mdItemType))
        {
            return AjaxResult.error("分类编码已存在！");
        }
        return toAjax(mdItemTypeService.insertMdItemType(mdItemType));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:edit')")
    @Log(title = "物料产品分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdItemType mdItemType)
    {
        if (!mdItemTypeService.checkItemTypeCodeUnique(mdItemType))
        {
            return AjaxResult.error("分类编码已存在！");
        }
        // 检查循环引用：不能将父类型设为自己或子孙节点
        if (!mdItemTypeService.canSetParentType(mdItemType.getItemTypeId(), mdItemType.getParentTypeId()))
        {
            return AjaxResult.error("父类型不能选择自身或子孙节点！");
        }
        return toAjax(mdItemTypeService.updateMdItemType(mdItemType));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:remove')")
    @Log(title = "物料产品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{itemTypeIds}")
    public AjaxResult remove(@PathVariable Long[] itemTypeIds)
    {
        return toAjax(mdItemTypeService.deleteMdItemTypeByIds(itemTypeIds));
    }
}
