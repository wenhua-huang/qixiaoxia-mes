package com.ruoyi.system.service.mes.pur;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 采购单 Service 单元测试
 * 覆盖：级联删除 + 状态流转（审批/下单/关闭/取消/终止收货/强制关闭）
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

        for (Long id : ids) {
            verify(purOrderLineService).deletePurOrderLineByOrderId(id);
        }
        verify(purOrderMapper).deletePurOrderByOrderIds(ids);
    }

    @Test
    @DisplayName("3. 级联删除：即使没有行数据也能正常删除头")
    void testCascadeDeleteWithNoLines() {
        when(purOrderLineService.deletePurOrderLineByOrderId(1L)).thenReturn(0);
        when(purOrderMapper.deletePurOrderByOrderId(1L)).thenReturn(1);

        int result = service.deletePurOrderByOrderId(1L);

        assertThat(result).isEqualTo(1);
        verify(purOrderLineService).deletePurOrderLineByOrderId(1L);
        verify(purOrderMapper).deletePurOrderByOrderId(1L);
    }

    // ──────────── 审批/下单测试 ────────────

    @Test
    @DisplayName("4. 审批：DRAFT -> APPROVED，自动写入审批人")
    void testApproveFromDraft() {
        PurOrderVO order = draftOrder();
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
        PurOrderVO order = draftOrder();
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
    @DisplayName("7. 下单：APPROVED -> ORDERED，行状态同步更新")
    void testOrderFromApproved() {
        PurOrderVO order = draftOrder();
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
        PurOrderVO order = draftOrder();
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.orderPurOrder(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已审批");
    }

    // ──────────── 关闭/强制关闭测试 ────────────

    @Test
    @DisplayName("9. 正常关闭：RECEIVED + 全部RECEIVED行 -> CLOSED")
    void testCloseWhenAllReceived() {
        PurOrderVO order = draftOrder();
        order.setStatus("RECEIVED");
        PurOrderLine line = newLine("物料A", "RECEIVED", BigDecimal.TEN, BigDecimal.TEN);

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        service.closePurOrder(1L, null);

        assertThat(order.getStatus()).isEqualTo("CLOSED");
        assertThat(line.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("10. 强制关闭：RECEIVING + 全行终态 + 有原因 -> CLOSED")
    void testForceCloseFromReceiving() {
        PurOrderVO order = draftOrder();
        order.setStatus("RECEIVING");
        PurOrderLine line1 = newLine("物料A", "RECEIVED", BigDecimal.TEN, BigDecimal.TEN);
        PurOrderLine line2 = newLine("物料B", "CLOSED", BigDecimal.ONE, BigDecimal.TEN); // 终止收货行

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(List.of(line1, line2));
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        service.closePurOrder(1L, "SUPPLIER_NO_MORE");

        assertThat(order.getStatus()).isEqualTo("CLOSED");
        assertThat(order.getCloseReason()).isEqualTo("SUPPLIER_NO_MORE");
        assertThat(line1.getStatus()).isEqualTo("CLOSED");
        // line2 已经是 CLOSED，不会被改
        assertThat(line2.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("11. 强制关闭：有终止行但无关闭原因 -> 拒绝")
    void testForceCloseRejectsWithoutReason() {
        PurOrderVO order = draftOrder();
        order.setStatus("RECEIVING");
        PurOrderLine line1 = newLine("物料A", "RECEIVED", BigDecimal.TEN, BigDecimal.TEN);
        PurOrderLine line2 = newLine("物料B", "CLOSED", BigDecimal.ONE, BigDecimal.TEN);

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(List.of(line1, line2));

        assertThatThrownBy(() -> service.closePurOrder(1L, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("关闭原因");
    }

    @Test
    @DisplayName("12. 关闭：存在非终态行 -> 拒绝")
    void testCloseRejectsWhenLineNotTerminal() {
        PurOrderVO order = draftOrder();
        order.setStatus("RECEIVING");
        PurOrderLine line = newLine("物料A", "RECEIVING", BigDecimal.ONE, BigDecimal.TEN);

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));

        assertThatThrownBy(() -> service.closePurOrder(1L, "DEMAND_CHANGE"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("未完成");
    }

    @Test
    @DisplayName("13. 关闭：非RECEIVED/RECEIVING状态拒绝")
    void testCloseRejectsNonReceived() {
        PurOrderVO order = draftOrder();
        order.setStatus("ORDERED");
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.closePurOrder(1L, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已收货");
    }

    // ──────────── 取消测试 ────────────

    @Test
    @DisplayName("14. 取消订单：ORDERED + 全行未收 -> CANCEL")
    void testCancelOrderFromOrdered() {
        PurOrderVO order = draftOrder();
        order.setStatus("ORDERED");
        PurOrderLine line = newLine("物料A", "ORDERED", BigDecimal.ZERO, BigDecimal.TEN);

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        service.cancelPurOrder(1L, "SUPPLIER_STOP");

        assertThat(order.getStatus()).isEqualTo("CANCEL");
        assertThat(order.getCancelReason()).isEqualTo("SUPPLIER_STOP");
        assertThat(order.getCancelBy()).isEqualTo("tester");
        assertThat(line.getStatus()).isEqualTo("CANCEL");
    }

    @Test
    @DisplayName("15. 取消订单：有收货记录 -> 拒绝")
    void testCancelOrderRejectsWhenReceived() {
        PurOrderVO order = draftOrder();
        order.setStatus("ORDERED");
        PurOrderLine line = newLine("物料A", "RECEIVING", BigDecimal.ONE, BigDecimal.TEN);

        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));

        assertThatThrownBy(() -> service.cancelPurOrder(1L, "SUPPLIER_STOP"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("收货记录");
    }

    @Test
    @DisplayName("16. 取消订单：RECEIVING状态拒绝（已有收货）")
    void testCancelOrderRejectsReceivingStatus() {
        PurOrderVO order = draftOrder();
        order.setStatus("RECEIVING");
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.cancelPurOrder(1L, "DEMAND_CHANGE"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("草稿");
    }

    // ──────────── 行级取消测试 ────────────

    @Test
    @DisplayName("17. 取消行：ORDERED + 未收 -> CANCEL，全行取消时头自动转CANCEL")
    void testCancelLineFromOrdered() {
        PurOrderLine line = newLine("物料A", "ORDERED", BigDecimal.ZERO, BigDecimal.TEN);
        line.setLineId(10L);
        line.setOrderId(1L);

        PurOrderVO order = draftOrder();
        order.setStatus("ORDERED");

        when(purOrderLineService.selectPurOrderLineByLineId(10L)).thenReturn(line);
        when(purOrderLineService.updatePurOrderLine(any())).thenReturn(1);
        // 联动检查：只有一行且已取消，头应自动转CANCEL
        when(purOrderMapper.selectPurOrderByOrderId(1L)).thenReturn(order);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(line));
        when(purOrderMapper.updatePurOrder(any(PurOrder.class))).thenReturn(1);

        service.cancelPurOrderLine(10L, "DEMAND_CHANGE");

        assertThat(line.getStatus()).isEqualTo("CANCEL");
        assertThat(line.getCancelReason()).isEqualTo("DEMAND_CHANGE");
        // 头自动转CANCEL（全行取消且全部qtyReceived==0）
        assertThat(order.getStatus()).isEqualTo("CANCEL");
    }

    @Test
    @DisplayName("18. 取消行：有收货记录 -> 拒绝")
    void testCancelLineRejectsWhenReceived() {
        PurOrderLine line = newLine("物料A", "RECEIVING", BigDecimal.ONE, BigDecimal.TEN);
        line.setLineId(10L);

        when(purOrderLineService.selectPurOrderLineByLineId(10L)).thenReturn(line);

        assertThatThrownBy(() -> service.cancelPurOrderLine(10L, "DEMAND_CHANGE"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("收货记录");
    }

    // ──────────── 终止收货测试 ────────────

    @Test
    @DisplayName("19. 终止收货：RECEIVING + 部分收货 -> CLOSED")
    void testTerminateLineFromReceiving() {
        PurOrderLine line = newLine("物料A", "RECEIVING", BigDecimal.ONE, BigDecimal.TEN);
        line.setLineId(10L);

        when(purOrderLineService.selectPurOrderLineByLineId(10L)).thenReturn(line);
        when(purOrderLineService.updatePurOrderLine(any())).thenReturn(1);

        service.terminatePurOrderLine(10L, "SUPPLIER_NO_MORE");

        assertThat(line.getStatus()).isEqualTo("CLOSED");
        assertThat(line.getCancelReason()).isEqualTo("SUPPLIER_NO_MORE");
        assertThat(line.getCloseTime()).isNotNull();
    }

    @Test
    @DisplayName("20. 终止收货：未收货 -> 拒绝（应改用取消）")
    void testTerminateLineRejectsWhenNoReceived() {
        PurOrderLine line = newLine("物料A", "RECEIVING", BigDecimal.ZERO, BigDecimal.TEN);
        line.setLineId(10L);

        when(purOrderLineService.selectPurOrderLineByLineId(10L)).thenReturn(line);

        assertThatThrownBy(() -> service.terminatePurOrderLine(10L, "SUPPLIER_NO_MORE"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("没有收货记录");
    }

    @Test
    @DisplayName("21. 终止收货：已全部收完 -> 拒绝")
    void testTerminateLineRejectsWhenFullyReceived() {
        PurOrderLine line = newLine("物料A", "RECEIVING", BigDecimal.TEN, BigDecimal.TEN);
        line.setLineId(10L);

        when(purOrderLineService.selectPurOrderLineByLineId(10L)).thenReturn(line);

        assertThatThrownBy(() -> service.terminatePurOrderLine(10L, "SUPPLIER_NO_MORE"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已全部收完");
    }

    // ──────────── 新增测试 ────────────

    @Test
    @DisplayName("22. 新增：采购人自动填充为当前登录用户")
    void testInsertSetsPurchaserDefault() {
        PurOrder order = new PurOrder();
        order.setOrderCode("PO-TEST-INSERT");
        order.setPurchaser(null);

        when(purOrderMapper.insertPurOrder(any(PurOrder.class))).thenReturn(1);

        service.insertPurOrder(order);

        assertThat(order.getPurchaser()).isEqualTo("tester");
        verify(purOrderMapper).insertPurOrder(order);
    }

    @Test
    @DisplayName("23. 新增：采购人已指定则不覆盖")
    void testInsertKeepsExistingPurchaser() {
        PurOrder order = new PurOrder();
        order.setOrderCode("PO-TEST-INSERT");
        order.setPurchaser("张三");

        when(purOrderMapper.insertPurOrder(any(PurOrder.class))).thenReturn(1);

        service.insertPurOrder(order);

        assertThat(order.getPurchaser()).isEqualTo("张三");
        verify(purOrderMapper).insertPurOrder(order);
    }

    // ──────────── helpers ────────────

    private PurOrderVO draftOrder() {
        PurOrderVO order = new PurOrderVO();
        order.setOrderId(1L);
        order.setOrderCode("PO-TEST-001");
        order.setStatus("DRAFT");
        return order;
    }

    private PurOrderLine newLine(String name, String status, BigDecimal received, BigDecimal ordered) {
        PurOrderLine line = new PurOrderLine();
        line.setItemName(name);
        line.setStatus(status);
        line.setQuantityReceived(received);
        line.setQuantityOrdered(ordered);
        return line;
    }
}
