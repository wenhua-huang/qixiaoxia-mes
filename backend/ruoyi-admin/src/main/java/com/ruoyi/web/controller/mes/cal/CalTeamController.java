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
import com.ruoyi.system.domain.mes.cal.CalTeam;
import com.ruoyi.system.service.mes.cal.ICalTeamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 班组Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/team")
public class CalTeamController extends BaseController
{
    @Autowired
    private ICalTeamService calTeamService;

    /**
     * 查询班组列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:team:list')")
    @GetMapping("/list")
    public TableDataInfo list(CalTeam calTeam)
    {
        startPage();
        List<CalTeam> list = calTeamService.selectCalTeamList(calTeam);
        return getDataTable(list);
    }

    /**
     * 导出班组列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:team:export')")
    @Log(title = "班组", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CalTeam calTeam)
    {
        List<CalTeam> list = calTeamService.selectCalTeamList(calTeam);
        ExcelUtil<CalTeam> util = new ExcelUtil<CalTeam>(CalTeam.class);
        util.exportExcel(response, list, "班组数据");
    }

    /**
     * 获取班组详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:team:query')")
    @GetMapping(value = "/{teamId}")
    public AjaxResult getInfo(@PathVariable("teamId") Long teamId)
    {
        return success(calTeamService.selectCalTeamByTeamId(teamId));
    }

    /**
     * 新增班组
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:team:add')")
    @Log(title = "班组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalTeam calTeam)
    {
        calTeamService.insertCalTeam(calTeam);
        return success(calTeam);
    }

    /**
     * 修改班组
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:team:edit')")
    @Log(title = "班组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CalTeam calTeam)
    {
        return toAjax(calTeamService.updateCalTeam(calTeam));
    }

    /**
     * 删除班组
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:team:remove')")
    @Log(title = "班组", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamIds}")
    public AjaxResult remove(@PathVariable Long[] teamIds)
    {
        return toAjax(calTeamService.deleteCalTeamByTeamIds(teamIds));
    }
}
