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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdItemType;
import com.ruoyi.system.domain.mes.md.MdItemTypeAttr;
import com.ruoyi.system.domain.mes.md.MdAttrDef;
import com.ruoyi.system.domain.mes.md.AttrBindParam;
import com.ruoyi.system.domain.mes.md.CreateAttrAndBindParam;
import com.ruoyi.system.service.mes.md.IMdItemTypeAttrService;
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

    @Autowired
    private IMdItemTypeAttrService mdItemTypeAttrService;

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
        // 查询全部分类（不按 enableFlag 过滤），确保树结构完整
        List<MdItemType> list = mdItemTypeService.selectMdItemTypeList(new MdItemType());
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

    // ==================== 扩展属性绑定 ====================

    /**
     * 查询某分类的有效属性 schema（含继承，沿父链聚合）。
     * 供物料页/订单明细页动态渲染扩展属性表单用。
     */
    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:query')")
    @GetMapping("/effAttrSchema/{typeId}")
    public AjaxResult effAttrSchema(@PathVariable("typeId") Long typeId)
    {
        return AjaxResult.success(
            mdItemTypeAttrService.selectEffAttrSchema(SecurityUtils.getFactoryId(), typeId));
    }

    /**
     * 查询某分类直接绑定的属性（不含继承，配置页展示用）。
     */
    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:attr')")
    @GetMapping("/attrBind/{typeId}")
    public AjaxResult attrBind(@PathVariable("typeId") Long typeId)
    {
        return AjaxResult.success(mdItemTypeAttrService.selectBindByTypeId(typeId));
    }

    /**
     * 全量保存某分类的属性绑定（先删后插）。
     */
    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:attr')")
    @Log(title = "分类扩展属性绑定", businessType = BusinessType.UPDATE)
    @PutMapping("/attrBind")
    public AjaxResult saveAttrBind(@RequestBody AttrBindParam param)
    {
        if (param.getTypeId() == null) {
            return AjaxResult.error("typeId 不能为空");
        }
        mdItemTypeAttrService.saveBind(SecurityUtils.getFactoryId(), param.getTypeId(), param.getBinds());
        return AjaxResult.success();
    }

    /**
     * 新建属性并绑定到当前分类（隐式字典：attr_code 存在则复用，不重复创建）。
     * 供分类配置弹窗"+ 新建属性"内联创建用，用户无需跳到字典页。
     */
    @PreAuthorize("@ss.hasPermi('mes:md:itemtype:attr')")
    @Log(title = "新建属性并绑定分类", businessType = BusinessType.INSERT)
    @PostMapping("/attrBind/createAttrAndBind")
    public AjaxResult createAttrAndBind(@RequestBody CreateAttrAndBindParam param)
    {
        if (param.getTypeId() == null) {
            return AjaxResult.error("typeId 不能为空");
        }
        if (param.getAttrDef() == null) {
            return AjaxResult.error("attrDef 不能为空");
        }
        MdItemTypeAttr bind = mdItemTypeAttrService.createAttrAndBind(
            SecurityUtils.getFactoryId(), param.getTypeId(), param.getAttrDef(),
            Boolean.TRUE.equals(param.getRequired()));
        return AjaxResult.success(bind);
    }
}
