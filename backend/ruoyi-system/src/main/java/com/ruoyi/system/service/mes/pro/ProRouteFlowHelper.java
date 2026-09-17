package com.ruoyi.system.service.mes.pro;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;

/**
 * 工艺路线工序流查询：前驱 / 检验前驱 / 下一波并行节点。
 *
 * <p>节点定位以 order_num 为准（mapper 已按 order_num asc 返回，这里再防御性排序）；
 * 同 order_num 多个节点视为并行（FS），{@link #nextWave} 整波返回。
 *
 * @author qixiaoxia
 */
@Component
public class ProRouteFlowHelper {

    @Autowired
    private ProRouteProcessMapper routeProcessMapper;

    /** 路线全部节点（按 order_num 升序）；路线不存在返回空列表 */
    public List<ProRouteProcess> nodes(Long routeId) {
        List<ProRouteProcess> nodes = routeProcessMapper.selectProRouteProcessByRouteId(routeId);
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .sorted(Comparator.comparing(ProRouteProcess::getOrderNum,
                        Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    /** 当前工序节点；不在路线上返回 empty */
    public Optional<ProRouteProcess> currentNode(Long routeId, Long processId) {
        if (routeId == null || processId == null) {
            return Optional.empty();
        }
        return currentNode(nodes(routeId), processId);
    }

    /** 当前工序节点（基于预载节点列表，批量场景按路线复用、避免重复查库） */
    public Optional<ProRouteProcess> currentNode(List<ProRouteProcess> nodes, Long processId) {
        if (processId == null || nodes == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(n -> processId.equals(n.getProcessId()))
                .findFirst();
    }

    /** 前驱节点：order_num 严格小于当前节点的最大节点（首道工序返回 empty） */
    public Optional<ProRouteProcess> prevNode(Long routeId, Long processId) {
        if (routeId == null) {
            return Optional.empty();
        }
        return prevNode(nodes(routeId), processId);
    }

    /**
     * 前驱节点（基于预载节点列表）：首道工序返回 empty。
     * 一期限制：同 order_num 并行前驱只取一个（max 同序时随机一个），并行拓扑下
     * 上机默认值只计单个前驱产出；待路线支持并行汇聚语义后改为前驱求和。
     */
    public Optional<ProRouteProcess> prevNode(List<ProRouteProcess> nodes, Long processId) {
        Integer curOrder = currentOrder(nodes, processId);
        if (curOrder == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(n -> n.getOrderNum() != null && n.getOrderNum() < curOrder)
                .max(Comparator.comparing(ProRouteProcess::getOrderNum));
    }

    /** 前驱中最近的检验节点（is_check='Y'），供 Task 4 质检流使用 */
    public Optional<ProRouteProcess> prevCheckNode(Long routeId, Long processId) {
        Integer curOrder = currentOrder(routeId, processId);
        if (curOrder == null) {
            return Optional.empty();
        }
        return prevCheckNode(nodes(routeId), processId);
    }

    /**
     * 前驱中最近的检验节点（基于预载节点列表，批量富化场景按路线复用）。
     * 一期限制：并行检验前驱只取 order_num 最大的一个，即跟单硬拦一期仅保证
     * 串行/单检验前驱路线；存在同序并行检验工序时另一节点 FAIL 不会被发现，
     * 待二期改为「任一检验前驱 FAIL 即拦」。
     */
    public Optional<ProRouteProcess> prevCheckNode(List<ProRouteProcess> nodes, Long processId) {
        Integer curOrder = currentOrder(nodes, processId);
        if (curOrder == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(n -> n.getOrderNum() != null && n.getOrderNum() < curOrder)
                .filter(n -> "Y".equals(n.getIsCheck()))
                .max(Comparator.comparing(ProRouteProcess::getOrderNum));
    }

    /**
     * 下一波节点：order_num 严格大于当前节点的最小序号上的全部节点
     * （同 order_num 的并行 FS 节点整波返回）；末道工序返回空列表。
     */
    public List<ProRouteProcess> nextWave(Long routeId, Long processId) {
        Integer curOrder = currentOrder(routeId, processId);
        if (curOrder == null) {
            return List.of();
        }
        List<ProRouteProcess> nodes = nodes(routeId);
        Optional<Integer> nextOrder = nodes.stream()
                .map(ProRouteProcess::getOrderNum)
                .filter(o -> o != null && o > curOrder)
                .min(Integer::compareTo);
        if (nextOrder.isEmpty()) {
            return List.of();
        }
        Integer target = nextOrder.get();
        return nodes.stream().filter(n -> target.equals(n.getOrderNum())).collect(Collectors.toList());
    }

    private Integer currentOrder(Long routeId, Long processId) {
        if (routeId == null) {
            return null;
        }
        return currentOrder(nodes(routeId), processId);
    }

    private Integer currentOrder(List<ProRouteProcess> nodes, Long processId) {
        return currentNode(nodes, processId).map(ProRouteProcess::getOrderNum).orElse(null);
    }
}
