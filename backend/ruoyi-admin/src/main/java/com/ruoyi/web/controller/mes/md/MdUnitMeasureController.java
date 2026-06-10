package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdUnitMeasureService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdUnitMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 单位管理Controller
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
@RestController
@RequestMapping("/mes/md/unitmeasure")
public class MdUnitMeasureController extends BaseController
{
    @Autowired
    private IMdUnitMeasureService mdUnitMeasureService;

    /**
     * 查询单位列表
     */
    @PreAuthorize("@ss.hasPermi('mes:md:unitmeasure:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdUnitMeasure mdUnitMeasure)
    {
        startPage();
        List<MdUnitMeasure> list = mdUnitMeasureService.selectMdUnitMeasureList(mdUnitMeasure);
        return getDataTable(list);
    }

    /**
     * 查询所有主单位列表（primary_unit 为空的都是主单位）
     */
    @GetMapping("/listprimary")
    public AjaxResult listPrimary()
    {
        MdUnitMeasure mdUnitMeasure = new MdUnitMeasure();
        mdUnitMeasure.setEnableFlag("1");
        List<MdUnitMeasure> list = mdUnitMeasureService.selectMdUnitMeasureList(mdUnitMeasure);
        // 过滤出主单位（primary_unit 为空或 null 的）
        list.removeIf(u -> u.getPrimaryUnit() != null && !u.getPrimaryUnit().isEmpty());
        return AjaxResult.success(list);
    }

    /**
     * 查询所有启用的单位
     */
    @GetMapping("/selectall")
    public AjaxResult selectAll()
    {
        MdUnitMeasure mdUnitMeasure = new MdUnitMeasure();
        mdUnitMeasure.setEnableFlag("1");
        List<MdUnitMeasure> list = mdUnitMeasureService.selectMdUnitMeasureList(mdUnitMeasure);
        return AjaxResult.success(list);
    }

    /**
     * 导出单位列表
     */
    @PreAuthorize("@ss.hasPermi('mes:md:unitmeasure:export')")
    @Log(title = "单位管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdUnitMeasure mdUnitMeasure)
    {
        List<MdUnitMeasure> list = mdUnitMeasureService.selectMdUnitMeasureList(mdUnitMeasure);
        ExcelUtil<MdUnitMeasure> util = new ExcelUtil<MdUnitMeasure>(MdUnitMeasure.class);
        util.exportExcel(response, list, "单位数据");
    }

    /**
     * 获取单位详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:md:unitmeasure:query')")
    @GetMapping(value = "/{unitId}")
    public AjaxResult getInfo(@PathVariable("unitId") Long unitId)
    {
        return AjaxResult.success(mdUnitMeasureService.selectMdUnitMeasureByUnitId(unitId));
    }

    /**
     * 新增单位
     */
    @PreAuthorize("@ss.hasPermi('mes:md:unitmeasure:add')")
    @Log(title = "单位管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdUnitMeasure mdUnitMeasure)
    {
        if (!mdUnitMeasureService.checkUnitCodeUnique(mdUnitMeasure))
        {
            return AjaxResult.error("单位编码已存在！");
        }
        return toAjax(mdUnitMeasureService.insertMdUnitMeasure(mdUnitMeasure));
    }

    /**
     * 修改单位
     */
    @PreAuthorize("@ss.hasPermi('mes:md:unitmeasure:edit')")
    @Log(title = "单位管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdUnitMeasure mdUnitMeasure)
    {
        if (!mdUnitMeasureService.checkUnitCodeUnique(mdUnitMeasure))
        {
            return AjaxResult.error("单位编码已存在！");
        }
        return toAjax(mdUnitMeasureService.updateMdUnitMeasure(mdUnitMeasure));
    }

    /**
     * 删除单位
     */
    @PreAuthorize("@ss.hasPermi('mes:md:unitmeasure:remove')")
    @Log(title = "单位管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{unitIds}")
    public AjaxResult remove(@PathVariable Long[] unitIds)
    {
        return toAjax(mdUnitMeasureService.deleteMdUnitMeasureByUnitIds(unitIds));
    }
}
