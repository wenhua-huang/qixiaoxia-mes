package com.ruoyi.web.controller.mes.pur;

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
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 采购订单行Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/pur/order-line")
public class PurOrderLineController extends BaseController
{
    @Autowired
    private IPurOrderLineService purOrderLineService;

    /**
     * 查询采购订单行列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order-line:list')")
    @GetMapping("/list")
    public TableDataInfo list(PurOrderLine purOrderLine)
    {
        startPage();
        List<PurOrderLine> list = purOrderLineService.selectPurOrderLineList(purOrderLine);
        return getDataTable(list);
    }

    /**
     * 导出采购订单行列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order-line:export')")
    @Log(title = "采购订单行", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PurOrderLine purOrderLine)
    {
        List<PurOrderLine> list = purOrderLineService.selectPurOrderLineList(purOrderLine);
        ExcelUtil<PurOrderLine> util = new ExcelUtil<PurOrderLine>(PurOrderLine.class);
        util.exportExcel(response, list, "采购订单行数据");
    }

    /**
     * 获取采购订单行详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order-line:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return success(purOrderLineService.selectPurOrderLineByLineId(lineId));
    }

    /**
     * 新增采购订单行
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order-line:add')")
    @Log(title = "采购订单行", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PurOrderLine purOrderLine)
    {
        purOrderLineService.insertPurOrderLine(purOrderLine);
        return success(purOrderLine);
    }

    /**
     * 修改采购订单行
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order-line:edit')")
    @Log(title = "采购订单行", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PurOrderLine purOrderLine)
    {
        return toAjax(purOrderLineService.updatePurOrderLine(purOrderLine));
    }

    /**
     * 删除采购订单行
     */
    @PreAuthorize("@ss.hasPermi('mes:pur:order-line:remove')")
    @Log(title = "采购订单行", businessType = BusinessType.DELETE)
	@DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(purOrderLineService.deletePurOrderLineByLineIds(lineIds));
    }
}
