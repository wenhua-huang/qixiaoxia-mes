package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcDefectService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcDefect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 质检缺陷字典Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@RestController
@RequestMapping("/mes/qc/defect")
public class QcDefectController extends BaseController
{
    @Autowired
    private IQcDefectService qcDefectService;

    /**
     * 查询质检缺陷字典列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:defect:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcDefect qcDefect)
    {
        startPage();
        List<QcDefect> list = qcDefectService.selectQcDefectList(qcDefect);
        return getDataTable(list);
    }

    /**
     * 导出质检缺陷字典列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:defect:export')")
    @Log(title = "质检缺陷字典", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcDefect qcDefect)
    {
        List<QcDefect> list = qcDefectService.selectQcDefectList(qcDefect);
        ExcelUtil<QcDefect> util = new ExcelUtil<QcDefect>(QcDefect.class);
        util.exportExcel(response, list, "质检缺陷字典数据");
    }

    /**
     * 获取质检缺陷字典详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:defect:query')")
    @GetMapping(value = "/{defectId}")
    public AjaxResult getInfo(@PathVariable("defectId") Long defectId)
    {
        return AjaxResult.success(qcDefectService.selectQcDefectByDefectId(defectId));
    }

    /**
     * 新增质检缺陷字典
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:defect:add')")
    @Log(title = "质检缺陷字典", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcDefect qcDefect)
    {
        return toAjax(qcDefectService.insertQcDefect(qcDefect));
    }

    /**
     * 修改质检缺陷字典
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:defect:edit')")
    @Log(title = "质检缺陷字典", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcDefect qcDefect)
    {
        return toAjax(qcDefectService.updateQcDefect(qcDefect));
    }

    /**
     * 删除质检缺陷字典
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:defect:remove')")
    @Log(title = "质检缺陷字典", businessType = BusinessType.DELETE)
    @DeleteMapping("/{defectIds}")
    public AjaxResult remove(@PathVariable Long[] defectIds)
    {
        return toAjax(qcDefectService.deleteQcDefectByDefectIds(defectIds));
    }
}
