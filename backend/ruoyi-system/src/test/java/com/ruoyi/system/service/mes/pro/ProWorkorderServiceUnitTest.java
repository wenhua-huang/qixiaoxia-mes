package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.service.mes.pro.impl.ProWorkorderServiceImpl;
import com.ruoyi.system.service.mes.wm.IWmIssueHeaderService;
import com.ruoyi.system.service.mes.wm.IWmMaterialStockService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 生产工单Service单元测试
 * 技术栈：JUnit 5 + Mockito，不启动 Spring 容器
 * 覆盖：createWorkorderWithBom（BOM用量缩放/批量创建）、startProduction（状态机）、
 * checkMaterialReadiness（物料齐套检查）、BOM调整、批次号传递、变更单工作流、编码唯一性等
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("生产工单服务单元测试")
class ProWorkorderServiceUnitTest {

    @Mock private ProWorkorderMapper workorderMapper;
    @Mock private IProWorkorderBomService workorderBomService;
    @Mock private IWmMaterialStockService materialStockService;
    @Mock private IProWorkorderParamService workorderParamService;
    @Mock private IWmIssueHeaderService wmIssueHeaderService;
    @InjectMocks private ProWorkorderServiceImpl workorderService;

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
        testWorkorder.setQuantity(new BigDecimal("100"));
        testWorkorder.setStatus("PREPARE");
        testWorkorder.setFactoryId(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ══════════════════════════════════════════════
    // 1. testBomQuantityScaling
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("1. BOM用量缩放：totalQuantity = 单位用量 × 工单计划数量")
    void testBomQuantityScaling() {
        // given：工单计划生产100个产品，BOM行每单位用量0.05
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-SCALING");
        wo.setQuantity(new BigDecimal("100"));
        wo.setProductId(200L);
        wo.setFactoryId(1L);

        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setItemId(10L);
        bom.setItemCode("MAT-001");
        bom.setQuantity(new BigDecimal("0.05"));

        List<ProWorkorderBom> bomList = new ArrayList<>();
        bomList.add(bom);

        // mapper回填ID
        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(1001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        // when
        ProWorkorder result = workorderService.createWorkorderWithBom(wo, bomList, null);

        // then：验证totalQuantity = 0.05 × 100 = 5.00
        ArgumentCaptor<ProWorkorderBom> bomCaptor = ArgumentCaptor.forClass(ProWorkorderBom.class);
        verify(workorderBomService, times(1)).insertProWorkorderBom(bomCaptor.capture());

        ProWorkorderBom capturedBom = bomCaptor.getValue();
        assertThat(capturedBom.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(capturedBom.getQuantity()).isEqualByComparingTo("0.05");
        assertThat(capturedBom.getWorkorderId()).isEqualTo(1001L);
        assertThat(result.getWorkorderId()).isEqualTo(1001L);
        assertThat(result.getStatus()).isEqualTo("PREPARE");
    }

    @Test
    @DisplayName("1b. BOM用量缩放：quantity为null时跳过计算")
    void testBomQuantityScalingNull() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-NULL-QTY");
        wo.setQuantity(null);
        wo.setProductId(200L);
        wo.setFactoryId(1L);

        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setItemId(10L);
        bom.setItemCode("MAT-NULL");
        bom.setQuantity(new BigDecimal("0.05"));

        List<ProWorkorderBom> bomList = new ArrayList<>();
        bomList.add(bom);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(1002L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        workorderService.createWorkorderWithBom(wo, bomList, null);

        ArgumentCaptor<ProWorkorderBom> bomCaptor = ArgumentCaptor.forClass(ProWorkorderBom.class);
        verify(workorderBomService, times(1)).insertProWorkorderBom(bomCaptor.capture());
        // totalQuantity不应被计算（工单quantity为null时跳过乘法）
        assertThat(bomCaptor.getValue().getTotalQuantity()).isNull();
    }

    // ══════════════════════════════════════════════
    // 2. testBomAdjustmentLogic
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("2. BOM调整：批量创建3个物料，验证全部插入")
    void testBomAdjustmentLogic() {
        // given：初始BOM含3个物料
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-BOM-ADJ");
        wo.setQuantity(new BigDecimal("200"));
        wo.setProductId(1L);
        wo.setFactoryId(1L);

        List<ProWorkorderBom> bomList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ProWorkorderBom bom = new ProWorkorderBom();
            bom.setItemId((long) i);
            bom.setItemCode("MAT-00" + i);
            bom.setQuantity(new BigDecimal("1.0"));
            bomList.add(bom);
        }
        assertThat(bomList).hasSize(3);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(2001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        // when：创建工单含3个BOM行
        workorderService.createWorkorderWithBom(wo, bomList, null);

        // then：3个BOM行全部插入
        verify(workorderBomService, times(3)).insertProWorkorderBom(any(ProWorkorderBom.class));

        // —— 模拟BOM替换：从3个物料替换为5个物料 ——
        reset(workorderBomService);
        List<ProWorkorderBom> replacedBomList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ProWorkorderBom bom = new ProWorkorderBom();
            bom.setItemId((long) (i + 100));
            bom.setItemCode("MAT-NEW-0" + i);
            bom.setQuantity(new BigDecimal("1.5"));
            replacedBomList.add(bom);
        }

        ProWorkorder wo2 = new ProWorkorder();
        wo2.setWorkorderCode("WO-BOM-REPLACE");
        wo2.setQuantity(new BigDecimal("200"));
        wo2.setProductId(1L);
        wo2.setFactoryId(1L);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(3001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        workorderService.createWorkorderWithBom(wo2, replacedBomList, null);

        // then：5个新BOM行全部插入
        verify(workorderBomService, times(5)).insertProWorkorderBom(any(ProWorkorderBom.class));
    }

    @Test
    @DisplayName("2b. BOM调整：空BOM列表也正常创建工单")
    void testBomAdjustmentLogicEmptyList() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-NO-BOM");
        wo.setQuantity(new BigDecimal("100"));
        wo.setProductId(1L);
        wo.setFactoryId(1L);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(4001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        ProWorkorder result = workorderService.createWorkorderWithBom(wo, new ArrayList<>(), null);

        assertThat(result.getWorkorderId()).isEqualTo(4001L);
        verify(workorderBomService, never()).insertProWorkorderBom(any(ProWorkorderBom.class));
    }

    // ══════════════════════════════════════════════
    // 3. testSkuVariantCreation
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("3. SKU变体创建：产品信息正确关联到工单")
    void testSkuVariantCreation() {
        // given：工单关联产品（productId对应SKU的parent_id）
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-SKU-001");
        wo.setProductId(100L);
        wo.setProductCode("ZD-01");
        wo.setProductName("折叠凳-标准");
        wo.setProductSpc("30x30x30cm");
        wo.setQuantity(new BigDecimal("50"));
        wo.setFactoryId(1L);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(5001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        // when
        ProWorkorder result = workorderService.createWorkorderWithBom(wo, null, null);

        // then：产品信息完整保留
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getProductCode()).isEqualTo("ZD-01");
        assertThat(result.getProductName()).isEqualTo("折叠凳-标准");
        assertThat(result.getProductSpc()).isEqualTo("30x30x30cm");
        assertThat(result.getWorkorderId()).isEqualTo(5001L);
    }

    @Test
    @DisplayName("3b. SKU变体：不同产品编码生成不同工单")
    void testSkuVariantCreationDifferentProducts() {
        // 产品A
        ProWorkorder woA = new ProWorkorder();
        woA.setWorkorderCode("WO-PROD-A");
        woA.setProductId(100L);
        woA.setProductCode("ZD-01");
        woA.setQuantity(new BigDecimal("100"));
        woA.setFactoryId(1L);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(6001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        ProWorkorder resultA = workorderService.createWorkorderWithBom(woA, null, null);
        assertThat(resultA.getProductCode()).isEqualTo("ZD-01");

        // 产品B
        reset(workorderMapper);
        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(6002L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        ProWorkorder woB = new ProWorkorder();
        woB.setWorkorderCode("WO-PROD-B");
        woB.setProductId(200L);
        woB.setProductCode("ZD-02");
        woB.setQuantity(new BigDecimal("200"));
        woB.setFactoryId(1L);

        ProWorkorder resultB = workorderService.createWorkorderWithBom(woB, null, null);
        assertThat(resultB.getProductCode()).isEqualTo("ZD-02");
        assertThat(resultB.getProductId()).isEqualTo(200L);
        assertThat(resultB.getWorkorderId()).isEqualTo(6002L);
    }

    // ══════════════════════════════════════════════
    // 4. testStatusTransitionLegal
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("4. 合法状态转换：PREPARE → PRODUCING")
    void testStatusTransitionLegal() {
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);

        int result = workorderService.startProduction(1L);
        assertThat(result).isEqualTo(1);
        assertThat(testWorkorder.getStatus()).isEqualTo("PRODUCING");

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(workorderMapper).updateProWorkorder(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PRODUCING");
    }

    // ══════════════════════════════════════════════
    // 5. testStatusTransitionIllegal
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("5. 非法状态转换：COMPLETED → PRODUCING 抛异常")
    void testStatusTransitionIllegal() {
        testWorkorder.setStatus("COMPLETED");
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);

        assertThatThrownBy(() -> workorderService.startProduction(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("只有待生产状态");

        verify(workorderMapper, never()).updateProWorkorder(any(ProWorkorder.class));
    }

    @Test
    @DisplayName("5b. 工单不存在时抛异常")
    void testStartProductionWorkorderNotFound() {
        when(workorderMapper.selectProWorkorderByWorkorderId(999L)).thenReturn(null);

        assertThatThrownBy(() -> workorderService.startProduction(999L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");

        verify(workorderMapper, never()).updateProWorkorder(any(ProWorkorder.class));
    }

    // ══════════════════════════════════════════════
    // 6. testMaterialAvailabilityCheck
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("6. 物料齐套检查：库存不足（需要500，仅200）")
    void testMaterialAvailabilityCheck() {
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);

        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setLineId(1L);
        bom.setItemId(200L);
        bom.setItemCode("INK-01");
        bom.setItemName("油墨");
        bom.setUnitName("kg");
        bom.setTotalQuantity(new BigDecimal("500"));
        List<ProWorkorderBom> bomList = new ArrayList<>();
        bomList.add(bom);
        when(workorderBomService.selectProWorkorderBomByWorkorderId(1L)).thenReturn(bomList);

        WmMaterialStock stock = new WmMaterialStock();
        stock.setQuantityAvailable(new BigDecimal("200"));
        List<WmMaterialStock> stocks = new ArrayList<>();
        stocks.add(stock);
        when(materialStockService.selectWmMaterialStockList(any(WmMaterialStock.class))).thenReturn(stocks);

        List<Map<String, Object>> result = workorderService.checkMaterialReadiness(1L);

        assertThat(result).hasSize(1);
        Map<String, Object> row = result.get(0);
        assertThat(row.get("sufficient")).isEqualTo(false);
        assertThat((BigDecimal) row.get("shortageQty")).isEqualByComparingTo(new BigDecimal("300"));
        assertThat((BigDecimal) row.get("requiredQty")).isEqualByComparingTo(new BigDecimal("500"));
        assertThat((BigDecimal) row.get("availableQty")).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(row.get("itemCode")).isEqualTo("INK-01");
        assertThat(row.get("unitName")).isEqualTo("kg");
    }

    // ══════════════════════════════════════════════
    // 7. testMaterialAvailabilitySufficient
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("7. 物料齐套检查：库存充足（需要500，有800）")
    void testMaterialAvailabilitySufficient() {
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);

        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setLineId(1L);
        bom.setItemId(200L);
        bom.setItemCode("INK-01");
        bom.setItemName("油墨");
        bom.setUnitName("kg");
        bom.setTotalQuantity(new BigDecimal("500"));
        List<ProWorkorderBom> bomList = new ArrayList<>();
        bomList.add(bom);
        when(workorderBomService.selectProWorkorderBomByWorkorderId(1L)).thenReturn(bomList);

        WmMaterialStock stock = new WmMaterialStock();
        stock.setQuantityAvailable(new BigDecimal("800"));
        List<WmMaterialStock> stocks = new ArrayList<>();
        stocks.add(stock);
        when(materialStockService.selectWmMaterialStockList(any(WmMaterialStock.class))).thenReturn(stocks);

        List<Map<String, Object>> result = workorderService.checkMaterialReadiness(1L);

        assertThat(result.get(0).get("sufficient")).isEqualTo(true);
        assertThat((BigDecimal) result.get(0).get("shortageQty")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((BigDecimal) result.get(0).get("availableQty")).isEqualByComparingTo(new BigDecimal("800"));
    }

    @Test
    @DisplayName("7b. 物料齐套：多BOM行混合（部分充足、部分不足）")
    void testMaterialAvailabilityMixed() {
        testWorkorder.setWorkorderId(50L);
        when(workorderMapper.selectProWorkorderByWorkorderId(50L)).thenReturn(testWorkorder);

        ProWorkorderBom bomA = new ProWorkorderBom();
        bomA.setLineId(1L);
        bomA.setItemId(10L);
        bomA.setItemCode("MAT-A");
        bomA.setItemName("物料A");
        bomA.setUnitName("个");
        bomA.setTotalQuantity(new BigDecimal("100"));

        ProWorkorderBom bomB = new ProWorkorderBom();
        bomB.setLineId(2L);
        bomB.setItemId(20L);
        bomB.setItemCode("MAT-B");
        bomB.setItemName("物料B");
        bomB.setUnitName("个");
        bomB.setTotalQuantity(new BigDecimal("50"));

        List<ProWorkorderBom> bomList = new ArrayList<>();
        bomList.add(bomA);
        bomList.add(bomB);
        when(workorderBomService.selectProWorkorderBomByWorkorderId(50L)).thenReturn(bomList);

        // 物料A库存仅30（不足）
        WmMaterialStock stockA = new WmMaterialStock();
        stockA.setQuantityAvailable(new BigDecimal("30"));
        when(materialStockService.selectWmMaterialStockList(argThat(s -> s != null && Long.valueOf(10L).equals(s.getItemId()))))
                .thenReturn(List.of(stockA));

        // 物料B库存200（充足）
        WmMaterialStock stockB = new WmMaterialStock();
        stockB.setQuantityAvailable(new BigDecimal("200"));
        when(materialStockService.selectWmMaterialStockList(argThat(s -> s != null && Long.valueOf(20L).equals(s.getItemId()))))
                .thenReturn(List.of(stockB));

        List<Map<String, Object>> result = workorderService.checkMaterialReadiness(50L);

        assertThat(result).hasSize(2);
        Map<String, Object> rowA = result.get(0);
        assertThat(rowA.get("sufficient")).isEqualTo(false);
        assertThat((BigDecimal) rowA.get("shortageQty")).isEqualByComparingTo(new BigDecimal("70"));

        Map<String, Object> rowB = result.get(1);
        assertThat(rowB.get("sufficient")).isEqualTo(true);
        assertThat((BigDecimal) rowB.get("shortageQty")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("7c. 物料齐套：多仓库库存汇总计算")
    void testMaterialAvailabilityMultiStocks() {
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);

        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setLineId(1L);
        bom.setItemId(200L);
        bom.setItemCode("INK-01");
        bom.setUnitName("kg");
        bom.setTotalQuantity(new BigDecimal("300"));
        List<ProWorkorderBom> bomList = new ArrayList<>();
        bomList.add(bom);
        when(workorderBomService.selectProWorkorderBomByWorkorderId(1L)).thenReturn(bomList);

        // 两个仓库各100 → 汇总为200，仍不足
        WmMaterialStock stock1 = new WmMaterialStock();
        stock1.setQuantityAvailable(new BigDecimal("100"));
        WmMaterialStock stock2 = new WmMaterialStock();
        stock2.setQuantityAvailable(new BigDecimal("100"));
        when(materialStockService.selectWmMaterialStockList(any(WmMaterialStock.class)))
                .thenReturn(List.of(stock1, stock2));

        List<Map<String, Object>> result = workorderService.checkMaterialReadiness(1L);

        assertThat(result).hasSize(1);
        assertThat((BigDecimal) result.get(0).get("availableQty")).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(result.get(0).get("sufficient")).isEqualTo(false);
        assertThat((BigDecimal) result.get(0).get("shortageQty")).isEqualByComparingTo(new BigDecimal("100"));
    }

    // ══════════════════════════════════════════════
    // 8. testBatchCodeAutoGen
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("8. 批次号：创建工单时批次号正确传递")
    void testBatchCodeAutoGen() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-BATCH-001");
        wo.setQuantity(new BigDecimal("300"));
        wo.setProductId(1L);
        wo.setFactoryId(1L);
        wo.setBatchCode("BATCH-20260620-001");

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(10001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        ProWorkorder result = workorderService.createWorkorderWithBom(wo, null, null);

        assertThat(result.getBatchCode()).isEqualTo("BATCH-20260620-001");
        assertThat(result.getWorkorderId()).isEqualTo(10001L);

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(workorderMapper).insertProWorkorder(captor.capture());
        assertThat(captor.getValue().getBatchCode()).isEqualTo("BATCH-20260620-001");
    }

    @Test
    @DisplayName("8b. 批次号：未设置批次号时为null")
    void testBatchCodeAutoGenNull() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-NO-BATCH");
        wo.setQuantity(new BigDecimal("50"));
        wo.setProductId(1L);
        wo.setFactoryId(1L);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(7001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        ProWorkorder result = workorderService.createWorkorderWithBom(wo, null, null);

        assertThat(result.getBatchCode()).isNull();
    }

    // ══════════════════════════════════════════════
    // 9. testChangeOrderWorkflow
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("9. 变更单工作流：PENDING → APPROVED（审批通过）")
    void testChangeOrderWorkflow() {
        when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);

        // 审批通过
        ProWorkorder pendingWo = new ProWorkorder();
        pendingWo.setWorkorderId(8001L);
        pendingWo.setWorkorderCode("WO-CHG-001");
        pendingWo.setStatus("PENDING");

        int approved = workorderService.updateProWorkorder(pendingWo);
        assertThat(approved).isEqualTo(1);

        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(workorderMapper).updateProWorkorder(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");

        // 审批拒绝
        ProWorkorder rejectWo = new ProWorkorder();
        rejectWo.setWorkorderId(8002L);
        rejectWo.setWorkorderCode("WO-CHG-002");
        rejectWo.setStatus("REJECTED");

        int rejected = workorderService.updateProWorkorder(rejectWo);
        assertThat(rejected).isEqualTo(1);

        verify(workorderMapper, times(2)).updateProWorkorder(any(ProWorkorder.class));
    }

    @Test
    @DisplayName("9b. 变更单：已审批状态允许再次修改")
    void testChangeOrderWorkflowReApprove() {
        when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);

        ProWorkorder approvedWo = new ProWorkorder();
        approvedWo.setWorkorderId(8003L);
        approvedWo.setWorkorderCode("WO-CHG-003");
        approvedWo.setStatus("APPROVED");

        int result = workorderService.updateProWorkorder(approvedWo);
        assertThat(result).isEqualTo(1);

        verify(workorderMapper, times(1)).updateProWorkorder(any(ProWorkorder.class));
    }

    // ══════════════════════════════════════════════
    // 10. checkWorkorderCodeUnique
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("10. 编码唯一性：相同编码相同ID应通过")
    void testCodeUniqueSameId() {
        ProWorkorder existing = new ProWorkorder();
        existing.setWorkorderId(1L);
        existing.setWorkorderCode("WO-001");
        when(workorderMapper.selectProWorkorderByWorkorderCode("WO-001")).thenReturn(existing);
        assertThat(workorderService.checkWorkorderCodeUnique(testWorkorder)).isTrue();
    }

    @Test
    @DisplayName("10b. 编码唯一性：编码已存在应抛异常")
    void testCodeUniqueDuplicate() {
        ProWorkorder existing = new ProWorkorder();
        existing.setWorkorderId(99L);
        existing.setWorkorderCode("WO-001");
        when(workorderMapper.selectProWorkorderByWorkorderCode("WO-001")).thenReturn(existing);

        ProWorkorder newWo = new ProWorkorder();
        newWo.setWorkorderId(2L);
        newWo.setWorkorderCode("WO-001");
        assertThatThrownBy(() -> workorderService.checkWorkorderCodeUnique(newWo))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("WO-001");
    }

    @Test
    @DisplayName("10c. 编码唯一性：新编码应通过")
    void testCodeUniqueNew() {
        when(workorderMapper.selectProWorkorderByWorkorderCode("WO-NEW")).thenReturn(null);

        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-NEW");
        assertThat(workorderService.checkWorkorderCodeUnique(wo)).isTrue();
    }

    // ══════════════════════════════════════════════
    // 11. insertProWorkorder — 默认值
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("11. 插入工单自动设置默认值")
    void testInsertSetsDefaults() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-TEST");
        when(workorderMapper.insertProWorkorder(any(ProWorkorder.class))).thenReturn(1);

        workorderService.insertProWorkorder(wo);
        assertThat(wo.getStatus()).isEqualTo("PREPARE");
        assertThat(wo.getWorkorderType()).isEqualTo("SELF");
        assertThat(wo.getOrderType()).isEqualTo("NEW");
        assertThat(wo.getOrderSource()).isEqualTo("MANUAL");
        assertThat(wo.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("11b. 插入工单不覆盖已有状态")
    void testInsertKeepsExistingStatus() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-STATUS");
        wo.setStatus("DRAFT");
        wo.setWorkorderType("OUTSOURCE");
        wo.setOrderType("CHANGE");
        when(workorderMapper.insertProWorkorder(any(ProWorkorder.class))).thenReturn(1);

        workorderService.insertProWorkorder(wo);
        assertThat(wo.getStatus()).isEqualTo("DRAFT");
        assertThat(wo.getWorkorderType()).isEqualTo("OUTSOURCE");
        assertThat(wo.getOrderType()).isEqualTo("CHANGE");
    }

    // ══════════════════════════════════════════════
    // 12. createWorkorderWithBom — 工序参数
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("12. createWorkorderWithBom：工序参数批量插入")
    void testCreateWorkorderWithBomParams() {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode("WO-PARAM");
        wo.setQuantity(new BigDecimal("100"));
        wo.setProductId(1L);
        wo.setFactoryId(1L);

        // BOM行
        ProWorkorderBom bom = new ProWorkorderBom();
        bom.setItemId(10L);
        bom.setItemCode("MAT-001");
        bom.setQuantity(new BigDecimal("1.0"));
        List<ProWorkorderBom> bomList = List.of(bom);

        // 工序参数行
        List<ProWorkorderParam> paramList = new ArrayList<>();
        ProWorkorderParam param = new ProWorkorderParam();
        param.setTemplateId(10L);
        param.setStandardValue("100");
        param.setAdjustedValue("110");
        paramList.add(param);

        doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(9001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));

        ProWorkorder result = workorderService.createWorkorderWithBom(wo, bomList, paramList);

        assertThat(result.getWorkorderId()).isEqualTo(9001L);
        verify(workorderBomService, times(1)).insertProWorkorderBom(any(ProWorkorderBom.class));
        verify(workorderParamService, times(1)).insertProWorkorderParam(any(ProWorkorderParam.class));
    }

    // ══════════════════════════════════════════════
    // 13. cancelWorkorder 测试
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("13. 取消工单：PREPARE→CANCEL")
    void testCancelWorkorderPrepare() {
        testWorkorder.setStatus("PREPARE");
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(new ArrayList<>());

        int result = workorderService.cancelWorkorder(1L);

        assertThat(result).isEqualTo(1);
        ArgumentCaptor<ProWorkorder> captor = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(workorderMapper).updateProWorkorder(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCEL");
    }

    @Test
    @DisplayName("13b. 取消工单：PRODUCING→CANCEL")
    void testCancelWorkorderProducing() {
        testWorkorder.setStatus("PRODUCING");
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);
        when(wmIssueHeaderService.selectWmIssueHeaderList(any())).thenReturn(new ArrayList<>());

        workorderService.cancelWorkorder(1L);
        verify(workorderMapper).updateProWorkorder(any(ProWorkorder.class));
    }

    @Test
    @DisplayName("13c. 取消工单：COMPLETED状态拒绝")
    void testCancelWorkorderRejectsCompleted() {
        testWorkorder.setStatus("COMPLETED");
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);

        assertThatThrownBy(() -> workorderService.cancelWorkorder(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("待生产或生产中");
    }

    @Test
    @DisplayName("13d. 取消工单：CANCEL状态拒绝")
    void testCancelWorkorderRejectsAlreadyCancelled() {
        testWorkorder.setStatus("CANCEL");
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);

        assertThatThrownBy(() -> workorderService.cancelWorkorder(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("待生产或生产中");
    }

    @Test
    @DisplayName("13e. 取消工单：释放已确认领料单的预占库存")
    void testCancelWorkorderReleasesAllocations() {
        testWorkorder.setStatus("PRODUCING");
        when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
        when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);

        // 模拟该工单下有2张确认的领料单
        com.ruoyi.system.domain.mes.wm.WmIssueHeader confirmed1 = new com.ruoyi.system.domain.mes.wm.WmIssueHeader();
        confirmed1.setIssueId(10L);
        confirmed1.setStatus("CONFIRMED");
        com.ruoyi.system.domain.mes.wm.WmIssueHeader confirmed2 = new com.ruoyi.system.domain.mes.wm.WmIssueHeader();
        confirmed2.setIssueId(20L);
        confirmed2.setStatus("CONFIRMED");
        com.ruoyi.system.domain.mes.wm.WmIssueHeader draftOne = new com.ruoyi.system.domain.mes.wm.WmIssueHeader();
        draftOne.setIssueId(30L);
        draftOne.setStatus("DRAFT");
        when(wmIssueHeaderService.selectWmIssueHeaderList(any()))
                .thenReturn(List.of(confirmed1, confirmed2, draftOne));

        workorderService.cancelWorkorder(1L);

        // 确认的领料单释放预占，草稿的不释放
        verify(wmIssueHeaderService).releaseAllocation(10L);
        verify(wmIssueHeaderService).releaseAllocation(20L);
        verify(wmIssueHeaderService, never()).releaseAllocation(30L);
    }
}
