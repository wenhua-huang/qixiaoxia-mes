package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcIpqcService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 过程检验单Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 单据来源：报工确认/成品入库自动生成 + 本控制器手工创建（首检/巡检/抽检）。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
@RestController
@RequestMapping("/mes/qc/ipqc")
public class QcIpqcController extends BaseController
{
    @Autowired
    private IQcIpqcService qcIpqcService;

    /**
     * 查询过程检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcIpqc qcipqc)
    {
        startPage();
        List<QcIpqc> list = qcIpqcService.selectQcIpqcList(qcipqc);
        return getDataTable(list);
    }

    /**
     * 按来源单据查检验单（供流转卡工序/入库单页面查检验状态）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:list')")
    @GetMapping("/listBySource")
    public AjaxResult listBySource(String sourceDocType, Long sourceDocId)
    {
        return AjaxResult.success(qcIpqcService.listBySource(sourceDocType, sourceDocId));
    }

    /**
     * 导出过程检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:export')")
    @Log(title = "过程检验单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcIpqc qcipqc)
    {
        List<QcIpqc> list = qcIpqcService.selectQcIpqcList(qcipqc);
        ExcelUtil<QcIpqc> util = new ExcelUtil<QcIpqc>(QcIpqc.class);
        util.exportExcel(response, list, "过程检验单数据");
    }

    /**
     * 获取过程检验单详细信息（含检验行 lines + 缺陷记录 defectRecords）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:query')")
    @GetMapping(value = "/{ipqcId}")
    public AjaxResult getInfo(@PathVariable("ipqcId") Long ipqcId)
    {
        return AjaxResult.success(qcIpqcService.selectQcIpqcByIpqcId(ipqcId));
    }

    /**
     * 新增过程检验单（手工创建首检/巡检/抽检；编码缺省自动生成；body 可携带 lines 一并落库）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:add')")
    @Log(title = "过程检验单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcIpqc qcipqc)
    {
        return toAjax(qcIpqcService.insertQcIpqc(qcipqc));
    }

    /**
     * 修改过程检验单（lines 非 null 时全删全插替换）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:edit')")
    @Log(title = "过程检验单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcIpqc qcipqc)
    {
        return toAjax(qcIpqcService.updateQcIpqc(qcipqc));
    }

    /**
     * 关闭（作废）过程检验单（仅待检验/检验中可关；已判定单据不可关）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:edit')")
    @Log(title = "过程检验单", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{ipqcId}")
    public AjaxResult close(@PathVariable("ipqcId") Long ipqcId)
    {
        qcIpqcService.closeIpqc(ipqcId);
        return AjaxResult.success();
    }

    /**
     * 执行判定（行值录入完成后触发；FAIL 可带让步理由升级为 CONCESSION）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:judge')")
    @Log(title = "IPQC判定", businessType = BusinessType.UPDATE)
    @PutMapping("/judge/{ipqcId}")
    public AjaxResult judge(@PathVariable("ipqcId") Long ipqcId, @RequestBody Map<String, String> body)
    {
        qcIpqcService.judgeIpqc(ipqcId, body == null ? null : body.get("concessionReason"));
        return AjaxResult.success();
    }

    /**
     * 删除过程检验单（级联删除检验行与缺陷记录）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:remove')")
    @Log(title = "过程检验单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ipqcIds}")
    public AjaxResult remove(@PathVariable Long[] ipqcIds)
    {
        return toAjax(qcIpqcService.deleteQcIpqcByIpqcIds(ipqcIds));
    }
}
