package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmTransferService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 调拨转移单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/transfer")
public class WmTransferController extends BaseController
{
    @Autowired
    private IWmTransferService wmTransferService;

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmTransfer entity)
    {
        startPage();
        List<WmTransfer> list = wmTransferService.selectWmTransferList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmTransfer> list = wmTransferService.selectWmTransferAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:export')")
    @Log(title = "调拨转移单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmTransfer entity)
    {
        List<WmTransfer> list = wmTransferService.selectWmTransferList(entity);
        ExcelUtil<WmTransfer> util = new ExcelUtil<WmTransfer>(WmTransfer.class);
        util.exportExcel(response, list, "调拨转移单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:query')")
    @GetMapping(value = "/{transferId}")
    public AjaxResult getInfo(@PathVariable("transferId") Long transferId)
    {
        return AjaxResult.success(wmTransferService.selectWmTransferByTransferId(transferId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:add')")
    @Log(title = "调拨转移单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmTransfer entity)
    {
        return toAjax(wmTransferService.insertWmTransfer(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:edit')")
    @Log(title = "调拨转移单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmTransfer entity)
    {
        return toAjax(wmTransferService.updateWmTransfer(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transfer:remove')")
    @Log(title = "调拨转移单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{transferIds}")
    public AjaxResult remove(@PathVariable Long[] transferIds)
    {
        return toAjax(wmTransferService.deleteWmTransferByTransferIds(transferIds));
    }
}