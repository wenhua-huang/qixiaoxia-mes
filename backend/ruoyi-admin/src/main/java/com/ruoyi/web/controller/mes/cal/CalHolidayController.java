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
import com.ruoyi.system.domain.mes.cal.CalHoliday;
import com.ruoyi.system.service.mes.cal.ICalHolidayService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 节假日设置Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/holiday")
public class CalHolidayController extends BaseController
{
    @Autowired
    private ICalHolidayService calHolidayService;

    /**
     * 查询节假日设置列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:holiday:list')")
    @GetMapping("/list")
    public TableDataInfo list(CalHoliday calHoliday)
    {
        startPage();
        List<CalHoliday> list = calHolidayService.selectCalHolidayList(calHoliday);
        return getDataTable(list);
    }

    /**
     * 导出节假日设置列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:holiday:export')")
    @Log(title = "节假日设置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CalHoliday calHoliday)
    {
        List<CalHoliday> list = calHolidayService.selectCalHolidayList(calHoliday);
        ExcelUtil<CalHoliday> util = new ExcelUtil<CalHoliday>(CalHoliday.class);
        util.exportExcel(response, list, "节假日设置数据");
    }

    /**
     * 获取节假日设置详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:holiday:query')")
    @GetMapping(value = "/{holidayId}")
    public AjaxResult getInfo(@PathVariable("holidayId") Long holidayId)
    {
        return success(calHolidayService.selectCalHolidayByHolidayId(holidayId));
    }

    /**
     * 新增节假日设置
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:holiday:add')")
    @Log(title = "节假日设置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalHoliday calHoliday)
    {
        calHolidayService.insertCalHoliday(calHoliday);
        return success(calHoliday);
    }

    /**
     * 修改节假日设置
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:holiday:edit')")
    @Log(title = "节假日设置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalHoliday calHoliday)
    {
        return toAjax(calHolidayService.updateCalHoliday(calHoliday));
    }

    /**
     * 删除节假日设置
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:holiday:remove')")
    @Log(title = "节假日设置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{holidayIds}")
    public AjaxResult remove(@PathVariable Long[] holidayIds)
    {
        return toAjax(calHolidayService.deleteCalHolidayByHolidayIds(holidayIds));
    }
}
