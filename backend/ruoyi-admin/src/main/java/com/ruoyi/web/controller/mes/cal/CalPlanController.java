package com.ruoyi.web.controller.mes.cal;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
import com.ruoyi.system.domain.mes.cal.CalPlan;
import com.ruoyi.system.domain.mes.cal.CalPlanTeam;
import com.ruoyi.system.service.mes.cal.ICalPlanService;
import com.ruoyi.system.service.mes.cal.ICalPlanTeamService;
import com.ruoyi.system.service.mes.cal.ICalTeamshiftService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 排班计划Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/cal/plan")
public class CalPlanController extends BaseController
{
    @Autowired
    private ICalPlanService calPlanService;

    @Autowired
    private ICalPlanTeamService calPlanTeamService;

    @Autowired
    private ICalTeamshiftService calTeamshiftService;

    /**
     * 查询排班计划列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(CalPlan calPlan)
    {
        startPage();
        List<CalPlan> list = calPlanService.selectCalPlanList(calPlan);
        return getDataTable(list);
    }

    /**
     * 导出排班计划列表
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan:export')")
    @Log(title = "排班计划", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CalPlan calPlan)
    {
        List<CalPlan> list = calPlanService.selectCalPlanList(calPlan);
        ExcelUtil<CalPlan> util = new ExcelUtil<CalPlan>(CalPlan.class);
        util.exportExcel(response, list, "排班计划数据");
    }

    /**
     * 获取排班计划详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan:query')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") Long planId)
    {
        return success(calPlanService.selectCalPlanByPlanId(planId));
    }

    /**
     * 新增排班计划
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan:add')")
    @Log(title = "排班计划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CalPlan calPlan)
    {
        calPlanService.insertCalPlan(calPlan);
        return success(calPlan);
    }

    /**
     * 修改排班计划
     * 当状态变更为"已确认"时，自动生成排班明细
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan:edit')")
    @Log(title = "排班计划", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional
    public AjaxResult edit(@RequestBody CalPlan calPlan)
    {
        if ("CONFIRMED".equals(calPlan.getStatus())) {
            // 检查班组配置
            CalPlanTeam teamParam = new CalPlanTeam();
            teamParam.setPlanId(calPlan.getPlanId());
            List<CalPlanTeam> teams = calPlanTeamService.selectCalPlanTeamList(teamParam);
            if (teams == null || teams.isEmpty()) {
                return AjaxResult.error("请先配置班组!");
            }
            if ("SHIFT_TWO".equals(calPlan.getShiftType()) && teams.size() != 2) {
                return AjaxResult.error("两班倒需要配置两个班组!");
            }
            if ("SHIFT_THREE".equals(calPlan.getShiftType()) && teams.size() != 3) {
                return AjaxResult.error("三班倒需要配置三个班组!");
            }
            // 生成排班明细
            calTeamshiftService.genRecords(calPlan.getPlanId());
        }
        return toAjax(calPlanService.updateCalPlan(calPlan));
    }

    /**
     * 删除排班计划
     */
    @PreAuthorize("@ss.hasPermi('mes:cal:plan:remove')")
    @Log(title = "排班计划", businessType = BusinessType.DELETE)
	@DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(calPlanService.deleteCalPlanByPlanIds(planIds));
    }
}
