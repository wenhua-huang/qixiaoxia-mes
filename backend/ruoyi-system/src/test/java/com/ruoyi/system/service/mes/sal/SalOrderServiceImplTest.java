package com.ruoyi.system.service.mes.sal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProRoute;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.pro.ProRouteResolveService;
import com.ruoyi.system.service.mes.sal.impl.SalOrderServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 销售订单Service单元测试（五态模型：PENDING_ACCEPT/CONFIRMED/PRODUCING/SHIPPED/CLOSED + CANCEL）
 * 覆盖:createWithLines/createFromCrm(建单即 PENDING_ACCEPT) / acceptOrder(接单) /
 *      updateWithLines/closeOrder/cancelOrder/deleteSalOrderByOrderIds(状态守卫) /
 *      toWorkorder(CONFIRMED+PRODUCING 两态可转, PENDING_ACCEPT 拦截, 可转量校验, 工单回填)
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("销售订单服务单元测试")
class SalOrderServiceImplTest
{
    @Mock private SalOrderMapper salOrderMapper;
    @Mock private SalOrderLineMapper salOrderLineMapper;
    @Mock private MdItemMapper mdItemMapper;
    @Mock private IProWorkorderService proWorkorderService;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ProRouteResolveService proRouteResolveService;
    @Mock private ProRouteProductMapper proRouteProductMapper;
    @Mock private ProRouteMapper proRouteMapper;
    @Mock private ProRouteProcessMapper proRouteProcessMapper;

    @InjectMocks
    private SalOrderServiceImpl salOrderService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private MockedStatic<DateUtils> dateUtilsMock;

