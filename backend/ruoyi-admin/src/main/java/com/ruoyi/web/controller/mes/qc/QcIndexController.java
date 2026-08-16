package com.ruoyi.web.controller.mes.qc;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.qc.IQcIndexService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.qc.QcIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 质检检测项Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@RestController
@RequestMapping("/mes/qc/index")
public class QcIndexController extends BaseController
{
    @Autowired
    private IQcIndexService qcIndexService;

    /**
     * 查询质检检测项列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:index:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcIndex qcIndex)
    {
        startPage();
        List<QcIndex> list = qcIndexService.selectQcIndexList(qcIndex);
        return getDataTable(list);
    }

    /**
     * 导出质检检测项列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:index:export')")
    @Log(title = "质检检测项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcIndex qcIndex)
    {
        List<QcIndex> list = qcIndexService.selectQcIndexList(qcIndex);
        ExcelUtil<QcIndex> util = new ExcelUtil<QcIndex>(QcIndex.class);
        util.exportExcel(response, list, "质检检测项数据");
    }

    /**
     * 获取质检检测项详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:index:query')")
    @GetMapping(value = "/{indexId}")
    public AjaxResult getInfo(@PathVariable("indexId") Long indexId)
    {
        return AjaxResult.success(qcIndexService.selectQcIndexByIndexId(indexId));
    }

    /**
     * 新增质检检测项
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:index:add')")
    @Log(title = "质检检测项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcIndex qcIndex)
    {
        return toAjax(qcIndexService.insertQcIndex(qcIndex));
    }

    /**
     * 修改质检检测项
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:index:edit')")
    @Log(title = "质检检测项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QcIndex qcIndex)
    {
        return toAjax(qcIndexService.updateQcIndex(qcIndex));
    }

    /**
     * 删除质检检测项
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:index:remove')")
    @Log(title = "质检检测项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{indexIds}")
    public AjaxResult remove(@PathVariable Long[] indexIds)
    {
        return toAjax(qcIndexService.deleteQcIndexByIndexIds(indexIds));
    }
}
