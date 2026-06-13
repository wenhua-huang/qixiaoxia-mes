package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmTransferDetailService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmTransferDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 调拨转移单明细表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/transfer_detail")
public class WmTransferDetailController extends BaseController
{
    @Autowired
    private IWmTransferDetailService wmTransferDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmTransferDetail entity)
    {
        startPage();
        List<WmTransferDetail> list = wmTransferDetailService.selectWmTransferDetailList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmTransferDetail> list = wmTransferDetailService.selectWmTransferDetailAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:export')")
    @Log(title = "调拨转移单明细表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmTransferDetail entity)
    {
        List<WmTransferDetail> list = wmTransferDetailService.selectWmTransferDetailList(entity);
        ExcelUtil<WmTransferDetail> util = new ExcelUtil<WmTransferDetail>(WmTransferDetail.class);
        util.exportExcel(response, list, "调拨转移单明细表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:query')")
    @GetMapping(value = "/{detailId}")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId)
    {
        return AjaxResult.success(wmTransferDetailService.selectWmTransferDetailByDetailId(detailId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:add')")
    @Log(title = "调拨转移单明细表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmTransferDetail entity)
    {
        return toAjax(wmTransferDetailService.insertWmTransferDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:edit')")
    @Log(title = "调拨转移单明细表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmTransferDetail entity)
    {
        return toAjax(wmTransferDetailService.updateWmTransferDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer_detail:remove')")
    @Log(title = "调拨转移单明细表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{detailIds}")
    public AjaxResult remove(@PathVariable Long[] detailIds)
    {
        return toAjax(wmTransferDetailService.deleteWmTransferDetailByDetailIds(detailIds));
    }
}