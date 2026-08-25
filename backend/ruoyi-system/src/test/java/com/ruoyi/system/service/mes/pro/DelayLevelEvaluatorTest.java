package com.ruoyi.system.service.mes.pro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.ruoyi.system.domain.mes.pro.ProConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

class DelayLevelEvaluatorTest {

    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static Date d(String s) throws Exception { return F.parse(s); }

    @ParameterizedTest(name = "[{index}] plan={0}~{1} actual={2}~{3} status={4} => {5}")
    @CsvSource({
        // 未到期：计划结束在未来 24h 之外
        "'2026-08-22 08:00','2026-08-30 18:00',,,'PRODUCING',NORMAL",
        // 临期：计划结束在未来 24h 内
        "'2026-08-22 08:00','2026-08-23 08:00',,,'PRODUCING',WARNING",
        // 延期：计划结束已过、未完成
        "'2026-08-20 08:00','2026-08-21 08:00',,,'PRODUCING',DELAY",
        // 完工按时
        "'2026-08-20 08:00','2026-08-22 08:00','2026-08-20 09:00','2026-08-22 07:00','COMPLETED',NORMAL",
        // 完工延期
        "'2026-08-20 08:00','2026-08-22 08:00','2026-08-20 09:00','2026-08-23 07:00','COMPLETED',FINISHED_DELAY",
        // 取消不判延期
        "'2026-08-20 08:00','2026-08-21 08:00',,,'CANCEL',NORMAL",
    })
    void task_levels(String ps, String pe, String as, String ae, String status, String expected) throws Exception {
        String level = DelayLevelEvaluator.evaluateTask(
            d(ps), d(pe), as==null?null:d(as), ae==null?null:d(ae),
            status, new BigDecimal("100"), new BigDecimal("50"), 24, 10, d("2026-08-22 12:00"));
        assertThat(level).isEqualTo(expected);
    }

    @ParameterizedTest(name = "workorder req={0} finish={1} status={2} => {3}")
    @CsvSource({
        "'2026-08-30',,'PRODUCING',NORMAL",
        "'2026-08-23',,'PRODUCING',WARNING",   // 交期在2天内
        "'2026-08-20',,'PRODUCING',DELAY",      // 已过交期
        "'2026-08-20','2026-08-20','COMPLETED',NORMAL",       // 按时完工（交期内完工）
        "'2026-08-20','2026-08-25','COMPLETED',FINISHED_DELAY", // 超交期完工
        "'2026-08-20',,'CANCEL',NORMAL",
    })
    void workorder_levels(String req, String fin, String status, String expected) throws Exception {
        String level = DelayLevelEvaluator.evaluateWorkorder(
            d(req+" 00:00"), fin==null?null:d(fin+" 00:00"), status, 2, d("2026-08-22 12:00"));
        assertThat(level).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("进度滞后：产出进度比时间进度低超容差")
    void behind_schedule() throws Exception {
        // 计划 100h 的任务已过 80h（时间进度80%），只产出 50%（容差10，80-50=30>10）=> BEHIND
        // 为简化用日期差与数量：planStart 到 now 占 (end-start) 的 80%，produced/quantity=50%
        String level = DelayLevelEvaluator.evaluateTask(
            d("2026-08-01 00:00"), d("2026-08-11 00:00"), // 10 天
            d("2026-08-01 08:00"), null, "PRODUCING",
            new BigDecimal("100"), new BigDecimal("50"), 24, 10,
            d("2026-08-09 00:00")); // 过了 8 天 = 80%
        assertThat(level).isEqualTo(DELAY_BEHIND);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("未排产任务(planEnd=null) 不判延期/滞后，返回 NORMAL")
    void unscheduled_task_is_normal() throws Exception {
        String level = DelayLevelEvaluator.evaluateTask(
            d("2026-08-01 00:00"), null, d("2026-08-01 08:00"), null,
            TASK_STATUS_PRODUCING, new BigDecimal("100"), BigDecimal.ZERO, 24, 10,
            d("2026-08-09 00:00"));
        assertThat(level).isEqualTo(DELAY_NORMAL);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("非生产中状态(PREPARE) 即使进度落后也不判 BEHIND")
    void non_producing_does_not_trigger_behind() throws Exception {
        String level = DelayLevelEvaluator.evaluateTask(
            d("2026-08-01 00:00"), d("2026-08-11 00:00"), d("2026-08-01 08:00"), null,
            TASK_STATUS_PREPARE, new BigDecimal("100"), new BigDecimal("0"), 24, 10,
            d("2026-08-09 00:00"));
        assertThat(level).isEqualTo(DELAY_NORMAL);
    }
}
