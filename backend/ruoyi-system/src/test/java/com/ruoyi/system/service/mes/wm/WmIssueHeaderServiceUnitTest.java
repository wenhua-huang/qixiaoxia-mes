package com.ruoyi.system.service.mes.wm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
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
 * 覆盖：confirmIssue（预占库存）、releaseAllocation（释放预占）、executeIssue（出库状态校验）、issueOut（分批发料 + available 钳制）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("领料单服务单元测试")
class WmIssueHeaderServiceUnitTest {

    @Mock private WmIssueHeaderMapper issueHeaderMapper;
    @Mock private WmIssueLineMapper issueLineMapper;
    @Mock private WmMaterialStockMapper materialStockMapper;
    @Mock private WmTransactionMapper transactionMapper;
    @Mock private ProMaterialTraceMapper materialTraceMapper;
    @Mock private ProWorkorderMapper proWorkorderMapper;
    @Mock private com.ruoyi.system.mapper.mes.wm.WmIssueDetailMapper issueDetailMapper;
    @Mock private com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator autoCodeGenerator;
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

        // Mock lockTemplate：直接执行 Runnable action（绕过 Redis）；lenient 因 submit/approve 单条不走锁
        lenient().doAnswer(inv -> { Runnable action = inv.getArgument(2); action.run(); return null; })
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
        testLine.setBatchId(1L); // 指定批次 → confirm 走 loadStockForUpdate 精确匹配路径

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
    @DisplayName("1. 确认领料单：成功预占库存（DRAFT→ALLOCATED，扣quantityAvailable）")
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
        assertThat(stockCaptor.getValue().getQuantityOnhand())
                .isEqualByComparingTo(new BigDecimal("200")); // 预占不动 onhand → occupied=onhand-available=50 体现

        // 验证事务记录：ALLOCATE
        ArgumentCaptor<WmTransaction> txCaptor = ArgumentCaptor.forClass(WmTransaction.class);
        verify(transactionMapper).insertWmTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo("ALLOCATE");
        assertThat(txCaptor.getValue().getQuantity()).isEqualByComparingTo(new BigDecimal("-50"));

