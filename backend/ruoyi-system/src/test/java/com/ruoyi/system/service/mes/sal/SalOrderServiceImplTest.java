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
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
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
 * 销售订单Service单元测试
 * 覆盖:createWithLines / submitOrder/approveOrder/rejectOrder(审核链路+状态守卫) / cancelOrder /
 *      deleteSalOrderByOrderIds(状态守卫) / toWorkorder(可转量校验 + 工单回填 order_source/source_code/sales_order_line_id)
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
    @DisplayName("createWithLines - 正常创建头+行")
    void createWithLines_ok()
    {
        SalOrder order = new SalOrder();
        order.setOrderCode("SO001");
        order.setStatus("PREPARE");
        SalOrderLine line = new SalOrderLine();
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("100"));
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order);
        req.setLines(Collections.singletonList(line));
        // mock insertSalOrder 回填自增ID(真实DB由useGeneratedKeys回填)
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(200L);
            return 1;
        });

        SalOrder result = salOrderService.createWithLines(req);

        verify(salOrderMapper).insertSalOrder(order);
        verify(salOrderLineMapper).insertSalOrderLine(any(SalOrderLine.class));
        assertThat(result.getStatus()).isEqualTo("PREPARE");
        assertThat(line.getOrderId()).isEqualTo(200L);
        assertThat(line.getLineNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("submitOrder - 待提交且有行 -> 待审核(PENDING)")
    void submitOrder_ok()
    {
        SalOrder order = buildOrder(1L, "SO001", "PREPARE");
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(order);
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L))
                .thenReturn(Collections.singletonList(new SalOrderLine()));
        when(salOrderMapper.updateSalOrder(any(SalOrder.class))).thenReturn(1);

        int n = salOrderService.submitOrder(1L);

        assertThat(n).isEqualTo(1);
        verify(salOrderMapper).updateSalOrder(argThat(o -> "PENDING".equals(o.getStatus())));
    }

    @Test
    @DisplayName("submitOrder - 无明细行 -> 拒绝")
    void submitOrder_noLines_rejected()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "PREPARE"));
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> salOrderService.submitOrder(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("明细行");
    }

    @Test
    @DisplayName("approveOrder - 待审核 -> 已确认,写入审核人")
    void approveOrder_ok()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "PENDING"));
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L))
                .thenReturn(Collections.singletonList(new SalOrderLine()));
        when(salOrderMapper.updateSalOrder(any(SalOrder.class))).thenReturn(1);

        salOrderService.approveOrder(1L);

        verify(salOrderMapper).updateSalOrder(argThat(o -> "CONFIRMED".equals(o.getStatus()) && "tester".equals(o.getApproveBy())));
    }

    @Test
    @DisplayName("approveOrder - 非待审核 -> 拒绝")
    void approveOrder_notPending_rejected()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "PREPARE"));
        assertThatThrownBy(() -> salOrderService.approveOrder(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("待审核");
    }

    @Test
    @DisplayName("rejectOrder - PENDING 回退 PREPARE,记录驳回意见")
    void rejectOrder_ok()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "PENDING"));
        when(salOrderMapper.updateSalOrder(any(SalOrder.class))).thenReturn(1);

        salOrderService.rejectOrder(1L, "价格异常");

        verify(salOrderMapper).updateSalOrder(argThat(o -> "PREPARE".equals(o.getStatus()) && "价格异常".equals(o.getApproveRemark())));
    }

    @Test
    @DisplayName("rejectOrder - 驳回意见为空 -> 拒绝")
    void rejectOrder_noRemark_rejected()
    {
        assertThatThrownBy(() -> salOrderService.rejectOrder(1L, ""))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("审核意见");
    }

    @Test
    @DisplayName("approveOrder - 无明细行 -> 拒绝(防空单审核)")
    void approveOrder_noLines_rejected()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "PENDING"));
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> salOrderService.approveOrder(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("明细行");
        verify(salOrderMapper, never()).updateSalOrder(any());
    }

    @Test
    @DisplayName("submitOrder - 重新提交清空上次驳回原因")
    void submitOrder_clearsApproveRemark()
    {
        SalOrder order = buildOrder(1L, "SO001", "PREPARE");
        order.setApproveRemark("上次驳回原因");
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(order);
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L))
                .thenReturn(Collections.singletonList(new SalOrderLine()));
        when(salOrderMapper.updateSalOrder(any(SalOrder.class))).thenReturn(1);

        salOrderService.submitOrder(1L);

        verify(salOrderMapper).updateSalOrder(argThat(o ->
                "PENDING".equals(o.getStatus()) && "".equals(o.getApproveRemark())));
    }

    @Test
    @DisplayName("batchSubmit - 空数组 -> 抛异常")
    void batchSubmit_empty_rejected()
    {
        assertThatThrownBy(() -> salOrderService.batchSubmit(new Long[0]))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未选择");
    }

    @Test
    @DisplayName("batchSubmit - 部分成功部分失败,返回汇总")
    void batchSubmit_partialFailure()
    {
        // ID=1 PREPARE 有行 -> 成功; ID=2 CONFIRMED -> 状态不允许,失败
        SalOrder o1 = buildOrder(1L, "SO001", "PREPARE");
        SalOrder o2 = buildOrder(2L, "SO002", "CONFIRMED");
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(o1);
        when(salOrderMapper.selectSalOrderByOrderId(2L)).thenReturn(o2);
        when(salOrderLineMapper.selectSalOrderLineByOrderId(1L))
                .thenReturn(Collections.singletonList(new SalOrderLine()));
        when(salOrderMapper.updateSalOrder(any(SalOrder.class))).thenReturn(1);

        Map<String, Object> r = salOrderService.batchSubmit(new Long[]{1L, 2L});

        assertThat(r.get("total")).isEqualTo(2);
        assertThat(r.get("successCount")).isEqualTo(1);
        assertThat(r.get("failedCount")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failures = (List<Map<String, Object>>) r.get("failures");
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).get("orderId")).isEqualTo(2L);
    }

    @Test
    @DisplayName("cancelOrder - 已关闭 -> 拒绝")
    void cancelOrder_closed_rejected()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "CLOSED"));
        assertThatThrownBy(() -> salOrderService.cancelOrder(1L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不可取消");
    }

    @Test
    @DisplayName("delete - 非待确认状态 -> 拒绝(Fix2 防工单孤儿)")
    void delete_nonPrepare_rejected()
    {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "CONFIRMED"));
        assertThatThrownBy(() -> salOrderService.deleteSalOrderByOrderIds(new Long[]{1L}))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不可删除");
        verify(salOrderMapper, never()).deleteSalOrderByOrderIds(any());
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
    @DisplayName("toWorkorder - 非已确认订单 -> 拒绝")
    void toWorkorder_notConfirmed_rejected()
    {
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(buildLine(10L, 1L, new BigDecimal("100")));
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO001", "PREPARE"));

        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L);
        req.setQuantity(new BigDecimal("10"));
        req.setWorkorderCode("WO001");

        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已确认");
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

    @SuppressWarnings("unused")
    private List<SalOrderLine> unused() { return Collections.emptyList(); }
}
