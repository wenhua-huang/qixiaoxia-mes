package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.vo.CardSuborderVO;
import com.ruoyi.system.domain.mes.pro.vo.DelayItemVO;
import com.ruoyi.system.domain.mes.pro.vo.ProcessProgressRowVO;
import com.ruoyi.system.domain.mes.pro.vo.WorkorderProgressVO;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProProgressMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.mes.pro.DelayLevelEvaluator;
import com.ruoyi.system.service.mes.pro.IProProgressService;
import com.ruoyi.system.service.mes.pro.ProDates;

/**
 * 工单进度 Service 实现
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
@Service
public class ProProgressServiceImpl implements IProProgressService
{
    @Autowired
    private ProWorkorderMapper workorderMapper;
    @Autowired
    private ProTaskMapper taskMapper;
    @Autowired
    private ProCardMapper cardMapper;
    @Autowired
    private ProCardProcessMapper cardProcessMapper;
    @Autowired
    private ProProgressMapper progressMapper;
    @Autowired
    private ISysConfigService configService;

    @Override
    public WorkorderProgressVO getWorkorderProgress(Long workorderId)
    {
        ProWorkorder wo = workorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null)
        {
            throw new ServiceException("工单不存在");
        }
        WorkorderProgressVO vo = new WorkorderProgressVO();
        BeanUtils.copyProperties(wo, vo);
        fillTimes(wo.getWorkorderId(), vo);
        vo.setCompletionRate(percent(wo.getQuantityProduced(), wo.getQuantity()));
        int warnDays = cfgInt(ProConstants.CFG_WARN_DAYS, 2);
        vo.setDelayLevel(DelayLevelEvaluator.evaluateWorkorder(
                wo.getRequestDate(), wo.getFinishDate(), wo.getStatus(), warnDays, new Date()));
        vo.setProcesses(buildProcessRows(workorderId));
        vo.setCards(buildCards(workorderId));
        return vo;
    }

    private void fillTimes(Long workorderId, WorkorderProgressVO vo)
    {
        Map<String, Object> agg = progressMapper.aggregateWorkorderTime(workorderId);
        if (agg == null)
        {
            return;
        }
        vo.setPlanStartTime(ProDates.toDate(agg.get("planStart")));
        vo.setPlanEndTime(ProDates.toDate(agg.get("planEnd")));
        vo.setActualStartTime(ProDates.toDate(agg.get("actualStart")));
        vo.setActualEndTime(ProDates.toDate(agg.get("actualEnd")));
    }

    private List<ProcessProgressRowVO> buildProcessRows(Long workorderId)
    {
        ProTask q = new ProTask();
        q.setWorkorderId(workorderId);
        List<ProTask> tasks = taskMapper.selectProTaskList(q);
        if (tasks.isEmpty())
        {
            return List.of();
        }
        List<Long> taskIds = tasks.stream().map(ProTask::getTaskId).toList();
        Map<Long, Map<String, Object>> actualMap = progressMapper.aggregateActualByTaskIds(taskIds)
                .stream().collect(Collectors.toMap(
                        m -> ((Number) m.get("taskId")).longValue(), m -> m, (a, b) -> a));
        int warnHours = cfgInt(ProConstants.CFG_WARN_HOURS, 24);
        int tol = cfgInt(ProConstants.CFG_BEHIND_TOLERANCE, 10);
        return tasks.stream()
                .map(t -> toProcessRow(t, actualMap.get(t.getTaskId()), warnHours, tol))
                .toList();
    }

    private ProcessProgressRowVO toProcessRow(ProTask t, Map<String, Object> actual,
            int warnHours, int tol)
    {
        ProcessProgressRowVO row = new ProcessProgressRowVO();
        BeanUtils.copyProperties(t, row);
        // ProTask.startTime/endTime -> VO.planStartTime/planEndTime 名称不一致，显式赋值
        row.setPlanStartTime(t.getStartTime());
        row.setPlanEndTime(t.getEndTime());
        if (actual != null)
        {
            row.setActualStartTime(ProDates.toDate(actual.get("actualStart")));
            row.setActualEndTime(ProDates.toDate(actual.get("actualEnd")));
        }
        row.setCompletionRate(percent(t.getQuantityProduced(), t.getQuantity()));
        row.setDelayLevel(DelayLevelEvaluator.evaluateTask(
                t.getStartTime(), t.getEndTime(),
                row.getActualStartTime(), row.getActualEndTime(),
                t.getStatus(), t.getQuantity(), t.getQuantityProduced(),
                warnHours, tol, new Date()));
        return row;
    }

    private List<CardSuborderVO> buildCards(Long workorderId)
    {
        ProCard cq = new ProCard();
        cq.setWorkorderId(workorderId);
        List<ProCard> cards = cardMapper.selectProCardList(cq);
        return cards.stream().map(this::toCardRow).toList();
    }

    private CardSuborderVO toCardRow(ProCard c)
    {
        CardSuborderVO row = new CardSuborderVO();
        BeanUtils.copyProperties(c, row);
        ProCardProcess pq = new ProCardProcess();
        pq.setCardId(c.getCardId());
        List<ProCardProcess> cps = cardProcessMapper.selectProCardProcessList(pq);
        Date actualStart = cps.stream().map(ProCardProcess::getInputTime)
                .filter(Objects::nonNull).min(Date::compareTo).orElse(null);
        Date actualEnd = cps.stream().map(ProCardProcess::getOutputTime)
                .filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        long finished = cps.stream().filter(p -> p.getOutputTime() != null).count();
        row.setActualStartTime(actualStart);
        row.setActualEndTime(actualEnd);
        row.setTotalProcessCount(cps.size());
        row.setFinishedProcessCount((int) finished);
        row.setCompletionRate(cps.isEmpty() ? 0 : (int) (finished * 100 / cps.size()));
        return row;
    }

    @Override
    public List<DelayItemVO> selectDelayList(String objectType, String riskLevel,
            Long workshopId, Long teamId, String keyword)
    {
        if ("TASK".equals(objectType))
        {
            int warnHours = cfgInt(ProConstants.CFG_WARN_HOURS, 24);
            int tol = cfgInt(ProConstants.CFG_BEHIND_TOLERANCE, 10);
            List<DelayItemVO> list = progressMapper.selectTaskDelays(
                    riskLevel, workshopId, teamId, keyword, warnHours);
            list.forEach(item -> applyTaskDelayLevel(item, warnHours, tol));
            return list;
        }
        int warnDays = cfgInt(ProConstants.CFG_WARN_DAYS, 2);
        List<DelayItemVO> list = progressMapper.selectWorkorderDelays(
                riskLevel, workshopId, keyword, warnDays);
        list.forEach(item -> applyWorkorderDelayLevel(item, warnDays));
        return list;
    }

    private void applyTaskDelayLevel(DelayItemVO item, int warnHours, int tol)
    {
        String level = DelayLevelEvaluator.evaluateTask(
                null, item.getPlanTime(), null, item.getActualEndTime(),
                item.getStatus(), null, null, warnHours, tol, new Date());
        item.setDelayLevel(level);
    }

    private void applyWorkorderDelayLevel(DelayItemVO item, int warnDays)
    {
        String level = DelayLevelEvaluator.evaluateWorkorder(
                item.getRequestDate(), item.getActualEndTime(),
                item.getStatus(), warnDays, new Date());
        item.setDelayLevel(level);
    }

    /** 读取 sys_config 整数，缺失/非法时返回默认值。 */
    private int cfgInt(String key, int def)
    {
        String v = configService.selectConfigByKey(key);
        if (v == null || v.isBlank())
        {
            return def;
        }
        try
        {
            return Integer.parseInt(v.trim());
        }
        catch (NumberFormatException e)
        {
            return def;
        }
    }

    /** 完成率：null/0 总数返回 0，否则 HALF_UP 取整。 */
    private Integer percent(BigDecimal part, BigDecimal total)
    {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0)
        {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP).intValue();
    }
}
