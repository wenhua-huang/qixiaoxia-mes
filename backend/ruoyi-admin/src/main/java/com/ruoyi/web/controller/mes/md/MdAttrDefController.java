package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.system.domain.mes.md.MdAttrDef;
import com.ruoyi.system.service.mes.md.IMdAttrDefService;

/**
 * 物料扩展属性字典Controller
 * <p>attr_def 为全局表（factory_id 恒为 0），不受 FactoryIdInterceptor 隔离。
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
@RestController
@RequestMapping("/mes/md/attrDef")
public class MdAttrDefController extends BaseController
{
    @Autowired
    private IMdAttrDefService mdAttrDefService;

    @PreAuthorize("@ss.hasPermi('mes:md:attrDef:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdAttrDef mdAttrDef)
    {
        // attr_def 为全局字典(factory_id=0)，数据量小，不分页（避免 PageHelper count 破坏 @SkipFactoryId）
        List<MdAttrDef> list = mdAttrDefService.selectMdAttrDefList(mdAttrDef);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(list);
        rsp.setTotal(list.size());
        return rsp;
    }

    @PreAuthorize("@ss.hasPermi('mes:md:attrDef:export')")
    @Log(title = "物料扩展属性字典", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdAttrDef mdAttrDef)
    {
        List<MdAttrDef> list = mdAttrDefService.selectMdAttrDefList(mdAttrDef);
        ExcelUtil<MdAttrDef> util = new ExcelUtil<>(MdAttrDef.class);
        util.exportExcel(response, list, "物料扩展属性字典");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:attrDef:query')")
    @GetMapping("/{attrId}")
    public AjaxResult getInfo(@PathVariable Long attrId)
    {
        return success(mdAttrDefService.selectMdAttrDefByAttrId(attrId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:attrDef:add')")
    @Log(title = "物料扩展属性字典", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdAttrDef mdAttrDef)
    {
        if (!mdAttrDefService.checkAttrCodeUnique(mdAttrDef))
        {
            return error("新增失败，属性编码'" + mdAttrDef.getAttrCode() + "'已存在");
        }
        return toAjax(mdAttrDefService.insertMdAttrDef(mdAttrDef));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:attrDef:edit')")
    @Log(title = "物料扩展属性字典", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdAttrDef mdAttrDef)
    {
        if (!mdAttrDefService.checkAttrCodeUnique(mdAttrDef))
        {
            return error("修改失败，属性编码'" + mdAttrDef.getAttrCode() + "'已存在");
        }
        return toAjax(mdAttrDefService.updateMdAttrDef(mdAttrDef));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:attrDef:remove')")
    @Log(title = "物料扩展属性字典", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attrIds}")
    public AjaxResult remove(@PathVariable Long[] attrIds)
    {
        return toAjax(mdAttrDefService.deleteMdAttrDefByAttrIds(attrIds));
    }
}
