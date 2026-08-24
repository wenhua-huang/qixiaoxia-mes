package com.ruoyi.system.service.mes.pro;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ruoyi.system.domain.mes.pro.vo.ProTaskProgressRow;

class ProProgressMathTest
{
    private ProTaskProgressRow task(String status, int orderNum,
            String qty, String produced, int setupMin, String unitMin)
    {
        ProTaskProgressRow t = new ProTaskProgressRow();
        t.setStatus(status);
        t.setOrderNum(orderNum);
        t.setProcessName("工序" + orderNum);
        t.setQuantity(new BigDecimal(qty));
        t.setQuantityProduced(produced == null ? null : new BigDecimal(produced));
        t.setSetupDuration(setupMin);
        t.setUnitDuration(new BigDecimal(unitMin));
        return t;
    }

    @Test
    void percent_normal_rounds_half_up()
    {
        assertThat(ProProgressMath.percent(new BigDecimal("3"), new BigDecimal("10"))).isEqualTo(30);
        assertThat(ProProgressMath.percent(new BigDecimal("1"), new BigDecimal("3"))).isEqualTo(33);
        assertThat(ProProgressMath.percent(new BigDecimal("2"), new BigDecimal("3"))).isEqualTo(67);
    }

    @Test
    void percent_null_or_zero_total_returns_0()
    {
        assertThat(ProProgressMath.percent(null, new BigDecimal("10"))).isZero();
        assertThat(ProProgressMath.percent(new BigDecimal("3"), null)).isZero();
        assertThat(ProProgressMath.percent(new BigDecimal("3"), BigDecimal.ZERO)).isZero();
    }

    @Test
    void remainingSeconds_started_current_task_excludes_setup()
    {
        // 工单 PRODUCING，当前(第一道)工序：unit 2 分/个 × 剩余 6 个 = 12 分 = 720 秒（setup 已发生）
        ProTaskProgressRow cur = task("PRODUCING", 1, "10", "4", 30, "2");
        assertThat(ProProgressMath.remainingSeconds("PRODUCING", List.of(cur))).isEqualTo(720L);
    }

    @Test
    void remainingSeconds_not_started_first_task_includes_setup()
    {
        // 工单 PREPARE 未开工，第一道也需 setup：30 + 2×10 = 50 分 = 3000 秒
        ProTaskProgressRow cur = task("NORMAL", 1, "10", "0", 30, "2");
        assertThat(ProProgressMath.remainingSeconds("PREPARE", List.of(cur))).isEqualTo(3000L);
    }

    @Test
    void remainingSeconds_later_tasks_always_include_setup()
    {
        // 已开工，当前工序1剩6个(无setup)：2×6=12分；后续工序2未做：setup10 + 1×5=15分 → 共 27 分 = 1620 秒
        ProTaskProgressRow cur = task("PRODUCING", 1, "10", "4", 30, "2");
        ProTaskProgressRow next = task("PRODUCING", 2, "5", "0", 10, "1");
        assertThat(ProProgressMath.remainingSeconds("PRODUCING", List.of(cur, next))).isEqualTo(1620L);
    }

    @Test
    void remainingSeconds_all_done_returns_0()
    {
        ProTaskProgressRow done = task("PRODUCING", 1, "10", "10", 30, "2");
        assertThat(ProProgressMath.remainingSeconds("PRODUCING", List.of(done))).isZero();
    }

    @Test
    void remainingSeconds_empty_returns_0()
    {
        assertThat(ProProgressMath.remainingSeconds("PRODUCING", List.of())).isZero();
        assertThat(ProProgressMath.remainingSeconds("PRODUCING", null)).isZero();
    }

    @Test
    void currentProcessName_picks_earliest_unfinished_ignoring_status()
    {
        // 开工后所有任务都是 PRODUCING；工序1已报满，工序2做了一半 → 当前应是工序2
        ProTaskProgressRow t1 = task("PRODUCING", 1, "10", "10", 0, "1");
        ProTaskProgressRow t2 = task("PRODUCING", 2, "10", "3", 0, "1");
        t2.setProcessName("贴绳");
        ProTaskProgressRow t3 = task("PRODUCING", 3, "10", "0", 0, "1");
        assertThat(ProProgressMath.currentProcessName(List.of(t1, t2, t3))).isEqualTo("贴绳");
    }

    @Test
    void currentProcessName_all_done_returns_null()
    {
        ProTaskProgressRow done = task("PRODUCING", 1, "10", "10", 0, "1");
        assertThat(ProProgressMath.currentProcessName(List.of(done))).isNull();
    }

    @Test
    void currentProcessName_empty_returns_null()
    {
        assertThat(ProProgressMath.currentProcessName(List.of())).isNull();
    }

    @Test
    void stage_terminal_returns_null()
    {
        ProTaskProgressRow t = task("COMPLETED", 1, "10", "10", 0, "1");
        assertThat(ProProgressMath.resolveStage("COMPLETED", List.of(t), true)).isNull();
        assertThat(ProProgressMath.resolveStage("CANCEL", List.of(), false)).isNull();
    }

    @Test
    void stage_no_tasks_unscheduled_when_never_scheduled()
    {
        assertThat(ProProgressMath.resolveStage("PRODUCING", List.of(), false))
                .isEqualTo(ProProgressMath.STAGE_UNSCHEDULED);
        assertThat(ProProgressMath.resolveStage("PREPARE", List.of(), false))
                .isEqualTo(ProProgressMath.STAGE_UNSCHEDULED);
    }

    @Test
    void stage_pending_complete_when_producing_but_all_tasks_done()
    {
        // 工单 PRODUCING、活动任务为空，但工序步骤存在（任务全部 COMPLETED）→ 待完工
        assertThat(ProProgressMath.resolveStage("PRODUCING", List.of(), true))
                .isEqualTo(ProProgressMath.STAGE_PENDING_COMPLETE);
        // 非 PRODUCING（如 PREPARE）即使已排产但无活动任务，仍为未排产
        assertThat(ProProgressMath.resolveStage("PREPARE", List.of(), true))
                .isEqualTo(ProProgressMath.STAGE_UNSCHEDULED);
    }

    @Test
    void stage_producing_when_workorder_producing()
    {
        ProTaskProgressRow t = task("NORMAL", 1, "10", "0", 0, "1");
        assertThat(ProProgressMath.resolveStage("PRODUCING", List.of(t), true))
                .isEqualTo(ProProgressMath.STAGE_PRODUCING);
    }

    @Test
    void stage_pending_when_workorder_prepare()
    {
        ProTaskProgressRow t = task("NORMAL", 1, "10", "0", 0, "1");
        assertThat(ProProgressMath.resolveStage("PREPARE", List.of(t), true))
                .isEqualTo(ProProgressMath.STAGE_PENDING);
    }
}
