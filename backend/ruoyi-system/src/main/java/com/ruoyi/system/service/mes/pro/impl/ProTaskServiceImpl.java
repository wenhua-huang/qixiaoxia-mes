package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.pro.ProProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.service.mes.pro.IProTaskService;

/**
 * 生产任务/排产Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProTaskServiceImpl implements IProTaskService
{
    @Autowired
    private ProTaskMapper proTaskMapper;

    @Autowired
    private ProWorkorderMapper proWorkorderMapper;

    @Autowired
    private ProProcessMapper proProcessMapper;

    @Autowired
    private ProRouteMapper proRouteMapper;

    @Autowired
    private ProRouteProcessMapper proRouteProcessMapper;

    @Autowired
    private MdWorkstationMapper mdWorkstationMapper;

    @Override
    public ProTask selectProTaskByTaskId(Long taskId)
    {
        return proTaskMapper.selectProTaskByTaskId(taskId);
    }

    @Override
    public List<ProTask> selectProTaskList(ProTask proTask)
    {
        return proTaskMapper.selectProTaskList(proTask);
    }

    @Override
    public List<ProTask> selectAll()
    {
        return proTaskMapper.selectProTaskList(new ProTask());
    }

    /**
     * 自动填充关联字段（工单、产品、工序、工艺路线）
     */
    private void autoFillRelatedFields(ProTask proTask)
    {
        // 从工单获取关联信息
        if (proTask.getWorkorderId() != null)
        {
            ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(proTask.getWorkorderId());
            if (wo != null)
            {
                if (proTask.getWorkorderCode() == null) proTask.setWorkorderCode(wo.getWorkorderCode());
                if (proTask.getWorkorderName() == null) proTask.setWorkorderName(wo.getWorkorderName());
                if (proTask.getItemId() == null) proTask.setItemId(wo.getProductId());
                if (proTask.getItemCode() == null) proTask.setItemCode(wo.getProductCode());
                if (proTask.getItemName() == null) proTask.setItemName(wo.getProductName());
                if (proTask.getSpecification() == null) proTask.setSpecification(wo.getProductSpc());
                if (proTask.getUnitOfMeasure() == null) proTask.setUnitOfMeasure(wo.getUnitOfMeasure());
                if (proTask.getUnitName() == null) proTask.setUnitName(wo.getUnitName());
                if (proTask.getClientId() == null) proTask.setClientId(wo.getClientId());
                if (proTask.getClientCode() == null) proTask.setClientCode(wo.getClientCode());
                if (proTask.getClientName() == null) proTask.setClientName(wo.getClientName());
                if (proTask.getRequestDate() == null) proTask.setRequestDate(wo.getRequestDate());
            }
        }

        // 从工序获取信息
        if (proTask.getProcessId() != null)
        {
            ProProcess proc = proProcessMapper.selectProProcessByProcessId(proTask.getProcessId());
            if (proc != null)
            {
                if (proTask.getProcessCode() == null) proTask.setProcessCode(proc.getProcessCode());
                if (proTask.getProcessName() == null) proTask.setProcessName(proc.getProcessName());
            }
        }

        // 从工作站获取信息
        if (proTask.getWorkstationId() != null)
        {
            try {
                MdWorkstation ws = mdWorkstationMapper.selectMdWorkstationByWorkstationId(proTask.getWorkstationId());
                if (ws != null)
                {
                    if (proTask.getWorkstationCode() == null) proTask.setWorkstationCode(ws.getWorkstationCode());
                    if (proTask.getWorkstationName() == null) proTask.setWorkstationName(ws.getWorkstationName());
                }
            } catch (Exception e) {
                // fallback: workstation not found, use defaults
            }
        }
        // 如果工作站编码仍为空，设置默认值
        if (proTask.getWorkstationCode() == null || proTask.getWorkstationCode().isEmpty()) proTask.setWorkstationCode("-");
        if (proTask.getWorkstationName() == null || proTask.getWorkstationName().isEmpty()) proTask.setWorkstationName("-");
        // 其他必填非空字段默认值
        if (proTask.getTaskCode() == null || proTask.getTaskCode().isEmpty()) proTask.setTaskCode("TASK" + System.currentTimeMillis());
        if (proTask.getTaskName() == null || proTask.getTaskName().isEmpty()) proTask.setTaskName(proTask.getTaskCode());
        if (proTask.getWorkorderCode() == null || proTask.getWorkorderCode().isEmpty()) proTask.setWorkorderCode("-");
        if (proTask.getWorkorderName() == null || proTask.getWorkorderName().isEmpty()) proTask.setWorkorderName("-");
        if (proTask.getItemCode() == null || proTask.getItemCode().isEmpty()) proTask.setItemCode("-");
        if (proTask.getItemName() == null || proTask.getItemName().isEmpty()) proTask.setItemName("-");
        if (proTask.getUnitOfMeasure() == null || proTask.getUnitOfMeasure().isEmpty()) proTask.setUnitOfMeasure("PCS");

        // 尝试从工单路线获取 route_id
        if (proTask.getRouteId() == null && proTask.getWorkorderId() != null && proTask.getProcessId() != null)
        {
            ProRouteProcess query = new ProRouteProcess();
            query.setProcessId(proTask.getProcessId());
            List<ProRouteProcess> routes = proRouteProcessMapper.selectProRouteProcessList(query);
            if (routes != null && !routes.isEmpty())
            {
                ProRouteProcess match = routes.stream()
                    .filter(r -> r.getProcessId().equals(proTask.getProcessId()))
                    .findFirst().orElse(null);
                if (match != null && match.getRouteId() != null)
                {
                    proTask.setRouteId(match.getRouteId());
                }
            }
        }
        // fallback: set route_id = 0 if still null (to satisfy NOT NULL constraint)
        if (proTask.getRouteId() == null)
        {
            proTask.setRouteId(0L);
        }
    }

    @Override
    @Transactional
    public int insertProTask(ProTask proTask)
    {
        proTask.setCreateTime(DateUtils.getNowDate());
        proTask.setCreateBy(SecurityUtils.getUsername());
        if (proTask.getStatus() == null) proTask.setStatus(ProConstants.TASK_STATUS_NORMAL);

        // 默认值
        if (proTask.getQuantity() == null) proTask.setQuantity(BigDecimal.ONE);
        if (proTask.getQuantityProduced() == null) proTask.setQuantityProduced(BigDecimal.ZERO);
        if (proTask.getQuantityQualified() == null) proTask.setQuantityQualified(BigDecimal.ZERO);
        if (proTask.getQuantityUnqualified() == null) proTask.setQuantityUnqualified(BigDecimal.ZERO);
        if (proTask.getQuantityChanged() == null) proTask.setQuantityChanged(BigDecimal.ZERO);
        if (proTask.getSetupDuration() == null) proTask.setSetupDuration(0);
        if (proTask.getUnitDuration() == null) proTask.setUnitDuration(BigDecimal.ZERO);
        if (proTask.getOfflineQty() == null) proTask.setOfflineQty(0);
        if (proTask.getDuration() == null) proTask.setDuration(1);

        // 自动填充工单、产品、工序等关联字段
        autoFillRelatedFields(proTask);

        // 自动生成任务名称：[产品名]【数量】单位
        if (proTask.getTaskName() == null || proTask.getTaskName().isEmpty() || proTask.getTaskName().equals(proTask.getTaskCode()))
        {
            StringBuilder sb = new StringBuilder();
            if (proTask.getItemName() != null) sb.append(proTask.getItemName());
            if (proTask.getQuantity() != null) sb.append("【").append(proTask.getQuantity().stripTrailingZeros().toPlainString()).append("】");
            if (proTask.getUnitName() != null) sb.append(proTask.getUnitName());
            if (sb.length() > 0) proTask.setTaskName(sb.toString());
        }

        return proTaskMapper.insertProTask(proTask);
    }

    @Override
    public List<Map<String, Object>> selectProcessProgressByWorkorder(Long workorderId)
    {
        ProTask query = new ProTask();
        query.setWorkorderId(workorderId);
        List<ProTask> tasks = proTaskMapper.selectProTaskList(query);

        // 按 processId 分组聚合
        Map<Long, Map<String, Object>> grouped = new java.util.LinkedHashMap<>();
        for (ProTask t : tasks)
        {
            Long pid = t.getProcessId() != null ? t.getProcessId() : 0L;
            Map<String, Object> row = grouped.computeIfAbsent(pid, k -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("processId", pid);
                m.put("processCode", t.getProcessCode());
                m.put("processName", t.getProcessName());
                m.put("quantityScheduled", BigDecimal.ZERO);
                m.put("quantityProduced", BigDecimal.ZERO);
                m.put("quantityQualified", BigDecimal.ZERO);
                m.put("quantityUnqualified", BigDecimal.ZERO);
                return m;
            });
            // 累加
            addBigDecimal(row, "quantityScheduled", t.getQuantity());
            addBigDecimal(row, "quantityProduced", t.getQuantityProduced());
            addBigDecimal(row, "quantityQualified", t.getQuantityQualified());
            addBigDecimal(row, "quantityUnqualified", t.getQuantityUnqualified());
        }
        return new ArrayList<>(grouped.values());
    }

    private void addBigDecimal(Map<String, Object> row, String key, BigDecimal val)
    {
        if (val == null) return;
        BigDecimal cur = (BigDecimal) row.getOrDefault(key, BigDecimal.ZERO);
        row.put(key, cur.add(val));
    }

    @Override
    public int updateProTask(ProTask proTask)
    {
        proTask.setUpdateTime(DateUtils.getNowDate());
        proTask.setUpdateBy(SecurityUtils.getUsername());
        return proTaskMapper.updateProTask(proTask);
    }

    @Override
    public int deleteProTaskByTaskIds(Long[] taskIds)
    {
        return proTaskMapper.deleteProTaskByTaskIds(taskIds);
    }

    @Override
    public int deleteProTaskByTaskId(Long taskId)
    {
        return proTaskMapper.deleteProTaskByTaskId(taskId);
    }

    // ==================== 任务状态流转（从 Controller 下沉到 Service）====================

    /** 终态集合：已结束不再占用工作站 */
    private static final List<String> TASK_INACTIVE = Arrays.asList(ProConstants.TASK_STATUS_COMPLETED, ProConstants.TASK_STATUS_CANCEL);
    /** 可下发的前置状态 */
    private static final List<String> TASK_DISPATCHABLE = Arrays.asList(ProConstants.TASK_STATUS_NORMAL, ProConstants.TASK_STATUS_PREPARE);
    /** 可取消的前置状态（所有非终态）*/
    private static final List<String> TASK_CANCELLABLE = Arrays.asList(ProConstants.TASK_STATUS_NORMAL, ProConstants.TASK_STATUS_PREPARE, ProConstants.TASK_STATUS_PRODUCING, ProConstants.TASK_STATUS_PAUSED);

    @Override
    public void dispatchTask(Long taskId)
    {
        ProTask task = proTaskMapper.selectProTaskByTaskId(taskId);
        if (task == null) throw new ServiceException("任务不存在");
        if (!TASK_DISPATCHABLE.contains(task.getStatus()))
            throw new ServiceException("只有待排产/正常状态的任务才能下发，当前状态：" + task.getStatus());
        task.setStatus(ProConstants.TASK_STATUS_PRODUCING);
        task.setUpdateTime(DateUtils.getNowDate());
        task.setUpdateBy(SecurityUtils.getUsername());
        proTaskMapper.updateProTask(task);
    }

    @Override
    public void completeTask(Long taskId)
    {
        ProTask task = proTaskMapper.selectProTaskByTaskId(taskId);
        if (task == null) throw new ServiceException("任务不存在");
        if (!ProConstants.TASK_STATUS_PRODUCING.equals(task.getStatus()))
            throw new ServiceException("只有生产中的任务才能完成，当前状态：" + task.getStatus());
        task.setStatus(ProConstants.TASK_STATUS_COMPLETED);
        task.setUpdateTime(DateUtils.getNowDate());
        task.setUpdateBy(SecurityUtils.getUsername());
        proTaskMapper.updateProTask(task);

        // 末工序任务完成 → 检查工单是否可自动完工（双保险，与报工审核的自动完工互补）
        tryAutoCompleteWorkorder(task);
    }

    @Override
    public void cancelTask(Long taskId)
    {
        ProTask task = proTaskMapper.selectProTaskByTaskId(taskId);
        if (task == null) throw new ServiceException("任务不存在");
        if (TASK_INACTIVE.contains(task.getStatus()))
            throw new ServiceException("已完成/取消的任务不能操作，当前状态：" + task.getStatus());
        task.setStatus(ProConstants.TASK_STATUS_CANCEL);
        task.setUpdateTime(DateUtils.getNowDate());
        task.setUpdateBy(SecurityUtils.getUsername());
        proTaskMapper.updateProTask(task);
    }

    // ==================== 工单级联 ====================

    @Override
    public void dispatchByWorkorder(Long workorderId)
    {
        proTaskMapper.updateStatusByWorkorder(workorderId, TASK_DISPATCHABLE, ProConstants.TASK_STATUS_PRODUCING);
    }

    @Override
    public void cancelByWorkorder(Long workorderId)
    {
        proTaskMapper.updateStatusByWorkorder(workorderId, TASK_CANCELLABLE, ProConstants.TASK_STATUS_CANCEL);
    }

    /**
     * 末工序任务完成时，检查工单是否可自动完工。
     * 条件：完成的是末工序 + 工单 PRODUCING + 已生产 ≥ 计划 → 工单 COMPLETED。
     * 与报工审核（onFeedbackAudited）的自动完工逻辑互补，确保任务完成路径也能触发完工。
     */
    private void tryAutoCompleteWorkorder(ProTask task)
    {
        if (task.getWorkorderId() == null || task.getRouteId() == null) return;
        ProRouteProcess lastProc = proRouteProcessMapper.selectLastProcessByRouteId(task.getRouteId());
        if (lastProc == null || !lastProc.getProcessId().equals(task.getProcessId())) return;

        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(task.getWorkorderId());
        if (wo == null || !"PRODUCING".equals(wo.getStatus())) return;  // 工单状态暂无常量类，沿用字符串

        BigDecimal produced = wo.getQuantityProduced() != null ? wo.getQuantityProduced() : BigDecimal.ZERO;
        BigDecimal planned = wo.getQuantity() != null ? wo.getQuantity() : BigDecimal.ZERO;
        if (produced.compareTo(planned) >= 0)
        {
            wo.setStatus("COMPLETED");  // 工单状态暂无常量类，沿用字符串
            wo.setFinishDate(new Date());
            wo.setUpdateTime(DateUtils.getNowDate());
            wo.setUpdateBy(SecurityUtils.getUsername());
            proWorkorderMapper.updateProWorkorder(wo);
        }
    }
}