        // 验证 header 状态改为 ALLOCATED（已预占）
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("ALLOCATED");
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
        testHeader.setStatus("ALLOCATED");
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
    @DisplayName("9. 释放预占：ALLOCATED→APPROVED，恢复quantityAvailable")
    void testReleaseAllocationSuccess() {
        testHeader.setStatus("ALLOCATED");
        testStock.setQuantityAvailable(new BigDecimal("150")); // 已预占50
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        // release 反查 ALLOCATE 事务 + 按 materialStockId 归还 available
        WmTransaction allocTx = new WmTransaction();
        allocTx.setMaterialStockId(1L);
        allocTx.setTransactionType("ALLOCATE");
        allocTx.setQuantity(new BigDecimal("-50"));
        when(transactionMapper.selectWmTransactionList(any(WmTransaction.class))).thenReturn(List.of(allocTx));
        when(materialStockMapper.selectWmMaterialStockByMaterialStockId(1L)).thenReturn(testStock);

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

        // 验证 header 恢复为 APPROVED（释放预占后回到已下达，可再次预占）
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("10. 释放预占：非ALLOCATED状态拒绝")
    void testReleaseAllocationRejectsNonAllocated() {
        testHeader.setStatus("DRAFT");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.releaseAllocation(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已预占");
    }

    @Test
    @DisplayName("11. 释放预占：库存已不存在时跳过（不抛异常）")
    void testReleaseAllocationStockGone() {
        testHeader.setStatus("ALLOCATED");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        WmTransaction allocTx = new WmTransaction();
        allocTx.setMaterialStockId(1L);
        allocTx.setTransactionType("ALLOCATE");
        allocTx.setQuantity(new BigDecimal("-50"));
        when(transactionMapper.selectWmTransactionList(any(WmTransaction.class))).thenReturn(List.of(allocTx));
        when(materialStockMapper.selectWmMaterialStockByMaterialStockId(1L)).thenReturn(null); // 库存已不存在

        // 不抛异常，正常恢复状态
        service.releaseAllocation(1L);
        verify(issueHeaderMapper).updateWmIssueHeader(any(WmIssueHeader.class));
    }

    // ══════════════════════════════════════════════
    // executeIssue 状态校验测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("12. 执行出库：DRAFT状态拒绝（必须先预占）")
    void testExecuteIssueRejectsDraft() {
        testHeader.setStatus("DRAFT");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.executeIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已预占");
    }

    @Test
    @DisplayName("13. 执行出库：ALLOCATED状态允许执行")
    void testExecuteIssueAllowsAllocated() {
        testHeader.setStatus("ALLOCATED");
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
        // available 钳制为 min(原150, 新onhand150)=150（消费预占不变，与原语义一致）
        assertThat(stockCaptor.getValue().getQuantityAvailable())
                .isEqualByComparingTo(new BigDecimal("150"));

        // header 状态改为 ISSUED（已发料）
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("14. 执行出库：已ISSUED拒绝重复执行")
    void testExecuteIssueRejectsReExecution() {
        testHeader.setStatus("ISSUED");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);

        assertThatThrownBy(() -> service.executeIssue(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已执行");
    }

    @Test
    @DisplayName("15. 执行出库：库存不足拒绝")
    void testExecuteIssueInsufficientOnhand() {
        testHeader.setStatus("ALLOCATED");
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
        testHeader.setStatus("ALLOCATED"); // 模拟确认（预占）后状态变更
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
        // onhand 减少；available = min(原150, 新onhand150) = 150（消费预占，不变）
        assertThat(stockAfterConfirm.getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(stockAfterConfirm.getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("150"));
    }

    // ══════════════════════════════════════════════
    // issueOut 测试（分批发料出库 + available 钳制）
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("17. 发料出库：available 钳制 ≤ onhand（修复 available 虚高根因）")
    void testIssueOutClampsAvailableToOnhand() {
        testHeader.setStatus("ALLOCATED");
        testHeader.setQuantityTotal(new BigDecimal("10"));
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        // 模拟脏数据：available(100) 虚高 > onhand(20)
        WmMaterialStock dirtyStock = new WmMaterialStock();
        dirtyStock.setMaterialStockId(1L);
        dirtyStock.setItemId(100L);
        dirtyStock.setItemCode("MAT-001");
        dirtyStock.setBatchId(999L);
        dirtyStock.setQuantityOnhand(new BigDecimal("20"));
        dirtyStock.setQuantityAvailable(new BigDecimal("100"));
        // 重构后：指定批次先发只读探测 loadMaterialStock，再按 stockId 升序 for update
        when(materialStockMapper.loadMaterialStock(any(WmMaterialStock.class))).thenReturn(dirtyStock);
        when(materialStockMapper.selectMaterialStockForUpdateById(1L)).thenReturn(dirtyStock);
        // 净预占 10（该批次已预占，toSwap=0 直接发料，验证 available 钳制）
        WmTransaction allocTx = new WmTransaction();
        allocTx.setMaterialStockId(1L);
        allocTx.setTransactionType("ALLOCATE");
        allocTx.setQuantity(new BigDecimal("-10"));
        when(transactionMapper.selectWmTransactionList(any(WmTransaction.class))).thenReturn(List.of(allocTx));

        WmIssueDetail d = new WmIssueDetail();
        d.setLineId(1L);
        d.setItemId(100L);
        d.setItemCode("MAT-001");
        d.setItemName("测试物料");
        d.setUnitOfMeasure("KG");
        d.setBatchId(999L); // 指定批次 → issueOutSingleBatch
        d.setQuantity(new BigDecimal("10"));

        service.issueOut(1L, List.of(d));

        // 出库后 onhand=10；available=min(100,10)=10（被钳到 onhand，不再虚高）
        ArgumentCaptor<WmMaterialStock> stockCaptor = ArgumentCaptor.forClass(WmMaterialStock.class);
        verify(materialStockMapper).updateWmMaterialStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(stockCaptor.getValue().getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    @DisplayName("18. 发料出库：净预占足但 onhand 不足 → 报预占库存不足（回归 issue 231）")
    void testIssueOutThrowsWhenAllocatedOnhandInsufficient() {
        testHeader.setStatus("ALLOCATED");
        testHeader.setQuantityTotal(new BigDecimal("100"));
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        // 净预占 stock5=100（ALLOCATE -100）
        WmTransaction allocTx = new WmTransaction();
        allocTx.setMaterialStockId(5L);
        allocTx.setTransactionType("ALLOCATE");
        allocTx.setQuantity(new BigDecimal("-100"));
        when(transactionMapper.selectWmTransactionList(any(WmTransaction.class))).thenReturn(List.of(allocTx));
        // 实际 onhand 只有 14（被历史发料扣走，available 却虚高）
        WmMaterialStock phantomStock = new WmMaterialStock();
        phantomStock.setMaterialStockId(5L);
        phantomStock.setItemId(100L);
        phantomStock.setItemCode("MAT-001");
        phantomStock.setItemName("测试物料");
        phantomStock.setUnitOfMeasure("KG");
        phantomStock.setQuantityOnhand(new BigDecimal("14"));
        when(materialStockMapper.selectMaterialStockForUpdateById(5L)).thenReturn(phantomStock);

        WmIssueDetail d = new WmIssueDetail();
        d.setLineId(1L);
        d.setItemId(100L);
        d.setItemCode("MAT-001");
        d.setBatchId(null); // 未指定批次 → 按净预占扣
        d.setQuantity(new BigDecimal("100"));

        assertThatThrownBy(() -> service.issueOut(1L, List.of(d)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("预占库存不足");
    }

    @Test
    @DisplayName("19. 发料出库：净预占与 onhand 都足 → 成功，available 消费预占不变，状态→ISSUED")
    void testIssueOutSuccessWhenAllocationMeetsDemand() {
        testHeader.setStatus("ALLOCATED");
        testHeader.setQuantityTotal(new BigDecimal("100"));
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        WmTransaction allocTx = new WmTransaction();
        allocTx.setMaterialStockId(5L);
        allocTx.setTransactionType("ALLOCATE");
        allocTx.setQuantity(new BigDecimal("-100"));
        when(transactionMapper.selectWmTransactionList(any(WmTransaction.class))).thenReturn(List.of(allocTx));
        WmMaterialStock stock = new WmMaterialStock();
        stock.setMaterialStockId(5L);
        stock.setItemId(100L);
        stock.setItemCode("MAT-001");
        stock.setItemName("测试物料");
        stock.setUnitOfMeasure("KG");
        stock.setQuantityOnhand(new BigDecimal("200"));
        stock.setQuantityAvailable(new BigDecimal("100")); // 已预占 100
        when(materialStockMapper.selectMaterialStockForUpdateById(5L)).thenReturn(stock);

        WmIssueDetail d = new WmIssueDetail();
        d.setLineId(1L);
        d.setItemId(100L);
        d.setItemCode("MAT-001");
        d.setBatchId(null);
        d.setQuantity(new BigDecimal("100"));

        service.issueOut(1L, List.of(d));

        // onhand 扣 100；available=min(原100, 新onhand100)=100（消费预占，不变）
        ArgumentCaptor<WmMaterialStock> stockCaptor = ArgumentCaptor.forClass(WmMaterialStock.class);
        verify(materialStockMapper).updateWmMaterialStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(stockCaptor.getValue().getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("100"));
        ArgumentCaptor<WmIssueHeader> headerCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).updateWmIssueHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getStatus()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("20. 释放预占：净预占为 0 时幂等（已释放过不重复归还 available）")
    void testReleaseNoOpWhenNetAllocationZero() {
        testHeader.setStatus("ALLOCATED");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        // 事务历史含 ALLOCATE(-50) + 已有 RELEASE(+50) → 净预占 0（模拟已释放过）
        WmTransaction allocTx = new WmTransaction();
        allocTx.setMaterialStockId(1L);
        allocTx.setTransactionType("ALLOCATE");
        allocTx.setQuantity(new BigDecimal("-50"));
        WmTransaction prevRelease = new WmTransaction();
        prevRelease.setMaterialStockId(1L);
        prevRelease.setTransactionType("RELEASE");
        prevRelease.setQuantity(new BigDecimal("50"));
        when(transactionMapper.selectWmTransactionList(any(WmTransaction.class)))
                .thenReturn(List.of(allocTx, prevRelease));

        service.releaseAllocation(1L);

        // 净预占 0 → 不归还 available、不写 RELEASE（幂等，防 release 重复归还根因）
        verify(materialStockMapper, never()).updateWmMaterialStock(any(WmMaterialStock.class));
        verify(transactionMapper, never()).insertWmTransaction(any(WmTransaction.class));
        // 状态仍正常流转 APPROVED
        verify(issueHeaderMapper).updateWmIssueHeader(any(WmIssueHeader.class));
    }

    @Test
    @DisplayName("21. 确认预占：FIFO 多批次自动分配，按批次扣 available + 写 ALLOCATE")
    void testConfirmFifoAllocation() {
        testLine.setBatchId(null); // 未指定批次 → 走 selectAvailableStocksForFifo FIFO
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(testHeader);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));
        WmMaterialStock s1 = new WmMaterialStock();
        s1.setMaterialStockId(10L);
        s1.setItemId(100L);
        s1.setItemCode("MAT-001");
        s1.setItemName("测试物料");
        s1.setUnitOfMeasure("KG");
        s1.setQuantityOnhand(new BigDecimal("100"));
        s1.setQuantityAvailable(new BigDecimal("100"));
        when(materialStockMapper.selectAvailableStocksForFifo(eq(100L), any(), eq("NORMAL")))
                .thenReturn(List.of(s1));

        service.confirmIssue(1L);

        // FIFO 预占 50：s1 avail 100→50，onhand 不变
        ArgumentCaptor<WmMaterialStock> stockCaptor = ArgumentCaptor.forClass(WmMaterialStock.class);
        verify(materialStockMapper).updateWmMaterialStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(stockCaptor.getValue().getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("100"));
        verify(transactionMapper).insertWmTransaction(any(WmTransaction.class));
    }

    // ══════════════════════════════════════════════
    // 批量操作测试（batchSubmitForApprove / batchApprove / batchConfirmIssue）
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("22. 批量提交审核：混合 DRAFT+非草稿，成功1失败1，failures 含原因")
    void testBatchSubmitMixedStatus() {
        // 单1：DRAFT + 有明细 → 成功；单2：PENDING → 失败
        WmIssueHeader h1 = new WmIssueHeader(); h1.setIssueId(1L); h1.setStatus("DRAFT");
        WmIssueHeader h2 = new WmIssueHeader(); h2.setIssueId(2L); h2.setIssueCode("LL002"); h2.setIssueName("单2"); h2.setStatus("PENDING");
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(h1, h1); // submit 内查 + executeBatch 失败回查（成功路径不回查）
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(2L)).thenReturn(h2);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class))).thenReturn(List.of(testLine));

        Map<String, Object> r = service.batchSubmitForApprove(new Long[]{1L, 2L});

        assertThat(r.get("total")).isEqualTo(2);
        assertThat(r.get("successCount")).isEqualTo(1);
        assertThat(r.get("failedCount")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failures = (List<Map<String, Object>>) r.get("failures");
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).get("issueCode")).isEqualTo("LL002");
        assertThat((String) failures.get(0).get("reason")).contains("草稿");
    }

    @Test
    @DisplayName("23. 批量预占库存：库存不足单据进 failures，其余成功，返回明细含 issueCode/reason")
    void testBatchConfirmInsufficientStock() {
        // 单1：APPROVED + 库存足 → 成功；单3：APPROVED + 库存不足 → 失败
        WmIssueHeader h1 = new WmIssueHeader(); h1.setIssueId(1L); h1.setStatus("APPROVED"); h1.setWarehouseId(1L);
        WmIssueHeader h3 = new WmIssueHeader(); h3.setIssueId(3L); h3.setIssueCode("LL003"); h3.setIssueName("单3"); h3.setStatus("APPROVED"); h3.setWarehouseId(1L);
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(1L)).thenReturn(h1);
        when(issueHeaderMapper.selectWmIssueHeaderByIssueId(3L)).thenReturn(h3);
        WmIssueLine lineFor3 = new WmIssueLine(); lineFor3.setLineId(3L); lineFor3.setIssueId(3L); lineFor3.setItemId(100L);
        lineFor3.setQuantityIssue(new BigDecimal("50")); lineFor3.setWarehouseId(1L); lineFor3.setBatchId(1L);
        when(issueLineMapper.selectWmIssueLineList(any(WmIssueLine.class)))
                .thenReturn(List.of(testLine))     // 单1
                .thenReturn(List.of(lineFor3));     // 单3
        // 单1 库存足(200)，单3 库存不足(10)
        WmMaterialStock stockEnough = new WmMaterialStock(); stockEnough.setMaterialStockId(1L); stockEnough.setItemId(100L);
        stockEnough.setQuantityOnhand(new BigDecimal("200")); stockEnough.setQuantityAvailable(new BigDecimal("200"));
        WmMaterialStock stockLow = new WmMaterialStock(); stockLow.setMaterialStockId(1L); stockLow.setItemId(100L);
        stockLow.setQuantityOnhand(new BigDecimal("200")); stockLow.setQuantityAvailable(new BigDecimal("10"));
        when(materialStockMapper.loadMaterialStockForUpdate(any(WmMaterialStock.class)))
                .thenReturn(stockEnough).thenReturn(stockLow);

        Map<String, Object> r = service.batchConfirmIssue(new Long[]{1L, 3L});

        assertThat(r.get("successCount")).isEqualTo(1);
        assertThat(r.get("failedCount")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failures = (List<Map<String, Object>>) r.get("failures");
        assertThat(failures.get(0).get("issueCode")).isEqualTo("LL003");
        assertThat((String) failures.get(0).get("reason")).contains("可用库存不足");
    }

    @Test
    @DisplayName("24. 批量操作：空数组拒绝")
    void testBatchRejectsEmpty() {
        assertThatThrownBy(() -> service.batchSubmitForApprove(new Long[]{}))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未选择");
        assertThatThrownBy(() -> service.batchApprove(null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未选择");
    }

    // ══════════════════════════════════════════════
    // insertWmIssueHeader 工序级幂等测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("25. 创建领料单：有taskId时仍按processId查重，拦截排产前已存在的同工序单")
    void testInsertBlocksDuplicateByProcessIdWhenTaskIdPresent() {
        // 排产前已存在一张 DRAFT 单：processId=200, taskId=null
        WmIssueHeader existingDraft = new WmIssueHeader();
        existingDraft.setIssueId(99L);
        existingDraft.setIssueCode("LL-EXISTING");
        existingDraft.setStatus("DRAFT");

        // 新单：排产后有了 taskId，但 processId 相同
        WmIssueHeader newHeader = new WmIssueHeader();
        newHeader.setIssueCode("LL-NEW");
        newHeader.setWorkorderId(10L);
        newHeader.setWorkorderCode("WO-001");
        newHeader.setIssueType("PRODUCE");
        newHeader.setWarehouseId(1L);
        newHeader.setProcessId(200L);
        newHeader.setProcessName("印刷");
        newHeader.setTaskId(602L);

        when(issueHeaderMapper.selectWmIssueHeaderList(any(WmIssueHeader.class)))
                .thenReturn(List.of(existingDraft));

        assertThatThrownBy(() -> service.insertWmIssueHeader(newHeader))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已有进行中的领料单");

        // 验证查询用的是 processId（不是 taskId），锁 key 也含 processId
        ArgumentCaptor<WmIssueHeader> qCaptor = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(issueHeaderMapper).selectWmIssueHeaderList(qCaptor.capture());
        assertThat(qCaptor.getValue().getProcessId()).isEqualTo(200L);
        assertThat(qCaptor.getValue().getTaskId()).isNull();

        ArgumentCaptor<String> lockCaptor = ArgumentCaptor.forClass(String.class);
        verify(lockTemplate).execute(lockCaptor.capture(), anyLong(), any(Runnable.class));
        assertThat(lockCaptor.getValue()).contains(":p:200");

        verify(issueHeaderMapper, never()).insertWmIssueHeader(any());
    }

    @Test
    @DisplayName("26. 创建领料单：同processId已有单为终态(CANCELED)时允许重建")
    void testInsertAllowsWhenExistingIsTerminal() {
        WmIssueHeader canceled = new WmIssueHeader();
        canceled.setIssueId(99L);
        canceled.setIssueCode("LL-CANCELED");
        canceled.setStatus("CANCELED"); // 终态

        WmIssueHeader newHeader = new WmIssueHeader();
        newHeader.setIssueCode("LL-NEW");
        newHeader.setWorkorderId(10L);
        newHeader.setWorkorderCode("WO-001");
        newHeader.setIssueType("PRODUCE");
        newHeader.setWarehouseId(1L);
        newHeader.setProcessId(200L);
        newHeader.setProcessName("印刷");

        when(issueHeaderMapper.selectWmIssueHeaderList(any(WmIssueHeader.class)))
                .thenReturn(List.of(canceled));

        service.insertWmIssueHeader(newHeader);

        // 终态单不阻断 → 执行了 insert
        verify(issueHeaderMapper).insertWmIssueHeader(newHeader);
    }
}
