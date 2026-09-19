package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 报工「本次上机数量」默认值解析单元测试。
 *
 * <p>规则：首道工序默认=任务排产数；其余工序=上一道工序累计已审核(AUDITED)报工产出
 * − 本工序累计上机数，差额最小钳 0；无路线/节点信息返回 null（不默认）。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
class ProInputQuantityResolverTest {

    private static final Long WORKORDER_ID = 100L;
    private static final Long ROUTE_ID = 9L;
    private static final Long FIRST_PROCESS_ID = 1L;
    private static final Long PREV_PROCESS_ID = 2L;
    private static final Long CURRENT_PROCESS_ID = 3L;

    @Mock
    private ProRouteFlowHelper flow;

    @Mock
    private ProFeedbackMapper feedbackMapper;

    @InjectMocks
    private ProInputQuantityResolver resolver;

    private ProRouteProcess node(long pid, int order) {
        ProRouteProcess n = new ProRouteProcess();
        n.setProcessId(pid);
        n.setOrderNum(order);
        return n;
    }

    private void stubCurrentAndPrev(boolean firstProcess) {
        lenient().when(flow.currentNode(ROUTE_ID, CURRENT_PROCESS_ID))
                .thenReturn(Optional.of(node(CURRENT_PROCESS_ID, firstProcess ? 1 : 2)));
        if (firstProcess) {
            lenient().when(flow.prevNode(ROUTE_ID, CURRENT_PROCESS_ID)).thenReturn(Optional.empty());
        } else {
            lenient().when(flow.prevNode(ROUTE_ID, CURRENT_PROCESS_ID))
                    .thenReturn(Optional.of(node(PREV_PROCESS_ID, 1)));
        }
    }

    @Test
    @DisplayName("首道工序: 默认值=任务排产数量")
    void should_return_task_quantity_when_first_process() {
        stubCurrentAndPrev(true);
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("非首道: 上工序已审产出500、本工序上机0 → 默认500")
    void should_return_produced_when_no_input_yet() {
        stubCurrentAndPrev(false);
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(new BigDecimal("500"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(BigDecimal.ZERO);
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("非首道: 上工序已审500、本工序已上机480 → 默认剩余20")
    void should_return_remaining_when_some_input() {
        stubCurrentAndPrev(false);
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(new BigDecimal("500"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(new BigDecimal("480"));
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("非首道: 上工序多批报工300+200由SQL聚合为500 → 默认500")
    void should_use_aggregated_produced_when_multiple_batches() {
        stubCurrentAndPrev(false);
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(new BigDecimal("300").add(new BigDecimal("200")));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(BigDecimal.ZERO);
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("非首道: 本工序上机超过上工序产出(差额为负) → 钳为0")
    void should_clamp_to_zero_when_remaining_negative() {
        stubCurrentAndPrev(false);
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(new BigDecimal("300"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(new BigDecimal("500"));
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("当前工序不在路线上 → 返回 null（不默认、不留痕）")
    void should_return_null_when_current_node_missing() {
        when(flow.currentNode(ROUTE_ID, CURRENT_PROCESS_ID)).thenReturn(Optional.empty());
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("无路线/工序信息 → 返回 null")
    void should_return_null_when_route_or_process_missing() {
        assertThat(resolver.resolveDefaultInput(
                WORKORDER_ID, null, CURRENT_PROCESS_ID, new BigDecimal("1000"))).isNull();
        assertThat(resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, null, new BigDecimal("1000"))).isNull();
    }

    @Test
    @DisplayName("工单级生产: 不区分流转卡，按工单+工序聚合")
    void should_work_when_card_null() {
        when(flow.currentNode(ROUTE_ID, CURRENT_PROCESS_ID))
                .thenReturn(Optional.of(node(CURRENT_PROCESS_ID, 2)));
        when(flow.prevNode(ROUTE_ID, CURRENT_PROCESS_ID))
                .thenReturn(Optional.of(node(PREV_PROCESS_ID, 1)));
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(new BigDecimal("120"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(new BigDecimal("20"));
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("预载重载·非首道: 基于 nodesLoader 提供的节点计算且 loader 只调一次")
    void should_resolve_from_preloaded_nodes() {
        List<ProRouteProcess> nodes = List.of(
                node(PREV_PROCESS_ID, 1), node(CURRENT_PROCESS_ID, 2));
        AtomicInteger loaderCalls = new AtomicInteger();
        Function<Long, List<ProRouteProcess>> loader = rid -> {
            loaderCalls.incrementAndGet();
            return rid.equals(ROUTE_ID) ? nodes : List.of();
        };
        when(flow.currentNode(nodes, CURRENT_PROCESS_ID))
                .thenReturn(Optional.of(node(CURRENT_PROCESS_ID, 2)));
        when(flow.prevNode(nodes, CURRENT_PROCESS_ID))
                .thenReturn(Optional.of(node(PREV_PROCESS_ID, 1)));
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(new BigDecimal("500"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(new BigDecimal("120"));
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID,
                new BigDecimal("1000"), loader);
        assertThat(result).isEqualByComparingTo("380");
        assertThat(loaderCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("预载重载·首道: 节点内无前驱 → 直接返回排产数，不查报工聚合")
    void should_return_task_quantity_when_first_process_with_preloaded_nodes() {
        List<ProRouteProcess> nodes = List.of(node(CURRENT_PROCESS_ID, 1));
        Function<Long, List<ProRouteProcess>> loader = rid -> nodes;
        when(flow.currentNode(nodes, CURRENT_PROCESS_ID))
                .thenReturn(Optional.of(node(CURRENT_PROCESS_ID, 1)));
        when(flow.prevNode(nodes, CURRENT_PROCESS_ID)).thenReturn(Optional.empty());
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID,
                new BigDecimal("888"), loader);
        assertThat(result).isEqualByComparingTo("888");
        verifyNoInteractions(feedbackMapper);
    }

    @Test
    @DisplayName("预载重载·当前节点缺失 → null，且不查报工聚合")
    void should_return_null_when_current_node_missing_with_preloaded_nodes() {
        List<ProRouteProcess> nodes = List.of(node(PREV_PROCESS_ID, 1));
        Function<Long, List<ProRouteProcess>> loader = rid -> nodes;
        when(flow.currentNode(nodes, CURRENT_PROCESS_ID)).thenReturn(Optional.empty());
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID,
                new BigDecimal("1000"), loader);
        assertThat(result).isNull();
        verifyNoInteractions(feedbackMapper);
    }

    @Test
    @DisplayName("SQL 返回 null（无报工行）按 0 处理")
    void should_treat_null_sum_as_zero() {
        stubCurrentAndPrev(false);
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, PREV_PROCESS_ID))
                .thenReturn(null);
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, CURRENT_PROCESS_ID))
                .thenReturn(null);
        BigDecimal result = resolver.resolveDefaultInput(
                WORKORDER_ID, ROUTE_ID, CURRENT_PROCESS_ID, new BigDecimal("1000"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
