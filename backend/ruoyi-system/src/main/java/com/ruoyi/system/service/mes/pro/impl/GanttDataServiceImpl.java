package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.mes.pro.DelayLevelEvaluator;
import com.ruoyi.system.service.mes.pro.IGanttDataService;
import com.ruoyi.system.service.mes.pro.IScheduleService;
import com.ruoyi.system.service.mes.pro.ProDates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 甘特图数据组装Service实现
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
@Service
public class GanttDataServiceImpl implements IGanttDataService
{
    private static final Logger log = LoggerFactory.getLogger(GanttDataServiceImpl.class);

    @Autowired
    private ProTaskMapper proTaskMapper;

    @Autowired
    private ProWorkorderMapper proWorkorderMapper;

    @Autowired
    private ProRouteProcessMapper proRouteProcessMapper;

    @Autowired
    private ProRouteProductMapper proRouteProductMapper;

    @Autowired
    private IScheduleService scheduleService;

    @Autowired
    private ProProgressMapper progressMapper;

    @Autowired
    private ISysConfigService configService;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> buildWorkOrderGantt(Long workorderId)
    {
        return buildWorkOrderGantt(workorderId, true);
    }

    @Override
    public Map<String, Object> buildWorkOrderGantt(Long workorderId, boolean autoSchedule)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> tasks = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        // 1. 加载工单
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) {
            result.put("tasks", tasks);
            result.put("links", links);
            return result;
        }

        // 2. 加载工单关联的工艺路线(通过 routeProductId)
        Long routeId = null;
        if (wo.getRouteProductId() != null) {
            ProRouteProduct rp = proRouteProductMapper.selectProRouteProductByRecordId(wo.getRouteProductId());
            if (rp != null) {
                routeId = rp.getRouteId();
            }
        }

        // 3. 加载路线工序列表(按 order_num 排序)
        Map<Long, ProRouteProcess> processMap = new LinkedHashMap<>();
        if (routeId != null) {
            ProRouteProcess query = new ProRouteProcess();
            query.setRouteId(routeId);
            List<ProRouteProcess> processes = proRouteProcessMapper.selectProRouteProcessList(query);
            processes.stream()
                .sorted(Comparator.comparing(p -> p.getOrderNum() != null ? p.getOrderNum() : 0))
                .forEach(p -> processMap.put(p.getProcessId(), p));
        }

        // 4. 加载工单的所有排产任务
        ProTask taskQuery = new ProTask();
        taskQuery.setWorkorderId(workorderId);
        List<ProTask> taskList = proTaskMapper.selectProTaskList(taskQuery);

        // 自动排产：无任务且有关联工艺路线时，先排产再返回数据（仅编辑型甘特页；只读详情页不触发写操作）
        log.info("甘特图加载: workorderId={}, taskCount={}, routeId={}, autoSchedule={}", workorderId, taskList.size(), routeId, autoSchedule);
        if (autoSchedule && taskList.isEmpty() && routeId != null) {
            try {
                scheduleService.scheduleWorkOrder(workorderId);
                taskList = proTaskMapper.selectProTaskList(taskQuery);
            } catch (Exception e) {
                log.warn("自动排产失败: workorderId={}, error={}", workorderId, e.toString());
            }
        }

        Map<Long, Map<String, Object>> actualMap = loadActualMap(taskList);
        int warnHours = cfgInt(ProConstants.CFG_WARN_HOURS, 24);
        int tol = cfgInt(ProConstants.CFG_BEHIND_TOLERANCE, 10);

        // 按 processId 分组
        Map<Long, List<ProTask>> tasksByProcess = taskList.stream()
            .collect(Collectors.groupingBy(ProTask::getProcessId));

        // 5. 构建甘特图树
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("id", "WO-" + wo.getWorkorderId());
        project.put("text", wo.getWorkorderName() + " / " + wo.getProductName());
        project.put("type", "project");
        List<Map<String, Object>> children = new ArrayList<>();

        String prevTaskId = null;
        int linkCounter = 1;

        for (Map.Entry<Long, ProRouteProcess> entry : processMap.entrySet()) {
            Long processId = entry.getKey();
            ProRouteProcess rp = entry.getValue();
            List<ProTask> ptList = tasksByProcess.getOrDefault(processId, Collections.emptyList());

            for (ProTask pt : ptList) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", pt.getTaskId().toString());
                item.put("text", (rp.getProcessName() != null ? rp.getProcessName() : "工序") +
                    " → " + (pt.getWorkstationName() != null ? pt.getWorkstationName() : ""));
                item.put("start", pt.getStartTime() != null ? sdf.format(pt.getStartTime()) : null);
                item.put("end", pt.getEndTime() != null ? sdf.format(pt.getEndTime()) : null);
                item.put("duration", pt.getDuration());
                item.put("processId", pt.getProcessId());
                item.put("processName", rp.getProcessName());
                item.put("workstationId", pt.getWorkstationId());
                item.put("colorCode", rp.getColorCode() != null ? rp.getColorCode() : com.ruoyi.system.domain.mes.pro.ProConstants.DEFAULT_COLOR_CODE);
                item.put("predecessorId", pt.getPredecessorId());
                item.put("status", pt.getStatus());
                item.put("quantity", pt.getQuantity());
                item.put("quantityProduced", pt.getQuantityProduced());
                enrichItem(pt, item, actualMap, warnHours, tol);
                children.add(item);

                // 构建依赖连线(沿 route_process.next_process_id)
                if (prevTaskId != null) {
                    Map<String, Object> link = new LinkedHashMap<>();
                    link.put("id", "L" + linkCounter++);
                    link.put("source", prevTaskId);
                    link.put("target", pt.getTaskId().toString());
                    link.put("type", "FS");
                    links.add(link);
                }
                prevTaskId = pt.getTaskId().toString();
            }
        }

        project.put("children", children);
        tasks.add(project);

        result.put("tasks", tasks);
        result.put("links", links);
        return result;
    }

    @Override
    public Map<String, Object> buildWorkstationGantt(Long workstationId, String startDate, String endDate)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> tasks = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        // 查询该工作站的所有任务(时间范围内)
        // 简化实现：先查所有该工作站的任务
        ProTask taskQuery = new ProTask();
        taskQuery.setWorkstationId(workstationId);
        List<ProTask> taskList = proTaskMapper.selectProTaskList(taskQuery);

        Map<Long, Map<String, Object>> actualMap = loadActualMap(taskList);
        int warnHours = cfgInt(ProConstants.CFG_WARN_HOURS, 24);
        int tol = cfgInt(ProConstants.CFG_BEHIND_TOLERANCE, 10);

        // 按工单分组
        Map<Long, List<ProTask>> tasksByWO = taskList.stream()
            .collect(Collectors.groupingBy(ProTask::getWorkorderId));

        int linkCounter = 1;
        for (Map.Entry<Long, List<ProTask>> entry : tasksByWO.entrySet()) {
            Long woId = entry.getKey();
            ProTask first = entry.getValue().get(0);

            Map<String, Object> project = new LinkedHashMap<>();
            project.put("id", "WO-" + woId);
            project.put("text", first.getWorkorderName() + " / " + first.getItemName());
            project.put("type", "project");

            List<Map<String, Object>> children = new ArrayList<>();
            String prevId = null;
            for (ProTask pt : entry.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", pt.getTaskId().toString());
                item.put("text", pt.getProcessName() + " → " + pt.getWorkstationName());
                item.put("start", pt.getStartTime() != null ? sdf.format(pt.getStartTime()) : null);
                item.put("end", pt.getEndTime() != null ? sdf.format(pt.getEndTime()) : null);
                item.put("colorCode", pt.getColorCode() != null ? pt.getColorCode() : com.ruoyi.system.domain.mes.pro.ProConstants.DEFAULT_COLOR_CODE);
                item.put("status", pt.getStatus());
                enrichItem(pt, item, actualMap, warnHours, tol);
                children.add(item);

                if (prevId != null) {
                    Map<String, Object> link = new LinkedHashMap<>();
                    link.put("id", "L" + linkCounter++);
                    link.put("source", prevId);
                    link.put("target", pt.getTaskId().toString());
                    link.put("type", "FS");
                    links.add(link);
                }
                prevId = pt.getTaskId().toString();
            }
            project.put("children", children);
            tasks.add(project);
        }

        result.put("tasks", tasks);
        result.put("links", links);
        return result;
    }

    @Override
    public List<Map<String, Object>> availableWorkstations(Long processId, String processType,
                                                            Date startTime, Date endTime,
                                                            Long excludeTaskId, Long factoryId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<MdWorkstation> candidates = scheduleService.matchCandidates(processId, processType, factoryId);
        boolean checkIdle = startTime != null && endTime != null;
        for (MdWorkstation ws : candidates) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("workstationId", ws.getWorkstationId());
            m.put("workstationCode", ws.getWorkstationCode());
            m.put("workstationName", ws.getWorkstationName());
            m.put("workstationType", ws.getWorkstationType());
            m.put("processId", ws.getProcessId());
            m.put("processType", ws.getProcessType());
            m.put("processName", ws.getProcessName());
            m.put("capacity", ws.getCapacity());
            m.put("status", ws.getStatus());
            // 未提供时段时 idle=null（前端不标注），避免误标"空闲"；提供了则按冲突判定
            Boolean idle = null;
            if (checkIdle) {
                int conflict = proTaskMapper.countConflict(ws.getWorkstationId(), startTime, endTime, factoryId, excludeTaskId);
                idle = (conflict == 0);
            }
            m.put("idle", idle);
            result.add(m);
        }
        // 空闲排前（仅当时段判定有效时）
        if (checkIdle) {
            result.sort((a, b) -> Boolean.compare((Boolean) a.get("idle"), (Boolean) b.get("idle")) * -1);
        }
        return result;
    }

    /** 批量聚合任务的实际开始/结束，避免 N+1；空列表返回空 Map。 */
    private Map<Long, Map<String, Object>> loadActualMap(List<ProTask> taskList) {
        List<Long> ids = taskList.stream().map(ProTask::getTaskId).toList();
        if (ids.isEmpty()) return Map.of();
        return progressMapper.aggregateActualByTaskIds(ids).stream()
            .collect(Collectors.toMap(
                m -> ((Number) m.get("taskId")).longValue(), m -> m, (a, b) -> a));
    }

    /** 给单个甘特 task 节点回填实际时间/完成率/延期等级。 */
    private void enrichItem(ProTask pt, Map<String, Object> item,
                            Map<Long, Map<String, Object>> actualMap, int warnHours, int tol) {
        Map<String, Object> act = actualMap.get(pt.getTaskId());
        Date aStart = act == null ? null : ProDates.toDate(act.get("actualStart"));
        Date aEnd = act == null ? null : ProDates.toDate(act.get("actualEnd"));
        item.put("actualStartTime", aStart != null ? sdf.format(aStart) : null);
        item.put("actualEndTime", aEnd != null ? sdf.format(aEnd) : null);
        item.put("progressPercent", percent(pt.getQuantityProduced(), pt.getQuantity()));
        String level = DelayLevelEvaluator.evaluateTask(pt.getStartTime(), pt.getEndTime(),
            aStart, aEnd, pt.getStatus(), pt.getQuantity(), pt.getQuantityProduced(),
            warnHours, tol, new Date());
        item.put("delayLevel", level);
        item.put("behindSchedule", ProConstants.DELAY_BEHIND.equals(level));
    }

    /** 读 sys_config 整数，缺失或非法返回默认值。 */
    private int cfgInt(String key, int def) {
        String v = configService.selectConfigByKey(key);
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private Integer percent(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) return 0;
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }
}
