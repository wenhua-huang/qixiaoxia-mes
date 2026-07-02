package com.ruoyi.web.controller.mes.md;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.md.MdItemVendor;
import com.ruoyi.system.service.mes.md.IMdItemVendorService;

/**
 * 物料供应商关系Controller
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
@RestController
@RequestMapping("/mes/md/itemvendor")
public class MdItemVendorController extends BaseController
{
    @Autowired
    private IMdItemVendorService service;

    @GetMapping("/list")
    public TableDataInfo list(MdItemVendor query) { startPage(); return getDataTable(service.selectList(query)); }

    @GetMapping("/listByVendorId/{vendorId}")
    public AjaxResult listByVendorId(@PathVariable Long vendorId) { return success(service.selectByVendorId(vendorId)); }

    @GetMapping("/listByItemId/{itemId}")
    public AjaxResult listByItemId(@PathVariable Long itemId) { return success(service.selectByItemId(itemId)); }

    @GetMapping("/{recordId}")
    public AjaxResult getInfo(@PathVariable Long recordId) { return success(service.selectByRecordId(recordId)); }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:edit')")
    @Log(title = "物料供应商关系", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdItemVendor entity) { return toAjax(service.insert(entity)); }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:edit')")
    @Log(title = "物料供应商关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdItemVendor entity) { return toAjax(service.update(entity)); }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:edit')")
    @Log(title = "物料供应商关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) { return toAjax(service.deleteByRecordIds(recordIds)); }
}
