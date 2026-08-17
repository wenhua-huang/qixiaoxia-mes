package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
import com.ruoyi.system.domain.mes.wm.WmOutsourceIssueLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.service.mes.wm.IOutsourceService;

/**
 * 通用外协管理 Controller
 *
 * 三步流程统一入口：发货→厂商录结果→收货。
 * 分切/印刷等业务通过 sourceType 区分，领域逻辑由 Strategy 处理。
 *
 * @author qixiaoxia
 */
@RestController
@RequestMapping("/mes/wm/outsource")
public class WmOutsourceController extends BaseController
{
    @Autowired
    private IOutsourceService outsourceService;

    /** 外协单列表（厂商账号自动按 vendorId 过滤） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmOutsourceOrder query)
    {
        startPage();
        List<WmOutsourceOrder> list = outsourceService.selectList(query);
        return getDataTable(list);
    }

    /** 外协单详情（含发料行+收货行） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:query')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId)
    {
        return success(outsourceService.selectByOrderId(orderId));
    }

    /** Step1 创建外协发货单（我方操作，草稿模式不扣料） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:add')")
    @Log(title = "外协发货", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public AjaxResult create(@RequestBody OutsourceRequest req)
    {
        return AjaxResult.success(outsourceService.createOutsource(req));
    }

    /** 修改草稿单发料行（仓库/批次/数量，仅 DRAFT 状态） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:add')")
    @Log(title = "外协发料行", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/issueLines")
    public AjaxResult updateIssueLines(@PathVariable Long orderId,
                                       @RequestBody List<WmOutsourceIssueLine> lines)
    {
        outsourceService.updateIssueLines(orderId, lines);
        return AjaxResult.success();
    }

    /** 删除草稿外协单（仅 DRAFT 状态，未扣库存，级联删发料行） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:remove')")
    @Log(title = "外协发货", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderId}")
    public AjaxResult remove(@PathVariable Long orderId)
    {
        outsourceService.deleteOutsource(orderId);
        return AjaxResult.success();
    }

    /** 执行发料（草稿 DRAFT → 已发料 ISSUED，扣库存） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:execute')")
    @Log(title = "外协执行发料", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/execute")
    public AjaxResult execute(@PathVariable Long orderId)
    {
        return AjaxResult.success(outsourceService.executeOutsource(orderId));
    }

    /** 批量执行发料（逐张草稿单扣库存发料，单条失败不影响其他单） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:execute')")
    @Log(title = "外协批量执行发料", businessType = BusinessType.UPDATE)
    @PostMapping("/batchExecute")
    public AjaxResult batchExecute(@RequestBody List<Long> orderIds)
    {
        return AjaxResult.success(outsourceService.batchExecuteOutsource(orderIds));
    }

    /** Step1.5 厂商签收（ISSUED → VENDOR_RCVD，厂商操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:vendorReceive')")
    @Log(title = "外协厂商签收", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/vendorReceive")
    public AjaxResult vendorReceive(@PathVariable Long orderId)
    {
        return AjaxResult.success(outsourceService.vendorReceive(orderId));
    }

    /** Step2 厂商录加工结果（厂商操作，支持分批补录；录满自动完成） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:result')")
    @Log(title = "外协录结果", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/result")
    public AjaxResult recordResult(@PathVariable Long orderId,
                                   @RequestBody List<WmOutsourceRecptLine> resultLines)
    {
        return AjaxResult.success(outsourceService.recordResult(orderId, resultLines));
    }

    /** Step2.5 厂商手动完成（PROCESSING → FINISHED，允许短交，厂商操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:complete')")
    @Log(title = "外协加工完成", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/complete")
    public AjaxResult complete(@PathVariable Long orderId)
    {
        return AjaxResult.success(outsourceService.complete(orderId));
    }

    /** Step2.6 厂商发货（FINISHED → SHIPPED，厂商操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:ship')")
    @Log(title = "外协厂商发货", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/ship")
    public AjaxResult ship(@PathVariable Long orderId)
    {
        return AjaxResult.success(outsourceService.ship(orderId));
    }

    /** Step3 我方收货（SHIPPED → RECEIVED，我方操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:receive')")
    @Log(title = "外协收货", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/receive")
    public AjaxResult receive(@PathVariable Long orderId)
    {
        return AjaxResult.success(outsourceService.receiveOutsource(orderId));
    }

    /** 批量收货（逐单入库收货，单条失败不影响其他单） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:receive')")
    @Log(title = "外协批量收货", businessType = BusinessType.UPDATE)
    @PostMapping("/batchReceive")
    public AjaxResult batchReceive(@RequestBody List<Long> orderIds)
    {
        return AjaxResult.success(outsourceService.batchReceiveOutsource(orderIds));
    }
}
