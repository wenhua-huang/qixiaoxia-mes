package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import static com.ruoyi.system.domain.mes.pro.ProConstants.*;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.service.mes.cal.IWorkCalendarService;
import com.ruoyi.system.service.mes.pro.IProChangeoverService;
import com.ruoyi.system.service.mes.pro.IScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 排产计算服务实现
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
@Service
public class ScheduleServiceImpl implements IScheduleService
{
    @Autowired
    private ProWorkorderMapper workorderMapper;
    @Autowired
    private ProRouteProductMapper routeProductMapper;
    @Autowired
    private ProRouteProcessMapper routeProcessMapper;
    @Autowired
    private ProTaskMapper taskMapper;
    @Autowired
    private IWorkCalendarService calendarService;
    @Autowired
    private IProChangeoverService changeoverService;
    @Autowired
    private com.ruoyi.system.mapper.mes.md.MdWorkstationMapper workstationMapper;

    private static final ThreadLocal<SimpleDateFormat> SDF_ISO = new ThreadLocal<SimpleDateFormat>() {
        @Override protected SimpleDateFormat initialValue() { return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); }
    };
    private static final ThreadLocal<SimpleDateFormat> SDF_SPACE = new ThreadLocal<SimpleDateFormat>() {
        @Override protected SimpleDateFormat initialValue() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); }
    };

    private Date parseDate(String s) {
        try { return SDF_ISO.get().parse(s); } catch (Exception e) { }
        try { return SDF_SPACE.get().parse(s); } catch (Exception e) { }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> scheduleWorkOrder(Long workorderId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 加载工单
        ProWorkorder wo = workorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) { result.put("error", "工单不存在"); return result; }
        Long factoryId = wo.getFactoryId();

        // 2. 获取工艺路线
        Long routeId = null;
        if (wo.getRouteProductId() != null) {
            ProRouteProduct rp = routeProductMapper.selectProRouteProductByRecordId(wo.getRouteProductId());
            if (rp != null) routeId = rp.getRouteId();
        }
        if (routeId == null) { result.put("error", "工单未关联工艺路线"); return result; }

        // 3. 加载路线工序(按 order_num 排序)
        ProRouteProcess rpQ = new ProRouteProcess();
        rpQ.setRouteId(routeId);
        List<ProRouteProcess> processes = routeProcessMapper.selectProRouteProcessList(rpQ);
        processes.sort(Comparator.comparing(p -> p.getOrderNum() != null ? p.getOrderNum() : 0));

        // 4. 加载排产任务(按 processId 分组)，无任务的工序自动创建
        ProTask tQ = new ProTask();
        tQ.setWorkorderId(workorderId);
        List<ProTask> tasks = taskMapper.selectProTaskList(tQ);
        Map<Long, List<ProTask>> taskMap = new LinkedHashMap<>();
        for (ProTask t : tasks) {
            taskMap.computeIfAbsent(t.getProcessId(), k -> new ArrayList<>()).add(t);
        }
        // 自动为无任务的工序创建任务
        for (ProRouteProcess rp : processes) {
            if (!taskMap.containsKey(rp.getProcessId())) {
                ProTask newTask = new ProTask();
                newTask.setFactoryId(factoryId);
                newTask.setWorkorderId(workorderId);
                newTask.setWorkorderCode(wo.getWorkorderCode());
                newTask.setWorkorderName(wo.getWorkorderName());
                newTask.setProcessId(rp.getProcessId());
                newTask.setProcessCode(rp.getProcessCode());
                newTask.setProcessName(rp.getProcessName());
                newTask.setRouteId(routeId);
                newTask.setItemId(wo.getProductId());
                newTask.setItemCode(wo.getProductCode());
                newTask.setItemName(wo.getProductName());
                newTask.setQuantity(wo.getQuantity() != null ? wo.getQuantity() : BigDecimal.ONE);
                newTask.setUnitOfMeasure(wo.getUnitOfMeasure() != null ? wo.getUnitOfMeasure() : DEFAULT_UNIT);
                newTask.setUnitName(wo.getUnitName() != null ? wo.getUnitName() : DEFAULT_UNIT);
                // 先匹配工作站
                MdWorkstation wsQ = new MdWorkstation();
                wsQ.setProcessId(rp.getProcessId());
                wsQ.setFactoryId(factoryId);
                List<MdWorkstation> wss = workstationMapper.selectMdWorkstationList(wsQ);
                if (!wss.isEmpty()) {
                    newTask.setWorkstationId(wss.get(0).getWorkstationId());
                    newTask.setWorkstationCode(wss.get(0).getWorkstationCode());
                    newTask.setWorkstationName(wss.get(0).getWorkstationName());
                } else {
                    newTask.setWorkstationId(0L);
                    newTask.setWorkstationCode("AUTO");
                    newTask.setWorkstationName("自动分配");
                }
                // 从产能计算 unitDuration（个/分钟 = 产能/60）
                BigDecimal unitDur = BigDecimal.ZERO;
                if (!wss.isEmpty() && wss.get(0).getCapacity() != null && wss.get(0).getCapacity() > 0) {
                    unitDur = BigDecimal.valueOf(60.0 / wss.get(0).getCapacity());
                }
                newTask.setUnitDuration(unitDur);
                newTask.setSetupDuration(0);
                newTask.setStatus(TASK_STATUS_NORMAL);
                newTask.setColorCode(rp.getColorCode() != null ? rp.getColorCode() : DEFAULT_COLOR_CODE);
                newTask.setTaskCode(wo.getWorkorderCode() + "-" + String.format("%03d", rp.getOrderNum() != null ? rp.getOrderNum() : 1));
                newTask.setTaskName((rp.getProcessName() != null ? rp.getProcessName() : "工序") + "-自动");
                newTask.setCreateTime(DateUtils.getNowDate());
                newTask.setCreateBy(SYSTEM_USER);
                // taskCode/taskName auto-generated in insert
                taskMapper.insertProTask(newTask);
                taskMap.computeIfAbsent(rp.getProcessId(), k -> new ArrayList<>()).add(newTask);
            }
        }

        // 5. 起始时间
        Date startTime = calendarService.getNearestWorkingDay(new Date(), factoryId);

        // 6. 逐工序计算
        Date currentTime = startTime;
        Long prevProcessId = null;
        List<String> updated = new ArrayList<>();

        for (ProRouteProcess rp : processes) {
            // 换型时间
            if (prevProcessId != null) {
                Long wsId = null;
                List<ProTask> pt = taskMap.get(prevProcessId);
                if (pt != null && !pt.isEmpty()) wsId = pt.get(0).getWorkstationId();
                int changeoverMins = changeoverService.getChangeoverMinutes(prevProcessId, rp.getProcessId(), wsId, factoryId);
                if (changeoverMins > 0) {
                    currentTime = calendarService.calculateEndTime(currentTime, changeoverMins * 60L, factoryId);
                }
            }

            // 准备时长
            if (rp.getDefaultPreTime() != null && rp.getDefaultPreTime() > 0) {
                currentTime = calendarService.calculateEndTime(currentTime, rp.getDefaultPreTime() * 60L, factoryId);
            }

            // 该工序的任务
            List<ProTask> ptList = taskMap.getOrDefault(rp.getProcessId(), Collections.emptyList());
            for (ProTask task : ptList) {
                // 调机时长
                if (task.getSetupDuration() != null && task.getSetupDuration() > 0) {
                    currentTime = calendarService.calculateEndTime(currentTime, task.getSetupDuration() * 60L, factoryId);
                }
                // 排产时间(分钟) = 数量 × 单位耗时(分钟/个)
                BigDecimal qty = task.getQuantity() != null ? task.getQuantity() : BigDecimal.ONE;
                BigDecimal unit = task.getUnitDuration() != null ? task.getUnitDuration() : BigDecimal.ZERO;
                long durationMins = qty.multiply(unit).longValue();
                if (durationMins <= 0) durationMins = DEFAULT_DURATION_MINUTES;

                task.setStartTime(currentTime);
                task.setEndTime(calendarService.calculateEndTime(currentTime, durationMins * 60L, factoryId));
                task.setDuration((int) durationMins);
                task.setUpdateTime(DateUtils.getNowDate());
                taskMapper.updateProTask(task);
                currentTime = task.getEndTime();
                updated.add(String.valueOf(task.getTaskId()));
            }

            // 等待时长
            if (rp.getDefaultSufTime() != null && rp.getDefaultSufTime() > 0) {
                currentTime = calendarService.calculateEndTime(currentTime, rp.getDefaultSufTime() * 60L, factoryId);
            }

            prevProcessId = rp.getProcessId();
        }

        result.put("updatedTasks", updated);
        result.put("workorderId", workorderId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> moveTask(Long taskId, String newStartTime, String newEndTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> updatedIds = new ArrayList<>();

        try {
            ProTask task = taskMapper.selectProTaskByTaskId(taskId);
            if (task == null) { result.put("error", "任务不存在"); return result; }

            Long factoryId = task.getFactoryId();
            Date newStart = parseDate(newStartTime);
            Date newEnd = parseDate(newEndTime);

            result.put("workorderId", task.getWorkorderId());

            // 1. 更新当前任务（duration = 实际时间差）
            task.setStartTime(newStart);
            task.setEndTime(newEnd);
            long actualDurationMs = newEnd.getTime() - newStart.getTime();
            task.setDuration((int) (actualDurationMs / 60000)); // 转换为分钟
            task.setUpdateTime(DateUtils.getNowDate());
            taskMapper.updateProTask(task);
            updatedIds.add(String.valueOf(taskId));

            // 2. 获取工艺路线（通过工单 → routeProduct → route）
            ProWorkorder wo = workorderMapper.selectProWorkorderByWorkorderId(task.getWorkorderId());
            Long routeId = null;
            if (wo != null && wo.getRouteProductId() != null) {
                ProRouteProduct rp = routeProductMapper.selectProRouteProductByRecordId(wo.getRouteProductId());
                if (rp != null) routeId = rp.getRouteId();
            }
            if (routeId == null) { result.put("updatedTasks", updatedIds); return result; }

            // 3. 找到当前工序的后续工序
            ProRouteProcess rpQ = new ProRouteProcess();
            rpQ.setRouteId(routeId);
            List<ProRouteProcess> allRp = routeProcessMapper.selectProRouteProcessList(rpQ);

            // 按order_num排序找到当前工序位置
            allRp.sort(Comparator.comparing(p -> p.getOrderNum() != null ? p.getOrderNum() : 0));
            int idx = -1;
            for (int i = 0; i < allRp.size(); i++) {
                if (allRp.get(i).getProcessId().equals(task.getProcessId())) { idx = i; break; }
            }

            // 0. 前置约束（仅 SS 类型）：不能早于前道工序的结束时间+换型
            if (idx > 0) {
                Long prevProcessId = allRp.get(idx - 1).getProcessId();
                String linkType = allRp.get(idx - 1).getLinkType();
                if (!LINK_TYPE_FS.equals(linkType)) {
                    ProTask prevQ = new ProTask();
                    prevQ.setWorkorderId(task.getWorkorderId());
                    prevQ.setProcessId(prevProcessId);
                    List<ProTask> prevTasks = taskMapper.selectProTaskList(prevQ);
                    if (!prevTasks.isEmpty()) {
                        Date maxPrevEnd = prevTasks.stream()
                            .map(ProTask::getEndTime).filter(Objects::nonNull)
                            .max(Date::compareTo).orElse(null);
                        if (maxPrevEnd != null) {
                            int changeoverMins = changeoverService.getChangeoverMinutes(prevProcessId, task.getProcessId(), task.getWorkstationId(), factoryId);
                            Date minStart = calendarService.calculateEndTime(maxPrevEnd, changeoverMins * 60L, factoryId);
                            if (newStart.before(minStart)) {
                                long durMs = newEnd.getTime() - newStart.getTime();
                                newStart = minStart;
                                newEnd = calendarService.calculateEndTime(newStart, durMs / 1000, factoryId);
                            }
                        }
                    }
                }
            }

            // 4. 级联更新后续工序
            Date currentTime = newEnd;
            for (int i = idx + 1; i < allRp.size(); i++) {
                ProRouteProcess rp = allRp.get(i);
                Long prevProcessId = allRp.get(i - 1).getProcessId();

                // 换型
                int changeoverMins = changeoverService.getChangeoverMinutes(prevProcessId, rp.getProcessId(), task.getWorkstationId(), factoryId);
                if (changeoverMins > 0) {
                    currentTime = calendarService.calculateEndTime(currentTime, changeoverMins * 60L, factoryId);
                }
                if (rp.getDefaultPreTime() != null && rp.getDefaultPreTime() > 0) {
                    currentTime = calendarService.calculateEndTime(currentTime, rp.getDefaultPreTime() * 60L, factoryId);
                }

                ProTask tQ2 = new ProTask();
                tQ2.setWorkorderId(task.getWorkorderId());
                tQ2.setProcessId(rp.getProcessId());
                List<ProTask> successors = taskMapper.selectProTaskList(tQ2);

                for (ProTask st : successors) {
                    if (st.getSetupDuration() != null && st.getSetupDuration() > 0) {
                        currentTime = calendarService.calculateEndTime(currentTime, st.getSetupDuration() * 60L, factoryId);
                    }
                    // 保留原时长，只平移时间
                    long durMins = st.getDuration() != null && st.getDuration() > 0 ? st.getDuration() : DEFAULT_DURATION_MINUTES;

                    st.setStartTime(currentTime);
                    st.setEndTime(calendarService.calculateEndTime(currentTime, durMins * 60L, factoryId));
                    st.setDuration((int) durMins);
                    st.setUpdateTime(DateUtils.getNowDate());
                    taskMapper.updateProTask(st);
                    currentTime = st.getEndTime();
                    updatedIds.add(String.valueOf(st.getTaskId()));
                }

                if (rp.getDefaultSufTime() != null && rp.getDefaultSufTime() > 0) {
                    currentTime = calendarService.calculateEndTime(currentTime, rp.getDefaultSufTime() * 60L, factoryId);
                }
            }

            result.put("updatedTasks", updatedIds);

            // 5. 安全网：确保所有任务按工序顺序排列
            enforceOrder(task.getWorkorderId(), factoryId);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /** 确保所有任务按工序顺序排列（后道 ≥ 前道结束时间） */
    private void enforceOrder(Long workorderId, Long factoryId) {
        ProTask q = new ProTask(); q.setWorkorderId(workorderId);
        List<ProTask> all = taskMapper.selectProTaskList(q);
        if (all.size() <= 1) return;

        // 通过工单 → routeProduct → routeId → routeProcess 获取 order_num
        ProWorkorder wo = workorderMapper.selectProWorkorderByWorkorderId(workorderId);
        Long routeId = null;
        if (wo != null && wo.getRouteProductId() != null) {
            ProRouteProduct rp = routeProductMapper.selectProRouteProductByRecordId(wo.getRouteProductId());
            if (rp != null) routeId = rp.getRouteId();
        }
        if (routeId == null) return;

        ProRouteProcess rpQ = new ProRouteProcess(); rpQ.setRouteId(routeId);
        List<ProRouteProcess> rps = routeProcessMapper.selectProRouteProcessList(rpQ);
        Map<Long, Integer> orderMap = new HashMap<>();
        for (ProRouteProcess r : rps) {
            orderMap.put(r.getProcessId(), r.getOrderNum() != null ? r.getOrderNum() : 99);
        }

        all.sort((a, b) -> Integer.compare(
            orderMap.getOrDefault(a.getProcessId(), 99),
            orderMap.getOrDefault(b.getProcessId(), 99)));

        // 按 link_type 约束：SS=必须先后，FS=可并行
        for (int i = 1; i < all.size(); i++) {
            ProTask prev = all.get(i - 1);
            ProTask cur = all.get(i);
            String linkType = getLinkType(routeId, prev.getProcessId());
            if (LINK_TYPE_SS.equals(linkType) || linkType == null) {  // SS 或未设=必须先后
                if (prev.getEndTime() != null && cur.getEndTime() != null &&
                    cur.getStartTime() != null && cur.getStartTime().before(prev.getEndTime())) {
                    long durMs = cur.getEndTime().getTime() - cur.getStartTime().getTime();
                    cur.setStartTime(prev.getEndTime());
                    cur.setEndTime(new Date(prev.getEndTime().getTime() + durMs));
                    cur.setDuration((int)(durMs / 60000));
                    cur.setUpdateTime(DateUtils.getNowDate());
                    taskMapper.updateProTask(cur);
                }
            }
            // FS=并行：不约束，允许重叠
        }
    }

    private String getLinkType(Long routeId, Long processId) {
        ProRouteProcess q = new ProRouteProcess();
        q.setRouteId(routeId);
        q.setProcessId(processId);
        List<ProRouteProcess> list = routeProcessMapper.selectProRouteProcessList(q);
        return (!list.isEmpty()) ? list.get(0).getLinkType() : LINK_TYPE_SS;
    }
}
