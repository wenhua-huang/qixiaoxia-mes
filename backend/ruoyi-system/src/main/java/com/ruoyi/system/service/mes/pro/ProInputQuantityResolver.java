package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.Optional;

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
 * <p>聚合按 workorderId + processId，cardId 非空时进一步限定同一张流转卡；
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
                                          Long cardId, BigDecimal firstProcessDefault) {
        if (routeId == null || processId == null) {
            return null;
        }
        Optional<ProRouteProcess> current = flow.currentNode(routeId, processId);
        if (current.isEmpty()) {
            return null;
        }
        Optional<ProRouteProcess> prev = flow.prevNode(routeId, processId);
        if (prev.isEmpty()) {
            // 首道工序：默认带出任务排产数量
            return firstProcessDefault;
        }
        BigDecimal produced = nz(feedbackMapper.sumAuditedQuantityFeedback(
                workorderId, prev.get().getProcessId(), cardId));
        BigDecimal used = nz(feedbackMapper.sumQuantityInput(workorderId, processId, cardId));
        return produced.subtract(used).max(BigDecimal.ZERO);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
