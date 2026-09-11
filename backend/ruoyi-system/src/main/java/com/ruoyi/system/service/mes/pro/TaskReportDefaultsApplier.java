package com.ruoyi.system.service.mes.pro;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.qc.QcBlockInfo;

/**
 * 报工入口任务默认值富化：为可报工任务回填「本次上机数量」系统默认值与质检阻塞态。
 *
 * <p>三个报工入口（待报工列表 / 工单报工入口 / 扫码反查）统一走本组件，
 * 默认值解析规则见 {@link ProInputQuantityResolver}，质检阻塞判定见 {@link IProQcBlockService}。
 * 一期默认值与锁态均按工单+工序粒度，不区分流转卡，保证各入口与提交时重算同源。
 *
 * @author qixiaoxia
 */
@Component
public class TaskReportDefaultsApplier {

    @Autowired
    private ProInputQuantityResolver inputResolver;

    @Autowired
    private ProRouteFlowHelper flow;

    @Autowired
    private IProQcBlockService qcBlockService;

    /** 批量富化；tasks 为 null 时安全跳过。同一路线的工序节点只查一次库（消除 N+1） */
    public void apply(Collection<ProTask> tasks) {
        if (tasks == null) {
            return;
        }
        Map<Long, List<ProRouteProcess>> nodesCache = new HashMap<>();
        Function<Long, List<ProRouteProcess>> nodesLoader =
                routeId -> nodesCache.computeIfAbsent(routeId, flow::nodes);
        tasks.forEach(t -> apply(t, nodesLoader));
    }

    /** 富化单个任务：首道工序默认排产数量，其余按上道已审产出差额；追加质检阻塞态 */
    public void apply(ProTask task) {
        if (task == null) {
            return;
        }
        task.setDefaultQuantityInput(inputResolver.resolveDefaultInput(
                task.getWorkorderId(), task.getRouteId(), task.getProcessId(),
                task.getQuantity()));
        applyQcBlock(task);
    }

    /** 批量内部入口：复用按 routeId 缓存的节点列表（上机数量与质检判定共用一次路线查询） */
    private void apply(ProTask task, Function<Long, List<ProRouteProcess>> nodesLoader) {
        if (task == null) {
            return;
        }
        List<ProRouteProcess> nodes = task.getRouteId() == null
                ? List.of() : nodesLoader.apply(task.getRouteId());
        task.setDefaultQuantityInput(inputResolver.resolveDefaultInput(
                task.getWorkorderId(), task.getRouteId(), task.getProcessId(),
                task.getQuantity(), nodesLoader));
        QcBlockInfo block = qcBlockService.findBlock(task.getWorkorderId(), task.getProcessId(),
                task.getTaskId(), nodes);
        task.setQcBlocked(block != null);
        task.setQcBlockReason(block == null ? null : block.getReason());
    }

    /** 单条富化的质检阻塞态（无预载节点，findBlock 内部按 routeId 查路线） */
    private void applyQcBlock(ProTask task) {
        QcBlockInfo block = qcBlockService.findBlock(task.getWorkorderId(), task.getRouteId(),
                task.getProcessId(), task.getTaskId());
        // 保证前端拿到布尔而非 null
        task.setQcBlocked(block != null);
        task.setQcBlockReason(block == null ? null : block.getReason());
    }
}
