package com.ruoyi.system.service.mes.pro;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.system.domain.mes.pro.ProTask;

/**
 * 报工入口任务默认值富化：为可报工任务回填「本次上机数量」系统默认值。
 *
 * <p>三个报工入口（待报工列表 / 工单报工入口 / 扫码反查）统一走本组件，
 * 默认值解析规则见 {@link ProInputQuantityResolver}。
 *
 * <p>本组件只富化 defaultQuantityInput；质检阻塞态（qcBlocked/qcBlockReason）
 * 由后续任务追加，此处不引用任何 QC 服务。
 *
 * @author qixiaoxia
 */
@Component
public class TaskReportDefaultsApplier {

    @Autowired
    private ProInputQuantityResolver inputResolver;

    /** 批量富化；tasks 为 null 时安全跳过 */
    public void apply(Collection<ProTask> tasks, Long cardId) {
        if (tasks == null) {
            return;
        }
        tasks.forEach(t -> apply(t, cardId));
    }

    /** 富化单个任务：首道工序默认排产数量，其余按上道已审产出差额 */
    public void apply(ProTask task, Long cardId) {
        if (task == null) {
            return;
        }
        task.setDefaultQuantityInput(inputResolver.resolveDefaultInput(
                task.getWorkorderId(), task.getRouteId(), task.getProcessId(),
                cardId, task.getQuantity()));
    }
}
