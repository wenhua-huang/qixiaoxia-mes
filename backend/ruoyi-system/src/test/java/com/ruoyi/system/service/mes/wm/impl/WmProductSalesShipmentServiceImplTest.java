package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.WmProductSalesConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesBoxMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesShipmentMapper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 销售出库-发运单 Service 单元测试
 * 覆盖：createShipment（多次发运累加 + 状态推导 + 箱回写 + 发运量≤出库确认量）/
 *      deleteShipment（回滚箱 + 头表扣减）/
 *      receive（签收 + 全签收头表 RECEIVED）/
 *      错误路径（零箱/已签收删/重复发运/状态守卫/超额发运）
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("销售发运单服务单元测试")
class WmProductSalesShipmentServiceImplTest
{
    @Mock private WmProductSalesShipmentMapper shipmentMapper;
    @Mock private WmProductSalesBoxMapper boxMapper;
    @Mock private WmProductSalesMapper salesMapper;
    @Mock private AutoCodeGenerator autoCodeGenerator;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private WmProductSalesShipmentServiceImpl shipmentService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private MockedStatic<DateUtils> dateUtilsMock;

    @BeforeEach
    void setUp()
    {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
        dateUtilsMock = mockStatic(DateUtils.class);
        dateUtilsMock.when(DateUtils::getNowDate).thenReturn(new Date());

        // Mock lockTemplate：直接执行 Runnable/Supplier（绕过 Redis）
        when(lockTemplate.executeWithResult(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(inv -> { Supplier<?> s = inv.getArgument(2); return s.get(); });
        doAnswer(inv -> { ((Runnable) inv.getArgument(2)).run(); return null; })
                .when(lockTemplate).execute(anyString(), anyLong(), any(Runnable.class));

        // 手动建 txTemplate（@PostConstruct 在 Mockito 下不触发）
        ReflectionTestUtils.setField(shipmentService, "txTemplate", new TransactionTemplate(transactionManager));
        // TransactionTemplate.execute 需要真实事务管理器 → mock 直接执行 callback
        when(transactionManager.getTransaction(any())).thenReturn(null);
    }

    @AfterEach
    void tearDown()
    {
        securityUtilsMock.close();
        dateUtilsMock.close();
    }

    // ═══════════════ createShipment ═══════════════

    @Test
    @DisplayName("createShipment - 勾选 1 箱发运，累加 shippedQuantity 并回写箱")
    void createShipment_withBoxes_ok()
    {
        WmProductSales header = buildHeader(215L, "POSTED", "UN_SHIPPED", "0", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);

        WmProductSalesBox box = buildBox(201L, 215L, "PACKED", "3");
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(box);
        when(autoCodeGenerator.genSerialCode(eq("SHIP_NO"), anyString())).thenReturn("SHP001");
        when(shipmentMapper.insertWmProductSalesShipment(any())).thenAnswer(inv -> {
            ((WmProductSalesShipment) inv.getArgument(0)).setShipmentId(500L);
            return 1;
        });

        WmProductSalesShipment req = new WmProductSalesShipment();
        req.setSalesId(215L);
        req.setShipMethod("LOGISTICS");
        WmProductSalesBox ref = new WmProductSalesBox();
        ref.setBoxId(201L);
        req.setBoxes(Collections.singletonList(ref));

        shipmentService.createShipment(req);

        // 箱回写走 markShipped
        verify(boxMapper).markShipped(eq(201L), eq(500L), anyString(), any(Date.class));
        // 头表累加 shippedQuantity=3，shipStatus=PARTIAL_SHIPPED
        assertThat(header.getShippedQuantity()).isEqualByComparingTo("3");
        assertThat(header.getShipStatus()).isEqualTo("PARTIAL_SHIPPED");
        assertThat(header.getStatus()).isEqualTo("POSTED"); // 未全发，主状态不变
        verify(salesMapper).updateWmProductSales(header);
    }

    @Test
    @DisplayName("createShipment - 未指定箱默认取全部 PACKED，全发完主状态→SHIPPED")
    void createShipment_defaultAllBoxes_fullShip()
    {
        WmProductSales header = buildHeader(215L, "POSTED", "UN_SHIPPED", "0", "5", "5");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);

        List<WmProductSalesBox> all = new ArrayList<>();
        all.add(buildBox(201L, 215L, "PACKED", "3"));
        all.add(buildBox(202L, 215L, "PACKED", "2"));
        all.add(buildBox(203L, 215L, "SHIPPED", "1")); // 已发运的不再发
        when(boxMapper.selectBoxesBySalesId(215L)).thenReturn(all);
        when(autoCodeGenerator.genSerialCode(anyString(), anyString())).thenReturn("SHP002");
        when(shipmentMapper.insertWmProductSalesShipment(any())).thenAnswer(inv -> {
            ((WmProductSalesShipment) inv.getArgument(0)).setShipmentId(501L);
            return 1;
        });

        WmProductSalesShipment req = new WmProductSalesShipment();
        req.setSalesId(215L);
        shipmentService.createShipment(req);

        // 全部 5 发完 → 主状态 SHIPPED + shipStatus SHIPPED
        assertThat(header.getShippedQuantity()).isEqualByComparingTo("5");
        assertThat(header.getShipStatus()).isEqualTo("SHIPPED");
        assertThat(header.getStatus()).isEqualTo("SHIPPED");
        verify(boxMapper).markShipped(eq(201L), anyLong(), anyString(), any(Date.class));
        verify(boxMapper).markShipped(eq(202L), anyLong(), anyString(), any(Date.class));
        verify(boxMapper, never()).markShipped(eq(203L), anyLong(), anyString(), any(Date.class));
    }

    @Test
    @DisplayName("createShipment - 零箱拒绝")
    void createShipment_emptyBoxes_reject()
    {
        WmProductSales header = buildHeader(215L, "POSTED", "UN_SHIPPED", "0", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);
        when(boxMapper.selectBoxesBySalesId(215L)).thenReturn(new ArrayList<>());

        WmProductSalesShipment req = new WmProductSalesShipment();
        req.setSalesId(215L);
        assertThatThrownBy(() -> shipmentService.createShipment(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未关联任何已装箱");
    }

    @Test
    @DisplayName("createShipment - 箱不属于本出库单拒绝")
    void createShipment_boxNotBelong_reject()
    {
        WmProductSales header = buildHeader(215L, "POSTED", "UN_SHIPPED", "0", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);
        WmProductSalesBox other = buildBox(201L, 999L, "PACKED", "3"); // 属于 999
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(other);

        WmProductSalesShipment req = new WmProductSalesShipment();
        req.setSalesId(215L);
        WmProductSalesBox ref = new WmProductSalesBox();
        ref.setBoxId(201L);
        req.setBoxes(Collections.singletonList(ref));

        assertThatThrownBy(() -> shipmentService.createShipment(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不属于本出库单");
    }

    @Test
    @DisplayName("createShipment - 已发运的箱重复发运拒绝")
    void createShipment_boxAlreadyShipped_reject()
    {
        WmProductSales header = buildHeader(215L, "POSTED", "PARTIAL_SHIPPED", "3", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);
        WmProductSalesBox shipped = buildBox(201L, 215L, "SHIPPED", "3");
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(shipped);

        WmProductSalesShipment req = new WmProductSalesShipment();
        req.setSalesId(215L);
        WmProductSalesBox ref = new WmProductSalesBox();
        ref.setBoxId(201L);
        req.setBoxes(Collections.singletonList(ref));

        assertThatThrownBy(() -> shipmentService.createShipment(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已发运，不可重复发运");
    }

    // ═══════════════ deleteShipment（回滚） ═══════════════

    @Test
    @DisplayName("deleteShipment - 回滚箱到 PACKED + shipment_id=null，头表扣减")
    void deleteShipment_rollback_ok()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "IN_TRANSIT", "3");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);

        List<WmProductSalesBox> boxes = Collections.singletonList(buildBox(201L, 215L, "SHIPPED", "3"));
        when(boxMapper.selectBoxesByShipmentId(500L)).thenReturn(boxes);

        WmProductSales header = buildHeader(215L, "POSTED", "PARTIAL_SHIPPED", "5", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);
        // 删 500 后剩 2，非全发，仍有在途 → PARTIAL_SHIPPED
        when(shipmentMapper.selectShipmentsBySalesId(215L))
                .thenReturn(Collections.singletonList(buildShipment(501L, 215L, "IN_TRANSIT", "2")));

        shipmentService.deleteWmProductSalesShipmentByShipmentId(500L);

        verify(boxMapper).rollbackToPacked(eq(201L), anyString(), any(Date.class));
        assertThat(header.getShippedQuantity()).isEqualByComparingTo("2");
        assertThat(header.getShipStatus()).isEqualTo("PARTIAL_SHIPPED");
        verify(shipmentMapper).deleteWmProductSalesShipmentByShipmentId(500L);
    }

    @Test
    @DisplayName("deleteShipment - 已签收拒绝删除")
    void deleteShipment_received_reject()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "RECEIVED", "3");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);

        assertThatThrownBy(() -> shipmentService.deleteWmProductSalesShipmentByShipmentId(500L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已签收的发运单不可删除");
        verify(shipmentMapper, never()).deleteWmProductSalesShipmentByShipmentId(anyLong());
    }

    @Test
    @DisplayName("deleteShipment - 全发完删除一个后回退 SHIPPED→POSTED 主状态")
    void deleteShipment_fullToPartial_statusRollback()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "IN_TRANSIT", "5");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);
        when(boxMapper.selectBoxesByShipmentId(500L)).thenReturn(new ArrayList<>());

        WmProductSales header = buildHeader(215L, "SHIPPED", "SHIPPED", "10", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);
        when(shipmentMapper.selectShipmentsBySalesId(215L))
                .thenReturn(Collections.singletonList(buildShipment(501L, 215L, "IN_TRANSIT", "5")));

        shipmentService.deleteWmProductSalesShipmentByShipmentId(500L);

        assertThat(header.getShippedQuantity()).isEqualByComparingTo("5");
        assertThat(header.getStatus()).isEqualTo("POSTED"); // 主状态回退
        assertThat(header.getShipStatus()).isEqualTo("PARTIAL_SHIPPED");
    }

