package com.ruoyi.system.service.mes.wm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueHeaderMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.service.mes.wm.impl.WmIssueHeaderServiceImpl;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 领料单 Service 单元测试
 * 覆盖：confirmIssue（预占库存）、releaseAllocation（释放预占）、executeIssue（出库状态校验）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("领料单服务单元测试")
class WmIssueHeaderServiceUnitTest {

    @Mock private WmIssueHeaderMapper issueHeaderMapper;
    @Mock private WmIssueLineMapper issueLineMapper;
    @Mock private WmMaterialStockMapper materialStockMapper;
    @Mock private WmTransactionMapper transactionMapper;
    @Mock private ProMaterialTraceMapper materialTraceMapper;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private WmIssueHeaderServiceImpl service;

    private WmIssueHeader testHeader;
    private WmIssueLine testLine;
    private WmMaterialStock testStock;
    private MockedStatic<SecurityUtils> securityUtilsMock;
    private MockedStatic<DateUtils> dateUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
        dateUtilsMock = mockStatic(DateUtils.class);
        dateUtilsMock.when(DateUtils::getNowDate).thenReturn(new Date());

        // Mock lockTemplate：直接执行 Runnable action（绕过 Redis）
        doAnswer(inv -> { Runnable action = inv.getArgument(2); action.run(); return null; })
                .when(lockTemplate).execute(anyString(), anyLong(), any(Runnable.class));

        // 手动创建 txTemplate（@PostConstruct 在 Mockito 下不触发）
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        ReflectionTestUtils.setField(service, "txTemplate", tt);

        // 测试数据
        testHeader = new WmIssueHeader();
        testHeader.setIssueId(1L);
        testHeader.setIssueCode("LL001");
        testHeader.setIssueName("测试领料单");
        testHeader.setStatus("DRAFT");
        testHeader.setWorkorderId(10L);
        testHeader.setWorkorderCode("WO-001");
        testHeader.setWarehouseId(1L);

        testLine = new WmIssueLine();
        testLine.setLineId(1L);
        testLine.setIssueId(1L);
        testLine.setItemId(100L);
        testLine.setItemCode("MAT-001");
        testLine.setItemName("测试物料");
        testLine.setQuantityIssue(new BigDecimal("50"));
        testLine.setUnitOfMeasure("KG");
        testLine.setUnitName("千克");
        testLine.setWarehouseId(1L);

