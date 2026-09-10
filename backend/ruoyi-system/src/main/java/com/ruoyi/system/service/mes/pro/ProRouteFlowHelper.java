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
        return nodes(routeId).stream()
                .filter(n -> processId.equals(n.getProcessId()))
                .findFirst();
    }

    /** 前驱节点：order_num 严格小于当前节点的最大节点（首道工序返回 empty） */
    public Optional<ProRouteProcess> prevNode(Long routeId, Long processId) {
        Integer curOrder = currentOrder(routeId, processId);
        if (curOrder == null) {
            return Optional.empty();
        }
        return nodes(routeId).stream()
                .filter(n -> n.getOrderNum() != null && n.getOrderNum() < curOrder)
                .max(Comparator.comparing(ProRouteProcess::getOrderNum));
    }

    /** 前驱中最近的检验节点（is_check='Y'），供 Task 4 质检流使用 */
    public Optional<ProRouteProcess> prevCheckNode(Long routeId, Long processId) {
        Integer curOrder = currentOrder(routeId, processId);
        if (curOrder == null) {
            return Optional.empty();
        }
        return nodes(routeId).stream()
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
        Optional<ProRouteProcess> cur = currentNode(routeId, processId);
        return cur.map(ProRouteProcess::getOrderNum).orElse(null);
    }
}
