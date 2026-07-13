package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.*;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.domain.mes.wm.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptMapper;
import com.ruoyi.system.service.mes.pro.*;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * generateReceiptDocuments 重构后单测（feedback 级幂等）。
 *
 * 覆盖：
 * 1) 自动路径 (feedbackId != null) — 数量取本次 feedback 合格数，log 带 sourceFeedbackId
 * 2) 自动路径幂等 — 同一 feedback 已有 log → buildExistingRecptInfo 分支
 * 3) 手动路径 (feedbackId == null) — 数量 = produced − sumReceipted
 * 4) 手动路径差额为 0 → 空返回
 * 5) DB 唯一索引兜底 — insertLog 抛 DuplicateKeyException 转为"已存在"结构
 *
 * @author qixiaoxia
 * @date 2026-07-13
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("产品入库单生成 - feedback 级幂等")
class ProWorkorderDocServiceReceiptTest {

    @Mock private ProWorkorderMapper proWorkorderMapper;
    @Mock private ProFeedbackMapper proFeedbackMapper;
    @Mock private ProFeedbackConsumeMapper proConsumeMapper;
    @Mock private ProRouteProcessMapper proRouteProcessMapper;
    @Mock private ProDocGenerationLogMapper docLogMapper;
    @Mock private IProWorkorderBomService proWorkorderBomService;
    @Mock private IProWorkorderService proWorkorderService;
    @Mock private IProTaskService proTaskService;
    @Mock private IWmIssueHeaderService wmIssueHeaderService;
    @Mock private IWmIssueLineService wmIssueLineService;
    @Mock private IWmRtIssueService wmRtIssueService;
    @Mock private IWmRtIssueLineService wmRtIssueLineService;
    @Mock private IWmProductRecptService wmProductRecptService;
    @Mock private IWmProductRecptLineService wmProductRecptLineService;
    @Mock private WmProductRecptMapper wmProductRecptMapper;
    @Mock private IWmMaterialStockService wmMaterialStockService;
    @Mock private IWmWarehouseService wmWarehouseService;
    @Mock private IPurOrderService purOrderService;
    @Mock private IPurOrderLineService purOrderLineService;
    @Mock private com.ruoyi.system.mapper.mes.md.MdItemVendorMapper mdItemVendorMapper;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager txManager;

    @InjectMocks
    private ProWorkorderDocServiceImpl docService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private ProWorkorder wo;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("test-user");
        securityUtilsMock.when(SecurityUtils::getFactoryId).thenReturn(1L);

        wo = new ProWorkorder();
        wo.setWorkorderId(1L);
        wo.setWorkorderCode("WO-001");
        wo.setWorkorderName("测试工单");
        wo.setProductId(100L);
        wo.setProductCode("PROD-001");
        wo.setProductName("测试产品");
        wo.setQuantity(new BigDecimal("100"));
        wo.setQuantityProduced(new BigDecimal("100"));
        wo.setUnitOfMeasure("PCS");
        wo.setUnitName("个");
        wo.setStatus("PRODUCING");
        wo.setFactoryId(1L);

        // Lock mock — 直接执行 supplier
        doAnswer(inv -> {
            java.util.function.Supplier<?> s = inv.getArgument(1);
            return s.get();
        }).when(lockTemplate).execute(anyString(), any(java.util.function.Supplier.class));
        when(txManager.getTransaction(any())).thenReturn(mock(org.springframework.transaction.TransactionStatus.class));

        // 通用 mock
        when(proWorkorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(wo);
        when(wmWarehouseService.selectWmWarehouseAll()).thenReturn(Collections.emptyList());
        when(wmWarehouseService.selectWmWarehouseList(any())).thenReturn(Collections.emptyList());
        doAnswer(inv -> { WmProductRecpt r = inv.getArgument(0); r.setRecptId(300L); return 1; })
                .when(wmProductRecptService).insertWmProductRecpt(any());
        when(wmProductRecptLineService.insertWmProductRecptLine(any())).thenReturn(1);
        when(docLogMapper.insert(any())).thenReturn(1);
        when(wmProductRecptService.selectWmProductRecptByRecptId(300L)).thenAnswer(inv -> {
            WmProductRecpt r = new WmProductRecpt();
            r.setRecptId(300L); r.setRecptCode("PR001"); return r;
        });
    }

    @AfterEach
    void tearDown() { securityUtilsMock.close(); }