        testStock = new WmMaterialStock();
        testStock.setMaterialStockId(1L);
        testStock.setItemId(100L);
        testStock.setQuantityOnhand(new BigDecimal("200"));
        testStock.setQuantityAvailable(new BigDecimal("200"));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
        dateUtilsMock.close();
    }

    // ══════════════════════════════════════════════
    // confirmIssue 测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("1. 确认领料单：成功预占库存（DRAFT→CONFIRMED，扣quantityAvailable）")
    void testConfirmIssueSuccess() {
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        service.confirmIssue(1L);

        // 验证可用库存被扣减
        ArgumentCaptor<WmMaterialStock> stockCaptor = ArgumentCaptor.forClass(WmMaterialStock.class);
        verify(materialStockMapper).updateWmMaterialStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityAvailable())
                .isEqualByComparingTo(new BigDecimal("150")); // 200-50

        // 验证事务记录：ALLOCATE
        ArgumentCaptor<WmTransaction> txCaptor = ArgumentCaptor.forClass(WmTransaction.class);
        verify(transactionMapper).insertWmTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo("ALLOCATE");
        assertThat(txCaptor.getValue().getQuantity()).isEqualByComparingTo(new BigDecimal("-50"));

        // 验证 header 状态改为 CONFIRMED
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("2. 确认领料单：SELECT FOR UPDATE 锁定库存行（防并发超分）")
    void testConfirmIssueUsesForUpdate() {
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        service.confirmIssue(1L);

        // 必须调用 loadMaterialStockForUpdate（FOR UPDATE），不能调用 loadMaterialStock
        verify(materialStockMapper).loadMaterialStockForUpdate(any(WmMaterialStock.class));
        verify(materialStockMapper, never()).loadMaterialStock(any(WmMaterialStock.class));
    }

    @Test
    @DisplayName("3. 确认领料单：非DRAFT状态拒绝")
    void testConfirmIssueRejectsNonDraft() {
        testHeader.setStatus("CONFIRMED");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.confirmIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("草稿");
    }

    @Test
    @DisplayName("4. 确认领料单：无明细行拒绝")
    void testConfirmIssueRejectsEmptyLines() {
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.confirmIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("明细行");
    }

    @Test
    @DisplayName("5. 确认领料单：库存记录不存在")
    void testConfirmIssueStockNotFound() {
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(null);

        assertThatThrownBy(() -> service.confirmIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("库存记录不存在");
    }

    @Test
    @DisplayName("6. 确认领料单：可用库存不足")
    void testConfirmIssueInsufficientAvailable() {
        testStock.setQuantityAvailable(new BigDecimal("10")); // 只有10，需50
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        assertThatThrownBy(() -> service.confirmIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("可用库存不足");
    }

    @Test
    @DisplayName("7. 确认领料单：零量行跳过（查库存后跳过，不写事务）")
    void testConfirmIssueSkipsZeroQtyLine() {
        testLine.setQuantityIssue(BigDecimal.ZERO);
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));

        service.confirmIssue(1L);

        // 零量行先行跳过：不查库存、不写事务
        verify(materialStockMapper, never()).loadMaterialStockForUpdate(any(WmMaterialStock.class));
        verify(transactionMapper, never()).insertWmTransaction(any(WmTransaction.class));
    }

    @Test
    @DisplayName("8. 确认领料单：null quantityIssue 安全处理")
    void testConfirmIssueNullQty() {
        testLine.setQuantityIssue(null);
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));

        service.confirmIssue(1L);

        // null → BigDecimal.ZERO → 跳过，不查库存、不写事务
        verify(materialStockMapper, never()).loadMaterialStockForUpdate(any(WmMaterialStock.class));
        verify(transactionMapper, never()).insertWmTransaction(any(WmTransaction.class));
    }

    // ══════════════════════════════════════════════
    // releaseAllocation 测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("9. 释放预占：CONFIRMED→DRAFT，恢复quantityAvailable")
    void testReleaseAllocationSuccess() {
        testHeader.setStatus("CONFIRMED");
        testStock.setQuantityAvailable(new BigDecimal("150")); // 已预占50
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        service.releaseAllocation(1L);

        // 验证库存恢复
        ArgumentCaptor<WmMaterialStock> stockCaptor = ArgumentCaptor.forClass(WmMaterialStock.class);
        verify(materialStockMapper).updateWmMaterialStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityAvailable())
                .isEqualByComparingTo(new BigDecimal("200")); // 150+50

        // 验证 RELEASE 事务记录
        ArgumentCaptor<WmTransaction> txCaptor = ArgumentCaptor.forClass(WmTransaction.class);
        verify(transactionMapper).insertWmTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo("RELEASE");
        assertThat(txCaptor.getValue().getQuantity()).isEqualByComparingTo(new BigDecimal("50")); // 正数=释放

        // 验证 header 恢复为 DRAFT
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("10. 释放预占：非CONFIRMED状态拒绝")
    void testReleaseAllocationRejectsNonConfirmed() {
        testHeader.setStatus("DRAFT");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.releaseAllocation(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已确认");
    }

    @Test
    @DisplayName("11. 释放预占：库存已不存在时跳过（不抛异常）")
    void testReleaseAllocationStockGone() {
        testHeader.setStatus("CONFIRMED");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(null);

        // 不抛异常，正常恢复状态
        service.releaseAllocation(1L);
        verify(issueHeaderMapper).updateWmIssueHeader(any(WmIssueHeader.class));
    }

    // ══════════════════════════════════════════════
    // executeIssue 状态校验测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("12. 执行出库：DRAFT状态拒绝（必须先确认）")
    void testExecuteIssueRejectsDraft() {
        testHeader.setStatus("DRAFT");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.executeIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已确认");
    }

    @Test
    @DisplayName("13. 执行出库：CONFIRMED状态允许执行")
    void testExecuteIssueAllowsConfirmed() {
        testHeader.setStatus("CONFIRMED");
        testStock.setQuantityAvailable(new BigDecimal("150")); // available != onhand（已预占）
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        service.executeIssue(1L);

        // 验证只更新了 onhand
        ArgumentCaptor<WmMaterialStock> stockCaptor = ArgumentCaptor.forClass(WmMaterialStock.class);
        verify(materialStockMapper).updateWmMaterialStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityOnhand())
                .isEqualByComparingTo(new BigDecimal("150")); // 200-50
        // available 应为 null（防止误写回DB）
        assertThat(stockCaptor.getValue().getQuantityAvailable()).isNull();

        // header 状态改为 POSTED
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("POSTED");
    }

    @Test
    @DisplayName("14. 执行出库：已POSTED拒绝重复执行")
    void testExecuteIssueRejectsReExecution() {
        testHeader.setStatus("POSTED");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.executeIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已执行");
    }

    @Test
    @DisplayName("15. 执行出库：库存不足拒绝")
    void testExecuteIssueInsufficientOnhand() {
        testHeader.setStatus("CONFIRMED");
        testStock.setQuantityOnhand(new BigDecimal("10")); // 只有10，需50
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        assertThatThrownBy(() -> service.executeIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("库存不足");
    }

    // ══════════════════════════════════════════════
    // 完整流程测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("16. 完整流程：确认→出库，available和onhand分步扣减")
    void testFullFlowConfirmThenExecute() {
        // === Step 1: 确认 ===
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(testStock);

        service.confirmIssue(1L);
        // onhand 不变，available 减少
        assertThat(testStock.getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(testStock.getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("150"));

        // === Step 2: 执行出库 ===
        testHeader.setStatus("CONFIRMED"); // 模拟确认后状态变更
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        // 重新 mock stock（available 已被扣过）
        WmMaterialStock stockAfterConfirm = new WmMaterialStock();
        stockAfterConfirm.setMaterialStockId(1L);
        stockAfterConfirm.setItemId(100L);
        stockAfterConfirm.setQuantityOnhand(new BigDecimal("200"));
        stockAfterConfirm.setQuantityAvailable(new BigDecimal("150"));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class))).thenReturn(stockAfterConfirm);

        service.executeIssue(1L);
        // onhand 减少，但 execute 不写 available
        assertThat(stockAfterConfirm.getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("150"));
        // available 在 execute 中被置 null（跳过动态 UPDATE），保持 DB 中的 150
    }
}