    @BeforeEach
    void setUp()
    {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
        dateUtilsMock = mockStatic(DateUtils.class);
        dateUtilsMock.when(DateUtils::getNowDate).thenReturn(new Date());

        // Mock lockTemplate:直接执行 Supplier(绕过 Redis)
        when(lockTemplate.executeWithResult(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(inv -> { Supplier<?> s = inv.getArgument(2); return s.get(); });

        // 手动建 txTemplate(@PostConstruct 在 Mockito 下不触发)
        ReflectionTestUtils.setField(salOrderService, "txTemplate", new TransactionTemplate(transactionManager));

        // 默认: 行无手选路线且无默认路线可带(留空手选), 既有用例不受影响
        when(proRouteResolveService.resolve(any(), any(), any(), any())).thenReturn(RouteResolveResult.empty());
    }

    @AfterEach
    void tearDown()
    {
        securityUtilsMock.close();
        dateUtilsMock.close();
    }

    @Test
    @DisplayName("createWithLines - 忽略前端传入状态，强制落 PENDING_ACCEPT")
    void createWithLines_forcesPendingAccept() {
        SalOrder order = new SalOrder();
        order.setOrderCode("SO001");
        order.setStatus("CONFIRMED"); // 旧前端可能仍传已确认
        SalOrderLine line = new SalOrderLine();
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("100"));
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order); req.setLines(Collections.singletonList(line));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(200L); return 1;
        });

        SalOrder result = salOrderService.createWithLines(req);

        assertThat(result.getStatus()).isEqualTo("PENDING_ACCEPT");
    }

    @Test
    @DisplayName("createFromCrm - 推单即 PENDING_ACCEPT")
    void createFromCrm_pendingAccept() {
        // mdItemMapper 反查物料 + insert 回填 id；断言落库订单 status=PENDING_ACCEPT
        com.ruoyi.system.domain.mes.md.MdItem item = new com.ruoyi.system.domain.mes.md.MdItem();
        item.setItemId(9L); item.setItemCode("P1"); item.setItemName("产品");
        when(mdItemMapper.selectMdItemList(any())).thenReturn(Collections.singletonList(item));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(300L); return 1;
        });
        com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest crm =
                new com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest();
        crm.setOrderCode("CRM1"); crm.setOrderName("n"); crm.setClientName("c");
        com.ruoyi.system.domain.mes.sal.CrmOrderLineDTO dto = new com.ruoyi.system.domain.mes.sal.CrmOrderLineDTO();
        dto.setProductCode("P1"); dto.setQuantity(new BigDecimal("10"));
        crm.setLines(Collections.singletonList(dto));

        SalOrder result = salOrderService.createFromCrm(crm);

        assertThat(result.getStatus()).isEqualTo("PENDING_ACCEPT");
        verify(salOrderMapper).insertSalOrder(argThat(o -> "PENDING_ACCEPT".equals(o.getStatus())));
    }

    @Test
    @DisplayName("updateWithLines - 仅待接单/已确认可改；PRODUCING 拒绝且不改状态")
    void update_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        SalOrder o = buildOrder(1L, "SO1", "PRODUCING"); req.setOrder(o);
        assertThatThrownBy(() -> salOrderService.updateWithLines(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已确认");
    }

    @Test
    @DisplayName("closeOrder - 仅 SHIPPED 可结单")
    void closeOrder_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
        assertThatThrownBy(() -> salOrderService.closeOrder(1L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已出货");

        when(salOrderMapper.selectSalOrderByOrderId(2L)).thenReturn(buildOrder(2L, "SO2", "SHIPPED"));
        when(salOrderMapper.updateSalOrder(any())).thenReturn(1);
        salOrderService.closeOrder(2L);
        verify(salOrderMapper).updateSalOrder(argThat(x -> "CLOSED".equals(x.getStatus())));
    }

    @Test
    @DisplayName("cancelOrder - CONFIRMED/PRODUCING 可取消，SHIPPED/CLOSED 拒绝")
    void cancelOrder_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "SHIPPED"));
        assertThatThrownBy(() -> salOrderService.cancelOrder(1L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不可取消");
        when(salOrderMapper.selectSalOrderByOrderId(2L)).thenReturn(buildOrder(2L, "SO2", "PRODUCING"));
        when(salOrderMapper.updateSalOrder(any())).thenReturn(1);
        salOrderService.cancelOrder(2L);
        verify(salOrderMapper).updateSalOrder(argThat(x -> "CANCEL".equals(x.getStatus())));
    }

    @Test
    @DisplayName("acceptOrder - PENDING_ACCEPT 接单 -> CONFIRMED")
    void acceptOrder_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        when(salOrderMapper.updateSalOrder(any())).thenReturn(1);

        salOrderService.acceptOrder(1L);

        verify(salOrderMapper).updateSalOrder(argThat(x -> "CONFIRMED".equals(x.getStatus())));
    }

    @Test
    @DisplayName("acceptOrder - 非 PENDING_ACCEPT（CONFIRMED）拒绝接单且不写库")
    void acceptOrder_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "CONFIRMED"));

        assertThatThrownBy(() -> salOrderService.acceptOrder(1L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("待接单");
        verify(salOrderMapper, never()).updateSalOrder(any());
    }

    @Test
    @DisplayName("updateWithLines - PENDING_ACCEPT 无派生工单时可改，状态以库中值为准")
    void update_pendingAccept_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList())).thenReturn(java.util.Collections.emptyList());
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        req.setLines(Collections.emptyList());

        salOrderService.updateWithLines(req);

        verify(salOrderMapper).updateSalOrder(argThat(o -> "PENDING_ACCEPT".equals(o.getStatus())));
        verify(salOrderLineMapper).deleteSalOrderLineByOrderId(1L);
    }

    @Test
    @DisplayName("delete - PENDING_ACCEPT 无派生工单时可删")
    void delete_pendingAccept_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList())).thenReturn(java.util.Collections.emptyList());
        when(salOrderMapper.deleteSalOrderByOrderIds(any())).thenReturn(1);

        salOrderService.deleteSalOrderByOrderIds(new Long[]{1L});

        verify(salOrderLineMapper).deleteSalOrderLineByOrderId(1L);
        verify(salOrderMapper).deleteSalOrderByOrderIds(any());
    }

    @Test
    @DisplayName("cancelOrder - PENDING_ACCEPT 可取消 -> CANCEL")
    void cancelOrder_pendingAccept_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(3L)).thenReturn(buildOrder(3L, "SO3", "PENDING_ACCEPT"));
        when(salOrderMapper.updateSalOrder(any())).thenReturn(1);

        salOrderService.cancelOrder(3L);

        verify(salOrderMapper).updateSalOrder(argThat(x -> "CANCEL".equals(x.getStatus())));
    }

    @Test
    @DisplayName("toWorkorder - PENDING_ACCEPT 未接单拒绝转工单")
    void toWorkorder_pendingAccept_rejected() {
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(buildLine(10L, 1L, new BigDecimal("100")));
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L); req.setQuantity(new BigDecimal("10")); req.setWorkorderCode("W1");

        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已确认/生产中");
        verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
    }

    @Test
    @DisplayName("delete - 待接单/已确认之外状态不可删")
    void delete_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
        assertThatThrownBy(() -> salOrderService.deleteSalOrderByOrderIds(new Long[]{1L}))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不可删除");
    }

    @Test
    @DisplayName("updateWithLines - CONFIRMED 但已派生工单（含未开工）拒绝修改，不动头行")
    void update_gate_derivedWorkorder() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "CONFIRMED"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList()))
                .thenReturn(java.util.List.of(buildCountRow(1L, 1)));
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(buildOrder(1L, "SO1", "CONFIRMED"));

        assertThatThrownBy(() -> salOrderService.updateWithLines(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已派生工单");
        verify(salOrderMapper, never()).updateSalOrder(any());
        verify(salOrderLineMapper, never()).deleteSalOrderLineByOrderId(anyLong());
    }

    @Test
    @DisplayName("updateWithLines - CONFIRMED 且无派生工单时正常整单改单")
    void update_ok_noDerivedWorkorder() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "CONFIRMED"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList())).thenReturn(java.util.Collections.emptyList());
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(buildOrder(1L, "SO1", "CONFIRMED"));
        req.setLines(Collections.emptyList());

        salOrderService.updateWithLines(req);

        verify(salOrderMapper).updateSalOrder(any());
        verify(salOrderLineMapper).deleteSalOrderLineByOrderId(1L);
    }

    @Test
    @DisplayName("delete - CONFIRMED 但已派生工单（含未开工）拒绝删除")
    void delete_gate_derivedWorkorder() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "CONFIRMED"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList()))
                .thenReturn(java.util.List.of(buildCountRow(1L, 2)));

        assertThatThrownBy(() -> salOrderService.deleteSalOrderByOrderIds(new Long[]{1L}))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已派生工单");
        verify(salOrderLineMapper, never()).deleteSalOrderLineByOrderId(anyLong());
        verify(salOrderMapper, never()).deleteSalOrderByOrderIds(any());
    }

    @Test
    @DisplayName("delete - CONFIRMED 且无派生工单时正常删除")
    void delete_ok_noDerivedWorkorder() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "CONFIRMED"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList())).thenReturn(java.util.Collections.emptyList());
        when(salOrderMapper.deleteSalOrderByOrderIds(any())).thenReturn(1);

        salOrderService.deleteSalOrderByOrderIds(new Long[]{1L});

        verify(salOrderLineMapper).deleteSalOrderLineByOrderId(1L);
        verify(salOrderMapper).deleteSalOrderByOrderIds(any());
    }

    @Test
    @DisplayName("toWorkorder - CONFIRMED 与 PRODUCING 均可转；SHIPPED 拒绝")
    void toWorkorder_statusGate() {
        // PRODUCING 正向：保留现有 toWorkorder_ok_backfill 用例，把前置 mock 单状态保持 CONFIRMED 即可
        // 新增 SHIPPED 拒绝：
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(buildLine(10L, 1L, new BigDecimal("100")));
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "SHIPPED"));
        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L); req.setQuantity(new BigDecimal("10")); req.setWorkorderCode("W1");
        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class);
        verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
    }

    @Test
    @DisplayName("toWorkorder - 超过可转数量 -> 拒绝")
    void toWorkorder_overConvertible_rejected()
    {
        SalOrderLine line = buildLine(10L, 1L, new BigDecimal("100"));
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "CONFIRMED"));
        when(salOrderLineMapper.sumProducedQtyByLineId(10L)).thenReturn(new BigDecimal("80")); // 已转80,可转20

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("50")); // 超 20
        req.setWorkorderCode("WO001");

        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("可转数量");
        verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
    }

    @Test
    @DisplayName("toWorkorder - 正常转单:回填 order_source=SALES_ORDER/source_code/sales_order_line_id")
    void toWorkorder_ok_backfill()
    {
        SalOrderLine line = buildLine(10L, 1L, new BigDecimal("100"));
        line.setProductId(2L);
        line.setProductCode("P002");
        line.setProductName("纸袋");
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        SalOrder order = buildOrder(1L, "SO001", "CONFIRMED");
        order.setClientId(3L);
        order.setClientCode("C003");
        order.setClientName("客户");
        order.setClientOrderCode("PO1");
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(order);
        when(salOrderLineMapper.sumProducedQtyByLineId(10L)).thenReturn(BigDecimal.ZERO);
        when(proWorkorderService.createWorkorderWithBom(any(), any(), any())).thenAnswer(inv -> {
            ProWorkorder wo = inv.getArgument(0);
            wo.setWorkorderId(99L);
            return wo;
        });

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("50"));
        req.setWorkorderCode("WO001");

        ProWorkorder result = salOrderService.toWorkorder(req);

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(proWorkorderService).createWorkorderWithBom(captor.capture(), any(), any());
        ProWorkorder wo = captor.getValue();
        assertThat(wo.getOrderSource()).isEqualTo("SALES_ORDER");
        assertThat(wo.getSourceCode()).isEqualTo("SO001");
        assertThat(wo.getSalesOrderLineId()).isEqualTo(10L);
        assertThat(wo.getQuantity()).isEqualByComparingTo("50");
        assertThat(wo.getClientId()).isEqualTo(3L);
        assertThat(wo.getProductId()).isEqualTo(2L);
        assertThat(result.getWorkorderId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("toWorkorder - PRODUCING 同样可转:正常建工单并回填来源")
    void toWorkorder_producing_ok()
    {
        SalOrderLine line = buildLine(20L, 2L, new BigDecimal("100"));
        line.setProductId(4L);
        line.setProductCode("P004");
        line.setProductName("纸袋");
        when(salOrderLineMapper.selectSalOrderLineByLineId(20L)).thenReturn(line);
        SalOrder order = buildOrder(2L, "SO002", "PRODUCING");
        order.setClientId(5L);
        order.setClientCode("C005");
        order.setClientName("客户");
        order.setClientOrderCode("PO2");
        when(salOrderMapper.selectSalOrderByOrderId(2L)).thenReturn(order);
        when(salOrderLineMapper.sumProducedQtyByLineId(20L)).thenReturn(BigDecimal.ZERO);
        when(proWorkorderService.createWorkorderWithBom(any(), any(), any())).thenAnswer(inv -> {
            ProWorkorder wo = inv.getArgument(0);
            wo.setWorkorderId(98L);
            return wo;
        });

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(20L);
        req.setQuantity(new BigDecimal("50"));
        req.setWorkorderCode("WO002");

        ProWorkorder result = salOrderService.toWorkorder(req);

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(proWorkorderService, times(1)).createWorkorderWithBom(captor.capture(), any(), any());
        ProWorkorder wo = captor.getValue();
        assertThat(wo.getOrderSource()).isEqualTo("SALES_ORDER");
        assertThat(wo.getSourceCode()).isEqualTo("SO002");
        assertThat(wo.getSalesOrderLineId()).isEqualTo(20L);
        assertThat(wo.getQuantity()).isEqualByComparingTo("50");
        assertThat(wo.getClientId()).isEqualTo(5L);
        assertThat(wo.getProductId()).isEqualTo(4L);
        assertThat(result.getWorkorderId()).isEqualTo(98L);
    }

    @Test
    @DisplayName("enrichOrderProgress - 批量回填，无聚合行订单=0，封顶100由SQL保证")
    void enrichOrderProgress_batch() {
        SalOrder o1 = buildOrder(1L, "SO1", "PRODUCING");
        SalOrder o2 = buildOrder(2L, "SO2", "CONFIRMED");
        SalOrder o3 = buildOrder(3L, "SO3", "PRODUCING");
        com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow row =
                new com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow();
        row.setOrderId(3L); row.setProgressPercent(42);
        when(salOrderMapper.selectProgressByOrderIds(anyList())).thenReturn(java.util.List.of(row));

        salOrderService.enrichOrderProgress(java.util.List.of(o1, o2, o3));

        assertThat(o1.getProgressPercent()).isZero();
        assertThat(o2.getProgressPercent()).isZero();
        assertThat(o3.getProgressPercent()).isEqualTo(42);
    }

    @Test
    @DisplayName("enrichOrderProgress - 空列表直接返回不查库")
    void enrichOrderProgress_empty() {
        salOrderService.enrichOrderProgress(java.util.Collections.emptyList());
        verify(salOrderMapper, never()).selectProgressByOrderIds(anyList());
    }

    @Test
    @DisplayName("getDetail - 头节点带 progressPercent")
    void getDetail_withProgress() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L)).thenReturn(java.util.Collections.emptyList());
        com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow row =
                new com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow();
        row.setOrderId(1L); row.setProgressPercent(88);
        when(salOrderMapper.selectProgressByOrderIds(anyList())).thenReturn(java.util.List.of(row));

        SalOrder d = salOrderService.getDetail(1L);

        assertThat(d.getProgressPercent()).isEqualTo(88);
    }

    // ============ 默认工艺路线带出 / 外发校验 ============

    @Test
    @DisplayName("createWithLines - 行无路线时按头维度自动回填; 手选路线不被覆盖只补快照")
    void createWithLines_routeFillAndKeepManual()
    {
        // --- 场景A: 行无路线 -> resolveBatch 命中绑定2/路线20, 自动回填
        SalOrder orderA = buildOrder(null, "SO-ROUTE-A", "PREPARE");
        SalOrderLine lineA = new SalOrderLine();
        lineA.setProductId(100L);
        lineA.setProductName("演示产品");
        lineA.setQuantity(new BigDecimal("10"));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(1L);
            return 1;
        });
        when(proRouteResolveService.resolveBatch(anyList(), eq("STANDARD"), eq("N"), eq("N")))
                .thenReturn(Map.of(100L, matchedResult(2L, 20L)));
        when(proRouteMapper.selectByRouteIds(anyCollection())).thenReturn(List.of(route(20L, "RT-X", "路线X")));

        SalOrderCreateRequest reqA = new SalOrderCreateRequest();
        reqA.setOrder(orderA);
        reqA.setLines(Collections.singletonList(lineA));
        salOrderService.createWithLines(reqA);

        ArgumentCaptor<SalOrderLine> capA = ArgumentCaptor.forClass(SalOrderLine.class);
        verify(salOrderLineMapper).insertSalOrderLine(capA.capture());
        assertThat(capA.getValue().getRouteProductId()).isEqualTo(2L);
        assertThat(capA.getValue().getRouteCode()).isEqualTo("RT-X");

        // --- 场景B: 行已选绑定9 -> 不调 resolveBatch, 批量拉绑定校验归属并补快照
        reset(proRouteResolveService, proRouteProductMapper, proRouteMapper, salOrderLineMapper);
        SalOrder orderB = buildOrder(null, "SO-ROUTE-B", "PREPARE");
        SalOrderLine lineB = new SalOrderLine();
        lineB.setProductId(100L);
        lineB.setQuantity(new BigDecimal("10"));
        lineB.setRouteProductId(9L);
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(2L);
            return 1;
        });
        ProRouteProduct own = new ProRouteProduct();
        own.setRecordId(9L);
        own.setRouteId(90L);
        own.setItemId(100L);
        when(proRouteProductMapper.selectByRecordIds(anyCollection())).thenReturn(List.of(own));
        when(proRouteMapper.selectByRouteIds(anyCollection())).thenReturn(List.of(route(90L, "RT-MANUAL", "手选路线")));

        SalOrderCreateRequest reqB = new SalOrderCreateRequest();
        reqB.setOrder(orderB);
        reqB.setLines(Collections.singletonList(lineB));
        salOrderService.createWithLines(reqB);

        verify(proRouteResolveService, never()).resolveBatch(anyList(), anyString(), anyString(), anyString());
        ArgumentCaptor<SalOrderLine> capB = ArgumentCaptor.forClass(SalOrderLine.class);
        verify(salOrderLineMapper).insertSalOrderLine(capB.capture());
        assertThat(capB.getValue().getRouteProductId()).isEqualTo(9L);
        assertThat(capB.getValue().getRouteName()).isEqualTo("手选路线");
    }

    @Test
    @DisplayName("createWithLines - 外发=Y 解析被硬阻断 -> 整单拒绝且不落行")
    void createWithLines_outsourceBlocked_rejected()
    {
        SalOrder order = buildOrder(null, "SO-OUT-BLOCK", "PREPARE");
        order.setOutsourceFlag("Y");
        SalOrderLine line = new SalOrderLine();
        line.setProductId(100L);
        line.setQuantity(new BigDecimal("10"));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(3L);
            return 1;
        });
        when(proRouteResolveService.resolveBatch(anyList(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of(100L, RouteResolveResult.blocked("产品未配置含外发工序的工艺路线")));

        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order);
        req.setLines(Collections.singletonList(line));
        assertThatThrownBy(() -> salOrderService.createWithLines(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("外发");
        verify(salOrderLineMapper, never()).insertSalOrderLine(any());
    }

    @Test
    @DisplayName("createWithLines - 手选路线不属于该产品 -> 拒绝")
    void createWithLines_manualRouteOfOtherItem_rejected()
    {
        SalOrder order = buildOrder(null, "SO-ROUTE-OTHER", "PREPARE");
        SalOrderLine line = new SalOrderLine();
        line.setProductId(100L);
        line.setProductName("演示产品");
        line.setQuantity(new BigDecimal("10"));
        line.setRouteProductId(9L);
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(4L);
            return 1;
        });
        ProRouteProduct other = new ProRouteProduct();
        other.setRecordId(9L);
        other.setRouteId(90L);
        other.setItemId(999L);
        when(proRouteProductMapper.selectByRecordIds(anyCollection())).thenReturn(List.of(other));

        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order);
        req.setLines(Collections.singletonList(line));
        assertThatThrownBy(() -> salOrderService.createWithLines(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不属于该产品");
        verify(salOrderLineMapper, never()).insertSalOrderLine(any());
    }

    @Test
    @DisplayName("toWorkorder - 请求未指定路线时沿用订单行带出的路线")
    void toWorkorder_fallbackToLineRoute()
    {
        SalOrderLine line = buildLine(10L, 1L, new BigDecimal("100"));
        line.setProductId(2L);
        line.setRouteProductId(77L);
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "CONFIRMED"));
        when(salOrderLineMapper.sumProducedQtyByLineId(10L)).thenReturn(BigDecimal.ZERO);
        ProRouteProduct binding = new ProRouteProduct();
        binding.setRecordId(77L);
        binding.setRouteId(770L);
        binding.setItemId(2L);
        when(proRouteProductMapper.selectProRouteProductByRecordId(77L)).thenReturn(binding);
        when(proRouteMapper.selectProRouteByRouteId(770L)).thenReturn(route(770L, "RT-77", "沿用路线"));
        when(proWorkorderService.createWorkorderWithBom(any(), any(), any())).thenAnswer(inv -> {
            ((ProWorkorder) inv.getArgument(0)).setWorkorderId(99L);
            return inv.getArgument(0);
        });

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("50"));
        req.setWorkorderCode("WO001");
        salOrderService.toWorkorder(req);

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(proWorkorderService).createWorkorderWithBom(captor.capture(), any(), any());
        assertThat(captor.getValue().getRouteProductId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("toWorkorder - 外发订单但路线无外发节点 -> 拒绝")
    void toWorkorder_outsourceRouteWithoutNode_rejected()
    {
        SalOrderLine line = buildLine(10L, 1L, new BigDecimal("100"));
        line.setProductId(2L);
        line.setRouteProductId(77L);
        SalOrder order = buildOrder(1L, "SO001", "CONFIRMED");
        order.setOutsourceFlag("Y");
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(order);
        when(salOrderLineMapper.sumProducedQtyByLineId(10L)).thenReturn(BigDecimal.ZERO);
        ProRouteProduct binding = new ProRouteProduct();
        binding.setRecordId(77L);
        binding.setRouteId(770L);
        binding.setItemId(2L);
        when(proRouteProductMapper.selectProRouteProductByRecordId(77L)).thenReturn(binding);
        com.ruoyi.system.domain.mes.pro.ProRouteProcess node = new com.ruoyi.system.domain.mes.pro.ProRouteProcess();
        node.setIsOutsource("0");
        when(proRouteProcessMapper.selectProRouteProcessByRouteId(770L))
                .thenReturn(Collections.singletonList(node));

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("50"));
        req.setWorkorderCode("WO001");
        req.setRouteProductId(77L);
        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("外发");
        verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
    }

    // ============ 测试数据构造 ============
    @Test
    @DisplayName("toWorkorder - 请求与订单行均无路线时现场解析兜底(第三级)")
    void toWorkorder_resolveOnTheFly_when_bothNull()
    {
        SalOrderLine line = buildLine(10L, 1L, new BigDecimal("100"));
        line.setProductId(2L);
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "CONFIRMED"));
        when(salOrderLineMapper.sumProducedQtyByLineId(10L)).thenReturn(BigDecimal.ZERO);
        when(proRouteResolveService.resolve(eq(2L), any(), any(), any()))
                .thenReturn(matchedResult(88L, 880L));
        when(proWorkorderService.createWorkorderWithBom(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("50"));
        req.setWorkorderCode("WO001");
        salOrderService.toWorkorder(req);

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(proWorkorderService).createWorkorderWithBom(captor.capture(), any(), any());
        assertThat(captor.getValue().getRouteProductId()).isEqualTo(88L);
    }

    @Test
    @DisplayName("toWorkorder - 向导传入其他产品的路线 -> 拒绝(归属校验覆盖显式传值路径)")
    void toWorkorder_explicitRouteOfOtherItem_rejected()
    {
        SalOrderLine line = buildLine(10L, 1L, new BigDecimal("100"));
        line.setProductId(2L);
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "CONFIRMED"));
        when(salOrderLineMapper.sumProducedQtyByLineId(10L)).thenReturn(BigDecimal.ZERO);
        ProRouteProduct other = new ProRouteProduct();
        other.setRecordId(77L);
        other.setItemId(999L);
        when(proRouteProductMapper.selectProRouteProductByRecordId(77L)).thenReturn(other);

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("50"));
        req.setWorkorderCode("WO001");
        req.setRouteProductId(77L);
        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不属于该产品");
        verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
    }

    @Test
    @DisplayName("createWithLines - 非法标志位/订单类型 -> 拒绝(防绕过外发阻断)")
    void createWithLines_illegalDimensions_rejected()
    {
        SalOrder orderBadFlag = buildOrder(null, "SO-BAD-FLAG", "PREPARE");
        orderBadFlag.setOutsourceFlag("y");
        stubOrderCodeUnique("SO-BAD-FLAG");
        SalOrderCreateRequest req1 = new SalOrderCreateRequest();
        req1.setOrder(orderBadFlag);
        req1.setLines(Collections.emptyList());
        assertThatThrownBy(() -> salOrderService.createWithLines(req1))
                .isInstanceOf(ServiceException.class).hasMessageContaining("Y/N");

        SalOrder orderBadType = buildOrder(null, "SO-BAD-TYPE", "PREPARE");
        orderBadType.setOrderType("NEW");
        stubOrderCodeUnique("SO-BAD-TYPE");
        SalOrderCreateRequest req2 = new SalOrderCreateRequest();
        req2.setOrder(orderBadType);
        req2.setLines(Collections.emptyList());
        assertThatThrownBy(() -> salOrderService.createWithLines(req2))
                .isInstanceOf(ServiceException.class).hasMessageContaining("非法订单类型");
        verify(salOrderMapper, never()).insertSalOrder(any());
    }

    private void stubOrderCodeUnique(String code) {
        when(salOrderMapper.checkOrderCodeUnique(argThat(o -> o != null && code.equals(o.getOrderCode())))).thenReturn(null);
    }

    // ============ 测试数据构造 ============
    private SalOrder buildOrder(Long id, String code, String status)
    {
        SalOrder o = new SalOrder();
        o.setOrderId(id);
        o.setOrderCode(code);
        o.setStatus(status);
        return o;
    }

    private SalOrderLine buildLine(Long lineId, Long orderId, BigDecimal qty)
    {
        SalOrderLine l = new SalOrderLine();
        l.setLineId(lineId);
        l.setOrderId(orderId);
        l.setQuantity(qty);
        return l;
    }

    private com.ruoyi.system.domain.mes.sal.vo.SalOrderWorkorderCountRow buildCountRow(Long orderId, int count)
    {
        com.ruoyi.system.domain.mes.sal.vo.SalOrderWorkorderCountRow r =
                new com.ruoyi.system.domain.mes.sal.vo.SalOrderWorkorderCountRow();
        r.setOrderId(orderId);
        r.setWorkorderCount(count);
        return r;
    }

    @SuppressWarnings("unused")
    private List<SalOrderLine> unused() { return Collections.emptyList(); }

    private RouteResolveResult matchedResult(long recordId, long routeId) {
        ProRouteProduct rp = new ProRouteProduct();
        rp.setRecordId(recordId);
        rp.setRouteId(routeId);
        return RouteResolveResult.matched(rp);
    }

    private ProRoute route(long routeId, String code, String name) {
        ProRoute r = new ProRoute();
        r.setRouteId(routeId);
        r.setRouteCode(code);
        r.setRouteName(name);
        return r;
    }
}