    // ═══════════════ receive（签收） ═══════════════

    @Test
    @DisplayName("receive - 全发完且全部签收 → 头表 RECEIVED")
    void receive_allReceived_headerReceived()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "IN_TRANSIT", "5");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);
        // 签收后查列表只有这一条（已 RECEIVED）
        when(shipmentMapper.selectShipmentsBySalesId(215L))
                .thenAnswer(inv -> Collections.singletonList(ship));

        WmProductSales header = buildHeader(215L, "SHIPPED", "SHIPPED", "10", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);

        WmProductSalesShipment info = new WmProductSalesShipment();
        info.setReceivedBy("张三");
        shipmentService.receive(500L, info);

        assertThat(ship.getStatus()).isEqualTo("RECEIVED");
        assertThat(ship.getReceivedBy()).isEqualTo("张三");
        assertThat(header.getShipStatus()).isEqualTo("RECEIVED");
        verify(salesMapper).updateWmProductSales(header);
    }

    @Test
    @DisplayName("receive - 部分发运的签收不改头表 shipStatus")
    void receive_partialShipped_noHeaderChange()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "IN_TRANSIT", "3");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);
        when(shipmentMapper.selectShipmentsBySalesId(215L))
                .thenAnswer(inv -> Collections.singletonList(ship));

        WmProductSales header = buildHeader(215L, "POSTED", "PARTIAL_SHIPPED", "3", "10", "10");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);

        shipmentService.receive(500L, new WmProductSalesShipment());

        assertThat(ship.getStatus()).isEqualTo("RECEIVED");
        // 部分发运（PARTIAL_SHIPPED）签收单条不改头表
        assertThat(header.getShipStatus()).isEqualTo("PARTIAL_SHIPPED");
    }

    @Test
    @DisplayName("receive - 未来签收时间拒绝")
    void receive_futureTime_reject()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "IN_TRANSIT", "3");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);

        WmProductSalesShipment info = new WmProductSalesShipment();
        info.setReceivedTime(new Date(System.currentTimeMillis() + 86400000L)); // 明天

        assertThatThrownBy(() -> shipmentService.receive(500L, info))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能晚于当前时间");
    }

    @Test
    @DisplayName("receive - 已签收重复签收拒绝")
    void receive_alreadyReceived_reject()
    {
        WmProductSalesShipment ship = buildShipment(500L, 215L, "RECEIVED", "3");
        when(shipmentMapper.selectWmProductSalesShipmentByShipmentId(500L)).thenReturn(ship);

        assertThatThrownBy(() -> shipmentService.receive(500L, new WmProductSalesShipment()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不可签收");
    }

    // ═══════════════ 超额发运守卫 ═══════════════

    @Test
    @DisplayName("createShipment - 累计发运量超过已出库确认量拒绝")
    void createShipment_overPosted_reject()
    {
        // 已出库确认 5，已发运 3，再发 3 → 6 > 5，拒绝
        WmProductSales header = buildHeader(215L, "PARTIAL_POSTED", "PARTIAL_SHIPPED", "3", "10", "5");
        when(salesMapper.selectWmProductSalesBySalesId(215L)).thenReturn(header);
        WmProductSalesBox box = buildBox(201L, 215L, "PACKED", "3");
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(box);

        WmProductSalesShipment req = new WmProductSalesShipment();
        req.setSalesId(215L);
        WmProductSalesBox ref = new WmProductSalesBox();
        ref.setBoxId(201L);
        req.setBoxes(Collections.singletonList(ref));

        assertThatThrownBy(() -> shipmentService.createShipment(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("超过可发运量");
        verify(shipmentMapper, never()).insertWmProductSalesShipment(any());
    }

    // ═══════════════ 工具方法 ═══════════════

    private WmProductSales buildHeader(Long salesId, String status, String shipStatus,
                                       String shippedQty, String totalQty, String postedQty)
    {
        WmProductSales h = new WmProductSales();
        h.setSalesId(salesId);
        h.setStatus(status);
        h.setShipStatus(shipStatus);
        h.setShippedQuantity(new BigDecimal(shippedQty));
        h.setTotalQuantity(new BigDecimal(totalQty));
        h.setPostedQuantity(new BigDecimal(postedQty));
        return h;
    }

    private WmProductSalesBox buildBox(Long boxId, Long salesId, String status, String qty)
    {
        WmProductSalesBox b = new WmProductSalesBox();
        b.setBoxId(boxId);
        b.setSalesId(salesId);
        b.setBoxNo("BOX-" + boxId);
        b.setStatus(status);
        b.setQuantity(new BigDecimal(qty));
        return b;
    }

    private WmProductSalesShipment buildShipment(Long shipmentId, Long salesId, String status, String qty)
    {
        WmProductSalesShipment s = new WmProductSalesShipment();
        s.setShipmentId(shipmentId);
        s.setSalesId(salesId);
        s.setShipmentCode("SHP" + shipmentId);
        s.setStatus(status);
        s.setShippedQuantity(new BigDecimal(qty));
        return s;
    }
}
