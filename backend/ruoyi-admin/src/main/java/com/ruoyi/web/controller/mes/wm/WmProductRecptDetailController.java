package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import com.ruoyi.system.service.mes.wm.IWmProductRecptDetailService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.WmProductRecptDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 产品入库单明细Controller
 *
 * @author qixiaoxia
 * @date 2026-07-11
 */
@RestController
@RequestMapping("/mes/wm/product_recpt_detail")
public class WmProductRecptDetailController extends BaseController
{
    @Autowired
    private IWmProductRecptDetailService wmProductRecptDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductRecptDetail entity)
    {
        startPage();
        List<WmProductRecptDetail> list = wmProductRecptDetailService.selectWmProductRecptDetailList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId)
    {
        return AjaxResult.success(wmProductRecptDetailService.selectWmProductRecptDetailByDetailId(detailId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:add')")
    @Log(title = "产品入库单明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductRecptDetail entity)
    {
        wmProductRecptDetailService.insertWmProductRecptDetail(entity);
        return AjaxResult.success(entity);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:edit')")
    @Log(title = "产品入库单明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductRecptDetail entity)
    {
        return toAjax(wmProductRecptDetailService.updateWmProductRecptDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:remove')")
    @Log(title = "产品入库单明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds)
    {
        return toAjax(wmProductRecptDetailService.deleteWmProductRecptDetailByDetailIds(detailIds));
    }
}
