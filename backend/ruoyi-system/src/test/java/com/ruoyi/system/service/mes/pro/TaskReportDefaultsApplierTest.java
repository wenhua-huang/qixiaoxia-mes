package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报工默认值富化组件单元测试：重点验证批量富化按 routeId 缓存路线节点（消除 N+1），
 * 且结果与逐任务富化完全一致。
 *
 * <p>使用真实 {@link ProRouteFlowHelper} + {@link ProInputQuantityResolver}（仅 mapper 打桩），
 * 端到端验证节点查询次数与默认值。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
class TaskReportDefaultsApplierTest {

    private static final Long WORKORDER_ID = 100L;
    private static final Long ROUTE_9 = 9L;
    private static final Long ROUTE_8 = 8L;
    private static final Long FIRST_PROCESS_ID = 1L;
    private static final Long SECOND_PROCESS_ID = 2L;
    private static final Long OTHER_PROCESS_ID = 3L;

    @Mock
    private ProRouteProcessMapper routeProcessMapper;

    @Mock
    private ProFeedbackMapper feedbackMapper;

    @Mock
    private IProQcBlockService qcBlockService;

    private TaskReportDefaultsApplier applier;

    private ProRouteProcess node(long pid, int order) {
        ProRouteProcess n = new ProRouteProcess();
        n.setProcessId(pid);
        n.setOrderNum(order);
        return n;
    }

    private ProTask task(long routeId, long processId, String quantity) {
        ProTask t = new ProTask();
        t.setWorkorderId(WORKORDER_ID);
        t.setRouteId(routeId);
        t.setProcessId(processId);
        t.setQuantity(new BigDecimal(quantity));
        return t;
    }

    @BeforeEach
    void setUp() {
        ProRouteFlowHelper flow = new ProRouteFlowHelper();
        ReflectionTestUtils.setField(flow, "routeProcessMapper", routeProcessMapper);
        ProInputQuantityResolver resolver = new ProInputQuantityResolver();
        ReflectionTestUtils.setField(resolver, "flow", flow);
        ReflectionTestUtils.setField(resolver, "feedbackMapper", feedbackMapper);
        applier = new TaskReportDefaultsApplier();
        ReflectionTestUtils.setField(applier, "inputResolver", resolver);
        ReflectionTestUtils.setField(applier, "flow", flow);
        ReflectionTestUtils.setField(applier, "qcBlockService", qcBlockService);
    }

    @Test
    @DisplayName("批量富化: 同一路线多任务只查一次节点；首道/非首道结果与单条富化一致")
    void should_load_route_nodes_only_once_per_route_in_batch() {
        when(routeProcessMapper.selectProRouteProcessByRouteId(ROUTE_9))
                .thenReturn(List.of(node(FIRST_PROCESS_ID, 1), node(SECOND_PROCESS_ID, 2)));
        when(routeProcessMapper.selectProRouteProcessByRouteId(ROUTE_8))
                .thenReturn(List.of(node(OTHER_PROCESS_ID, 1)));
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, FIRST_PROCESS_ID))
                .thenReturn(new BigDecimal("500"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, SECOND_PROCESS_ID))
                .thenReturn(new BigDecimal("120"));

        ProTask first = task(ROUTE_9, FIRST_PROCESS_ID, "1000");
        ProTask second = task(ROUTE_9, SECOND_PROCESS_ID, "1000");
        ProTask otherRoute = task(ROUTE_8, OTHER_PROCESS_ID, "300");

        applier.apply(Arrays.asList(first, second, otherRoute, null));

        // 路线 9 两个任务只查一次（旧实现每任务 2~3 次），路线 8 一次
        verify(routeProcessMapper, times(1)).selectProRouteProcessByRouteId(ROUTE_9);
        verify(routeProcessMapper, times(1)).selectProRouteProcessByRouteId(ROUTE_8);
        // 首道=排产数；非首道=上道已审 500 − 本道上机 120 = 380；另一路线首道=300
        assertThat(first.getDefaultQuantityInput()).isEqualByComparingTo("1000");
        assertThat(second.getDefaultQuantityInput()).isEqualByComparingTo("380");
        assertThat(otherRoute.getDefaultQuantityInput()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("单条富化与批量富化结果一致（单条走无缓存原路径）")
    void should_match_batch_result_when_apply_one() {
        when(routeProcessMapper.selectProRouteProcessByRouteId(ROUTE_9))
                .thenReturn(List.of(node(FIRST_PROCESS_ID, 1), node(SECOND_PROCESS_ID, 2)));
        when(feedbackMapper.sumAuditedQuantityFeedback(WORKORDER_ID, FIRST_PROCESS_ID))
                .thenReturn(new BigDecimal("500"));
        when(feedbackMapper.sumQuantityInput(WORKORDER_ID, SECOND_PROCESS_ID))
                .thenReturn(new BigDecimal("120"));

        ProTask second = task(ROUTE_9, SECOND_PROCESS_ID, "1000");
        applier.apply(second);
        assertThat(second.getDefaultQuantityInput()).isEqualByComparingTo("380");
    }
}
