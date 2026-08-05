package com.ruoyi.system.web.controller.mes.wm;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
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

    /** Step1 创建外协发货单（我方操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:add')")
    @Log(title = "外协发货", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public AjaxResult create(@RequestBody OutsourceRequest req)
    {
        return AjaxResult.success(outsourceService.createOutsource(req));
    }

    /** Step2 厂商录加工结果（厂商操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:result')")
    @Log(title = "外协录结果", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/result")
    public AjaxResult recordResult(@PathVariable Long orderId,
                                   @RequestBody List<WmOutsourceRecptLine> resultLines)
    {
        return AjaxResult.success(outsourceService.recordResult(orderId, resultLines));
    }

    /** Step3 我方收货（我方操作） */
    @PreAuthorize("@ss.hasPermi('mes:wm:outsource:receive')")
    @Log(title = "外协收货", businessType = BusinessType.UPDATE)
    @PostMapping("/{orderId}/receive")
    public AjaxResult receive(@PathVariable Long orderId)
    {
        return AjaxResult.success(outsourceService.receiveOutsource(orderId));
    }
}
