package com.ruoyi.web.controller.mes.md;

import java.util.List;
import com.ruoyi.system.service.mes.md.IMdItemBatchConfigService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.md.MdItemBatchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 物料批次属性配置Controller
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/batchconfig")
public class MdItemBatchConfigController extends BaseController
{
    @Autowired
    private IMdItemBatchConfigService service;

    @PreAuthorize("@ss.hasPermi('mes:md:item:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdItemBatchConfig config)
    {
        startPage();
        List<MdItemBatchConfig> list = service.selectMdItemBatchConfigList(config);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:query')")
    @GetMapping(value = "/byItem/{itemId}")
    public AjaxResult getByItemId(@PathVariable("itemId") Long itemId)
    {
        return AjaxResult.success(service.selectMdItemBatchConfigByItemId(itemId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:query')")
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable("configId") Long configId)
    {
        return AjaxResult.success(service.selectMdItemBatchConfigById(configId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:add')")
    @Log(title = "批次配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdItemBatchConfig config)
    {
        return toAjax(service.insertMdItemBatchConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:edit')")
    @Log(title = "批次配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdItemBatchConfig config)
    {
        return toAjax(service.updateMdItemBatchConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:remove')")
    @Log(title = "批次配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds)
    {
        return toAjax(service.deleteMdItemBatchConfigByIds(configIds));
    }
}
