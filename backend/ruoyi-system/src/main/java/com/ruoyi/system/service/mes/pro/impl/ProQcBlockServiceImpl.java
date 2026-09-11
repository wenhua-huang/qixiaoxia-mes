package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.TodoTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.qc.QcBlockInfo;
import com.ruoyi.system.domain.mes.qc.QcBlockRelease;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import com.ruoyi.system.domain.mes.qc.WorkorderProcessPair;
import com.ruoyi.system.domain.mes.sys.SysTodoList;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.qc.QcBlockReleaseMapper;
import com.ruoyi.system.mapper.mes.qc.QcIpqcMapper;
import com.ruoyi.system.mapper.mes.sys.SysTodoListMapper;
import com.ruoyi.system.service.mes.pro.IProQcBlockService;
import com.ruoyi.system.service.mes.pro.ProRouteFlowHelper;
import com.ruoyi.system.service.mes.qc.QcConstants;

import jakarta.annotation.PostConstruct;

/**
 * 跟单质检不合格硬拦服务实现。
 *
 * <p>判定链：前驱检验节点 → 最新 COMPLETED 判定单 → FAIL 且无 (IPQC,任务) 放行记录 → 拦截。
 * 放行先锁后事务，留痕表 uk_ipqc_target 兜底并发重复放行；FAIL 判定后按路线下一波任务
 * 幂等生成 PRO_QC_BLOCK 待办（弱依赖，单条失败只告警不阻断判定）。
 *
 * @author qixiaoxia
 */
@Service
public class ProQcBlockServiceImpl implements IProQcBlockService
{
    private static final Logger log = LoggerFactory.getLogger(ProQcBlockServiceImpl.class);

    private static final String LOCK_RELEASE_PREFIX = "pro:qc:release:";
    private static final String FEEDBACK_TYPE_INTERNAL = "INTERNAL";
    private static final int RELEASE_REASON_MIN_LEN = 2;
    private static final int RELEASE_REASON_MAX_LEN = 500;
    /** sys_todo_list.handle_result 为 varchar(500)：前缀拼接后超长会导致放行事务整体回滚 */
    private static final int TODO_HANDLE_RESULT_MAX_LEN = 500;
    private static final int BATCH_STATE_MAX = 100;

    @Autowired
    private ProRouteFlowHelper flow;

    @Autowired
    private QcIpqcMapper qcIpqcMapper;

    @Autowired
    private QcBlockReleaseMapper blockReleaseMapper;

    @Autowired
    private ProTaskMapper proTaskMapper;

