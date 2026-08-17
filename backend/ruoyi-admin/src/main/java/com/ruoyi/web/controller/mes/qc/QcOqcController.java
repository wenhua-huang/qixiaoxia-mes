package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcOqcService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 出货检验单Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@RestController
@RequestMapping("/mes/qc/oqc")
public class QcOqcController extends BaseController
{
    @Autowired
    private IQcOqcService qcOqcService;

    /**
     * 查询出货检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcOqc qcoqc)
    {
        startPage();
        List<QcOqc> list = qcOqcService.selectQcOqcList(qcoqc);
        return getDataTable(list);
    }

    /**
     * 按来源单据查检验单（供下游单据页面查检验状态）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:list')")
    @GetMapping("/listBySource")
    public AjaxResult listBySource(String sourceDocType, Long sourceDocId)
    {
        return AjaxResult.success(qcOqcService.listBySource(sourceDocType, sourceDocId));
    }

    /**
     * 导出出货检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:export')")
    @Log(title = "出货检验单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcOqc qcoqc)
    {
        List<QcOqc> list = qcOqcService.selectQcOqcList(qcoqc);
        ExcelUtil<QcOqc> util = new ExcelUtil<QcOqc>(QcOqc.class);
        util.exportExcel(response, list, "出货检验单数据");
    }

    /**
     * 获取出货检验单详细信息（含检验行 lines + 缺陷记录 defectRecords）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:query')")
    @GetMapping(value = "/{oqcId}")
    public AjaxResult getInfo(@PathVariable("oqcId") Long oqcId)
    {
        return AjaxResult.success(qcOqcService.selectQcOqcByOqcId(oqcId));
    }

    /**
     * 新增出货检验单（编码缺省自动生成；body 可携带 lines 一并落库）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:add')")
    @Log(title = "出货检验单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcOqc qcoqc)
    {
        return toAjax(qcOqcService.insertQcOqc(qcoqc));
    }

    /**
     * 修改出货检验单（lines 非 null 时全删全插替换）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:edit')")
    @Log(title = "出货检验单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcOqc qcoqc)
    {
        return toAjax(qcOqcService.updateQcOqc(qcoqc));
    }

    /**
     * 关闭（作废）出货检验单（仅待检验/检验中可关；已判定单据不可关）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:edit')")
    @Log(title = "出货检验单", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{oqcId}")
    public AjaxResult close(@PathVariable("oqcId") Long oqcId)
    {
        qcOqcService.closeOqc(oqcId);
        return AjaxResult.success();
    }

    /**
     * 执行判定（行值录入完成后触发；FAIL 可带让步理由升级为 CONCESSION）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:judge')")
    @Log(title = "OQC判定", businessType = BusinessType.UPDATE)
    @PutMapping("/judge/{oqcId}")
    public AjaxResult judge(@PathVariable("oqcId") Long oqcId, @RequestBody Map<String, String> body)
    {
        qcOqcService.judgeOqc(oqcId, body == null ? null : body.get("concessionReason"));
        return AjaxResult.success();
    }

    /**
     * 删除出货检验单（级联删除检验行与缺陷记录）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:oqc:remove')")
    @Log(title = "出货检验单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{oqcIds}")
    public AjaxResult remove(@PathVariable Long[] oqcIds)
    {
        return toAjax(qcOqcService.deleteQcOqcByOqcIds(oqcIds));
    }
}
