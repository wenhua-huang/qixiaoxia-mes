package com.ruoyi.web.controller.mes.pro;

import java.util.List;
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
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderCreateRequest;
import com.ruoyi.system.domain.mes.pro.ProWorkorderDetailVO;
import com.ruoyi.system.domain.mes.pro.ProWorkorderDeviationVO;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.pro.IProWorkorderDocService;
import com.ruoyi.system.domain.mes.pro.ProDocGenerationRequestVO;
import com.ruoyi.system.domain.mes.pro.ProDocGenerationResultVO;
import com.ruoyi.system.domain.mes.pro.ProWorkorderKitDashboardVO;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 生产工单Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/workorder")
public class ProWorkorderController extends BaseController
{
    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private IProWorkorderDocService proWorkorderDocService;

    /**
     * 查询生产工单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProWorkorder proWorkorder)
    {
        startPage();
        List<ProWorkorder> list = proWorkorderService.selectProWorkorderList(proWorkorder);
        return getDataTable(list);
    }

    /**
     * 查询所有生产工单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:list')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<ProWorkorder> list = proWorkorderService.selectAll();
        return success(list);
    }

    /**
     * 导出生产工单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:export')")
    @Log(title = "生产工单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProWorkorder proWorkorder)
    {
        List<ProWorkorder> list = proWorkorderService.selectProWorkorderList(proWorkorder);
        ExcelUtil<ProWorkorder> util = new ExcelUtil<ProWorkorder>(ProWorkorder.class);
        util.exportExcel(response, list, "生产工单数据");
    }

    /**
     * 获取生产工单详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping(value = "/{workorderId}")
    public AjaxResult getInfo(@PathVariable("workorderId") Long workorderId)
    {
        return success(proWorkorderService.selectProWorkorderByWorkorderId(workorderId));
    }

    /**
     * 获取生产工单详情（含BOM和工序参数，合并返回）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping(value = "/detail/{workorderId}")
    public AjaxResult getDetail(@PathVariable("workorderId") Long workorderId)
    {
        ProWorkorderDetailVO vo = proWorkorderService.getWorkorderDetail(workorderId);
        return success(vo);
    }

    /**
     * 新增生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "生产工单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProWorkorder proWorkorder)
    {
        proWorkorderService.checkWorkorderCodeUnique(proWorkorder);
        proWorkorderService.insertProWorkorder(proWorkorder);
        return success(proWorkorder);
    }

    /**
     * 新增生产工单（含BOM和工序参数）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "生产工单(含BOM)", businessType = BusinessType.INSERT)
    @PostMapping("/createWithBom")
    public AjaxResult createWithBom(@RequestBody ProWorkorderCreateRequest request)
    {
        ProWorkorder workorder = proWorkorderService.createWorkorderWithBom(
            request.getWorkorder(), request.getBomList(), request.getParamList());
        return success(workorder);
    }

    /**
     * 偏离检测：比较提交的 BOM/参数 与 路线标准。
     * 纯计算，不持久化。前端据此弹窗询问用户是否创建变体。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @PostMapping("/checkDeviation")
    public AjaxResult checkDeviation(@RequestBody ProWorkorderCreateRequest request)
    {
        ProWorkorderDeviationVO result = proWorkorderService.checkDeviation(
            request.getWorkorder(), request.getBomList(), request.getParamList());
        return success(result);
    }

    /**
     * 修改生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProWorkorder proWorkorder)
    {
        return toAjax(proWorkorderService.updateProWorkorder(proWorkorder));
    }

    /**
     * 修改生产工单（含BOM和工序参数）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单(含BOM)", businessType = BusinessType.UPDATE)
    @PutMapping("/updateWithBom")
    public AjaxResult updateWithBom(@RequestBody ProWorkorderCreateRequest request)
    {
        ProWorkorder workorder = proWorkorderService.updateWorkorderWithBom(
            request.getWorkorder(), request.getBomList(), request.getParamList());
        return success(workorder);
    }

    /**
     * 删除生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @Log(title = "生产工单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{workorderIds}")
    public AjaxResult remove(@PathVariable Long[] workorderIds)
    {
        return toAjax(proWorkorderService.deleteProWorkorderByWorkorderIds(workorderIds));
    }

    /**
     * 开工：PREPARE → PRODUCING
     * 执行物料齐套检查，全部满足才允许开工
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单开工", businessType = BusinessType.UPDATE)
    @PutMapping("/start/{workorderId}")
    public AjaxResult start(@PathVariable Long workorderId)
    {
        // 1. 物料齐套检查
        List<java.util.Map<String, Object>> checkResult = proWorkorderService.checkMaterialReadiness(workorderId);
        for (java.util.Map<String, Object> row : checkResult) {
            if (Boolean.FALSE.equals(row.get("sufficient"))) {
                return error("物料齐套检查未通过：" + row.get("itemName") + " 缺少 " + row.get("shortageQty") + " " + row.get("unitName"));
            }
        }
        // 2. 全部满足，执行开工
        return toAjax(proWorkorderService.startProduction(workorderId));
    }

    /**
     * 开工前检查 + 自动生成领料单 + 开工
     * 四步：物料齐套检查 → 排产检查 → 按工序生成领料单 → 开工
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单开工检查", businessType = BusinessType.UPDATE)
    @PutMapping("/startWithCheck/{workorderId}")
    public AjaxResult startWithCheck(@PathVariable Long workorderId)
    {
        List<java.util.Map<String, Object>> steps = proWorkorderService.preStartCheck(workorderId);
        return success(steps);
    }

    /**
     * 物料齐套检查（实时计算，不持久化）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/checkMaterial/{workorderId}")
    public AjaxResult checkMaterial(@PathVariable Long workorderId)
    {
        return success(proWorkorderService.checkMaterialReadiness(workorderId));
    }

    /**
     * 取消工单：PREPARE/PRODUCING → CANCEL
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{workorderId}")
    public AjaxResult cancel(@PathVariable Long workorderId)
    {
        return toAjax(proWorkorderService.cancelWorkorder(workorderId));
    }

    /**
     * 加载工单齐套看板 — 汇总物料分析及领料/采购/退料/入库四类单据状态
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/kitDashboard/{workorderId}")
    public AjaxResult kitDashboard(@PathVariable Long workorderId)
    {
        ProWorkorderKitDashboardVO vo = proWorkorderDocService.loadKitDashboard(workorderId);
        return success(vo);
    }

    /**
     * 一键批量生成单据（齐套看板内操作）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "工单齐套文档生成", businessType = BusinessType.INSERT)
    @PostMapping("/generateDocs")
    public AjaxResult generateDocs(@RequestBody ProDocGenerationRequestVO request)
    {
        ProDocGenerationResultVO result = proWorkorderDocService.generateDocuments(request);
        return success(result);
    }

    /**
     * 单独生成产品入库单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生成产品入库单", businessType = BusinessType.INSERT)
    @PostMapping("/generateReceipt/{workorderId}")
    public AjaxResult generateReceipt(@PathVariable Long workorderId)
    {
        return success(proWorkorderDocService.generateProductReceipt(workorderId));
    }

    /**
     * 单独生成退料单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生成退料单", businessType = BusinessType.INSERT)
    @PostMapping("/generateReturn/{workorderId}")
    public AjaxResult generateReturn(@PathVariable Long workorderId)
    {
        return success(proWorkorderDocService.generateMaterialReturn(workorderId));
    }

    /**
     * 单独生成采购单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生成采购单", businessType = BusinessType.INSERT)
    @PostMapping("/generatePurOrder/{workorderId}")
    public AjaxResult generatePurOrder(@PathVariable Long workorderId)
    {
        java.util.Map<String, Object> result = proWorkorderDocService.generatePurchaseOrder(workorderId);
        return success(result);
    }

    /**
     * 采购单快捷创建向导 — 返回推荐数据（供应商、数量），不写库
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/purOrderWizard/{workorderId}")
    public AjaxResult purOrderWizard(@PathVariable Long workorderId)
    {
        return success(proWorkorderDocService.buildPurOrderWizard(workorderId));
    }

    /**
     * 采购向导弹窗确认提交 — 接收用户勾选/修改后的物料行，按供应商分组生成采购单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "采购向导生成采购单", businessType = BusinessType.INSERT)
    @PostMapping("/purOrderWizard/{workorderId}/submit")
    public AjaxResult submitPurOrderWizard(@PathVariable Long workorderId,
            @RequestBody java.util.List<com.ruoyi.system.domain.mes.pro.PurOrderWizardLineVO> lines)
    {
        return success(proWorkorderDocService.submitPurOrderWizard(workorderId, lines));
    }
}
