package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmTransactionService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 库存事务表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/transaction")
public class WmTransactionController extends BaseController
{
    @Autowired
    private IWmTransactionService wmTransactionService;

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmTransaction entity)
    {
        startPage();
        List<WmTransaction> list = wmTransactionService.selectWmTransactionList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmTransaction> list = wmTransactionService.selectWmTransactionAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:export')")
    @Log(title = "库存事务表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmTransaction entity)
    {
        List<WmTransaction> list = wmTransactionService.selectWmTransactionList(entity);
        ExcelUtil<WmTransaction> util = new ExcelUtil<WmTransaction>(WmTransaction.class);
        util.exportExcel(response, list, "库存事务表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:query')")
    @GetMapping(value = "/{transactionId}")
    public AjaxResult getInfo(@PathVariable("transactionId") Long transactionId)
    {
        return AjaxResult.success(wmTransactionService.selectWmTransactionByTransactionId(transactionId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:add')")
    @Log(title = "库存事务表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmTransaction entity)
    {
        return toAjax(wmTransactionService.insertWmTransaction(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:edit')")
    @Log(title = "库存事务表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmTransaction entity)
    {
        return toAjax(wmTransactionService.updateWmTransaction(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:remove')")
    @Log(title = "库存事务表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{transactionIds}")
    public AjaxResult remove(@PathVariable Long[] transactionIds)
    {
        return toAjax(wmTransactionService.deleteWmTransactionByTransactionIds(transactionIds));
    }
}