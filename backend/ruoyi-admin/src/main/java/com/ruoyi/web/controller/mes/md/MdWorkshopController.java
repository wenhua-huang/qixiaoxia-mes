package com.ruoyi.web.controller.mes.md;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdWorkshopService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdWorkshop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 车间管理Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/workshop")
public class MdWorkshopController extends BaseController
{
    @Autowired
    private IMdWorkshopService mdWorkshopService;

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdWorkshop mdWorkshop)
    {
        startPage();
        List<MdWorkshop> list = mdWorkshopService.selectMdWorkshopList(mdWorkshop);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        MdWorkshop mdWorkshop = new MdWorkshop();
        mdWorkshop.setEnableFlag("1");
        List<MdWorkshop> list = mdWorkshopService.selectMdWorkshopList(mdWorkshop);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:export')")
    @Log(title = "车间管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdWorkshop mdWorkshop)
    {
        List<MdWorkshop> list = mdWorkshopService.selectMdWorkshopList(mdWorkshop);
        ExcelUtil<MdWorkshop> util = new ExcelUtil<MdWorkshop>(MdWorkshop.class);
        util.exportExcel(response, list, "车间数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:query')")
    @GetMapping(value = "/{workshopId}")
    public AjaxResult getInfo(@PathVariable("workshopId") Long workshopId)
    {
        return AjaxResult.success(mdWorkshopService.selectMdWorkshopById(workshopId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:add')")
    @Log(title = "车间管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdWorkshop mdWorkshop)
    {
        if (!mdWorkshopService.checkWorkshopCodeUnique(mdWorkshop))
        {
            return AjaxResult.error("车间编码已存在！");
        }
        return toAjax(mdWorkshopService.insertMdWorkshop(mdWorkshop));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:edit')")
    @Log(title = "车间管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdWorkshop mdWorkshop)
    {
        if (!mdWorkshopService.checkWorkshopCodeUnique(mdWorkshop))
        {
            return AjaxResult.error("车间编码已存在！");
        }
        return toAjax(mdWorkshopService.updateMdWorkshop(mdWorkshop));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workshop:remove')")
    @Log(title = "车间管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{workshopIds}")
    public AjaxResult remove(@PathVariable Long[] workshopIds)
    {
        return toAjax(mdWorkshopService.deleteMdWorkshopByIds(workshopIds));
    }
}
