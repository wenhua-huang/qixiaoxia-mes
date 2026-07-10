package com.ruoyi.web.controller.mes.pro;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.mes.cal.IWorkCalendarService;
import com.ruoyi.system.service.mes.pro.IGanttDataService;
import com.ruoyi.system.service.mes.pro.IProSnapshotService;
import com.ruoyi.system.service.mes.pro.IScheduleService;
import com.ruoyi.system.domain.mes.pro.ProSnapshot;
import com.ruoyi.system.service.mes.pro.impl.ProSnapshotServiceImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 甘特图排产Controller
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
@RestController
@RequestMapping("/mes/pro/gantt")
public class ProGanttController extends BaseController
{
    @Autowired
    private IGanttDataService ganttDataService;
    @Autowired
    private IWorkCalendarService workCalendarService;
    @Autowired
    private IScheduleService scheduleService;
    @Autowired
    private IProSnapshotService snapshotService;

    /**
     * 获取单工单甘特图数据
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:query')")
    @GetMapping("/workorder/{workorderId}")
    public AjaxResult getWorkOrderGantt(@PathVariable("workorderId") Long workorderId)
    {
        Map<String, Object> data = ganttDataService.buildWorkOrderGantt(workorderId);
        return success(data);
    }

    /**
     * 获取工作站维度甘特图（多工单聚合）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:query')")
    @GetMapping("/workstation/{workstationId}")
    public AjaxResult getWorkstationGantt(
            @PathVariable("workstationId") Long workstationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate)
    {
        Map<String, Object> data = ganttDataService.buildWorkstationGantt(workstationId, startDate, endDate);
        return success(data);
    }

    /** 从登录用户获取 factoryId */
    private Long currentFactoryId() {
        try { return SecurityUtils.getLoginUser().getUser().getFactoryId(); } catch (Exception e) { return 1L; }
    }

    /** 获取日期范围内每天的工作状态（由节假日表 + 排班表决定） */
    @GetMapping("/calendar/dayStatus")
    public AjaxResult dayStatus(
            @RequestParam(required = false) Long factoryId,
            @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
            @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to)
    {
        Long fid = factoryId != null ? factoryId : currentFactoryId();
        return success(workCalendarService.getDayStatusList(from, to, fid));
    }

    /** 计算结束时间(startTime + workSeconds, 跳过非工作时间) */
    @GetMapping("/calendar/calculate")
    public AjaxResult calendarCalculate(
            @RequestParam(required = false) Long factoryId,
            @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") Date start,
            @RequestParam long seconds)
    {
        return success(workCalendarService.calculateEndTime(start, seconds, factoryId != null ? factoryId : currentFactoryId()));
    }

    /** 快照列表 */
    @GetMapping("/snapshot/list")
    public TableDataInfo snapshotList(ProSnapshot query) {
        startPage();
        return getDataTable(snapshotService.selectList(query));
    }

    /** 创建快照（保存当前工单任务数据） */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:edit')")
    @PostMapping("/snapshot")
    public AjaxResult snapshotCreate(@RequestBody Map<String, Object> body) {
        Long workorderId = body.get("workorderId") != null ? Long.valueOf(body.get("workorderId").toString()) : null;
        String name = body.get("name") != null ? body.get("name").toString() : null;
        if (workorderId == null) return error("缺少 workorderId");
        Long id = ((ProSnapshotServiceImpl) snapshotService).createWithTasks(workorderId, name);
        if (id == -1L) return error("该工单无排产任务");
        if (id == -2L) return error("排产数据未变化，无需重复保存");
        return success(Map.of("id", id));
    }

    /** 发布快照 */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:edit')")
    @PutMapping("/snapshot/{id}/publish")
    public AjaxResult snapshotPublish(@PathVariable Long id) {
        return toAjax(snapshotService.publish(id));
    }

    /** 废弃快照 */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:edit')")
    @PutMapping("/snapshot/{id}/discard")
    public AjaxResult snapshotDiscard(@PathVariable Long id) {
        return toAjax(snapshotService.discard(id));
    }

    /** 删除快照 */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:edit')")
    @DeleteMapping("/snapshot/{ids}")
    public AjaxResult snapshotDelete(@PathVariable Long[] ids) {
        return toAjax(snapshotService.deleteByIds(ids));
    }

    /** 自动排产 */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:schedule')")
    @PostMapping("/schedule/{workorderId}")
    public AjaxResult schedule(@PathVariable Long workorderId) {
        Map<String, Object> r = scheduleService.scheduleWorkOrder(workorderId);
        if (r.containsKey("error")) return error((String) r.get("error"));
        return success(r);
    }

    /** 查询某工序可用工作站：按工序类型过滤，并标记给定时段是否空闲（供排产弹窗下拉） */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:query')")
    @GetMapping("/availableWorkstations")
    public AjaxResult availableWorkstations(
            @RequestParam Long processId,
            @RequestParam(required = false) String processType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(required = false) Long excludeTaskId) {
        return success(ganttDataService.availableWorkstations(
                processId, processType, startTime, endTime, excludeTaskId, currentFactoryId()));
    }

    /** 拖拽移动任务 + 级联更新，直接返回最新甘特图数据 */
    @PreAuthorize("@ss.hasPermi('mes:pro:gantt:edit')")
    @PutMapping("/task/{taskId}/move")
    public AjaxResult moveTask(@PathVariable Long taskId, @RequestParam String newStart, @RequestParam String newEnd) {
        Map<String, Object> r = scheduleService.moveTask(taskId, newStart, newEnd);
        if (r.containsKey("error")) return error((String) r.get("error"));
        // 直接返回更新后的完整甘特图，前端无需二次查询
        Long workorderId = (Long) r.get("workorderId");
        if (workorderId != null) {
            return success(ganttDataService.buildWorkOrderGantt(workorderId));
        }
        return success(r);
    }
}
