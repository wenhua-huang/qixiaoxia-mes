package com.ruoyi.system.service.mes.pur;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
import com.ruoyi.system.service.mes.pur.impl.PurOrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 采购单 Service 单元测试
 * 覆盖：级联删除 + 状态流转（审批/下单/关闭）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("采购单服务单元测试")
class PurOrderServiceUnitTest {

    @Mock private PurOrderMapper purOrderMapper;
    @Mock private IPurOrderLineService purOrderLineService;
    @InjectMocks private PurOrderServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("1. 级联删除：单个采购单先删行再删头")
    void testCascadeDeleteSingle() {
        when(purOrderLineService.deletePurOrderLineByOrderId(1L)).thenReturn(1);
        when(purOrderMapper.deletePurOrderByOrderId(1L)).thenReturn(1);

        service.deletePurOrderByOrderId(1L);

        // 验证：先删行，后删头
        var inOrder = inOrder(purOrderLineService, purOrderMapper);
        inOrder.verify(purOrderLineService).deletePurOrderLineByOrderId(1L);
        inOrder.verify(purOrderMapper).deletePurOrderByOrderId(1L);
    }

    @Test
    @DisplayName("2. 级联删除：批量删除，每个订单都先删行再删头")
    void testCascadeDeleteBatch() {
        Long[] ids = {1L, 2L, 3L};
        when(purOrderLineService.deletePurOrderLineByOrderId(anyLong())).thenReturn(1);
        when(purOrderMapper.deletePurOrderByOrderIds(ids)).thenReturn(3);

        service.deletePurOrderByOrderIds(ids);

        // 每张订单都调用了 deletePurOrderLineByOrderId
        for (Long id : ids) {
            verify(purOrderLineService).deletePurOrderLineByOrderId(id);
        }
        verify(purOrderMapper).deletePurOrderByOrderIds(ids);
    }

    @Test
    @DisplayName("3. 级联删除：即使没有行数据也能正常删除头")
    void testCascadeDeleteWithNoLines() {
        when(purOrderLineService.deletePurOrderLineByOrderId(1L)).thenReturn(0); // 无行可删
        when(purOrderMapper.deletePurOrderByOrderId(1L)).thenReturn(1);

        int result = service.deletePurOrderByOrderId(1L);

        assertThat(result).isEqualTo(1);
        verify(purOrderLineService).deletePurOrderLineByOrderId(1L);
        verify(purOrderMapper).deletePurOrderByOrderId(1L);
    }

    // ──────────── 状态流转测试 ────────────

    @Test
    @DisplayName("4. 审批：DRAFT → APPROVED，自动写入审批人")
    void testApproveFromDraft() {
        PurOrder order = draftOrder();
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        service.approvePurOrder(1L);

        assertThat(order.getStatus()).isEqualTo("APPROVED");
        assertThat(order.getApprover()).isEqualTo("tester");
        verify(purOrderMapper).updatePurOrder(order);
    }

    @Test
    @DisplayName("5. 审批：非DRAFT状态拒绝审批")
    void testApproveRejectsNonDraft() {
        PurOrder order = draftOrder();
        order.setStatus("ORDERED");
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.approvePurOrder(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("草稿状态");
        verify(purOrderMapper, never()).updatePurOrder(any());
    }

    @Test
    @DisplayName("6. 审批：订单不存在抛异常")
    void testApproveOrderNotFound() {
        when(purOrderMapper.selectPurOrderByOrderId(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.approvePurOrder(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("7. 下单：APPROVED → ORDERED，行状态同步更新")
    void testOrderFromApproved() {
        PurOrder order = draftOrder();
        order.setStatus("APPROVED");
        PurOrderLine line = new PurOrderLine();
        line.setLineId(1L);
        line.setOrderId(1L);

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));

        service.orderPurOrder(1L);

        assertThat(order.getStatus()).isEqualTo("ORDERED");
        assertThat(order.getOrderDate()).isNotNull();
        assertThat(line.getStatus()).isEqualTo("ORDERED");
    }

    @Test
    @DisplayName("8. 下单：非APPROVED状态拒绝")
    void testOrderRejectsNonApproved() {
        PurOrder order = draftOrder();
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.orderPurOrder(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已审批");
    }

    @Test
    @DisplayName("9. 关闭：全部收完 → CLOSED")
    void testCloseWhenAllReceived() {
        PurOrder order = draftOrder();
        order.setStatus("RECEIVED");
        PurOrderLine line = new PurOrderLine();
        line.setItemName("测试物料");
        line.setQuantityOrdered(BigDecimal.TEN);
        line.setQuantityReceived(BigDecimal.TEN);  // 全部收完

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        service.closePurOrder(1L);

        assertThat(order.getStatus()).isEqualTo("CLOSED");
        assertThat(line.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("10. 关闭：未收完拒绝关闭")
    void testCloseRejectsWhenNotFullyReceived() {
        PurOrder order = draftOrder();
        order.setStatus("RECEIVED");
        PurOrderLine line = new PurOrderLine();
        line.setItemName("测试物料");
        line.setQuantityOrdered(BigDecimal.TEN);
        line.setQuantityReceived(BigDecimal.ONE);  // 只收了1个

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));

        assertThatThrownBy(() -> service.closePurOrder(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("未收完");
    }

    @Test
    @DisplayName("11. 关闭：quantityOrdered为null时安全处理")
    void testCloseWithNullQuantityOrdered() {
        PurOrder order = draftOrder();
        order.setStatus("RECEIVED");
        PurOrderLine line = new PurOrderLine();
        line.setItemName("测试物料");
        line.setQuantityOrdered(null);  // null → 视为0
        line.setQuantityReceived(BigDecimal.ONE);  // 已收 > 0

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        // 不会NPE，0 >= 0 通过
        service.closePurOrder(1L);
        assertThat(order.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("12. 关闭：非RECEIVED状态拒绝")
    void testCloseRejectsNonReceived() {
        PurOrder order = draftOrder();
        order.setStatus("ORDERED");
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.closePurOrder(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已收货");
    }

    // helper
    private PurOrder draftOrder() {
        PurOrder order = new PurOrder();
        order.setOrderId(1L);
        order.setOrderCode("PO-TEST-001");
        order.setStatus("DRAFT");
        return order;
    }
}
