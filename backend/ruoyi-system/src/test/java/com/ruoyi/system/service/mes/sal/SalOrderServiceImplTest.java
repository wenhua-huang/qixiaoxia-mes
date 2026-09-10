package com.ruoyi.system.service.mes.sal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
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
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.sal.impl.SalOrderServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 销售订单Service单元测试（四态模型：CONFIRMED/PRODUCING/SHIPPED/CLOSED + CANCEL）
 * 覆盖:createWithLines/createFromCrm(建单即 CONFIRMED) / updateWithLines/closeOrder/cancelOrder/
 *      deleteSalOrderByOrderIds(状态守卫) / toWorkorder(CONFIRMED+PRODUCING 两态可转,可转量校验,工单回填)
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
    }

    @AfterEach
    void tearDown()
    {
        securityUtilsMock.close();
        dateUtilsMock.close();
    }

    @Test
    @DisplayName("createWithLines - 忽略前端旧状态，强制落 CONFIRMED")
    void createWithLines_forcesConfirmed() {
        SalOrder order = new SalOrder();
        order.setOrderCode("SO001");
        order.setStatus("PREPARE"); // 旧前端可能仍传
        SalOrderLine line = new SalOrderLine();
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("100"));
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order); req.setLines(Collections.singletonList(line));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(200L); return 1;
        });

        SalOrder result = salOrderService.createWithLines(req);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("createFromCrm - 推单即 CONFIRMED")
    void createFromCrm_confirmed() {
        // mdItemMapper 反查物料 + insert 回填 id；断言落库订单 status=CONFIRMED
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

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(salOrderMapper).insertSalOrder(argThat(o -> "CONFIRMED".equals(o.getStatus())));
    }

    @Test
    @DisplayName("updateWithLines - 仅 CONFIRMED 可改；PRODUCING 拒绝且不改状态")
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
    @DisplayName("delete - 非 CONFIRMED 不可删")
    void delete_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
        assertThatThrownBy(() -> salOrderService.deleteSalOrderByOrderIds(new Long[]{1L}))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不可删除");
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
}
