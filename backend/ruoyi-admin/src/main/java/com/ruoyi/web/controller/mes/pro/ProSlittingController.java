package com.ruoyi.web.controller.mes.pro;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.pro.ProSlittingRecord;
import com.ruoyi.system.domain.mes.pro.SlittingRequest;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.service.mes.pro.IProSlittingService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 分切作业Controller
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
@RestController
@RequestMapping("/mes/pro/slitting")
public class ProSlittingController extends BaseController
{
    @Autowired
    private IProSlittingService proSlittingService;

    /**
     * 分切记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProSlittingRecord query) {
        startPage();
        List<ProSlittingRecord> list = proSlittingService.selectList(query);
        return getDataTable(list);
    }

    /**
     * 分切记录详情（含子卷列表）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:query')")
    @GetMapping("/{slitId}")
    public AjaxResult getInfo(@PathVariable Long slitId) {
        return success(proSlittingService.selectBySlitId(slitId));
    }

    /**
     * 执行分切作业（核心接口：领料出库 + 自动建母卷/子卷 + 库存 + 追溯 + 报工）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:add')")
    @Log(title = "分切作业", businessType = BusinessType.INSERT)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody SlittingRequest request) {
        return success(proSlittingService.executeSlitting(request));
    }

    /**
     * 查询物料在库库存（供前端选领料物料时展示可用批次）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:query')")
    @GetMapping("/availableStock")
    public AjaxResult availableStock(@RequestParam Long itemId) {
        List<WmMaterialStock> list = proSlittingService.listAvailableStock(itemId);
        return AjaxResult.success(list);
    }
}
