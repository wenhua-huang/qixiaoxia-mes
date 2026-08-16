package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcIqcService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 来料检验单Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@RestController
@RequestMapping("/mes/qc/iqc")
public class QcIqcController extends BaseController
{
    @Autowired
    private IQcIqcService qcIqcService;

    /**
     * 查询来料检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcIqc qciqc)
    {
        startPage();
        List<QcIqc> list = qcIqcService.selectQcIqcList(qciqc);
        return getDataTable(list);
    }

    /**
     * 按来源单据查检验单（供下游单据页面查检验状态）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:list')")
    @GetMapping("/listBySource")
    public AjaxResult listBySource(String sourceDocType, Long sourceDocId)
    {
        return AjaxResult.success(qcIqcService.listBySource(sourceDocType, sourceDocId));
    }

    /**
     * 导出来料检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:export')")
    @Log(title = "来料检验单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcIqc qciqc)
    {
        List<QcIqc> list = qcIqcService.selectQcIqcList(qciqc);
        ExcelUtil<QcIqc> util = new ExcelUtil<QcIqc>(QcIqc.class);
        util.exportExcel(response, list, "来料检验单数据");
    }

    /**
     * 获取来料检验单详细信息（含检验行 lines + 缺陷记录 defectRecords）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:query')")
    @GetMapping(value = "/{iqcId}")
    public AjaxResult getInfo(@PathVariable("iqcId") Long iqcId)
    {
        return AjaxResult.success(qcIqcService.selectQcIqcByIqcId(iqcId));
    }

    /**
     * 新增来料检验单（编码缺省自动生成；body 可携带 lines 一并落库）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:add')")
    @Log(title = "来料检验单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcIqc qciqc)
    {
        return toAjax(qcIqcService.insertQcIqc(qciqc));
    }

    /**
     * 修改来料检验单（lines 非 null 时全删全插替换）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:edit')")
    @Log(title = "来料检验单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcIqc qciqc)
    {
        return toAjax(qcIqcService.updateQcIqc(qciqc));
    }

    /**
     * 关闭（作废）来料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:edit')")
    @Log(title = "来料检验单", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{iqcId}")
    public AjaxResult close(@PathVariable("iqcId") Long iqcId)
    {
        qcIqcService.closeIqc(iqcId);
        return AjaxResult.success();
    }

    /**
     * 删除来料检验单（级联删除检验行与缺陷记录）
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:remove')")
    @Log(title = "来料检验单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{iqcIds}")
    public AjaxResult remove(@PathVariable Long[] iqcIds)
    {
        return toAjax(qcIqcService.deleteQcIqcByIqcIds(iqcIds));
    }
}
