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
import com.ruoyi.system.domain.mes.pro.ProRouteProductBom;
import com.ruoyi.system.service.mes.pro.IProRouteProductBomService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工艺路线产品BOMController
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/routeproductbom")
public class ProRouteProductBomController extends BaseController
{
    @Autowired
    private IProRouteProductBomService proRouteProductBomService;

    /**
     * 查询工艺路线产品BOM列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProRouteProductBom proRouteProductBom)
    {
        startPage();
        List<ProRouteProductBom> list = proRouteProductBomService.selectProRouteProductBomList(proRouteProductBom);
        return getDataTable(list);
    }

    /**
     * 根据路线ID查询BOM列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping("/listByRouteId/{routeId}")
    public AjaxResult listByRouteId(@PathVariable("routeId") Long routeId)
    {
        List<ProRouteProductBom> list = proRouteProductBomService.selectProRouteProductBomByRouteId(routeId);
        return success(list);
    }

    /**
     * 导出工艺路线产品BOM列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:export')")
    @Log(title = "工艺路线产品BOM", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProRouteProductBom proRouteProductBom)
    {
        List<ProRouteProductBom> list = proRouteProductBomService.selectProRouteProductBomList(proRouteProductBom);
        ExcelUtil<ProRouteProductBom> util = new ExcelUtil<ProRouteProductBom>(ProRouteProductBom.class);
        util.exportExcel(response, list, "工艺路线产品BOM数据");
    }

    /**
     * 获取工艺路线产品BOM详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proRouteProductBomService.selectProRouteProductBomByRecordId(recordId));
    }

    /**
     * 新增工艺路线产品BOM
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:add')")
    @Log(title = "工艺路线产品BOM", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProRouteProductBom proRouteProductBom)
    {
        proRouteProductBomService.insertProRouteProductBom(proRouteProductBom);
        return success(proRouteProductBom);
    }

    /**
     * 修改工艺路线产品BOM
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:edit')")
    @Log(title = "工艺路线产品BOM", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProRouteProductBom proRouteProductBom)
    {
        return toAjax(proRouteProductBomService.updateProRouteProductBom(proRouteProductBom));
    }

    /**
     * 删除工艺路线产品BOM
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:remove')")
    @Log(title = "工艺路线产品BOM", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proRouteProductBomService.deleteProRouteProductBomByRecordIds(recordIds));
    }
}
