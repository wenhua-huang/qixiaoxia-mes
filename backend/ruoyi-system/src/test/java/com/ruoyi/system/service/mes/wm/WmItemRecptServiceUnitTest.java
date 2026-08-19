package com.ruoyi.system.service.mes.wm;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.domain.mes.wm.ItemRecptReceiveBody;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.IQcGateService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmBatchService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;
import com.ruoyi.system.service.mes.wm.impl.WmItemRecptServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

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
    @Mock private PurOrderLineMapper purOrderLineMapper;
    @Mock private IWmStorageCoreService storageCoreService;
    @Mock private IWmBatchService wmBatchService;
    @Mock private IQcFactoryService qcFactoryService;
    @Mock private IQcGateService qcGateService;
    @Mock private AutoCodeGenerator autoCodeGenerator;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private WmItemRecptServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");

        // Mock lockTemplate：直接执行传入动作（绕过 Redis），覆盖 Runnable 与 Supplier 两种重载
        lenient().doAnswer(inv -> { Runnable action = inv.getArgument(2); action.run(); return null; })
                .when(lockTemplate).execute(anyString(), anyLong(), any(Runnable.class));
        lenient().doAnswer(inv -> { Supplier<?> action = inv.getArgument(2); return action.get(); })
                .when(lockTemplate).executeWithResult(anyString(), anyLong(), any(Supplier.class));

        // 手动创建 txTemplate（@PostConstruct 在 Mockito 下不触发）；mock 的 transactionManager 使回调直接执行
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        ReflectionTestUtils.setField(service, "txTemplate", tt);
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

        PurOrderVO purOrder = new PurOrderVO();
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
        // 模拟原子 SQL increment
        when(purOrderLineMapper.addQuantityReceived(eq(1L), any()))
            .thenAnswer(inv -> { poLine.setQuantityReceived(poLine.getQuantityReceived().add(inv.getArgument(1))); return 1; });

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

        PurOrderVO purOrder = new PurOrderVO();
        purOrder.setOrderId(100L);

        when(wmItemRecptMapper.selectWmItemRecptByRecptId(1L)).thenReturn(header);
        when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
        when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(recptLine));
        when(purOrderLineService.selectPurOrderLineList(any()))
            .thenReturn(Collections.singletonList(poLine))
            .thenReturn(Collections.singletonList(poLine));
        // 模拟原子 SQL increment
        when(purOrderLineMapper.addQuantityReceived(eq(1L), any()))
            .thenAnswer(inv -> { poLine.setQuantityReceived(poLine.getQuantityReceived().add(inv.getArgument(1))); return 1; });

        service.postItemRecpt(1L);

        // 全部收完时,writebackPoOnPost 会调用 checkAndAutoCloseOrder 推进 PO 头状态
        verify(purOrderService).checkAndAutoCloseOrder(eq(100L), isNull());
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
        // 模拟原子 SQL increment
        when(purOrderLineMapper.addQuantityReceived(eq(1L), any()))
            .thenAnswer(inv -> { poLine.setQuantityReceived(poLine.getQuantityReceived().add(inv.getArgument(1))); return 1; });

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

    @Nested
    @DisplayName("receiveWithLines — 到货登记 + 仓库传播")
    class ReceiveWithLinesTests {

        /**
         * 为末尾详情回读（selectWmItemRecptDetail → loadRecptLines）设置通用 mock。
         */
        private void stubDetailReadMocks(WmItemRecpt header) {
            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(201L);
            line.setQuantityRecpt(new BigDecimal("10.0000"));

            PurOrderLine poLine = new PurOrderLine();
            poLine.setLineId(1L);
            poLine.setOrderId(100L);
            poLine.setItemId(201L);

            PurOrderVO purOrder = new PurOrderVO();
            purOrder.setOrderId(100L);
            purOrder.setStatus("ORDERED");

            lenient().when(wmItemRecptMapper.selectWmItemRecptByRecptId(header.getRecptId())).thenReturn(header);
            lenient().when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(line));
            lenient().when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(poLine));
            lenient().when(purOrderService.selectPurOrderByOrderId(any())).thenReturn(purOrder);
        }

        @Test
        @DisplayName("10. header 无仓库 → 取第一行仓库填充 header")
        void shouldPropagateWarehouseFromFirstLineToHeader() {
            WmItemRecpt header = draftRecpt();
            header.setWarehouseId(null);
            header.setWarehouseCode(null);
            header.setWarehouseName(null);
            header.setPurOrderId(100L);

            WmItemRecptLine line1 = new WmItemRecptLine();
            line1.setItemId(208L);
            line1.setItemCode("AUX-GLUE-001");
            line1.setItemName("制袋胶水");
            line1.setQuantityRecpt(new BigDecimal("10.0000"));
            line1.setWarehouseId(1L);
            line1.setWarehouseCode("WH-001");
            line1.setWarehouseName("原料仓");

            WmItemRecptLine line2 = new WmItemRecptLine();
            line2.setItemId(209L);
            line2.setItemCode("AUX-INK-001");
            line2.setItemName("印刷油墨");
            line2.setQuantityRecpt(new BigDecimal("11.0000"));
            line2.setWarehouseId(200L);
            line2.setWarehouseCode("WH20260707004");
            line2.setWarehouseName("辅料");

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header);
            body.setLines(Arrays.asList(line1, line2));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            stubDetailReadMocks(header);

            service.receiveWithLines(body);

            // 验证 header insert 前 warehouse 已从第一行填充
            assertThat(header.getWarehouseId()).isEqualTo(1L);
            assertThat(header.getWarehouseCode()).isEqualTo("WH-001");
            assertThat(header.getWarehouseName()).isEqualTo("原料仓");

            // 验证两行都保留了各自仓库
            assertThat(line1.getWarehouseId()).isEqualTo(1L);
            assertThat(line1.getWarehouseName()).isEqualTo("原料仓");
            assertThat(line2.getWarehouseId()).isEqualTo(200L);
            assertThat(line2.getWarehouseName()).isEqualTo("辅料");

            // 验证行 insert 被调用2次
            verify(wmItemRecptLineService, times(2)).insertWmItemRecptLine(any());
            // 到货登记只建 DRAFT，不增库存/不确认/不过账
            verify(storageCoreService, never()).processItemRecpt(any());
            assertThat(header.getStatus()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("11. header 已有仓库 → 不覆盖")
        void shouldKeepExistingHeaderWarehouse() {
            WmItemRecpt header = draftRecpt();
            header.setWarehouseId(99L);
            header.setWarehouseCode("WH-099");
            header.setWarehouseName("已有仓库");
            header.setPurOrderId(100L);

            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L);
            line.setItemCode("AUX-GLUE-001");
            line.setItemName("制袋胶水");
            line.setQuantityRecpt(new BigDecimal("5.0000"));
            line.setWarehouseId(1L);
            line.setWarehouseCode("WH-001");
            line.setWarehouseName("原料仓");

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header);
            body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            stubDetailReadMocks(header);

            service.receiveWithLines(body);

            // header 仓库保持不变
            assertThat(header.getWarehouseId()).isEqualTo(99L);
            assertThat(header.getWarehouseName()).isEqualTo("已有仓库");
            // 行仓库不变
            assertThat(line.getWarehouseId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("12. header 为空 → 抛异常")
        void shouldRejectNullHeader() {
            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setLines(Collections.singletonList(new WmItemRecptLine()));

            assertThatThrownBy(() -> service.receiveWithLines(body))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("头信息不能为空");
        }

        @Test
        @DisplayName("13. lines 为空 → 抛异常")
        void shouldRejectEmptyLines() {
            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(draftRecpt());

            assertThatThrownBy(() -> service.receiveWithLines(body))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("行不能为空");
        }

        @Test
        @DisplayName("14. 采购入库 → 行的 purOrderLineId 按 itemId 匹配回填")
        void shouldBackfillPurOrderLineIdByItemId() {
            WmItemRecpt header = draftRecpt();
            header.setWarehouseId(1L);
            header.setPurOrderId(100L);

            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L);
            line.setItemCode("AUX-GLUE-001");
            line.setQuantityRecpt(new BigDecimal("10.0000"));
            line.setWarehouseId(1L);
            // 故意不设 purOrderLineId,验证会按 itemId 回填

            PurOrderLine poLine = new PurOrderLine();
            poLine.setLineId(555L);
            poLine.setOrderId(100L);
            poLine.setItemId(208L);

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header);
            body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            stubDetailReadMocks(header);
            // 测试专用 PO 行（itemId=208→lineId=555）必须在 stubConfirmMocks 之后，
            // 否则会被其通用 stub（itemId=201）覆盖，导致 backfill 匹配不到
            when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(poLine));

            service.receiveWithLines(body);

            // 验证 insert 行时 purOrderLineId 已被回填为 PO 行 ID
            ArgumentCaptor<WmItemRecptLine> captor = ArgumentCaptor.forClass(WmItemRecptLine.class);
            verify(wmItemRecptLineService).insertWmItemRecptLine(captor.capture());
            assertThat(captor.getValue().getPurOrderLineId()).isEqualTo(555L);
        }

        @Test
        @DisplayName("15. 非 PO 入库(purOrderId=null) → 不查 PO 行,不回填 purOrderLineId")
        void shouldSkipBackfillWhenNoPurOrder() {
            WmItemRecpt header = draftRecpt();
            header.setWarehouseId(1L);
            header.setPurOrderId(null);  // 非采购入库

            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L);
            line.setQuantityRecpt(new BigDecimal("10.0000"));
            line.setWarehouseId(1L);

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header);
            body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            stubDetailReadMocks(header);

            service.receiveWithLines(body);

            verify(purOrderLineMapper, never()).selectPurOrderLineList(any());
            ArgumentCaptor<WmItemRecptLine> captor = ArgumentCaptor.forClass(WmItemRecptLine.class);
            verify(wmItemRecptLineService).insertWmItemRecptLine(captor.capture());
            assertThat(captor.getValue().getPurOrderLineId()).isNull();
        }
    }

    @Nested
    @DisplayName("receiveWithLines — 批次号自动生成")
    class BatchCodeTests {

        private void stubDetailReadMocks(WmItemRecpt header) {
            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(201L);
            line.setQuantityRecpt(new BigDecimal("10.0000"));
            PurOrderLine poLine = new PurOrderLine();
            poLine.setLineId(1L); poLine.setOrderId(100L); poLine.setItemId(201L);
            PurOrderVO purOrder = new PurOrderVO();
            purOrder.setOrderId(100L); purOrder.setStatus("ORDERED");
            lenient().when(wmItemRecptMapper.selectWmItemRecptByRecptId(header.getRecptId())).thenReturn(header);
            lenient().when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(line));
            lenient().when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.singletonList(poLine));
            lenient().when(purOrderService.selectPurOrderByOrderId(any())).thenReturn(purOrder);
        }

        @Test
        @DisplayName("14. 行无 batchCode → 自动调用 getOrGenerateBatchCode")
        void shouldAutoGenBatchWhenNoBatchCode() {
            WmItemRecpt header = draftRecpt(); header.setPurOrderId(100L);
            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L); line.setItemCode("AUX-GLUE-001"); line.setItemName("制袋胶水");
            line.setQuantityRecpt(new BigDecimal("10.0000"));
            line.setWarehouseId(1L); line.setWarehouseCode("WH-001"); line.setWarehouseName("原料仓");

            WmBatch generated = new WmBatch();
            generated.setBatchId(500L); generated.setBatchCode("BAT20260707001");

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header); body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            when(wmBatchService.getOrGenerateBatchCode(any())).thenReturn(generated);
            stubDetailReadMocks(header);

            service.receiveWithLines(body);

            assertThat(line.getBatchId()).isEqualTo(500L);
            assertThat(line.getBatchCode()).isEqualTo("BAT20260707001");
            verify(wmBatchService).getOrGenerateBatchCode(any());
        }

        @Test
        @DisplayName("15. 行已有 batchCode → 不覆盖")
        void shouldNotOverwriteExistingBatchCode() {
            WmItemRecpt header = draftRecpt(); header.setPurOrderId(100L);
            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L); line.setItemCode("AUX-GLUE-001"); line.setItemName("制袋胶水");
            line.setQuantityRecpt(new BigDecimal("5.0000"));
            line.setBatchCode("EXISTING-BATCH");
            line.setBatchId(999L);
            line.setWarehouseId(1L); line.setWarehouseCode("WH-001"); line.setWarehouseName("原料仓");

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header); body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            stubDetailReadMocks(header);

            service.receiveWithLines(body);

            assertThat(line.getBatchId()).isEqualTo(999L);
            assertThat(line.getBatchCode()).isEqualTo("EXISTING-BATCH");
            verify(wmBatchService, never()).getOrGenerateBatchCode(any());
        }

        @Test
        @DisplayName("16. 物料无批次管理 → 跳过")
        void shouldSkipBatchWhenMaterialNotBatchManaged() {
            WmItemRecpt header = draftRecpt(); header.setPurOrderId(100L);
            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L); line.setItemCode("AUX-GLUE-001"); line.setItemName("制袋胶水");
            line.setQuantityRecpt(new BigDecimal("5.0000"));
            line.setWarehouseId(1L); line.setWarehouseCode("WH-001"); line.setWarehouseName("原料仓");

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header); body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            when(wmBatchService.getOrGenerateBatchCode(any())).thenReturn(null);
            stubDetailReadMocks(header);

            service.receiveWithLines(body);

            assertThat(line.getBatchId()).isNull();
            assertThat(line.getBatchCode()).isNull();
            verify(wmBatchService).getOrGenerateBatchCode(any());
        }

        @Test
        @DisplayName("17. receiveWithLines 返回入库单详情(头+行批次码)")
        void shouldReturnDetailWithBatchCodes() {
            WmItemRecpt header = draftRecpt(); header.setPurOrderId(100L);
            WmItemRecptLine line = new WmItemRecptLine();
            line.setItemId(208L); line.setItemCode("AUX-GLUE-001"); line.setItemName("制袋胶水");
            line.setQuantityRecpt(new BigDecimal("10.0000"));
            line.setWarehouseId(1L); line.setWarehouseCode("WH-001"); line.setWarehouseName("原料仓");

            WmBatch generated = new WmBatch();
            generated.setBatchId(500L); generated.setBatchCode("BAT20260707001");

            ItemRecptReceiveBody body = new ItemRecptReceiveBody();
            body.setHeader(header); body.setLines(Collections.singletonList(line));

            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptMapper.updateWmItemRecpt(any())).thenReturn(1);
            when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
            when(wmBatchService.getOrGenerateBatchCode(any())).thenReturn(generated);
            stubDetailReadMocks(header);
            // 详情回读(loadRecptLines)返回真实行对象（已带生成批次码），替代 stubConfirmMocks 的通用行
            when(wmItemRecptLineService.selectWmItemRecptLineList(any())).thenReturn(Collections.singletonList(line));

            WmItemRecpt result = service.receiveWithLines(body);

            // 返回的详情：头（DRAFT）+ 行，行携带生成的批次码，供 App 登记完成页展示
            assertThat(result).isNotNull();
            assertThat(result.getRecptId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo("DRAFT");
            assertThat(result.getLines()).hasSize(1);
            assertThat(result.getLines().get(0).getBatchId()).isEqualTo(500L);
            assertThat(result.getLines().get(0).getBatchCode()).isEqualTo("BAT20260707001");
        }
    }

    private WmItemRecpt draftRecpt() {
        WmItemRecpt recpt = new WmItemRecpt();
        recpt.setRecptId(1L);
        recpt.setRecptCode("RCP-001");
        recpt.setStatus("DRAFT");
        recpt.setWarehouseId(1L);
        return recpt;
    }

    @Nested
    @DisplayName("insertWmItemRecpt — 单号防重")
    class InsertCodeGuardTests {

        @Test
        @DisplayName("18. 同号已存在 → 拒绝重复提交，不 insert")
        void shouldRejectDuplicateRecptCode() {
            WmItemRecpt entity = new WmItemRecpt();
            entity.setRecptCode("00RCVT20260819001");
            entity.setStatus("DRAFT");
            entity.setWarehouseId(1L);

            WmItemRecpt existing = new WmItemRecpt();
            existing.setRecptId(218L);
            existing.setRecptCode("00RCVT20260819001");
            when(wmItemRecptMapper.selectByRecptCode("00RCVT20260819001")).thenReturn(existing);

            assertThatThrownBy(() -> service.insertWmItemRecpt(entity))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("已存在");
            verify(wmItemRecptMapper, never()).insertWmItemRecpt(any());
        }

        @Test
        @DisplayName("19. recptCode 为空 → 服务端调用编码器生成")
        void shouldGenerateCodeWhenEmpty() {
            WmItemRecpt entity = new WmItemRecpt();
            entity.setStatus("DRAFT");
            entity.setWarehouseId(1L);
            when(autoCodeGenerator.genSerialCode(eq("RECEIPT_NO"), eq(""))).thenReturn("00RCVT20260819002");
            when(wmItemRecptMapper.selectByRecptCode(anyString())).thenReturn(null);
            when(wmItemRecptMapper.insertWmItemRecpt(any())).thenReturn(1);

            service.insertWmItemRecpt(entity);

            assertThat(entity.getRecptCode()).isEqualTo("00RCVT20260819002");
            verify(autoCodeGenerator).genSerialCode("RECEIPT_NO", "");
            verify(wmItemRecptMapper).insertWmItemRecpt(any());
        }
    }
}
