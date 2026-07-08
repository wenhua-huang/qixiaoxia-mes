package com.ruoyi.web.controller.mes.wm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.domain.mes.wm.WmStorageLocation;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.service.mes.wm.IWmIssueHeaderService;
import com.ruoyi.system.service.mes.wm.IWmIssueLineService;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;
import com.ruoyi.system.service.mes.wm.IWmStorageLocationService;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 生产领料单Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/wm/issueheader")
public class WmIssueHeaderController extends BaseController
{
    @Autowired
    private IWmIssueHeaderService wmIssueHeaderService;
    @Autowired
    private IWmIssueLineService wmIssueLineService;
    @Autowired
    private IWmWarehouseService wmWarehouseService;
    @Autowired
    private IWmStorageLocationService wmStorageLocationService;
    @Autowired
    private IProWorkorderService proWorkorderService;

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmIssueHeader e) { startPage(); return getDataTable(wmIssueHeaderService.selectWmIssueHeaderList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(wmIssueHeaderService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:export')")
    @Log(title = "生产领料单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmIssueHeader e) {
        List<WmIssueHeader> list = wmIssueHeaderService.selectWmIssueHeaderList(e);
        ExcelUtil<WmIssueHeader> util = new ExcelUtil<WmIssueHeader>(WmIssueHeader.class);
        util.exportExcel(response, list, "生产领料单数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping("/detail/{issueId}")
    public AjaxResult detail(@PathVariable Long issueId) {
        WmIssueHeader header = wmIssueHeaderService.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) return error("领料单不存在");
        // 富化工单名称
        if (header.getWorkorderId() != null) {
            ProWorkorder wo = proWorkorderService.selectProWorkorderByWorkorderId(header.getWorkorderId());
            if (wo != null) {
                if (header.getWorkorderCode() == null) header.setWorkorderCode(wo.getWorkorderCode());
                if (header.getWorkorderName() == null) header.setWorkorderName(wo.getWorkorderName());
            }
        }
        // 富化仓库名称
        if (header.getWarehouseId() != null) {
            WmWarehouse wh = wmWarehouseService.selectWmWarehouseByWarehouseId(header.getWarehouseId());
            if (wh != null) {
                if (header.getWarehouseCode() == null) header.setWarehouseCode(wh.getWarehouseCode());
                if (header.getWarehouseName() == null) header.setWarehouseName(wh.getWarehouseName());
            }
        }
        // 富化库区名称
        if (header.getLocationId() != null) {
            WmStorageLocation loc = wmStorageLocationService.selectWmStorageLocationByLocationId(header.getLocationId());
            if (loc != null) {
                if (header.getLocationCode() == null) header.setLocationCode(loc.getLocationCode());
                if (header.getLocationName() == null) header.setLocationName(loc.getLocationName());
            }
        }
        WmIssueLine lineQ = new WmIssueLine();
        lineQ.setIssueId(issueId);
        List<WmIssueLine> lines = wmIssueLineService.selectWmIssueLineList(lineQ);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("header", header);
        result.put("lines", lines);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:query')")
    @GetMapping(value = "/{issueId}")
    public AjaxResult getInfo(@PathVariable("issueId") Long issueId) { return success(wmIssueHeaderService.selectWmIssueHeaderByIssueId(issueId)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:add')")
    @Log(title = "生产领料单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmIssueHeader e) { wmIssueHeaderService.insertWmIssueHeader(e); return success(e); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "生产领料单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmIssueHeader e) { return toAjax(wmIssueHeaderService.updateWmIssueHeader(e)); }

    @PreAuthorize("@ss.hasPermi('mes:wm:issue:remove')")
    @Log(title = "生产领料单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{issueIds}")
    public AjaxResult remove(@PathVariable Long[] issueIds) { return toAjax(wmIssueHeaderService.deleteWmIssueHeaderByIssueIds(issueIds)); }

    /** 根据工单BOM自动生成领料行 */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "生产领料单", businessType = BusinessType.UPDATE)
    @PutMapping("/loadBom/{issueId}/{workorderId}")
    public AjaxResult loadBom(@PathVariable Long issueId, @PathVariable Long workorderId)
    { return toAjax(wmIssueHeaderService.loadBomLines(issueId, workorderId)); }

    /** 确认领料单：DRAFT → ALLOCATED（已预占），扣减可用库存 */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "领料单确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{issueId}")
    public AjaxResult confirm(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.confirmIssue(issueId)); }

    /** 释放预占：ALLOCATED → DRAFT，恢复可用库存（反确认） */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "领料单释放预占", businessType = BusinessType.UPDATE)
    @PutMapping("/release/{issueId}")
    public AjaxResult release(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.releaseAllocation(issueId)); }

    /** 执行出库：ALLOCATED → ISSUED，扣减现有库存 + 写追溯（旧接口，全量发料） */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:edit')")
    @Log(title = "生产领料单", businessType = BusinessType.UPDATE)
    @PutMapping("/execute/{issueId}")
    public AjaxResult execute(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.executeIssue(issueId)); }

    // ══════════════════════════════════════════════
    // Phase 2：完整生命周期接口
    // ══════════════════════════════════════════════

    /** 提交审核：DRAFT → PENDING */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:submit')")
    @Log(title = "领料单提交审核", businessType = BusinessType.UPDATE)
    @PutMapping("/submit/{issueId}")
    public AjaxResult submit(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.submitForApprove(issueId)); }

    /** 审核通过：PENDING → APPROVED */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:approve')")
    @Log(title = "领料单审核通过", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{issueId}")
    public AjaxResult approve(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.approve(issueId)); }

    /** 审核退回：PENDING → DRAFT */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:approve')")
    @Log(title = "领料单审核退回", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{issueId}")
    public AjaxResult reject(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.reject(issueId)); }

    /**
     * 发料出库（支持分批）：ALLOCATED/PARTIAL_ISSUED → PARTIAL_ISSUED/ISSUED。
     * body：本次发料明细数组（item/batch/数量/库位）
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:issueOut')")
    @Log(title = "领料单发料出库", businessType = BusinessType.UPDATE)
    @PutMapping("/issueOut/{issueId}")
    public AjaxResult issueOut(@PathVariable Long issueId, @RequestBody List<WmIssueDetail> details)
    { return toAjax(wmIssueHeaderService.issueOut(issueId, details)); }

    /** 关闭：ISSUED/PARTIAL_ISSUED → CLOSED（终态） */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:close')")
    @Log(title = "领料单关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{issueId}")
    public AjaxResult close(@PathVariable Long issueId)
    { return toAjax(wmIssueHeaderService.close(issueId)); }

    /** 作废：非终态 → CANCELED（body 可选含 reason） */
    @PreAuthorize("@ss.hasPermi('mes:wm:issue:cancel')")
    @Log(title = "领料单作废", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{issueId}")
    public AjaxResult cancel(@PathVariable Long issueId, @RequestBody(required = false) Map<String, String> body)
    {
        String reason = body != null ? body.get("reason") : null;
        return toAjax(wmIssueHeaderService.cancel(issueId, reason));
    }
}
