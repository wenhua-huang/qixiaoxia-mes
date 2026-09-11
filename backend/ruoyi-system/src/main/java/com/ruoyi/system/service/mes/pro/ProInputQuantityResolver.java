package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;

/**
 * 报工「本次上机数量」默认值解析。
 *
 * <ul>
 *   <li>无路线/工序信息或当前工序不在路线上 → null（不默认、不留痕）；</li>
 *   <li>首道工序（无前驱）→ 任务排产数量 firstProcessDefault；</li>
 *   <li>其余工序 → 上一道工序累计已审核(AUDITED)报工产出 − 本工序累计上机数量，差额最小钳 0。</li>
 * </ul>
 *
 * <p>一期聚合按 workorderId + processId，不区分流转卡（与质检门控/放行口径一致，
 * 保证预填默认值与提交时重算值同源、不会产生假的人工修改痕迹）；
 * factory_id 由 FactoryIdInterceptor 自动注入。
 *
 * @author qixiaoxia
 */
@Component
public class ProInputQuantityResolver {

    @Autowired
    private ProRouteFlowHelper flow;

    @Autowired
    private ProFeedbackMapper feedbackMapper;

    /**
     * @param firstProcessDefault 首道工序默认值（调用方传 task.quantity 排产数）
     * @return 系统默认上机数量；null 表示无默认值（调用方不留痕）
     */
    public BigDecimal resolveDefaultInput(Long workorderId, Long routeId, Long processId,
                                          BigDecimal firstProcessDefault) {
        if (routeId == null || processId == null) {
            return null;
        }
        Optional<ProRouteProcess> current = flow.currentNode(routeId, processId);
        if (current.isEmpty()) {
            return null;
        }
        return resolveAfterCurrent(workorderId, processId, firstProcessDefault,
                flow.prevNode(routeId, processId));
    }

    /**
     * 批量富化专用重载：节点列表由 nodesLoader 提供（调用方可按 routeId 缓存，
     * 同一批任务同一路线只查一次库），解析规则与 {@link #resolveDefaultInput} 完全一致。
     *
     * @param nodesLoader 按 routeId 返回已排序节点列表（路线不存在返回空列表）
     */
    public BigDecimal resolveDefaultInput(Long workorderId, Long routeId, Long processId,
                                          BigDecimal firstProcessDefault,
                                          Function<Long, List<ProRouteProcess>> nodesLoader) {
        if (routeId == null || processId == null) {
            return null;
        }
        List<ProRouteProcess> nodes = nodesLoader.apply(routeId);
        Optional<ProRouteProcess> current = flow.currentNode(nodes, processId);
        if (current.isEmpty()) {
            return null;
        }
        return resolveAfterCurrent(workorderId, processId, firstProcessDefault,
                flow.prevNode(nodes, processId));
    }

    /** 当前节点已确认存在后的统一收尾：首道返回排产数，其余返回上道产出−本道上机（钳 0） */
    private BigDecimal resolveAfterCurrent(Long workorderId, Long processId,
                                           BigDecimal firstProcessDefault,
                                           Optional<ProRouteProcess> prev) {
        if (prev.isEmpty()) {
            // 首道工序：默认带出任务排产数量
            return firstProcessDefault;
        }
        BigDecimal produced = nz(feedbackMapper.sumAuditedQuantityFeedback(
                workorderId, prev.get().getProcessId()));
        BigDecimal used = nz(feedbackMapper.sumQuantityInput(workorderId, processId));
        return produced.subtract(used).max(BigDecimal.ZERO);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
