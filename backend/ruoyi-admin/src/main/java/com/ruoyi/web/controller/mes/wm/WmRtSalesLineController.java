package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmRtSalesLineService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmRtSalesLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 销售退货单行表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/rt_sales_line")
public class WmRtSalesLineController extends BaseController
{
    @Autowired
    private IWmRtSalesLineService wmRtSalesLineService;

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtSalesLine entity)
    {
        startPage();
        List<WmRtSalesLine> list = wmRtSalesLineService.selectWmRtSalesLineList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmRtSalesLine> list = wmRtSalesLineService.selectWmRtSalesLineAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:export')")
    @Log(title = "销售退货单行表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtSalesLine entity)
    {
        List<WmRtSalesLine> list = wmRtSalesLineService.selectWmRtSalesLineList(entity);
        ExcelUtil<WmRtSalesLine> util = new ExcelUtil<WmRtSalesLine>(WmRtSalesLine.class);
        util.exportExcel(response, list, "销售退货单行表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmRtSalesLineService.selectWmRtSalesLineByLineId(lineId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:add')")
    @Log(title = "销售退货单行表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtSalesLine entity)
    {
        return toAjax(wmRtSalesLineService.insertWmRtSalesLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:edit')")
    @Log(title = "销售退货单行表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRtSalesLine entity)
    {
        return toAjax(wmRtSalesLineService.updateWmRtSalesLine(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rt_sales_line:remove')")
    @Log(title = "销售退货单行表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(wmRtSalesLineService.deleteWmRtSalesLineByLineIds(lineIds));
    }
}