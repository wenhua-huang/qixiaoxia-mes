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
import com.ruoyi.system.domain.mes.pro.ProChangeover;
import com.ruoyi.system.service.mes.pro.IProChangeoverService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工序换型时间Controller
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
@RestController
@RequestMapping("/mes/pro/changeover")
public class ProChangeoverController extends BaseController
{
    @Autowired
    private IProChangeoverService proChangeoverService;

    /**
     * 查询换型时间列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:changeover:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProChangeover proChangeover)
    {
        startPage();
        List<ProChangeover> list = proChangeoverService.selectProChangeoverList(proChangeover);
        return getDataTable(list);
    }

    /**
     * 导出换型时间列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:changeover:export')")
    @Log(title = "换型时间", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProChangeover proChangeover)
    {
        List<ProChangeover> list = proChangeoverService.selectProChangeoverList(proChangeover);
        ExcelUtil<ProChangeover> util = new ExcelUtil<ProChangeover>(ProChangeover.class);
        util.exportExcel(response, list, "换型时间数据");
    }

    /**
     * 获取换型时间详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:changeover:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(proChangeoverService.selectProChangeoverById(id));
    }

    /**
     * 新增换型时间
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:changeover:add')")
    @Log(title = "换型时间", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProChangeover proChangeover)
    {
        return toAjax(proChangeoverService.insertProChangeover(proChangeover));
    }

    /**
     * 修改换型时间
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:changeover:edit')")
    @Log(title = "换型时间", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProChangeover proChangeover)
    {
        return toAjax(proChangeoverService.updateProChangeover(proChangeover));
    }

    /**
     * 删除换型时间
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:changeover:remove')")
    @Log(title = "换型时间", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(proChangeoverService.deleteProChangeoverByIds(ids));
    }
}
