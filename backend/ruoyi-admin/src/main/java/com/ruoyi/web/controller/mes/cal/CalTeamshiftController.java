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
import com.ruoyi.system.domain.mes.cal.CalTeamshift;
import com.ruoyi.system.service.mes.cal.ICalTeamshiftService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 班组排班明细Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/teamshift")
public class CalTeamshiftController extends BaseController
{
    @Autowired
    private ICalTeamshiftService calTeamshiftService;

    /**
     * 查询班组排班明细列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:teamshift:list')")
    @GetMapping("/list")
    public TableDataInfo list(CalTeamshift calTeamshift)
    {
        startPage();
        List<CalTeamshift> list = calTeamshiftService.selectCalTeamshiftList(calTeamshift);
        return getDataTable(list);
    }

    /**
     * 导出班组排班明细列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:teamshift:export')")
    @Log(title = "班组排班明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CalTeamshift calTeamshift)
    {
        List<CalTeamshift> list = calTeamshiftService.selectCalTeamshiftList(calTeamshift);
        ExcelUtil<CalTeamshift> util = new ExcelUtil<CalTeamshift>(CalTeamshift.class);
        util.exportExcel(response, list, "班组排班明细数据");
    }

    /**
     * 获取班组排班明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:teamshift:query')")
    @GetMapping(value = "/{teamshiftId}")
    public AjaxResult getInfo(@PathVariable("teamshiftId") Long teamshiftId)
    {
        return success(calTeamshiftService.selectCalTeamshiftByTeamshiftId(teamshiftId));
    }

    /**
     * 新增班组排班明细
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:teamshift:add')")
    @Log(title = "班组排班明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalTeamshift calTeamshift)
    {
        calTeamshiftService.insertCalTeamshift(calTeamshift);
        return success(calTeamshift);
    }

    /**
     * 修改班组排班明细
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:teamshift:edit')")
    @Log(title = "班组排班明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalTeamshift calTeamshift)
    {
        return toAjax(calTeamshiftService.updateCalTeamshift(calTeamshift));
    }

    /**
     * 删除班组排班明细
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:teamshift:remove')")
    @Log(title = "班组排班明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamshiftIds}")
    public AjaxResult remove(@PathVariable Long[] teamshiftIds)
    {
        return toAjax(calTeamshiftService.deleteCalTeamshiftByTeamshiftIds(teamshiftIds));
    }
}
