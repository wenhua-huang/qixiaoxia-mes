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
import com.ruoyi.system.domain.mes.pro.OutsourceReceiveRequest;
import com.ruoyi.system.domain.mes.pro.OutsourceResultRequest;
import com.ruoyi.system.domain.mes.pro.ProSlittingRecord;
import com.ruoyi.system.domain.mes.pro.SlittingRequest;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;
import com.ruoyi.system.service.mes.pro.IProSlittingService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 分切作业Controller（厂内 INTERNAL + 外协 OUTSOURCE）
 *
 * <p>权限：
 * <ul>
 *   <li>list/query：我方员工 + 外协厂商（厂商仅看自己的单）</li>
 *   <li>execute/add：仅我方员工（建单+发料 / 厂内执行）</li>
 *   <li>result：外协厂商录结果</li>
 *   <li>receive：我方员工确认收货</li>
 * </ul>
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
     * 分切记录列表（厂商账号自动按 vendor_id 过滤）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProSlittingRecord query) {
        startPage();
        List<ProSlittingRecord> list = proSlittingService.selectList(query);
        return getDataTable(list);
    }

    /**
     * 分切记录详情（含母卷 + 子卷列表）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:query')")
    @GetMapping("/{slitId}")
    public AjaxResult getInfo(@PathVariable Long slitId) {
        return success(proSlittingService.selectBySlitId(slitId));
    }

    /**
     * 执行分切（按 slitMode 分流）：
     * <ul>
     *   <li>INTERNAL（默认）：厂内一步完成领料+建卷+入库+报工</li>
     *   <li>OUTSOURCE：建外协单并发料（母卷 OUTSOURCED，状态 ISSUED）</li>
     * </ul>
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:add')")
    @Log(title = "分切作业", businessType = BusinessType.INSERT)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody SlittingRequest request) {
        return success(proSlittingService.executeSlitting(request));
    }

    /**
     * 外协厂商录分切结果（子卷规格 + 重量）。
     * 仅单据所属厂商可操作；提交后子卷 OUTSOURCED，单据 → SLITTING。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:result')")
    @Log(title = "外协分切录结果", businessType = BusinessType.UPDATE)
    @PostMapping("/{slitId}/result")
    public AjaxResult recordResult(@PathVariable Long slitId, @RequestBody OutsourceResultRequest request) {
        return success(proSlittingService.recordOutsourceResult(slitId, request));
    }

    /**
     * 我方确认外协收货：子卷入库、母卷消耗、报工、追溯，单据 → RECEIVED。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:add')")
    @Log(title = "外协分切收货", businessType = BusinessType.UPDATE)
    @PostMapping("/{slitId}/receive")
    public AjaxResult receive(@PathVariable Long slitId,
                              @RequestBody(required = false) OutsourceReceiveRequest request) {
        return success(proSlittingService.receiveOutsource(slitId, request));
    }

    /**
     * 查询物料在库库存（厂内选领料物料时展示可用批次）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:query')")
    @GetMapping("/availableStock")
    public AjaxResult availableStock(@RequestParam Long itemId) {
        List<WmMaterialStock> list = proSlittingService.listAvailableStock(itemId);
        return AjaxResult.success(list);
    }

    /**
     * 查询在库母卷（外协发料时按卷号选择；status=IN_STOCK）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:slitting:query')")
    @GetMapping("/availableParentRolls")
    public AjaxResult availableParentRolls(@RequestParam(required = false) Long itemId) {
        List<WmRollDetail> list = proSlittingService.listAvailableParentRolls(itemId);
        return AjaxResult.success(list);
    }
}
