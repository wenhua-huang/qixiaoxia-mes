package com.ruoyi.system.service.mes.wm;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.wm.impl.WmItemRecptServiceImpl;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 入库单收货确认 + 过账 单元测试
 * 覆盖：confirmItemRecpt / postItemRecpt + PO回写逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("入库单收货确认单元测试")
class WmItemRecptServiceUnitTest {

    @Mock private WmItemRecptMapper wmItemRecptMapper;
    @Mock private IWmItemRecptLineService wmItemRecptLineService;
    @Mock private IPurOrderService purOrderService;
    @Mock private IPurOrderLineService purOrderLineService;
    @Mock private IWmStorageCoreService storageCoreService;
    @InjectMocks private WmItemRecptServiceImpl service;

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
    @DisplayName("1. 确认收货：DRAFT → CONFIRMED，库存更新+PO回写在同一事务")
    void testConfirmItemRecpt() {
        WmItemRecpt header = draftRecpt();
        header.setPurOrderId(100L);
        header.setRecptCode("RCP-001");

        WmItemRecptLine line = new WmItemRecptLine();
        line.setLineId(1L);
        line.setRecptId(1L);
        line.setItemId(201L);
        line.setItemCode("ITEM-001");
        line.setItemName("测试物料");
        line.setQuantityRecpt(new BigDecimal("10.0000"));

        PurOrderLine poLine = new PurOrderLine();
        poLine.setLineId(1L);
        poLine.setOrderId(100L);
        poLine.setItemId(201L);

        PurOrder purOrder = new PurOrder();
        purOrder.setOrderId(100L);
        purOrder.setStatus("ORDERED");

        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(line));
        when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(poLine));
        when(purOrderService.selectPurOrderByOrderId(100L)).thenReturn(purOrder);

        service.confirmItemRecpt(1L);

        verify(storageCoreService).processItemRecpt(any());
        assertThat(header.getStatus()).isEqualTo("CONFIRMED");
        assertThat(poLine.getStatus()).isEqualTo("RECEIVING");
        assertThat(poLine.getArrivalNoticeId()).isEqualTo(1L);
        assertThat(purOrder.getStatus()).isEqualTo("RECEIVING");
    }

    @Test
    @DisplayName("2. 确认收货：非DRAFT状态拒绝")
    void testConfirmRejectsNonDraft() {
        WmItemRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);

        assertThatThrownBy(() -> service.confirmItemRecpt(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("草稿状态");
        verify(storageCoreService, never()).processItemRecpt(any());
    }

    @Test
    @DisplayName("3. 确认收货：无入库行拒绝")
    void testConfirmRejectsEmptyLines() {
        WmItemRecpt header = draftRecpt();
        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.confirmItemRecpt(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("没有入库行");
    }

    @Test
    @DisplayName("4. 确认收货：入库单不存在")
    void testConfirmNotFound() {
        when(wmItemRecptMapper.selectWmItemRecptByRecptId(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.confirmItemRecpt(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("5. 过账入库：CONFIRMED → POSTED，累加quantityReceived + updateBy")
    void testPostItemRecpt() {
        WmItemRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        header.setPurOrderId(100L);
        header.setRecptCode("RCP-001");

        WmItemRecptLine recptLine = new WmItemRecptLine();
        recptLine.setItemId(201L);
        recptLine.setQuantityRecpt(new BigDecimal("10.0000"));

        PurOrderLine poLine = new PurOrderLine();
        poLine.setLineId(1L);
        poLine.setOrderId(100L);
        poLine.setItemId(201L);
        poLine.setQuantityReceived(new BigDecimal("5.0000"));
        poLine.setQuantityOrdered(new BigDecimal("15.0000"));
        poLine.setStatus("RECEIVING");

        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
        when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(recptLine));
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(poLine));

        service.postItemRecpt(1L);

        assertThat(header.getStatus()).isEqualTo("POSTED");
        assertThat(poLine.getQuantityReceived()).isEqualByComparingTo(new BigDecimal("15.0000"));
        assertThat(poLine.getStatus()).isEqualTo("RECEIVED");
        assertThat(poLine.getUpdateBy()).isEqualTo("tester");
    }

    @Test
    @DisplayName("6. 过账入库：全部收完 → PO头 → RECEIVED")
    void testPostWhenAllReceived() {
        WmItemRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        header.setPurOrderId(100L);

        WmItemRecptLine recptLine = new WmItemRecptLine();
        recptLine.setItemId(201L);
        recptLine.setQuantityRecpt(new BigDecimal("10.0000"));

        PurOrderLine poLine = new PurOrderLine();
        poLine.setLineId(1L);
        poLine.setOrderId(100L);
        poLine.setItemId(201L);
        poLine.setQuantityReceived(BigDecimal.ZERO);
        poLine.setQuantityOrdered(new BigDecimal("10.0000"));
        poLine.setStatus("RECEIVING");

        PurOrder purOrder = new PurOrder();
        purOrder.setOrderId(100L);

        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
        when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(recptLine));
        when(purOrderLineService.selectPurOrderLineList(any()))
            .thenReturn(Collections.singletonList(poLine))
            .thenReturn(Collections.singletonList(poLine));
        when(purOrderService.selectPurOrderByOrderId(100L)).thenReturn(purOrder);

        service.postItemRecpt(1L);

        verify(purOrderService).updatePurOrder(purOrder);
        assertThat(purOrder.getStatus()).isEqualTo("RECEIVED");
    }

    @Test
    @DisplayName("7. 过账入库：部分收货 → PO头不变，行状态保持RECEIVING")
    void testPostWhenPartiallyReceived() {
        WmItemRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        header.setPurOrderId(100L);

        WmItemRecptLine recptLine = new WmItemRecptLine();
        recptLine.setItemId(201L);
        recptLine.setQuantityRecpt(new BigDecimal("3.0000"));

        PurOrderLine poLine = new PurOrderLine();
        poLine.setLineId(1L);
        poLine.setOrderId(100L);
        poLine.setItemId(201L);
        poLine.setQuantityReceived(BigDecimal.ZERO);
        poLine.setQuantityOrdered(new BigDecimal("10.0000"));
        poLine.setStatus("RECEIVING");

        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
        when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(recptLine));
        when(purOrderLineService.selectPurOrderLineList(any()))
            .thenReturn(Collections.singletonList(poLine))
            .thenReturn(Collections.singletonList(poLine));

        service.postItemRecpt(1L);

        verify(purOrderService, never()).updatePurOrder(any());
        assertThat(poLine.getQuantityReceived()).isEqualByComparingTo(new BigDecimal("3.0000"));
        assertThat(poLine.getStatus()).isEqualTo("RECEIVING");
    }

    @Test
    @DisplayName("8. 过账入库：非CONFIRMED状态拒绝")
    void testPostRejectsNonConfirmed() {
        WmItemRecpt header = draftRecpt();
        header.setStatus("POSTED");
        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);

        assertThatThrownBy(() -> service.postItemRecpt(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已确认");
    }

    @Test
    @DisplayName("9. 过账入库：无采购订单关联时跳过PO回写")
    void testPostSkipsPoWritebackWhenNoPoLink() {
        WmItemRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        header.setPurOrderId(null);

        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);

        service.postItemRecpt(1L);

        verify(purOrderLineService, never()).selectPurOrderLineList(any());
        assertThat(header.getStatus()).isEqualTo("POSTED");
    }

    private WmItemRecpt draftRecpt() {
        WmItemRecpt recpt = new WmItemRecpt();
        recpt.setRecptId(1L);
        recpt.setRecptCode("RCP-001");
        recpt.setStatus("DRAFT");
        recpt.setWarehouseId(1L);
        return recpt;
    }
}
