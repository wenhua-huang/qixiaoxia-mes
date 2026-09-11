package com.ruoyi.system.service.mes.pro;

import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.qc.QcBlockInfo;
import com.ruoyi.system.domain.mes.qc.QcIpqc;

/**
 * 跟单质检不合格硬拦服务：上道检验工序 FAIL 且无放行记录时，拦截下道工序报工；
 * 授权角色可放行（留痕 + 联动待办）。
 *
 * @author qixiaoxia
 */
public interface IProQcBlockService
{
    /**
     * 定位当前（工单+工序+任务）的质检阻塞。一期按工单+工序粒度判定，不区分流转卡
     * （放行同样按任务维度，一条放行对该工单该工序各卡生效）。
     *
     * @return 阻塞信息；null 表示可报工（无检验前驱 / 未判定 / PASS / CONCESSION / 已放行）
     */
    QcBlockInfo findBlock(Long workorderId, Long routeId, Long processId, Long taskId);

    /**
     * 批量富化专用：基于预载路线节点定位阻塞（同一路线只查一次节点，避免 N+1）。
     */
    QcBlockInfo findBlock(Long workorderId, Long processId, Long taskId,
                          List<ProRouteProcess> routeNodes);

    /** 报工提交硬门控：命中阻塞抛 {@link com.ruoyi.common.exception.ServiceException} */
    void assertReportable(ProFeedback feedback);

    /**
     * 授权放行：校验理由 → 任务维度加锁 → 锁内事务（定位阻塞 → 插放行记录 → 关闭拦截待办）。
     */
    void release(Long taskId, String reason);

    /**
     * IPQC 判定 FAIL 后置：对下一波并行工序的非终态任务逐一生成 PRO_QC_BLOCK 待办（幂等）。
     * 单条待办失败只告警不阻断判定；返回被拦工序名（去重，无任务也返回工序名）。
     */
    List<String> onIpqcFailed(QcIpqc ipqc);

    /** 仅计算被拦工序名（不建待办），供查询/提示场景 */
    List<String> blockedProcessNames(QcIpqc ipqc);

    /**
     * 批量查询任务锁态（Task5 PC 放行工作台用）。
     *
     * @return key=任务ID字符串，value={blocked:boolean, reason:string}
     */
    Map<String, Map<String, Object>> qcBlockState(List<Long> taskIds);
}