    @Autowired
    private SysTodoListMapper sysTodoListMapper;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTxTemplate()
    {
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public QcBlockInfo findBlock(Long workorderId, Long routeId, Long processId, Long taskId)
    {
        if (workorderId == null || routeId == null || processId == null)
        {
            return null;
        }
        return resolveBlock(workorderId, taskId, flow.prevCheckNode(routeId, processId));
    }

    @Override
    public QcBlockInfo findBlock(Long workorderId, Long processId, Long taskId,
                                 List<ProRouteProcess> routeNodes)
    {
        if (workorderId == null || processId == null || routeNodes == null)
        {
            return null;
        }
        return resolveBlock(workorderId, taskId, flow.prevCheckNode(routeNodes, processId));
    }

    /** 阻塞判定主干：检验前驱节点 → 最新判定单 → FAIL 且无放行记录则拦截（工单+工序粒度，不区分流转卡） */
    private QcBlockInfo resolveBlock(Long workorderId, Long taskId,
                                     Optional<ProRouteProcess> checkNodeOpt)
    {
        if (checkNodeOpt.isEmpty())
        {
            return null;
        }
        ProRouteProcess checkNode = checkNodeOpt.get();
        QcIpqc latest = qcIpqcMapper.selectLatestCompletedByProcess(
                workorderId, checkNode.getProcessId());
        if (latest == null || !QcConstants.RESULT_FAIL.equals(latest.getCheckResult()))
        {
            // 无已判定单 / PASS / CONCESSION 均放行：只硬拦已判不合格
            return null;
        }
        if (isReleased(latest.getIpqcId(), taskId))
        {
            return null;
        }
        return buildBlockInfo(latest, checkNode);
    }

    /**
     * 是否已人工放行。taskId 为 null（理论上不该发生）时退化为「该工单+工序任一放行记录即放行」。
     */
    private boolean isReleased(Long ipqcId, Long taskId)
    {
        if (taskId == null)
        {
            return blockReleaseMapper.existsByIpqc(ipqcId);
        }
        return blockReleaseMapper.existsByIpqcAndTask(ipqcId, taskId);
    }

    private QcBlockInfo buildBlockInfo(QcIpqc ipqc, ProRouteProcess checkNode)
    {
        String checkName = checkNode.getProcessName() != null
                ? checkNode.getProcessName() : ipqc.getProcessName();
        String reason = "上道检验工序「" + checkName + "」判定不合格（检验单号 " + ipqc.getIpqcCode()
                + "），本工序暂不可报工，请联系质检或有权限人员放行";
        return new QcBlockInfo(ipqc.getIpqcId(), ipqc.getIpqcCode(),
                checkNode.getProcessId(), checkName, reason);
    }

    @Override
    public void assertReportable(ProFeedback feedback)
    {
        // 与领料门控同口径：仅厂内自制报工受跟单质检门控，外协报工不拦
        if (feedback == null || !FEEDBACK_TYPE_INTERNAL.equals(feedback.getFeedbackType()))
        {
            return;
        }
        QcBlockInfo block = findBlock(feedback.getWorkorderId(), feedback.getRouteId(),
                feedback.getProcessId(), feedback.getTaskId());
        if (block != null)
        {
            throw new ServiceException(block.getReason());
        }
    }

    @Override
    public void release(Long taskId, String reason)
    {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.length() < RELEASE_REASON_MIN_LEN)
        {
            throw new ServiceException("请填写放行理由");
        }
        if (trimmed.length() > RELEASE_REASON_MAX_LEN)
        {
            throw new ServiceException("放行理由不能超过 " + RELEASE_REASON_MAX_LEN + " 字");
        }
        // 先锁后事务：锁防同人/多人并发重复放行，事务保证 留痕+待办关闭 原子
        lockTemplate.execute(LOCK_RELEASE_PREFIX + taskId,
                (Runnable) () -> txTemplate.execute(tx -> { doRelease(taskId, trimmed); return null; }));
    }

    private void doRelease(Long taskId, String reason)
    {
        ProTask task = proTaskMapper.selectProTaskByTaskId(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已被删除");
        }
        // 放行以任务维度定位（任务不挂具体流转卡；一条放行对该工单该工序各卡生效）
        QcBlockInfo block = findBlock(task.getWorkorderId(), task.getRouteId(),
                task.getProcessId(), taskId);
        if (block == null)
        {
            throw new ServiceException("当前任务无需放行");
        }
        try
        {
            blockReleaseMapper.insertQcBlockRelease(buildRelease(task, block, reason));
        }
        catch (DuplicateKeyException dup)
        {
            // uk_ipqc_target 兜底：锁失效/并发下重复放行
            throw new ServiceException("该任务已放行，请勿重复操作");
        }
        closeBlockTodo(block, taskId, reason);
    }

    private QcBlockRelease buildRelease(ProTask task, QcBlockInfo block, String reason)
    {
        QcBlockRelease r = new QcBlockRelease();
        r.setIpqcId(block.getIpqcId());
        r.setIpqcCode(block.getIpqcCode());
        r.setWorkorderId(task.getWorkorderId());
        r.setWorkorderCode(task.getWorkorderCode());
        r.setCheckProcessId(block.getCheckProcessId());
        r.setCheckProcessName(block.getCheckProcessName());
        r.setTargetTaskId(task.getTaskId());
        r.setTargetProcessId(task.getProcessId());
        r.setTargetProcessName(task.getProcessName());
        r.setReleaseReason(reason);
        r.setApproverId(SecurityUtils.getUserId());
        r.setApproverName(currentApproverName());
        Date now = new Date();
        r.setApproveTime(now);
        r.setCreateBy(SecurityUtils.getUsername());
        r.setCreateTime(now);
        return r;
    }

