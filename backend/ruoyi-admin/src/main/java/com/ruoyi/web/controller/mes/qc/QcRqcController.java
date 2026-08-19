package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcRqcService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcRqc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 退料检验单Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 单据来源：生产退料单执行退库前自动生成；判定合格后由退料单核验放行。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
@RestController
@RequestMapping("/mes/qc/rqc")
public class QcRqcController extends BaseController
{
    @Autowired
    private IQcRqcService qcRqcService;

    /**
     * 查询退料检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcRqc qcrqc)
    {
        startPage();
        List<QcRqc> list = qcRqcService.selectQcRqcList(qcrqc);
        return getDataTable(list);
    }

    /**
     * 按来源单据查检验单（供退料单页面查检验状态）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:list')")
    @GetMapping("/listBySource")
    public AjaxResult listBySource(String sourceDocType, Long sourceDocId)
    {
        return AjaxResult.success(qcRqcService.listBySource(sourceDocType, sourceDocId));
    }

    /**
     * 导出退料检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:export')")
    @Log(title = "退料检验单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcRqc qcrqc)
    {
        List<QcRqc> list = qcRqcService.selectQcRqcList(qcrqc);
        ExcelUtil<QcRqc> util = new ExcelUtil<QcRqc>(QcRqc.class);
        util.exportExcel(response, list, "退料检验单数据");
    }

    /**
     * 获取退料检验单详细信息（含检验行 lines + 缺陷记录 defectRecords）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:query')")
    @GetMapping(value = "/{rqcId}")
    public AjaxResult getInfo(@PathVariable("rqcId") Long rqcId)
    {
        return AjaxResult.success(qcRqcService.selectQcRqcByRqcId(rqcId));
    }

    /**
     * 新增退料检验单（编码缺省自动生成；body 可携带 lines 一并落库）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:add')")
    @Log(title = "退料检验单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcRqc qcrqc)
    {
        return toAjax(qcRqcService.insertQcRqc(qcrqc));
    }

    /**
     * 修改退料检验单（lines 非 null 时全删全插替换）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:edit')")
    @Log(title = "退料检验单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcRqc qcrqc)
    {
        return toAjax(qcRqcService.updateQcRqc(qcrqc));
    }

    /**
     * 关闭（作废）退料检验单（仅待检验/检验中可关；已判定单据不可关）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:edit')")
    @Log(title = "退料检验单", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{rqcId}")
    public AjaxResult close(@PathVariable("rqcId") Long rqcId)
    {
        qcRqcService.closeRqc(rqcId);
        return AjaxResult.success();
    }

    /**
     * 执行判定（行值录入完成后触发；FAIL 可带让步理由升级为 CONCESSION）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:judge')")
    @Log(title = "RQC判定", businessType = BusinessType.UPDATE)
    @PutMapping("/judge/{rqcId}")
    public AjaxResult judge(@PathVariable("rqcId") Long rqcId, @RequestBody Map<String, String> body)
    {
        qcRqcService.judgeRqc(rqcId, body == null ? null : body.get("concessionReason"));
        return AjaxResult.success();
    }

    /**
     * 删除退料检验单（级联删除检验行与缺陷记录）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:remove')")
    @Log(title = "退料检验单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{rqcIds}")
    public AjaxResult remove(@PathVariable Long[] rqcIds)
    {
        return toAjax(qcRqcService.deleteQcRqcByRqcIds(rqcIds));
    }
}
