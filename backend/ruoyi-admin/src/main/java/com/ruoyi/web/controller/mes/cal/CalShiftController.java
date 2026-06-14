package com.ruoyi.web.controller.mes.cal;

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
import com.ruoyi.system.domain.mes.cal.CalShift;
import com.ruoyi.system.service.mes.cal.ICalShiftService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 计划班次Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/shift")
public class CalShiftController extends BaseController
{
    @Autowired
    private ICalShiftService calShiftService;

    /**
     * 查询计划班次列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:shift:list')")
    @GetMapping("/list")
    public TableDataInfo list(CalShift calShift)
    {
        startPage();
        List<CalShift> list = calShiftService.selectCalShiftList(calShift);
        return getDataTable(list);
    }

    /**
     * 导出计划班次列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:shift:export')")
    @Log(title = "计划班次", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CalShift calShift)
    {
        List<CalShift> list = calShiftService.selectCalShiftList(calShift);
        ExcelUtil<CalShift> util = new ExcelUtil<CalShift>(CalShift.class);
        util.exportExcel(response, list, "计划班次数据");
    }

    /**
     * 获取计划班次详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:shift:query')")
    @GetMapping(value = "/{shiftId}")
    public AjaxResult getInfo(@PathVariable("shiftId") Long shiftId)
    {
        return success(calShiftService.selectCalShiftByShiftId(shiftId));
    }

    /**
     * 新增计划班次
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:shift:add')")
    @Log(title = "计划班次", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalShift calShift)
    {
        calShiftService.insertCalShift(calShift);
        return success(calShift);
    }

    /**
     * 修改计划班次
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:shift:edit')")
    @Log(title = "计划班次", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalShift calShift)
    {
        return toAjax(calShiftService.updateCalShift(calShift));
    }

    /**
     * 删除计划班次
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:shift:remove')")
    @Log(title = "计划班次", businessType = BusinessType.DELETE)
	@DeleteMapping("/{shiftIds}")
    public AjaxResult remove(@PathVariable Long[] shiftIds)
    {
        return toAjax(calShiftService.deleteCalShiftByShiftIds(shiftIds));
    }
}
