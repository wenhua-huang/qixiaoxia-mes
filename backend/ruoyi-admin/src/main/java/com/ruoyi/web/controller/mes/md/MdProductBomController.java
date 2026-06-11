package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdProductBomService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdProductBom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 产品BOMController（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/bom")
public class MdProductBomController extends BaseController
{
    @Autowired
    private IMdProductBomService mdProductBomService;

    @PreAuthorize("@ss.hasPermi('mes:md:bom:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdProductBom mdProductBom)
    {
        startPage();
        List<MdProductBom> list = mdProductBomService.selectMdProductBomList(mdProductBom);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:bom:export')")
    @Log(title = "产品BOM", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdProductBom mdProductBom)
    {
        List<MdProductBom> list = mdProductBomService.selectMdProductBomList(mdProductBom);
        ExcelUtil<MdProductBom> util = new ExcelUtil<MdProductBom>(MdProductBom.class);
        util.exportExcel(response, list, "产品BOM数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:bom:query')")
    @GetMapping(value = "/{bomId}")
    public AjaxResult getInfo(@PathVariable("bomId") Long bomId)
    {
        return AjaxResult.success(mdProductBomService.selectMdProductBomByBomId(bomId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:bom:add')")
    @Log(title = "产品BOM", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdProductBom mdProductBom)
    {
        if (mdProductBomService.checkBomCycle(mdProductBom))
        {
            return AjaxResult.error("BOM存在循环引用，请检查！");
        }
        return toAjax(mdProductBomService.insertMdProductBom(mdProductBom));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:bom:edit')")
    @Log(title = "产品BOM", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdProductBom mdProductBom)
    {
        if (mdProductBomService.checkBomCycle(mdProductBom))
        {
            return AjaxResult.error("BOM存在循环引用，请检查！");
        }
        return toAjax(mdProductBomService.updateMdProductBom(mdProductBom));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:bom:remove')")
    @Log(title = "产品BOM", businessType = BusinessType.DELETE)
    @DeleteMapping("/{bomIds}")
    public AjaxResult remove(@PathVariable Long[] bomIds)
    {
        return toAjax(mdProductBomService.deleteMdProductBomByBomIds(bomIds));
    }
}
