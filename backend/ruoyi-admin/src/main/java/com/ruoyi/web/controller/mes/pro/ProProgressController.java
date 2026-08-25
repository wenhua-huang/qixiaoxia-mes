package com.ruoyi.web.controller.mes.pro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.mes.pro.IProProgressService;
import com.ruoyi.system.service.mes.pro.IGanttDataService;

import java.util.Map;

/**
 * 工单进度 Controller
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
@RestController
@RequestMapping("/mes/pro/progress")
public class ProProgressController extends BaseController
{
    @Autowired
    private IProProgressService progressService;

    @Autowired
    private IGanttDataService ganttDataService;

    /**
     * 工单进度详情（含工序进度、流转卡进度）。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/{workorderId}")
    public AjaxResult getProgress(@PathVariable Long workorderId)
    {
        return success(progressService.getWorkorderProgress(workorderId));
    }

    /**
     * 延期预警分页列表。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/delayList")
    public TableDataInfo delayList(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String keyword)
    {
        startPage();
        return getDataTable(progressService.selectDelayList(
                objectType, riskLevel, workshopId, teamId, keyword));
    }

    /**
     * 工单进度详情内嵌的甘特图（只读）。
     * 复用工单查询权限，且不触发自动排产，避免只读弹窗产生写副作用或要求排产权限。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/{workorderId}/gantt")
    public AjaxResult getGantt(@PathVariable Long workorderId)
    {
        Map<String, Object> data = ganttDataService.buildWorkOrderGantt(workorderId, false);
        return success(data);
    }
}
