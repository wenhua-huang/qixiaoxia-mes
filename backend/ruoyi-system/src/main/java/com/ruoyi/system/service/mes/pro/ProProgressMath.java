package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.vo.ProTaskProgressRow;

/**
 * 工单进度纯计算：完成率、当前阶段、剩余标准工时。
 * 无 Spring 依赖，便于单测。
 *
 * <p>当前工序语义：开工后工单下所有任务均为 PRODUCING 状态，不能靠 status 判断"做到哪道"。
 * 真正的当前工序 = 按路线顺序(order_num)第一道仍有剩余数量的工序。</p>
 *
 * @author qixiaoxia
 * @date 2026-08-23
 */
public final class ProProgressMath
{
    /** 当前阶段：生产中 */
    public static final String STAGE_PRODUCING = ProConstants.TASK_STATUS_PRODUCING;
    /** 当前阶段：待开工 */
    public static final String STAGE_PENDING = "PENDING";
    /** 当前阶段：未排产（工单下无非终态任务，且从未排产） */
    public static final String STAGE_UNSCHEDULED = "UNSCHEDULED";
    /** 当前阶段：待完工（工单生产中，所有任务已完工但工单尚未结转 COMPLETED） */
    public static final String STAGE_PENDING_COMPLETE = "PENDING_COMPLETE";

    private static final Comparator<ProTaskProgressRow> BY_ORDER =
            Comparator.comparingInt((ProTaskProgressRow t) ->
                    t.getOrderNum() == null ? Integer.MAX_VALUE : t.getOrderNum());

    private ProProgressMath()
    {
    }

    /** 完成率：null/0 总数返回 0，否则 HALF_UP 取整。 */
    public static int percent(BigDecimal part, BigDecimal total)
    {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0)
        {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 当前阶段：
     * <ul>
     *   <li>终态工单 → null</li>
     *   <li>有活动任务 → PRODUCING（工单已开工）或 PENDING（待开工）</li>
     *   <li>无活动任务但已排产且工单生产中 → PENDING_COMPLETE（所有任务已完工，待工单结转）</li>
     *   <li>无活动任务且从未排产 → UNSCHEDULED</li>
     * </ul>
     */
    public static String resolveStage(String workorderStatus, List<ProTaskProgressRow> tasks,
            boolean hasScheduledTasks)
    {
        if (isTerminal(workorderStatus))
        {
            return null;
        }
        if (tasks == null || tasks.isEmpty())
        {
            if (hasScheduledTasks
                    && ProConstants.WORKORDER_STATUS_PRODUCING.equals(workorderStatus))
            {
                return STAGE_PENDING_COMPLETE;
            }
            return STAGE_UNSCHEDULED;
        }
        return ProConstants.WORKORDER_STATUS_PRODUCING.equals(workorderStatus)
                ? STAGE_PRODUCING : STAGE_PENDING;
    }

    /**
     * 当前工序名：路线顺序中第一道仍有剩余数量的工序；全部做完返回 null。
     */
    public static String currentProcessName(List<ProTaskProgressRow> tasks)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return null;
        }
        return tasks.stream()
                .filter(t -> remainingQty(t).signum() > 0)
                .min(BY_ORDER)
                .map(ProTaskProgressRow::getProcessName)
                .orElse(null);
    }

    /**
     * 剩余标准工时（秒）。
     * 当前工序（第一道未完工）：工单已开工(PRODUCING)时 setup 视为已发生，只算 unit × 剩余量；
     * 其余未完工工序：setup_duration(分钟)×60 + unit_duration × 剩余数量。
     */
    public static long remainingSeconds(String workorderStatus, List<ProTaskProgressRow> tasks)
    {
        if (tasks == null || tasks.isEmpty())
        {
            return 0L;
        }
        ProTaskProgressRow current = tasks.stream()
                .filter(t -> remainingQty(t).signum() > 0)
                .min(BY_ORDER).orElse(null);
        if (current == null)
        {
            return 0L;
        }
        boolean started = ProConstants.WORKORDER_STATUS_PRODUCING.equals(workorderStatus);
        long secs = taskRemainingSeconds(current, started);
        for (ProTaskProgressRow t : tasks)
        {
            if (t == current || remainingQty(t).signum() <= 0)
            {
                continue;
            }
            secs += taskRemainingSeconds(t, false);
        }
        return secs;
    }

    private static long taskRemainingSeconds(ProTaskProgressRow t, boolean setupIncurred)
    {
        BigDecimal unitMin = t.getUnitDuration() == null ? BigDecimal.ZERO : t.getUnitDuration();
        BigDecimal mins = unitMin.multiply(remainingQty(t));
        if (!setupIncurred)
        {
            int setup = t.getSetupDuration() == null ? 0 : t.getSetupDuration();
            mins = mins.add(BigDecimal.valueOf(setup));
        }
        return mins.multiply(BigDecimal.valueOf(60)).longValue();
    }

    private static BigDecimal remainingQty(ProTaskProgressRow t)
    {
        BigDecimal q = t.getQuantity() == null ? BigDecimal.ZERO : t.getQuantity();
        BigDecimal p = t.getQuantityProduced() == null ? BigDecimal.ZERO : t.getQuantityProduced();
        return q.subtract(p).max(BigDecimal.ZERO);
    }

    private static boolean isTerminal(String status)
    {
        return ProConstants.WORKORDER_STATUS_COMPLETED.equals(status)
                || ProConstants.WORKORDER_STATUS_CANCEL.equals(status);
    }
}
