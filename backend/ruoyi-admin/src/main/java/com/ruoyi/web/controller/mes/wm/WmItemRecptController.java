package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.tx.ItemRecptTxBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

/**
 * 物料入库单表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/item_recpt")
public class WmItemRecptController extends BaseController
{
    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecpt entity)
    {
        startPage();
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:export')")
    @Log(title = "物料入库单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmItemRecpt entity)
    {
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(entity);
        ExcelUtil<WmItemRecpt> util = new ExcelUtil<WmItemRecpt>(WmItemRecpt.class);
        util.exportExcel(response, list, "物料入库单表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmItemRecptService.selectWmItemRecptByRecptId(recptId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:add')")
    @Log(title = "物料入库单表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecpt entity)
    {
        return toAjax(wmItemRecptService.insertWmItemRecpt(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:edit')")
    @Log(title = "物料入库单表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmItemRecpt entity)
    {
        return toAjax(wmItemRecptService.updateWmItemRecpt(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:remove')")
    @Log(title = "物料入库单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        return toAjax(wmItemRecptService.deleteWmItemRecptByRecptIds(recptIds));
    }

    /**
     * 确认入库 — 更新状态 + 生成库存事务
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:item_recpt:edit')")
    @Log(title = "物料入库单确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{recptId}")
    public AjaxResult confirm(@PathVariable Long recptId) {
        WmItemRecpt header = wmItemRecptService.selectWmItemRecptByRecptId(recptId);
        if (header == null) return AjaxResult.error("入库单不存在");
        if (!"DRAFT".equals(header.getStatus())) return AjaxResult.error("仅草稿状态可确认");

        // 加载行 → 构建 TxBean
        WmItemRecptLine q = new WmItemRecptLine();
        q.setRecptId(recptId);
        java.util.List<WmItemRecptLine> lines = wmItemRecptLineService.selectWmItemRecptLineList(q);

        java.util.List<ItemRecptTxBean> txBeans = new ArrayList<>();
        for (WmItemRecptLine line : lines) {
            ItemRecptTxBean b = new ItemRecptTxBean();
            b.setSourceDocType("wm_item_recpt");
            b.setSourceDocId(recptId);
            b.setSourceDocCode(header.getRecptCode());
            b.setSourceDocLineId(line.getLineId());
            b.setItemId(line.getItemId());
            b.setItemCode(line.getItemCode());
            b.setItemName(line.getItemName());
            b.setSpecification(line.getSpecification());
            b.setUnitOfMeasure(line.getUnitOfMeasure());
            b.setUnitName(line.getUnitName());
            b.setTransactionQuantity(line.getQuantityRecpt());
            b.setBatchId(line.getBatchId());
            b.setBatchCode(line.getBatchCode());
            b.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            b.setWarehouseCode(line.getWarehouseCode());
            b.setWarehouseName(line.getWarehouseName());
            b.setLocationId(line.getLocationId());
            b.setAreaId(line.getAreaId());
            b.setVendorId(header.getVendorId());
            b.setVendorCode(header.getVendorId() != null ? header.getVendorId().toString() : null);
            txBeans.add(b);
        }

        if (txBeans.isEmpty()) return AjaxResult.error("没有入库行，无法确认");

        storageCoreService.processItemRecpt(txBeans);

        header.setStatus("CONFIRMED");
        wmItemRecptService.updateWmItemRecpt(header);
        return AjaxResult.success();
    }
}