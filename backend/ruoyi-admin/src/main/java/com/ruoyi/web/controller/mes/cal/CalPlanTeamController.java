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
import com.ruoyi.system.domain.mes.cal.CalPlanTeam;
import com.ruoyi.system.service.mes.cal.ICalPlanTeamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 计划班组关联Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/plan-team")
public class CalPlanTeamController extends BaseController
{
    @Autowired
    private ICalPlanTeamService calPlanTeamService;

    /**
     * 查询计划班组关联列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan-team:list')")
    @GetMapping("/list")
    public TableDataInfo list(CalPlanTeam calPlanTeam)
    {
        startPage();
        List<CalPlanTeam> list = calPlanTeamService.selectCalPlanTeamList(calPlanTeam);
        return getDataTable(list);
    }

    /**
     * 导出计划班组关联列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan-team:export')")
    @Log(title = "计划班组关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CalPlanTeam calPlanTeam)
    {
        List<CalPlanTeam> list = calPlanTeamService.selectCalPlanTeamList(calPlanTeam);
        ExcelUtil<CalPlanTeam> util = new ExcelUtil<CalPlanTeam>(CalPlanTeam.class);
        util.exportExcel(response, list, "计划班组关联数据");
    }

    /**
     * 获取计划班组关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan-team:query')")
    @GetMapping(value = "/{planTeamId}")
    public AjaxResult getInfo(@PathVariable("planTeamId") Long planTeamId)
    {
        return success(calPlanTeamService.selectCalPlanTeamByRecordId(planTeamId));
    }

    /**
     * 新增计划班组关联
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan-team:add')")
    @Log(title = "计划班组关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalPlanTeam calPlanTeam)
    {
        calPlanTeamService.insertCalPlanTeam(calPlanTeam);
        return success(calPlanTeam);
    }

    /**
     * 修改计划班组关联
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan-team:edit')")
    @Log(title = "计划班组关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalPlanTeam calPlanTeam)
    {
        return toAjax(calPlanTeamService.updateCalPlanTeam(calPlanTeam));
    }

    /**
     * 删除计划班组关联
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan-team:remove')")
    @Log(title = "计划班组关联", businessType = BusinessType.DELETE)
	@DeleteMapping("/{planTeamIds}")
    public AjaxResult remove(@PathVariable Long[] planTeamIds)
    {
        return toAjax(calPlanTeamService.deleteCalPlanTeamByRecordIds(planTeamIds));
    }
}
