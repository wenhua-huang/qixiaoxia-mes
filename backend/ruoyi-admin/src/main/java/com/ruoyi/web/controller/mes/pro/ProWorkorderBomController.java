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
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.service.mes.pro.IProWorkorderBomService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工单BOM组成Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/workorderbom")
public class ProWorkorderBomController extends BaseController
{
    @Autowired
    private IProWorkorderBomService proWorkorderBomService;

    /**
     * 查询工单BOM组成列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProWorkorderBom proWorkorderBom)
    {
        startPage();
        List<ProWorkorderBom> list = proWorkorderBomService.selectProWorkorderBomList(proWorkorderBom);
        return getDataTable(list);
    }

    /**
     * 根据工单ID查询BOM组成列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/listByWorkorderId/{workorderId}")
    public AjaxResult listByWorkorderId(@PathVariable("workorderId") Long workorderId)
    {
        List<ProWorkorderBom> list = proWorkorderBomService.selectProWorkorderBomByWorkorderId(workorderId);
        return success(list);
    }

    /**
     * 导出工单BOM组成列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:export')")
    @Log(title = "工单BOM组成", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProWorkorderBom proWorkorderBom)
    {
        List<ProWorkorderBom> list = proWorkorderBomService.selectProWorkorderBomList(proWorkorderBom);
        ExcelUtil<ProWorkorderBom> util = new ExcelUtil<ProWorkorderBom>(ProWorkorderBom.class);
        util.exportExcel(response, list, "工单BOM组成数据");
    }

    /**
     * 获取工单BOM组成详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return success(proWorkorderBomService.selectProWorkorderBomByLineId(lineId));
    }

    /**
     * 新增工单BOM组成
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "工单BOM组成", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProWorkorderBom proWorkorderBom)
    {
        proWorkorderBomService.insertProWorkorderBom(proWorkorderBom);
        return success(proWorkorderBom);
    }

    /**
     * 修改工单BOM组成
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "工单BOM组成", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProWorkorderBom proWorkorderBom)
    {
        return toAjax(proWorkorderBomService.updateProWorkorderBom(proWorkorderBom));
    }

    /**
     * 删除工单BOM组成
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @Log(title = "工单BOM组成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        return toAjax(proWorkorderBomService.deleteProWorkorderBomByLineIds(lineIds));
    }
}
