package com.ruoyi.system.service.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.WmProductSalesConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.event.mes.SalesShipmentCompletedEvent;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesBoxMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesShipmentMapper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.impl.WmProductSalesShipmentServiceImpl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WmProductSalesShipmentServiceUnitTest {
    @Mock WmProductSalesShipmentMapper shipmentMapper;
    @Mock WmProductSalesBoxMapper boxMapper;
    @Mock WmProductSalesMapper salesMapper;
    @Mock AutoCodeGenerator autoCodeGenerator;
    @Mock RedisLockTemplate lockTemplate;
    @Mock PlatformTransactionManager transactionManager;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks WmProductSalesShipmentServiceImpl shipmentService;

    private MockedStatic<SecurityUtils> securityMock;
    private MockedStatic<DateUtils> dateMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("tester");
        dateMock = mockStatic(DateUtils.class);
        dateMock.when(DateUtils::getNowDate).thenReturn(new Date());
        // 锁直执行（impl 的三参重载为 execute(String,long,Runnable)，回调返回值被丢弃）；
        // 真实 TransactionTemplate 套 mock manager（getTransaction/commit 默认 no-op）
        lenient().doAnswer(inv -> { ((Runnable) inv.getArgument(2)).run(); return null; })
                .when(lockTemplate).execute(anyString(), anyLong(), any(Runnable.class));
        ReflectionTestUtils.setField(shipmentService, "txTemplate", new TransactionTemplate(transactionManager));
    }

    @AfterEach
    void tearDown() { securityMock.close(); dateMock.close(); }

    @Test
    @DisplayName("整单发齐：header 转 SHIPPED 且发布 SalesShipmentCompletedEvent")
    void fullShipment_publishesEvent() {
        when(salesMapper.selectWmProductSalesBySalesId(1L)).thenReturn(
                buildHeader(new BigDecimal("100"), BigDecimal.ZERO));
        when(boxMapper.selectWmProductSalesBoxByBoxId(10L)).thenReturn(
                buildBox(10L, WmProductSalesConstants.BOX_STATUS_PACKED, new BigDecimal("100")));

        shipmentService.createShipment(buildRequest());

        verify(eventPublisher).publishEvent(argThat((Object e) -> e instanceof SalesShipmentCompletedEvent
                && ((SalesShipmentCompletedEvent) e).getSalesOrderId().equals(5L)
                && ((SalesShipmentCompletedEvent) e).getSalesId().equals(1L)
                && ((SalesShipmentCompletedEvent) e).getFactoryId().equals(1L)));
        verify(salesMapper).updateWmProductSales(argThat(h ->
                WmProductSalesConstants.STATUS_SHIPPED.equals(h.getStatus())));
    }

    @Test
    @DisplayName("部分发运 60/100：PARTIAL_SHIPPED 且不发事件")
    void partialShipment_noEvent() {
        when(salesMapper.selectWmProductSalesBySalesId(1L)).thenReturn(
                buildHeader(new BigDecimal("100"), BigDecimal.ZERO));
        when(boxMapper.selectWmProductSalesBoxByBoxId(10L)).thenReturn(
                buildBox(10L, WmProductSalesConstants.BOX_STATUS_PACKED, new BigDecimal("60")));

        shipmentService.createShipment(buildRequest());

        verify(eventPublisher, never()).publishEvent(any());
        verify(salesMapper).updateWmProductSales(argThat(h ->
                WmProductSalesConstants.SHIP_STATUS_PARTIAL_SHIPPED.equals(h.getShipStatus())
                        && !WmProductSalesConstants.STATUS_SHIPPED.equals(h.getStatus())));
    }

    /** POSTED+UN_SHIPPED 可发运；total=100，已发 alreadyShipped，挂销售订单 5 */
    private WmProductSales buildHeader(BigDecimal totalQty, BigDecimal alreadyShipped) {
        WmProductSales h = new WmProductSales();
        h.setSalesId(1L); h.setFactoryId(1L); h.setSalesOrderId(5L);
        h.setStatus(WmProductSalesConstants.STATUS_POSTED);
        h.setShipStatus(WmProductSalesConstants.SHIP_STATUS_UN_SHIPPED);
        h.setTotalQuantity(totalQty); h.setPostedQuantity(totalQty); h.setShippedQuantity(alreadyShipped);
        return h;
    }
    private WmProductSalesBox buildBox(Long boxId, String status, BigDecimal qty) {
        WmProductSalesBox b = new WmProductSalesBox();
        b.setBoxId(boxId); b.setSalesId(1L); b.setBoxNo("B-" + boxId);
        b.setStatus(status); b.setQuantity(qty);
        return b;
    }
    private WmProductSalesShipment buildRequest() {
        WmProductSalesShipment e = new WmProductSalesShipment();
        e.setSalesId(1L); e.setShipmentCode("SHIP-UT-1");
        WmProductSalesBox ref = new WmProductSalesBox(); ref.setBoxId(10L);
        e.setBoxes(List.of(ref));
        return e;
    }
}
