package com.ruoyi.system.service.mes.pro.impl;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.service.mes.pro.IGanttDataService;
import com.ruoyi.system.service.mes.pro.IScheduleService;
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

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> buildWorkOrderGantt(Long workorderId)
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

        // 自动排产：无任务且有关联工艺路线时，先排产再返回数据
        log.info("甘特图加载: workorderId={}, taskCount={}, routeId={}", workorderId, taskList.size(), routeId);
        if (taskList.isEmpty() && routeId != null) {
            try {
                scheduleService.scheduleWorkOrder(workorderId);
                taskList = proTaskMapper.selectProTaskList(taskQuery);
            } catch (Exception e) {
                log.warn("自动排产失败: workorderId={}, error={}", workorderId, e.toString());
            }
        }

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
}
