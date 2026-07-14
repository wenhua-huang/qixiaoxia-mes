package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmProductRecptService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptMobileBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 产品入库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-07-11
 */
@RestController
@RequestMapping("/mes/wm/product_recpt")
public class WmProductRecptController extends BaseController
{
    @Autowired
    private IWmProductRecptService wmProductRecptService;

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductRecpt entity)
    {
        startPage();
        List<WmProductRecpt> list = wmProductRecptService.selectWmProductRecptList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmProductRecpt> list = wmProductRecptService.selectWmProductRecptAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:export')")
    @Log(title = "产品入库单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductRecpt entity)
    {
        List<WmProductRecpt> list = wmProductRecptService.selectWmProductRecptList(entity);
        ExcelUtil<WmProductRecpt> util = new ExcelUtil<>(WmProductRecpt.class);
        util.exportExcel(response, list, "产品入库单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmProductRecptService.selectWmProductRecptDetail(recptId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:add')")
    @Log(title = "产品入库单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductRecpt entity)
    {
        wmProductRecptService.insertWmProductRecpt(entity);
        return AjaxResult.success(entity);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:edit')")
    @Log(title = "产品入库单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmProductRecpt entity)
    {
        return toAjax(wmProductRecptService.updateWmProductRecpt(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:remove')")
    @Log(title = "产品入库单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        return toAjax(wmProductRecptService.deleteWmProductRecptByRecptIds(recptIds));
    }

    /**
     * 确认收货（DRAFT -> CONFIRMED）- 更新库存
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:edit')")
    @Log(title = "产品入库单确认收货", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{recptId}")
    public AjaxResult confirm(@PathVariable("recptId") Long recptId) {
        try {
            wmProductRecptService.confirmProductRecpt(recptId);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 过账入库（CONFIRMED -> POSTED）
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:edit')")
    @Log(title = "产品入库单过账", businessType = BusinessType.UPDATE)
    @PutMapping("/post/{recptId}")
    public AjaxResult post(@PathVariable("recptId") Long recptId) {
        try {
            wmProductRecptService.postProductRecpt(recptId);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 移动端确认入库 — 更新行数量 + 确认 + 更新库存，单接口原子完成。
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:product_recpt:edit')")
    @Log(title = "产品入库单移动端确认", businessType = BusinessType.UPDATE)
    @PutMapping("/mobile/confirm/{recptId}")
    public AjaxResult mobileConfirm(@PathVariable("recptId") Long recptId,
                                    @RequestBody WmProductRecptMobileBody body) {
        try {
            wmProductRecptService.mobileConfirmProductRecpt(recptId, body);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