    // ═══════════════════════════════════════
    // 场景 1：自动路径 — 数量取本次 feedback 合格数
    // ═══════════════════════════════════════
    @Test
    @DisplayName("自动路径：feedbackId=205 → 生成入库单量=本次合格 100，log 带 sourceFeedbackId=205")
    void should_use_current_feedback_qualified_when_triggered_by_audit() {
        // Arrange：末工序 feedback 合格 100
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(205L); fb.setWorkorderId(1L);
        fb.setRouteId(10L); fb.setProcessId(6L);
        fb.setStatus("AUDITED");
        fb.setQuantityQualified(new BigDecimal("100"));
        when(proFeedbackMapper.selectProFeedbackByRecordId(205L)).thenReturn(fb);
        ProRouteProcess last = new ProRouteProcess();
        last.setProcessId(6L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(last);
        // 无既有 log
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = docService.onFeedbackAudited(205L);

        // Assert：入库单量为 feedback 合格 100
        verify(wmProductRecptService).insertWmProductRecpt(argThat(r ->
                r.getTotalQuantity() != null && r.getTotalQuantity().compareTo(new BigDecimal("100")) == 0));
        // log 带 sourceFeedbackId=205
        verify(docLogMapper).insert(argThat(log ->
                "RECPT".equals(log.getDocType()) && Long.valueOf(205L).equals(log.getSourceFeedbackId())));
        assertThat(result).isNotEmpty();
    }

    // ═══════════════════════════════════════
    // 场景 2：自动路径幂等 — 同一 feedback 已生成 → buildExistingRecptInfo
    // ═══════════════════════════════════════
    @Test
    @DisplayName("自动路径幂等：同一 feedbackId 已有 log → 不重复生成")
    void should_skip_when_feedback_already_generated_receipt() {
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(205L); fb.setWorkorderId(1L);
        fb.setRouteId(10L); fb.setProcessId(6L);
        fb.setStatus("AUDITED");
        fb.setQuantityQualified(new BigDecimal("100"));
        when(proFeedbackMapper.selectProFeedbackByRecordId(205L)).thenReturn(fb);
        ProRouteProcess last = new ProRouteProcess();
        last.setProcessId(6L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(last);
        // 已有该 feedback 的 log
        ProDocGenerationLog existing = new ProDocGenerationLog();
        existing.setDocId(300L); existing.setDocCode("PR001");
        existing.setDocType("RECPT"); existing.setSourceFeedbackId(205L);
        when(docLogMapper.selectList(any())).thenReturn(Collections.singletonList(existing));
        // Fix #5: buildExistingRecptInfo 现在校验 recpt 有效才算「已存在」
        WmProductRecpt existingRecpt = new WmProductRecpt();
        existingRecpt.setRecptId(300L); existingRecpt.setRecptCode("PR001"); existingRecpt.setStatus("DRAFT");
        when(wmProductRecptService.selectWmProductRecptByRecptId(300L)).thenReturn(existingRecpt);

        docService.onFeedbackAudited(205L);

        // 不应插入新入库单/新 log
        verify(wmProductRecptService, never()).insertWmProductRecpt(any());
        verify(docLogMapper, never()).insert(any());
    }

    // ═══════════════════════════════════════
    // 场景 3：手动路径 — quantity_produced=100, sumReceipted=40 → 差额 60
    // ═══════════════════════════════════════
    @Test
    @DisplayName("手动路径：produced=100, sumReceipted=40 → 生成差额 60 的入库单")
    void should_compute_delta_when_manual_generate() {
        when(wmProductRecptMapper.sumQuantityByWorkorderId(1L)).thenReturn(new BigDecimal("40"));
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        docService.generateProductReceipt(1L);

        // 入库单量 = 60
        verify(wmProductRecptService).insertWmProductRecpt(argThat(r ->
                r.getTotalQuantity() != null && r.getTotalQuantity().compareTo(new BigDecimal("60")) == 0));
        // log 的 sourceFeedbackId 为 null（手动路径）
        verify(docLogMapper).insert(argThat(log ->
                "RECPT".equals(log.getDocType()) && log.getSourceFeedbackId() == null));
    }

    // ═══════════════════════════════════════
    // 场景 4：手动路径差额为 0 → 空返回，不生成
    // ═══════════════════════════════════════
    @Test
    @DisplayName("手动路径：produced=100, sumReceipted=100 → 差额 0，不生成入库单")
    void should_return_empty_when_manual_but_no_delta() {
        when(wmProductRecptMapper.sumQuantityByWorkorderId(1L)).thenReturn(new BigDecimal("100"));
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        WmProductRecpt result = docService.generateProductReceipt(1L);

        assertThat(result).isNull();
        verify(wmProductRecptService, never()).insertWmProductRecpt(any());
        verify(docLogMapper, never()).insert(any());
    }

    // ═══════════════════════════════════════
    // 场景 5：并发场景 — insertLog 抛 DuplicateKeyException → 转为 ServiceException 让用户看到友好提示
    // (Fix Finding #4：Controller 拿到 ServiceException 后由 GlobalExceptionHandler 返回业务错误码，
    //  而非原生 DuplicateKeyException 冒泡的 HTTP 500)
    // ═══════════════════════════════════════
    @Test
    @DisplayName("并发场景：insertLog 抛 DuplicateKeyException → 转 ServiceException 提示重试")
    void should_translate_duplicate_key_to_service_exception() {
        // Arrange
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(205L); fb.setWorkorderId(1L);
        fb.setRouteId(10L); fb.setProcessId(6L);
        fb.setStatus("AUDITED");
        fb.setQuantityQualified(new BigDecimal("100"));
        when(proFeedbackMapper.selectProFeedbackByRecordId(205L)).thenReturn(fb);
        ProRouteProcess last = new ProRouteProcess();
        last.setProcessId(6L);
        when(proRouteProcessMapper.selectLastProcessByRouteId(10L)).thenReturn(last);
        when(docLogMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(docLogMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("Duplicate entry ..."));

        // Act & Assert：DuplicateKeyException 被转为 ServiceException
        assertThatThrownBy(() -> docService.onFeedbackAudited(205L))
                .isInstanceOf(com.ruoyi.common.exception.ServiceException.class)
                .hasMessageContaining("单据正在生成中");
        // 验证走到了 recpt/line 插入步骤 (保证测试意图 —— 事务边界内已 insert，靠外层回滚兜底)
        verify(wmProductRecptService).insertWmProductRecpt(any());
        verify(wmProductRecptLineService).insertWmProductRecptLine(any());
    }
}
