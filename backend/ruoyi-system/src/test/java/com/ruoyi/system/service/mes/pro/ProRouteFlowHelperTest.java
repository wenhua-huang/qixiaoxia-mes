package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工艺路线工序流 Helper 单元测试：前驱 / 最近检验前驱 / 下一波并行节点。
 *
 * <p>纯内存 mock，不连库；节点用 setter 构造（processId/orderNum/isCheck/linkType）。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
class ProRouteFlowHelperTest {

    @Mock
    private ProRouteProcessMapper mapper;

    @InjectMocks
    private ProRouteFlowHelper helper;

    private ProRouteProcess node(long pid, int order, String check) {
        return node(pid, order, check, "SS");
    }

    private ProRouteProcess node(long pid, int order, String check, String linkType) {
        ProRouteProcess n = new ProRouteProcess();
        n.setProcessId(pid);
        n.setOrderNum(order);
        n.setIsCheck(check);
        n.setLinkType(linkType);
        return n;
    }

    @Test
    @DisplayName("nodes: 透传 mapper 结果（mapper 已按 order_num 升序）")
    void should_return_nodes_when_route_exists() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N"), node(2, 2, "Y"), node(3, 3, "N")));
        assertThat(helper.nodes(9L)).extracting(ProRouteProcess::getProcessId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("currentNode: 按 processId 定位当前节点")
    void should_return_current_node_when_process_in_route() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N"), node(2, 2, "Y")));
        assertThat(helper.currentNode(9L, 2L)).get()
                .extracting(ProRouteProcess::getProcessId).isEqualTo(2L);
    }

    @Test
    @DisplayName("prevNode: 取 order_num 严格更小的最大节点")
    void should_return_greatest_smaller_when_prevNode() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N"), node(2, 2, "Y"), node(3, 3, "N")));
        assertThat(helper.prevNode(9L, 3L)).get()
                .extracting(ProRouteProcess::getProcessId).isEqualTo(2L);
    }

    @Test
    @DisplayName("prevNode: 首道工序无前驱返回 empty")
    void should_return_empty_when_first_node() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N"), node(2, 2, "Y")));
        assertThat(helper.prevNode(9L, 1L)).isEmpty();
    }

    @Test
    @DisplayName("prevCheckNode: 跳过非检验节点找到最近的检验节点")
    void should_return_nearest_check_when_prevCheckNode() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "Y"), node(2, 2, "N"), node(3, 3, "N")));
        assertThat(helper.prevCheckNode(9L, 3L)).get()
                .extracting(ProRouteProcess::getProcessId).isEqualTo(1L);
    }

    @Test
    @DisplayName("prevCheckNode: 前驱均非检验节点时返回 empty")
    void should_return_empty_when_no_prev_check_node() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(2, 2, "N"), node(3, 3, "N")));
        assertThat(helper.prevCheckNode(9L, 3L)).isEmpty();
    }

    @Test
    @DisplayName("nextWave: 返回下一序号的全部并行节点（同 order_num 全含）")
    void should_return_whole_next_wave_when_parallel() {
        // 1(SS,order1), 2/3(FS 同 order2), 4(order3)；current=1 → [2,3]
        when(mapper.selectProRouteProcessByRouteId(9L)).thenReturn(List.of(
                node(1, 1, "N", "SS"),
                node(2, 2, "N", "FS"),
                node(3, 2, "N", "FS"),
                node(4, 3, "N", "SS")));
        assertThat(helper.nextWave(9L, 1L)).extracting(ProRouteProcess::getProcessId)
                .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("nextWave: 末道工序无后继波返回空列表")
    void should_return_empty_when_last_node() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N"), node(2, 2, "N")));
        assertThat(helper.nextWave(9L, 2L)).isEmpty();
    }

    @Test
    @DisplayName("currentNode: 工序不在路线上返回 empty")
    void should_return_empty_when_process_not_in_route() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N")));
        assertThat(helper.currentNode(9L, 99L)).isEmpty();
        assertThat(helper.prevNode(9L, 99L)).isEmpty();
    }

    @Test
    @DisplayName("currentNode(预载列表): 直接基于传入列表定位，不查 mapper")
    void should_locate_current_node_from_preloaded_nodes() {
        List<ProRouteProcess> nodes = List.of(node(1, 1, "N"), node(2, 2, "Y"));
        assertThat(helper.currentNode(nodes, 2L)).get()
                .extracting(ProRouteProcess::getProcessId).isEqualTo(2L);
        assertThat(helper.currentNode(nodes, 99L)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("prevNode(预载列表): 取严格更小序号的最大节点；首道返回 empty")
    void should_locate_prev_node_from_preloaded_nodes() {
        List<ProRouteProcess> nodes = List.of(node(1, 1, "N"), node(2, 2, "Y"), node(3, 3, "N"));
        assertThat(helper.prevNode(nodes, 3L)).get()
                .extracting(ProRouteProcess::getProcessId).isEqualTo(2L);
        assertThat(helper.prevNode(nodes, 1L)).isEmpty();
        assertThat(helper.prevNode(nodes, 99L)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("prevNode(routeId): 单次定位内部只查一次 mapper（旧实现查 2 次）")
    void should_query_mapper_only_once_for_prevNode() {
        when(mapper.selectProRouteProcessByRouteId(9L))
                .thenReturn(List.of(node(1, 1, "N"), node(2, 2, "Y"), node(3, 3, "N")));
        assertThat(helper.prevNode(9L, 3L)).isPresent();
        verify(mapper, times(1)).selectProRouteProcessByRouteId(9L);
    }
}
