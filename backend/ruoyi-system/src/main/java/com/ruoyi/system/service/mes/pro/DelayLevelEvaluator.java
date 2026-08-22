package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static com.ruoyi.system.domain.mes.pro.ProConstants.*;

/** 延期风险等级判定纯函数，无 Spring 依赖，便于单测。 */
public final class DelayLevelEvaluator {

    private DelayLevelEvaluator() {}

    public static String evaluateWorkorder(Date requestDate, Date finishDate, String status,
                                          Integer warnDays, Date now) {
        if (WORKORDER_STATUS_CANCEL.equals(status)) return DELAY_NORMAL;
        Date ref = now == null ? new Date() : now;
        if (WORKORDER_STATUS_COMPLETED.equals(status)) {
            if (requestDate != null && finishDate != null && finishDate.after(requestDate)) {
                return DELAY_FINISHED_LATE;
            }
            return DELAY_NORMAL;
        }
        if (requestDate == null) return DELAY_NORMAL;
        long warnMs = (warnDays == null ? 2 : warnDays) * 24L * 3600_000L;
        if (requestDate.getTime() < ref.getTime()) return DELAY_OVERDUE;
        if (requestDate.getTime() - ref.getTime() <= warnMs) return DELAY_WARNING;
        return DELAY_NORMAL;
    }

    public static String evaluateTask(Date planStart, Date planEnd, Date actualStart, Date actualEnd,
                                      String status, BigDecimal quantity, BigDecimal quantityProduced,
                                      Integer warnHours, Integer behindTolerance, Date now) {
        if (TASK_STATUS_CANCEL.equals(status)) return DELAY_NORMAL;
        Date ref = now == null ? new Date() : now;
        if (TASK_STATUS_COMPLETED.equals(status)) {
            if (planEnd != null && actualEnd != null && actualEnd.after(planEnd)) return DELAY_FINISHED_LATE;
            return DELAY_NORMAL;
        }
        if (planEnd == null) return DELAY_NORMAL; // 未排产不判延期
        long warnMs = (warnHours == null ? 24 : warnHours) * 3600_000L;
        if (planEnd.getTime() < ref.getTime()) return DELAY_OVERDUE;
        if (planEnd.getTime() - ref.getTime() <= warnMs) return DELAY_WARNING;
        // 进行中且进度滞后
        if (TASK_STATUS_PRODUCING.equals(status) && planStart != null && actualStart != null
                && quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0
                && quantityProduced != null) {
            long total = planEnd.getTime() - planStart.getTime();
            long elapsed = ref.getTime() - planStart.getTime();
            if (total > 0 && elapsed > 0) {
                int timePct = (int) Math.min(100, elapsed * 100 / total);
                int prodPct = quantityProduced.multiply(BigDecimal.valueOf(100))
                    .divide(quantity, 0, RoundingMode.HALF_UP).intValue();
                int tol = behindTolerance == null ? 10 : behindTolerance;
                if (prodPct < timePct - tol) return DELAY_BEHIND;
            }
        }
        return DELAY_NORMAL;
    }
}
