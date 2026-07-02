package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.*;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.domain.mes.wm.*;
import com.ruoyi.system.domain.mes.pur.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.service.mes.pro.impl.ProWorkorderDocServiceImpl;
import com.ruoyi.system.service.mes.wm.*;
import com.ruoyi.system.service.mes.pur.*;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatcher;

/**
 * 工单齐套分析 → 触发生成单据 Service 单元测试
 * 技术栈：JUnit 5 + Mockito，不启动 Spring 容器
 * 覆盖：loadKitDashboard / generateDocuments / onFeedbackAudited / 各 generate* 方法 / 幂等
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("工单齐套文档生成服务单元测试")
class ProWorkorderDocServiceUnitTest {

    // ====== PRO Mappers ======
    @Mock private ProWorkorderMapper proWorkorderMapper;
    @Mock private ProFeedbackMapper proFeedbackMapper;
    @Mock private ProFeedbackConsumeMapper proConsumeMapper;
    @Mock private ProRouteProcessMapper proRouteProcessMapper;
    @Mock private ProDocGenerationLogMapper docLogMapper;

    // ====== PRO Services ======
    @Mock private IProWorkorderBomService proWorkorderBomService;
    @Mock private IProWorkorderService proWorkorderService;
    @Mock private IProTaskService proTaskService;

    // ====== WM Services ======
    @Mock private IWmIssueHeaderService wmIssueHeaderService;
    @Mock private IWmIssueLineService wmIssueLineService;
    @Mock private IWmRtIssueService wmRtIssueService;
    @Mock private IWmRtIssueLineService wmRtIssueLineService;
    @Mock private IWmItemRecptService wmItemRecptService;
    @Mock private IWmItemRecptLineService wmItemRecptLineService;
    @Mock private IWmMaterialStockService wmMaterialStockService;
    @Mock private IWmWarehouseService wmWarehouseService;

    // ====== Purchase Services ======
    @Mock private IPurOrderService purOrderService;
    @Mock private IPurOrderLineService purOrderLineService;
    @Mock private com.ruoyi.system.mapper.mes.md.MdItemVendorMapper mdItemVendorMapper;

    // ====== Infrastructure ======
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager txManager;

    @InjectMocks
    private ProWorkorderDocServiceImpl docService;

    private ProWorkorder testWorkorder;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");

        testWorkorder = new ProWorkorder();
        testWorkorder.setWorkorderId(1L);
        testWorkorder.setWorkorderCode("WO-001");
        testWorkorder.setWorkorderName("测试工单");
        testWorkorder.setProductId(100L);
        testWorkorder.setProductCode("PROD-001");
        testWorkorder.setProductName("测试产品");
        testWorkorder.setQuantity(new BigDecimal("100"));
        testWorkorder.setQuantityProduced(new BigDecimal("0"));
        testWorkorder.setUnitOfMeasure("PCS");
        testWorkorder.setUnitName("个");
        testWorkorder.setStatus("PREPARE");
        testWorkorder.setFactoryId(1L);

        // ---- Redis lock mock: execute() runs the Supplier and returns its result ----
        doAnswer(inv -> {
            Object supplier = inv.getArgument(1);
            if (supplier instanceof java.util.function.Supplier) {
                @SuppressWarnings("unchecked")
                java.util.function.Supplier<Object> s = (java.util.function.Supplier<Object>) supplier;
                return s.get();
            }
            // Runnable variant — just run it
            ((Runnable) supplier).run();
            return null;
        }).when(lockTemplate).execute(anyString(), any(java.util.function.Supplier.class));

        // ---- TransactionTemplate mock: support new TransactionTemplate(txManager).execute(callback) ----
        when(txManager.getTransaction(any())).thenReturn(mock(org.springframework.transaction.TransactionStatus.class));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ═══════════════════════════════════════
    // 1. loadKitDashboard 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("loadKitDashboard：PREPARE 工单，物料全部齐套")
    void should_returnDashboard_when_allSufficient() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // BOM
        ProWorkorderBom bom = bomRow(1L, 10L, "MAT-001", "白牛皮", "KG", new BigDecimal("50"));
        when(proWorkorderBomService.selectProWorkorderBomByWorkorderId(1L))
                .thenReturn(Collections.singletonList(bom));
        // 物料齐套检查（复用 IProWorkorderService）
        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", 10L); row.put("itemCode", "MAT-001"); row.put("itemName", "白牛皮");
        row.put("unitName", "KG"); row.put("requiredQty", new BigDecimal("50"));
        row.put("availableQty", new BigDecimal("800")); row.put("sufficient", true);
        row.put("shortageQty", BigDecimal.ZERO);
        materialRows.add(row);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);
        // 领料单/退料单/入库单 → 空
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        when(wmRtIssueService.selectWmRtIssueList(any())).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.emptyList());
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());

        ProWorkorderKitDashboardVO vo = docService.loadKitDashboard(1L);

        assertThat(vo.getWorkorderCode()).isEqualTo("WO-001");
        assertThat(vo.isAllSufficient()).isTrue();
        assertThat(vo.getSufficientCount()).isEqualTo(1);
        assertThat(vo.getShortageCount()).isEqualTo(0);
        assertThat(vo.isHasPurchaseRecommend()).isFalse();
        assertThat(vo.isHasIssueDocs()).isFalse();
        assertThat(vo.isReceiptReady()).isFalse();
    }

    @Test
    @DisplayName("loadKitDashboard：物料缺料，有采购建议")
    void should_returnPurchaseRecommend_when_shortageExists() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        ProWorkorderBom bom = bomRow(1L, 10L, "MAT-001", "胶水", "KG", new BigDecimal("200"));
        when(proWorkorderBomService.selectProWorkorderBomByWorkorderId(1L))
                .thenReturn(Collections.singletonList(bom));

        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", 10L); row.put("itemCode", "MAT-001"); row.put("itemName", "胶水");
        row.put("unitName", "KG"); row.put("requiredQty", new BigDecimal("200"));
        row.put("availableQty", new BigDecimal("50")); row.put("sufficient", false);
        row.put("shortageQty", new BigDecimal("150"));
        materialRows.add(row);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);
        // 无在途PO
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.emptyList());
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        when(wmRtIssueService.selectWmRtIssueList(any())).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.emptyList());
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());

        ProWorkorderKitDashboardVO vo = docService.loadKitDashboard(1L);

        assertThat(vo.isAllSufficient()).isFalse();
        assertThat(vo.getShortageCount()).isEqualTo(1);
        assertThat(vo.isHasPurchaseRecommend()).isTrue();
        assertThat(vo.getPurchaseRecommends()).hasSize(1);
        assertThat(vo.getPurchaseRecommends().get(0).getShortageQty()).isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("loadKitDashboard：COMPLETED 工单，有合格品可入库")
    void should_returnReceiptRecommend_when_completed() {
        testWorkorder.setStatus("COMPLETED");
        testWorkorder.setQuantityProduced(new BigDecimal("100"));
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(proWorkorderBomService.selectProWorkorderBomByWorkorderId(1L))
                .thenReturn(Collections.emptyList());
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(Collections.emptyList());
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        when(wmRtIssueService.selectWmRtIssueList(any())).thenReturn(Collections.emptyList());
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());
        // 有已审核的报工，合格 95 个
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setStatus("AUDITED");
        fb.setQuantityFeedback(new BigDecimal("100")); fb.setQuantityQualified(new BigDecimal("95"));
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.singletonList(fb));

        ProWorkorderKitDashboardVO vo = docService.loadKitDashboard(1L);

        assertThat(vo.getReceiptRecommend().isRecommended()).isTrue();
        assertThat(vo.getReceiptRecommend().getQualifiedQty()).isEqualByComparingTo("95");
        assertThat(vo.isReceiptReady()).isTrue();
    }

    // ═══════════════════════════════════════
    // 2. generateDocuments 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("generateDocuments：生成领料单")
    void should_generateIssue_when_requested() {
        testWorkorder.setStatus("PREPARE");
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        ProWorkorderBom bom = bomRow(1L, 10L, "MAT-001", "白牛皮", "KG", new BigDecimal("50"));
        when(proWorkorderBomService.selectProWorkorderBomByWorkorderId(1L))
                .thenReturn(Collections.singletonList(bom));
        when(proTaskService.selectProTaskList(any())).thenReturn(Collections.emptyList());
        // 无已有领料单
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseAll()).thenReturn(Collections.emptyList());
        doAnswer(inv -> { WmIssueHeader h = inv.getArgument(0); h.setIssueId(100L); return 1; })
                .when(wmIssueHeaderService).insertWmIssueHeader(any());
        when(wmIssueLineService.insertWmIssueLine(any())).thenReturn(1);
        when(wmIssueHeaderService.updateWmIssueHeader(any())).thenReturn(1);
        when(docLogMapper.insert(any())).thenReturn(1);
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        ProDocGenerationRequestVO req = new ProDocGenerationRequestVO();
        req.setWorkorderId(1L);
        req.setGenerateIssue(true);

        ProDocGenerationResultVO result = docService.generateDocuments(req);

        assertThat(result.getIssues()).isNotNull();
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).get("issueCode")).isNotNull();
    }

    @Test
    @DisplayName("generateDocuments：生成采购单（缺料项）")
    void should_generatePurOrder_when_shortage() {
        testWorkorder.setStatus("PREPARE");
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // 缺料物料
        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", 10L); row.put("itemCode", "MAT-001"); row.put("itemName", "胶水");
        row.put("unitName", "KG"); row.put("requiredQty", new BigDecimal("200"));
        row.put("availableQty", new BigDecimal("50")); row.put("sufficient", false);
        row.put("shortageQty", new BigDecimal("150"));
        materialRows.add(row);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);
        doAnswer(inv -> { PurOrder p = inv.getArgument(0); p.setOrderId(200L); return 1; })
                .when(purOrderService).insertPurOrder(any());
        when(purOrderLineService.insertPurOrderLine(any())).thenReturn(1);
        when(purOrderService.selectPurOrderByOrderId(200L)).thenAnswer(inv -> {
            PurOrder po = new PurOrder(); po.setOrderId(200L); po.setStatus("DRAFT"); return po;
        });
        when(purOrderService.updatePurOrder(any())).thenReturn(1);

        ProDocGenerationRequestVO req = new ProDocGenerationRequestVO();
        req.setWorkorderId(1L);
        req.setGeneratePurOrder(true);

        ProDocGenerationResultVO result = docService.generateDocuments(req);

        assertThat(result.getPurOrders()).isNotNull();
        assertThat(result.getPurOrders()).hasSize(1);
    }

    @Test
    @DisplayName("generateDocuments：幂等 — 重复调用不产生重复领料单")
    void should_skipIssue_when_alreadyGenerated() {
        testWorkorder.setStatus("PREPARE");
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        ProWorkorderBom bom = bomRow(1L, 10L, "MAT-001", "白牛皮", "KG", new BigDecimal("50"));
        when(proWorkorderBomService.selectProWorkorderBomByWorkorderId(1L))
                .thenReturn(Collections.singletonList(bom));
        // 已有排产任务
        ProTask task = new ProTask();
        task.setTaskId(20L); task.setProcessId(30L); task.setWorkorderId(1L);
        when(proTaskService.selectProTaskList(any())).thenReturn(Collections.singletonList(task));
        // 已有领料单（按 workorderId + taskId 查重）
        WmIssueHeader existing = new WmIssueHeader();
        existing.setIssueId(100L); existing.setIssueCode("LL001");
        existing.setWorkorderId(1L); existing.setTaskId(20L); existing.setStatus("DRAFT");
        when(wmIssueHeaderService.selectWmIssueHeaderList(any()))
                .thenReturn(Collections.singletonList(existing));
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);

        ProDocGenerationRequestVO req = new ProDocGenerationRequestVO();
        req.setWorkorderId(1L);
        req.setGenerateIssue(true);

        ProDocGenerationResultVO result = docService.generateDocuments(req);

        // 跳过生成 → issues 为空
        assertThat(result.getIssues()).isNullOrEmpty();
    }

    // ═══════════════════════════════════════
    // 3. onFeedbackAudited 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("onFeedbackAudited：末工序报工审核 → 自动生成入库单 + 退料单，数量达标则完成工单")
    void should_generateReceiptAndReturn_when_lastProcessAndQtyMet() {
        testWorkorder.setStatus("PRODUCING");
        testWorkorder.setQuantityProduced(new BigDecimal("100"));
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setRouteId(10L); fb.setProcessId(30L);
        fb.setTaskId(20L); fb.setQuantityFeedback(new BigDecimal("100"));
        fb.setQuantityQualified(new BigDecimal("100"));
        when(proFeedbackMapper.selectProFeedbackByRecordId(1L)).thenReturn(fb);
        // 末工序
        ProRouteProcess lastProcess = new ProRouteProcess();
        lastProcess.setRouteId(10L); lastProcess.setProcessId(30L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(lastProcess);
        // 再次查询工单用于 auto-complete
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // 入库单
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseAll()).thenReturn(Collections.emptyList());
        doAnswer(inv -> { WmItemRecpt r = inv.getArgument(0); r.setRecptId(300L); return 1; })
                .when(wmItemRecptService).insertWmItemRecpt(any());
        when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
        // 退料单 — 无已过账领料单，不生成
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        // docLog
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);
        // 更新工单状态
        when(proWorkorderMapper.updateProWorkorder(any())).thenReturn(1);
        // 已审核报工
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(
                Collections.singletonList(fb));

        List<Map<String, Object>> result = docService.onFeedbackAudited(1L);

        assertThat(result).isNotEmpty();
        // 应生成 1 张入库单
        boolean hasReceipt = result.stream().anyMatch(m -> m.containsKey("recptId"));
        assertThat(hasReceipt).isTrue();
        // 工单应被标记为 COMPLETED（因为 quantityProduced=100 >= quantity=100）
        verify(proWorkorderMapper).updateProWorkorder(any());
    }

    @Test
    @DisplayName("onFeedbackAudited：非末工序报工 → 不触发自动生成")
    void should_notGenerate_when_notLastProcess() {
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setRouteId(10L); fb.setProcessId(20L);
        when(proFeedbackMapper.selectProFeedbackByRecordId(1L)).thenReturn(fb);
        // 末工序是 30L，但报工是 20L → 不是末工序
        ProRouteProcess lastProcess = new ProRouteProcess();
        lastProcess.setRouteId(10L); lastProcess.setProcessId(30L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(lastProcess);

        List<Map<String, Object>> result = docService.onFeedbackAudited(1L);

        assertThat(result).isEmpty();
        // 不应生成任何单据
        verify(wmItemRecptService, never()).insertWmItemRecpt(any());
        verify(wmRtIssueService, never()).insertWmRtIssue(any());
    }

    @Test
    @DisplayName("onFeedbackAudited：末工序但已生产数量不足 → 生成入库单但不完成工单")
    void should_notComplete_when_quantityNotMet() {
        testWorkorder.setStatus("PRODUCING");
        testWorkorder.setQuantityProduced(new BigDecimal("50")); // < 100
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setRouteId(10L); fb.setProcessId(30L);
        fb.setQuantityFeedback(new BigDecimal("50"));
        fb.setQuantityQualified(new BigDecimal("50"));
        when(proFeedbackMapper.selectProFeedbackByRecordId(1L)).thenReturn(fb);
        ProRouteProcess lastProcess = new ProRouteProcess();
        lastProcess.setRouteId(10L); lastProcess.setProcessId(30L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(lastProcess);
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // 入库单
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseAll()).thenReturn(Collections.emptyList());
        doAnswer(inv -> { WmItemRecpt r = inv.getArgument(0); r.setRecptId(300L); return 1; })
                .when(wmItemRecptService).insertWmItemRecpt(any());
        when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.singletonList(fb));

        docService.onFeedbackAudited(1L);

        // 入库单已生成
        verify(wmItemRecptService).insertWmItemRecpt(any());
        // 但工单状态不应更新（因为 50 < 100）
        verify(proWorkorderMapper, never()).updateProWorkorder(any());
    }

    @Test
    @DisplayName("onFeedbackAudited：Redis 锁防并发")
    void should_useLock_when_onFeedbackAudited() {
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setRouteId(10L); fb.setProcessId(30L);
        when(proFeedbackMapper.selectProFeedbackByRecordId(1L)).thenReturn(fb);
        ProRouteProcess lastProcess = new ProRouteProcess();
        lastProcess.setRouteId(10L); lastProcess.setProcessId(30L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(lastProcess);
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.emptyList());

        docService.onFeedbackAudited(1L);

        // 验证使用了正确的锁 key
        verify(lockTemplate).execute(eq("feedback:audit:lastProcess:1"), any(java.util.function.Supplier.class));
    }

    // ═══════════════════════════════════════
    // 4. generateProductReceipt 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("generateProductReceipt：有合格品 → 生成入库单")
    void should_createReceipt_when_qualifiedExists() {
        testWorkorder.setStatus("COMPLETED");
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(wmItemRecptService.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setStatus("AUDITED");
        fb.setQuantityQualified(new BigDecimal("95"));
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.singletonList(fb));
        when(wmWarehouseService.selectWmWarehouseList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseAll()).thenReturn(Collections.emptyList());
        doAnswer(inv -> { WmItemRecpt r = inv.getArgument(0); r.setRecptId(300L); return 1; })
                .when(wmItemRecptService).insertWmItemRecpt(any());
        when(wmItemRecptLineService.insertWmItemRecptLine(any())).thenReturn(1);
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);
        when(wmItemRecptService.selectWmItemRecptByRecptId(300L)).thenAnswer(inv -> {
            WmItemRecpt r = new WmItemRecpt(); r.setRecptId(300L); r.setRecptCode("RK001"); return r;
        });

        WmItemRecpt result = docService.generateProductReceipt(1L);

        assertThat(result).isNotNull();
        assertThat(result.getRecptCode()).isEqualTo("RK001");
    }

    @Test
    @DisplayName("generateProductReceipt：幂等 — 已有入库单则跳过")
    void should_skipReceipt_when_alreadyExists() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // 已有日志记录
        ProDocGenerationLog existingLog = new ProDocGenerationLog();
        existingLog.setDocId(300L); existingLog.setDocCode("RK001");
        existingLog.setDocType("RECPT");
        when(docLogMapper.selectList(any())).thenReturn(Collections.singletonList(existingLog));
        when(wmItemRecptService.selectWmItemRecptByRecptId(300L)).thenAnswer(inv -> {
            WmItemRecpt r = new WmItemRecpt(); r.setRecptId(300L); r.setRecptCode("RK001"); return r;
        });

        WmItemRecpt result = docService.generateProductReceipt(1L);

        // 返回已有单据，不应插入新单据
        assertThat(result.getRecptCode()).isEqualTo("RK001");
        verify(wmItemRecptService, never()).insertWmItemRecpt(any());
    }

    // ═══════════════════════════════════════
    // 5. generateMaterialReturn 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("generateMaterialReturn：有余料 → 生成退料单")
    void should_createReturn_when_unconsumedMaterials() {
        testWorkorder.setStatus("COMPLETED");
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // 已过账的领料单
        WmIssueHeader issue = new WmIssueHeader();
        issue.setIssueId(50L); issue.setIssueCode("LL001"); issue.setWorkorderId(1L);
        issue.setStatus("POSTED");
        when(wmIssueHeaderService.selectWmIssueHeaderList(any()))
                .thenReturn(Collections.singletonList(issue));
        // 领料单行：领了 200
        WmIssueLine issueLine = new WmIssueLine();
        issueLine.setIssueId(50L); issueLine.setItemId(10L);
        issueLine.setItemCode("MAT-001"); issueLine.setItemName("胶水");
        issueLine.setUnitOfMeasure("KG"); issueLine.setUnitName("KG");
        issueLine.setQuantityIssue(new BigDecimal("200"));
        when(wmIssueLineService.selectWmIssueLineList(any()))
                .thenReturn(Collections.singletonList(issueLine));
        // 报工消耗了 150
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(1L); fb.setWorkorderId(1L); fb.setStatus("AUDITED");
        when(proFeedbackMapper.selectProFeedbackList(any())).thenReturn(Collections.singletonList(fb));
        ProFeedbackConsume consume = new ProFeedbackConsume();
        consume.setFeedbackId(1L); consume.setItemId(10L); consume.setQuantity(new BigDecimal("150"));
        when(proConsumeMapper.selectByFeedbackId(1L)).thenReturn(Collections.singletonList(consume));
        // 仓库
        when(wmWarehouseService.selectWmWarehouseList(any())).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseAll()).thenReturn(Collections.emptyList());
        // 退料单
        doAnswer(inv -> { WmRtIssue rt = inv.getArgument(0); rt.setRtId(400L); return 1; })
                .when(wmRtIssueService).insertWmRtIssue(any());
        when(wmRtIssueLineService.insertWmRtIssueLine(any())).thenReturn(1);
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);
        when(wmRtIssueService.selectWmRtIssueByRtId(400L)).thenAnswer(inv -> {
            WmRtIssue rt = new WmRtIssue(); rt.setRtId(400L); rt.setRtCode("RT001");
            rt.setQuantityTotal(new BigDecimal("50")); return rt;
        });

        WmRtIssue result = docService.generateMaterialReturn(1L);

        assertThat(result).isNotNull();
        assertThat(result.getQuantityTotal()).isEqualByComparingTo("50"); // 200-150=50
    }

    // ═══════════════════════════════════════
    // 6. generatePurchaseOrder 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("generatePurchaseOrder：缺料 → 生成采购单")
    void should_createPurOrder_when_shortage() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", 10L); row.put("itemCode", "MAT-001"); row.put("itemName", "胶水");
        row.put("unitName", "KG"); row.put("requiredQty", new BigDecimal("200"));
        row.put("availableQty", new BigDecimal("50")); row.put("sufficient", false);
        row.put("shortageQty", new BigDecimal("150"));
        materialRows.add(row);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);
        when(purOrderLineService.selectPurOrderLineList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any())).thenReturn(1);
        doAnswer(inv -> { PurOrder p = inv.getArgument(0); p.setOrderId(200L); return 1; })
                .when(purOrderService).insertPurOrder(any());
        when(purOrderLineService.insertPurOrderLine(any())).thenReturn(1);
        when(purOrderService.selectPurOrderByOrderId(200L)).thenAnswer(inv -> {
            PurOrder po = new PurOrder(); po.setOrderId(200L); po.setStatus("DRAFT"); return po;
        });
        when(purOrderService.updatePurOrder(any())).thenReturn(1);

        Map<String, Object> result = docService.generatePurchaseOrder(1L);

        assertThat(result).containsKey("purOrders");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pos = (List<Map<String, Object>>) result.get("purOrders");
        assertThat(pos).hasSize(1);
    }

    @Test
    @DisplayName("generatePurchaseOrder：全部齐套 → 不生成采购单")
    void should_skipPurOrder_when_allSufficient() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", 10L); row.put("itemCode", "MAT-001"); row.put("itemName", "白牛皮");
        row.put("unitName", "KG"); row.put("requiredQty", new BigDecimal("50"));
        row.put("availableQty", new BigDecimal("800")); row.put("sufficient", true);
        row.put("shortageQty", BigDecimal.ZERO);
        materialRows.add(row);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);

        Map<String, Object> result = docService.generatePurchaseOrder(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pos = (List<Map<String, Object>>) result.get("purOrders");
        assertThat(pos).isEmpty();
    }

    // ═══════════════════════════════════════
    // 7. buildPurOrderWizard 测试
    // ═══════════════════════════════════════

    @Test
    @DisplayName("buildPurOrderWizard：缺料物料 → 返回推荐行（按供应商分组）")
    void should_returnWizard_withVendorGroups() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        // 2项缺料：itemId=10（无供应商），itemId=20（有首选供应商）
        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("itemId", 10L); row1.put("itemCode", "MAT-001"); row1.put("itemName", "白牛皮");
        row1.put("unitName", "KG"); row1.put("shortageQty", new BigDecimal("150")); row1.put("sufficient", false);
        materialRows.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("itemId", 20L); row2.put("itemCode", "MAT-002"); row2.put("itemName", "胶水");
        row2.put("unitName", "KG"); row2.put("shortageQty", new BigDecimal("100")); row2.put("sufficient", false);
        materialRows.add(row2);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);

        // item 20 有首选供应商，item 10 无
        com.ruoyi.system.domain.mes.md.MdItemVendor iv = new com.ruoyi.system.domain.mes.md.MdItemVendor();
        iv.setItemId(20L); iv.setVendorId(200L); iv.setVendorCode("V-001"); iv.setVendorName("圣享纸业");
        iv.setIsPreferred("Y"); iv.setMinOrderQty(new BigDecimal("50")); iv.setLeadTimeDays(7);
        when(mdItemVendorMapper.selectList(argThat((com.ruoyi.system.domain.mes.md.MdItemVendor q) -> q != null && Long.valueOf(20L).equals(q.getItemId())))).thenReturn(Collections.singletonList(iv));
        when(mdItemVendorMapper.selectList(argThat((com.ruoyi.system.domain.mes.md.MdItemVendor q) -> q != null && !Long.valueOf(20L).equals(q.getItemId())))).thenReturn(Collections.emptyList());

        Map<String, Object> result = docService.buildPurOrderWizard(1L);

        assertThat(result).containsKey("lines");
        assertThat(result).containsKey("vendorCount");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) result.get("lines");
        assertThat(lines).hasSize(2);
        // item 20: hasVendor=true, checked=true
        assertThat(lines.get(1).get("hasVendor")).isEqualTo(true);
        assertThat(lines.get(1).get("checked")).isEqualTo(true);
        assertThat(lines.get(1).get("vendorName")).isEqualTo("圣享纸业");
        // item 10: hasVendor=false, checked=false (无供应商默认不勾选)
        assertThat(lines.get(0).get("hasVendor")).isEqualTo(false);
        assertThat(lines.get(0).get("checked")).isEqualTo(false);
    }

    @Test
    @DisplayName("buildPurOrderWizard：全部齐套 → 返回空列表")
    void should_returnEmpty_when_allSufficient() {
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        List<Map<String, Object>> materialRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", 10L); row.put("itemName", "纸"); row.put("shortageQty", BigDecimal.ZERO); row.put("sufficient", true);
        materialRows.add(row);
        when(proWorkorderService.checkMaterialReadiness(1L)).thenReturn(materialRows);

        Map<String, Object> result = docService.buildPurOrderWizard(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) result.get("lines");
        assertThat(lines).isEmpty();
    }

    // ====== helper ======

    private ProWorkorderBom bomRow(Long lineId, Long itemId, String itemCode, String itemName,
                                    String unitName, BigDecimal quantity) {
        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setLineId(lineId);
        bom.setProcessId(30L);
        bom.setProcessName("测试工序");
        bom.setItemId(itemId);
        bom.setItemCode(itemCode);
        bom.setItemName(itemName);
        bom.setUnitName(unitName);
        bom.setQuantity(quantity);
        bom.setTotalQuantity(quantity.multiply(new BigDecimal("100"))); // 单位用量 * 工单数量
        return bom;
    }
}