    /** 放行人快照：nickName(userName)，取不到姓名时退化为账号 */
    private String currentApproverName()
    {
        String userName = SecurityUtils.getUsername();
        try
        {
            String nickName = SecurityUtils.getLoginUser().getUser().getNickName();
            if (nickName != null && !nickName.isEmpty())
            {
                return nickName + "(" + userName + ")";
            }
        }
        catch (Exception ignored)
        {
            // 上下文取不到用户实体时退化为账号
        }
        return userName;
    }

    private void closeBlockTodo(QcBlockInfo block, Long taskId, String reason)
    {
        String handleResult = QcConstants.TODO_RESULT_BLOCK_RELEASED_PREFIX + reason;
        if (handleResult.length() > TODO_HANDLE_RESULT_MAX_LEN)
        {
            handleResult = handleResult.substring(0, TODO_HANDLE_RESULT_MAX_LEN);
        }
        String docCode = QcConstants.buildBlockTodoCode(block.getIpqcCode(), taskId);
        sysTodoListMapper.completePendingByDocAndCode(QcConstants.BLOCK_TODO_SOURCE_TYPE,
                block.getIpqcId(), docCode, new Date(), handleResult,
                SecurityUtils.getUsername());
    }

    @Override
    public List<String> onIpqcFailed(QcIpqc ipqc)
    {
        List<ProRouteProcess> wave = resolveNextWave(ipqc);
        if (wave.isEmpty())
        {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (ProRouteProcess node : wave)
        {
            names.add(node.getProcessName());  // 无任务也返回工序名，供检验页提示
            createTodosForNode(ipqc, node);
        }
        return new ArrayList<>(names);
    }

    @Override
    public List<String> blockedProcessNames(QcIpqc ipqc)
    {
        return resolveNextWave(ipqc).stream()
                .map(ProRouteProcess::getProcessName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream().collect(java.util.stream.Collectors.toList());
    }

    /**
     * 解析下一波并行工序；IPQC 头不存 route_id，按 检验单任务 → 同工单同检验工序任务 反查
     * （App 手工建单只带 workorderId+processId+cardId 不带 taskId；成品入库检等都查不到时返回空波）
     */
    private List<ProRouteProcess> resolveNextWave(QcIpqc ipqc)
    {
        if (ipqc == null || ipqc.getProcessId() == null)
        {
            return List.of();
        }
        Long routeId = ipqc.getRouteId();
        if (routeId == null && ipqc.getTaskId() != null)
        {
            ProTask sourceTask = proTaskMapper.selectProTaskByTaskId(ipqc.getTaskId());
            routeId = sourceTask == null ? null : sourceTask.getRouteId();
        }
        if (routeId == null && ipqc.getWorkorderId() != null)
        {
            ProTask query = new ProTask();
            query.setWorkorderId(ipqc.getWorkorderId());
            query.setProcessId(ipqc.getProcessId());
            routeId = proTaskMapper.selectProTaskList(query).stream()
                    .map(ProTask::getRouteId).filter(java.util.Objects::nonNull)
                    .findFirst().orElse(null);
        }
        if (routeId == null)
        {
            return List.of();
        }
        return flow.nextWave(routeId, ipqc.getProcessId());
    }

    private void createTodosForNode(QcIpqc ipqc, ProRouteProcess node)
    {
        ProTask query = new ProTask();
        query.setWorkorderId(ipqc.getWorkorderId());
        query.setProcessId(node.getProcessId());
        List<ProTask> tasks = proTaskMapper.selectProTaskList(query);
        if (tasks == null)
        {
            return;
        }
        for (ProTask task : tasks)
        {
            if (isTaskInactive(task.getStatus()))
            {
                continue;  // 已完成/已取消任务不再拦截
            }
            try
            {
                tryCreateBlockTodo(ipqc, task, node);
            }
            catch (Exception e)
            {
                // 弱依赖：单条待办失败不阻断 FAIL 判定主事务
                log.warn("创建质检拦截待办失败 ipqcId={} taskId={}", ipqc.getIpqcId(), task.getTaskId(), e);
            }
        }
    }

    private boolean isTaskInactive(String status)
    {
        for (String inactive : ProConstants.TASK_STATUS_INACTIVE)
        {
            if (inactive.equals(status))
            {
                return true;
            }
        }
        return false;
    }

    private void tryCreateBlockTodo(QcIpqc ipqc, ProTask task, ProRouteProcess node)
    {
        String docCode = QcConstants.buildBlockTodoCode(ipqc.getIpqcCode(), task.getTaskId());
        SysTodoList exist = sysTodoListMapper.selectPendingByDocAndCode(
                QcConstants.BLOCK_TODO_SOURCE_TYPE, ipqc.getIpqcId(), docCode);
        if (exist != null)
        {
            return;  // 幂等：同 IPQC + 同任务已有 PENDING 待办
        }
        SysTodoList todo = new SysTodoList();
        // sys_todo_list.user_id 为 NOT NULL 且无「全员待办」语义：负责人 → 派工报工人
        // → 当前判定人兜底，保证待办总有行动责任人（硬拦不依赖待办，放行入口在报工/任务页）
        Long assignee = task.getLeaderId() != null ? task.getLeaderId()
                : task.getWorkerId() != null ? task.getWorkerId()
                : SecurityUtils.getUserId();
        todo.setUserId(assignee);
        todo.setTodoType(TodoTypeEnum.PRO_QC_BLOCK.getCode());
        todo.setTodoTitle(buildBlockTodoTitle(ipqc, task, node));
        todo.setSourceDocId(ipqc.getIpqcId());
        todo.setSourceDocType(QcConstants.BLOCK_TODO_SOURCE_TYPE);
        todo.setSourceDocCode(docCode);
        todo.setPriority(QcConstants.TODO_PRIORITY_HIGH);
        todo.setStatus(QcConstants.TODO_STATUS_PENDING);
        todo.setCreateTime(new Date());
        sysTodoListMapper.insertSysTodoList(todo);
    }

    private String buildBlockTodoTitle(QcIpqc ipqc, ProTask task, ProRouteProcess node)
    {
        String workorderCode = ipqc.getWorkorderCode() != null
                ? ipqc.getWorkorderCode() : task.getWorkorderCode();
        String processName = task.getProcessName() != null ? task.getProcessName() : node.getProcessName();
        return "质检不合格拦截：" + workorderCode + "-" + processName + " 待放行/处理";
    }

    @Override
    public Map<String, Map<String, Object>> qcBlockState(List<Long> taskIds)
    {
        if (taskIds == null || taskIds.isEmpty())
        {
            return Map.of();
        }
        if (taskIds.size() > BATCH_STATE_MAX)
        {
            throw new ServiceException("单次最多查询 " + BATCH_STATE_MAX + " 个任务的锁态");
        }
        // 批量路径：任务 1 次 + 每路线节点 1 次 + 判定单 1 次 + 放行记录 1 次，杜绝逐任务 N+1
        List<Long> distinctIds = taskIds.stream().distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, ProTask> taskMap = proTaskMapper.selectProTaskByTaskIds(distinctIds).stream()
                .collect(java.util.stream.Collectors.toMap(ProTask::getTaskId, t -> t, (a, b) -> a));
        Map<Long, ProRouteProcess> checkNodeByTask = mapCheckNodes(taskMap);
        Map<String, QcIpqc> latestIpqcByPair = loadLatestIpqcs(taskMap.values(), checkNodeByTask);
        Set<String> releaseKeys = loadReleaseKeys(latestIpqcByPair.values());

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Long taskId : taskIds)
        {
            result.put(String.valueOf(taskId),
                    stateOf(taskId, taskMap.get(taskId), checkNodeByTask, latestIpqcByPair, releaseKeys));
        }
        return result;
    }

    /** 批量解析每个任务的前驱检验节点（路线节点按 routeId 缓存复用） */
    private Map<Long, ProRouteProcess> mapCheckNodes(Map<Long, ProTask> taskMap)
    {
        Map<Long, List<ProRouteProcess>> nodesCache = new HashMap<>();
        Map<Long, ProRouteProcess> checkNodeByTask = new HashMap<>();
        for (ProTask task : taskMap.values())
        {
            if (task.getRouteId() == null || task.getProcessId() == null)
            {
                continue;
            }
            List<ProRouteProcess> nodes = nodesCache.computeIfAbsent(task.getRouteId(), flow::nodes);
            flow.prevCheckNode(nodes, task.getProcessId())
                    .ifPresent(node -> checkNodeByTask.put(task.getTaskId(), node));
        }
        return checkNodeByTask;
    }

    /** 按全部（工单,检验工序）组批量取已判定单，每组保留 ipqc_id 最大的一张（SQL 已按 id 倒序） */
    private Map<String, QcIpqc> loadLatestIpqcs(java.util.Collection<ProTask> tasks,
                                                Map<Long, ProRouteProcess> checkNodeByTask)
    {
        // 按 "工单:工序" 键去重后再构造批量查询入参
        Map<String, WorkorderProcessPair> pairDedup = new LinkedHashMap<>();
        for (ProTask t : tasks)
        {
            if (t.getWorkorderId() == null || !checkNodeByTask.containsKey(t.getTaskId()))
            {
                continue;
            }
            Long checkProcessId = checkNodeByTask.get(t.getTaskId()).getProcessId();
            pairDedup.putIfAbsent(pairKey(t.getWorkorderId(), checkProcessId),
                    new WorkorderProcessPair(t.getWorkorderId(), checkProcessId));
        }
        List<WorkorderProcessPair> pairs = new ArrayList<>(pairDedup.values());
        if (pairs.isEmpty())
        {
            return Map.of();
        }
        Map<String, QcIpqc> latest = new HashMap<>();
        for (QcIpqc ipqc : qcIpqcMapper.selectLatestCompletedByProcessPairs(pairs))
        {
            latest.putIfAbsent(pairKey(ipqc.getWorkorderId(), ipqc.getProcessId()), ipqc);
        }
        return latest;
    }

    /** 批量取 FAIL 单的放行记录，返回 ipqcId:targetTaskId 键集合 */
    private Set<String> loadReleaseKeys(java.util.Collection<QcIpqc> ipqcs)
    {
        List<Long> failIpqcIds = ipqcs.stream()
                .filter(i -> QcConstants.RESULT_FAIL.equals(i.getCheckResult()))
                .map(QcIpqc::getIpqcId).distinct().collect(java.util.stream.Collectors.toList());
        if (failIpqcIds.isEmpty())
        {
            return Set.of();
        }
        return blockReleaseMapper.selectByIpqcIds(failIpqcIds).stream()
                .map(r -> r.getIpqcId() + ":" + r.getTargetTaskId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Map<String, Object> stateOf(Long taskId, ProTask task,
                                        Map<Long, ProRouteProcess> checkNodeByTask,
                                        Map<String, QcIpqc> latestIpqcByPair,
                                        Set<String> releaseKeys)
    {
        Map<String, Object> state = new HashMap<>(2);
        QcBlockInfo block = null;
        ProRouteProcess checkNode = task == null ? null : checkNodeByTask.get(taskId);
        if (task != null && checkNode != null)
        {
            QcIpqc latest = latestIpqcByPair.get(
                    pairKey(task.getWorkorderId(), checkNode.getProcessId()));
            boolean released = releaseKeys.contains(latest == null ? null
                    : latest.getIpqcId() + ":" + taskId);
            if (latest != null && QcConstants.RESULT_FAIL.equals(latest.getCheckResult()) && !released)
            {
                block = buildBlockInfo(latest, checkNode);
            }
        }
        state.put("blocked", block != null);
        state.put("reason", block == null ? null : block.getReason());
        return state;
    }

    private static String pairKey(Long workorderId, Long processId)
    {
        return workorderId + ":" + processId;
    }
}
