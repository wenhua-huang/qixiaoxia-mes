package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.domain.mes.qc.QcTemplateIndex;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOrderLineMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateIndexMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateProductMapper;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IQC 待检单生成工厂单测（Mockito 全 Mock，禁止连库）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IQC 待检单生成工厂")
class QcFactoryServiceImplTest {

    private static final String IQC_CODE = "IQC20260816-001";
    private static final String OQC_CODE = "OQC20260816-001";

    @Mock QcTemplateProductMapper bindMapper;
    @Mock QcIqcMapper iqcMapper;
    @Mock QcOqcMapper oqcMapper;
    @Mock QcOrderLineMapper lineMapper;
    @Mock QcTemplateIndexMapper templateIndexMapper;
    @Mock AutoCodeGenerator autoCodeGenerator;
    @Mock RedisLockTemplate lockTemplate;
    @Mock WmItemRecptMapper wmItemRecptMapper;
    @Mock WmProductSalesMapper wmProductSalesMapper;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks QcFactoryServiceImpl service;

    @Captor ArgumentCaptor<QcIqc> iqcCaptor;
    @Captor ArgumentCaptor<QcOqc> oqcCaptor;
    @Captor ArgumentCaptor<List<QcOrderLine>> linesCaptor;

    @BeforeEach
    void stubLockDirectRun() {
        // 生产代码走 execute(String, Runnable) 重载，等效 stub：锁内逻辑直接执行
        lenient().doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(lockTemplate).execute(anyString(), any(Runnable.class));
        // 用 mock 事务管理器构造真实 TransactionTemplate：execute 回调真实执行
        // （getTransaction/commit 走 mock no-op），覆盖"锁内三写包事务"路径
        service.initTx();
    }

    // ---- 测试数据构造 ----

    private WmItemRecpt header(Long recptId) {
        WmItemRecpt h = new WmItemRecpt();
        h.setRecptId(recptId);
        h.setRecptCode("RECPT001");
        h.setVendorId(9L);
        h.setVendorCode("V001");
        h.setVendorName("供应商A");
        return h;
    }

    private WmItemRecptLine line(Long itemId, double qty) {
        WmItemRecptLine l = new WmItemRecptLine();
        l.setItemId(itemId);
        l.setItemCode("ITEM-" + itemId);
        l.setItemName("物料" + itemId);
        l.setSpecification("10mm");
        l.setUnitOfMeasure("个");
        l.setQuantityRecpt(BigDecimal.valueOf(qty));
        return l;
    }

    /** 绑定：quantityCheck=5 / Ac=1 / 三率 0,2,3 */
    private QcTemplateProduct bind(Long templateId) {
        QcTemplateProduct b = new QcTemplateProduct();
        b.setTemplateId(templateId);
        b.setQuantityCheck(5);
        b.setQuantityUnqualified(1);
        b.setCrRate(BigDecimal.ZERO);
        b.setMajRate(BigDecimal.valueOf(2.0));
        b.setMinRate(BigDecimal.valueOf(3.0));
        return b;
    }

    private QcTemplateIndex index(Long id, String code) {
        QcTemplateIndex i = new QcTemplateIndex();
        i.setIndexId(id);
        i.setIndexCode(code);
        i.setIndexName("检测项" + id);
        i.setIndexType("QUANTITATIVE");
        i.setQcTool("卡尺");
        i.setQcResultType(QcConstants.RESULT_TYPE_NUMBER);
        i.setCheckMethod("实测");
        i.setStanderVal(BigDecimal.TEN);
        i.setUnitOfMeasure("mm");
        i.setThresholdMin(BigDecimal.ONE);
        i.setThresholdMax(BigDecimal.ONE);
        i.setOrderNum(id.intValue());
        return i;
    }

    private QcIqc orderWithStatus(String status) {
        QcIqc o = new QcIqc();
        o.setIqcId(200L);
        o.setIqcCode("IQC-EXIST");
        o.setStatus(status);
        return o;
    }

    // ---- 用例 ----

    @Test
    @DisplayName("物料无模板绑定时跳过生成")
    void should_skip_when_no_template_bind() {
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(null);

        service.generateIqcForItemRecpt(header(1L), Collections.singletonList(line(1L, 10)));

        verify(iqcMapper, never()).insertQcIqc(any());
    }

    @Test
    @DisplayName("同来源同物料已有未关闭检验单时不重复生成")
    void should_skip_when_active_order_exists() {
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(bind(10L));
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.singletonList(orderWithStatus(QcConstants.STATUS_PENDING)));

        service.generateIqcForItemRecpt(header(1L), Collections.singletonList(line(1L, 10)));

        verify(iqcMapper, never()).insertQcIqc(any());
        verify(lineMapper, never()).batchInsert(any());
    }

    @Test
    @DisplayName("生成时快照模板阈值并回填入库单头挂点")
    void should_snapshot_thresholds_and_backfill_header() {
        WmItemRecpt header = header(1L);
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(bind(10L));
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.emptyList());
        when(templateIndexMapper.selectByTemplateId(10L))
            .thenReturn(Arrays.asList(index(11L, "IDX-1"), index(12L, "IDX-2")));
        when(autoCodeGenerator.genSerialCode(QcConstants.CODE_RULE_IQC, null)).thenReturn(IQC_CODE);
        when(iqcMapper.insertQcIqc(any())).thenAnswer(inv -> {
            ((QcIqc) inv.getArgument(0)).setIqcId(100L);
            return 1;
        });

        service.generateIqcForItemRecpt(header, Collections.singletonList(line(1L, 10)));

        verify(iqcMapper).insertQcIqc(iqcCaptor.capture());
        QcIqc saved = iqcCaptor.getValue();
        assertEquals(5, saved.getQuantityMinCheck());
        assertEquals(1, saved.getQuantityMaxUnqualified());
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCrRateLimit()));
        assertEquals(0, BigDecimal.valueOf(2.0).compareTo(saved.getMajRateLimit()));
        assertEquals(0, BigDecimal.valueOf(3.0).compareTo(saved.getMinRateLimit()));
        assertEquals(QcConstants.STATUS_PENDING, saved.getStatus());
        assertEquals(IQC_CODE, saved.getIqcCode());
        assertEquals(Long.valueOf(10L), saved.getTemplateId());
        assertEquals(QcConstants.SOURCE_ITEM_RECPT, saved.getSourceDocType());
        assertEquals(Long.valueOf(1L), saved.getSourceDocId());
        assertEquals("RECPT001", saved.getSourceDocCode());
        assertEquals(Long.valueOf(1L), saved.getItemId());
        assertEquals("ITEM-1", saved.getItemCode());
        assertEquals(0, BigDecimal.TEN.compareTo(saved.getQuantityReceived()));
        assertEquals("供应商A", saved.getVendorName());
        assertNotNull(saved.getReceiveDate());

        assertEquals(Long.valueOf(100L), header.getIqcId());
        assertEquals(IQC_CODE, header.getIqcCode());

        verify(lineMapper).batchInsert(linesCaptor.capture());
        List<QcOrderLine> lines = linesCaptor.getValue();
        assertEquals(2, lines.size());
        QcOrderLine first = lines.get(0);
        assertEquals(QcConstants.TYPE_IQC, first.getQcType());
        assertEquals(Long.valueOf(100L), first.getQcId());
        assertEquals(Long.valueOf(11L), first.getIndexId());
        assertEquals("IDX-1", first.getIndexCode());
        assertEquals("检测项11", first.getIndexName());
        assertEquals("卡尺", first.getQcTool());
        assertEquals(QcConstants.RESULT_TYPE_NUMBER, first.getQcResultType());
        assertEquals("实测", first.getCheckMethod());
        assertEquals(0, BigDecimal.TEN.compareTo(first.getStanderVal()));
        assertEquals("mm", first.getUnitOfMeasure());
        assertEquals(0, BigDecimal.ONE.compareTo(first.getThresholdMin()));
        assertEquals(0, BigDecimal.ONE.compareTo(first.getThresholdMax()));
        assertEquals(Integer.valueOf(11), first.getOrderNum());
        assertNull(first.getCheckValText());

        verify(wmItemRecptMapper).updateWmItemRecptHeaderRefs(1L, 100L, IQC_CODE);
    }

    @Test
    @DisplayName("多行同物料合并为一张检验单")
    void should_merge_same_item_lines_into_one_order() {
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(bind(10L));
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.emptyList());
        when(templateIndexMapper.selectByTemplateId(10L)).thenReturn(Collections.emptyList());
        when(autoCodeGenerator.genSerialCode(QcConstants.CODE_RULE_IQC, null)).thenReturn(IQC_CODE);
        when(iqcMapper.insertQcIqc(any())).thenAnswer(inv -> {
            ((QcIqc) inv.getArgument(0)).setIqcId(100L);
            return 1;
        });

        service.generateIqcForItemRecpt(header(1L),
            Arrays.asList(line(1L, 3), line(1L, 4)));

        verify(iqcMapper, times(1)).insertQcIqc(iqcCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(7).compareTo(iqcCaptor.getValue().getQuantityReceived()));
        verify(wmItemRecptMapper, times(1)).updateWmItemRecptHeaderRefs(any(), any(), any());
    }

    @Test
    @DisplayName("closeBySource 关闭 PENDING/INSPECTING 单并跳过 COMPLETED/CLOSED")
    void should_close_active_orders_by_source() {
        QcIqc pending = orderWithStatus(QcConstants.STATUS_PENDING);
        pending.setIqcId(101L);
        QcIqc completed = orderWithStatus(QcConstants.STATUS_COMPLETED);
        completed.setIqcId(102L);
        QcIqc closed = orderWithStatus(QcConstants.STATUS_CLOSED);
        closed.setIqcId(103L);
        QcIqc inspecting = orderWithStatus(QcConstants.STATUS_INSPECTING);
        inspecting.setIqcId(104L);
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 5L, null))
            .thenReturn(Arrays.asList(pending, completed, closed, inspecting));

        service.closeBySource(QcConstants.SOURCE_ITEM_RECPT, 5L);

        ArgumentCaptor<QcIqc> cap = ArgumentCaptor.forClass(QcIqc.class);
        verify(iqcMapper, times(2)).updateQcIqc(cap.capture());
        assertEquals(Long.valueOf(101L), cap.getAllValues().get(0).getIqcId());
        assertEquals(QcConstants.STATUS_CLOSED, cap.getAllValues().get(0).getStatus());
        assertEquals(Long.valueOf(104L), cap.getAllValues().get(1).getIqcId());
        assertEquals(QcConstants.STATUS_CLOSED, cap.getAllValues().get(1).getStatus());
    }

    @Test
    @DisplayName("closeBySource 对未实现的来源类型当前为 no-op")
    void should_noop_close_for_unimplemented_source_type() {
        service.closeBySource(QcConstants.SOURCE_RT_ISSUE, 5L);

        verify(iqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(iqcMapper, never()).updateQcIqc(any());
        verify(oqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(oqcMapper, never()).updateQcOqc(any());
    }

    @Test
    @DisplayName("resolveTemplate 有工序时精确绑定命中即返回且不再查通用")
    void should_prefer_exact_bind_when_process_present() {
        QcTemplateProduct exact = bind(10L);
        when(bindMapper.selectEnabledBindExact("IPQC", 1L, 5L)).thenReturn(exact);

        assertSame(exact, service.resolveTemplate("IPQC", 1L, 5L));

        verify(bindMapper, never()).selectEnabledBindCommon(any(), any());
    }

    @Test
    @DisplayName("resolveTemplate 无工序时跳过精确查询直查通用绑定")
    void should_skip_exact_query_when_no_process() {
        assertNull(service.resolveTemplate("IQC", 1L, null));

        verify(bindMapper, never()).selectEnabledBindExact(any(), any(), any());
        verify(bindMapper).selectEnabledBindCommon("IQC", 1L);
    }

    @Test
    @DisplayName("编码规则异常时兜底生成时间戳编码不打断创建")
    void should_fallback_code_when_rule_broken() {
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(bind(10L));
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.emptyList());
        when(templateIndexMapper.selectByTemplateId(10L)).thenReturn(Collections.emptyList());
        when(autoCodeGenerator.genSerialCode(QcConstants.CODE_RULE_IQC, null))
            .thenThrow(new RuntimeException("编码规则[QC_IQC_CODE]不存在！"));
        when(iqcMapper.insertQcIqc(any())).thenAnswer(inv -> {
            ((QcIqc) inv.getArgument(0)).setIqcId(100L);
            return 1;
        });

        service.generateIqcForItemRecpt(header(1L), Collections.singletonList(line(1L, 10)));

        verify(iqcMapper).insertQcIqc(iqcCaptor.capture());
        assertTrue(iqcCaptor.getValue().getIqcCode().matches("IQC\\d{17}\\d{4}"),
            "兜底编码应为 IQC+17位时间戳+4位随机，实际=" + iqcCaptor.getValue().getIqcCode());
    }

    // ==================== OQC（销售出库）====================

    private WmProductSales salesHeader(Long salesId) {
        WmProductSales h = new WmProductSales();
        h.setSalesId(salesId);
        h.setSalesCode("SAL001");
        h.setClientId(8L);
        h.setClientCode("C001");
        h.setClientName("客户A");
        return h;
    }

    private WmProductSalesLine salesLine(Long itemId, double qty) {
        WmProductSalesLine l = new WmProductSalesLine();
        l.setItemId(itemId);
        l.setItemCode("ITEM-" + itemId);
        l.setItemName("物料" + itemId);
        l.setSpecification("5mm");
        l.setUnitOfMeasure("件");
        l.setQuantitySales(BigDecimal.valueOf(qty));
        return l;
    }

    private QcOqc oqcWithStatus(String status) {
        QcOqc o = new QcOqc();
        o.setOqcId(300L);
        o.setOqcCode("OQC-EXIST");
        o.setStatus(status);
        return o;
    }

    @Test
    @DisplayName("OQC：物料无模板绑定时跳过生成")
    void should_skip_oqc_when_no_template_bind() {
        when(bindMapper.selectEnabledBindCommon("OQC", 1L)).thenReturn(null);

        service.generateOqcForProductSales(salesHeader(1L), Collections.singletonList(salesLine(1L, 10)));

        verify(oqcMapper, never()).insertQcOqc(any());
    }

    @Test
    @DisplayName("OQC：同来源同物料已有未关闭检验单时不重复生成")
    void should_skip_oqc_when_active_order_exists() {
        when(bindMapper.selectEnabledBindCommon("OQC", 1L)).thenReturn(bind(10L));
        when(oqcMapper.selectBySource(QcConstants.SOURCE_PRODUCT_SALES, 1L, 1L))
            .thenReturn(Collections.singletonList(oqcWithStatus(QcConstants.STATUS_PENDING)));

        service.generateOqcForProductSales(salesHeader(1L), Collections.singletonList(salesLine(1L, 10)));

        verify(oqcMapper, never()).insertQcOqc(any());
        verify(lineMapper, never()).batchInsert(any());
    }

    @Test
    @DisplayName("OQC：生成时快照模板阈值、客户三件套取头、发货数量求和并回填出库单头挂点")
    void should_snapshot_oqc_thresholds_and_backfill_sales_header() {
        WmProductSales header = salesHeader(1L);
        when(bindMapper.selectEnabledBindCommon("OQC", 1L)).thenReturn(bind(10L));
        when(oqcMapper.selectBySource(QcConstants.SOURCE_PRODUCT_SALES, 1L, 1L))
            .thenReturn(Collections.emptyList());
        when(templateIndexMapper.selectByTemplateId(10L))
            .thenReturn(Collections.singletonList(index(11L, "IDX-1")));
        when(autoCodeGenerator.genSerialCode(QcConstants.CODE_RULE_OQC, null)).thenReturn(OQC_CODE);
        when(oqcMapper.insertQcOqc(any())).thenAnswer(inv -> {
            ((QcOqc) inv.getArgument(0)).setOqcId(200L);
            return 1;
        });

        service.generateOqcForProductSales(header, Arrays.asList(salesLine(1L, 3), salesLine(1L, 4)));

        verify(oqcMapper).insertQcOqc(oqcCaptor.capture());
        QcOqc saved = oqcCaptor.getValue();
        assertEquals(5, saved.getQuantityMinCheck());
        assertEquals(1, saved.getQuantityMaxUnqualified());
        assertEquals(0, BigDecimal.valueOf(2.0).compareTo(saved.getMajRateLimit()));
        assertEquals(QcConstants.STATUS_PENDING, saved.getStatus());
        assertEquals(OQC_CODE, saved.getOqcCode());
        assertEquals(QcConstants.SOURCE_PRODUCT_SALES, saved.getSourceDocType());
        assertEquals(Long.valueOf(1L), saved.getSourceDocId());
        assertEquals("SAL001", saved.getSourceDocCode());
        assertEquals(Long.valueOf(1L), saved.getItemId());
        // 同物料多行并一单：quantity_out=行数量和
        assertEquals(0, BigDecimal.valueOf(7).compareTo(saved.getQuantityOut()));
        // 客户三件套从出库单头快照
        assertEquals(Long.valueOf(8L), saved.getClientId());
        assertEquals("C001", saved.getClientCode());
        assertEquals("客户A", saved.getClientName());
        assertNotNull(saved.getOutDate());

        assertEquals(Long.valueOf(200L), header.getOqcId());
        assertEquals(OQC_CODE, header.getOqcCode());
        verify(wmProductSalesMapper).updateSalesHeaderRefs(1L, 200L, OQC_CODE);

        verify(lineMapper).batchInsert(linesCaptor.capture());
        assertEquals(QcConstants.TYPE_OQC, linesCaptor.getValue().get(0).getQcType());
        assertEquals(Long.valueOf(200L), linesCaptor.getValue().get(0).getQcId());
    }

    @Test
    @DisplayName("OQC：closeBySource 关闭 wm_product_sales 来源的 PENDING/INSPECTING 单并跳过 COMPLETED/CLOSED")
    void should_close_active_oqc_orders_by_source() {
        QcOqc pending = oqcWithStatus(QcConstants.STATUS_PENDING);
        pending.setOqcId(301L);
        QcOqc completed = oqcWithStatus(QcConstants.STATUS_COMPLETED);
        completed.setOqcId(302L);
        QcOqc inspecting = oqcWithStatus(QcConstants.STATUS_INSPECTING);
        inspecting.setOqcId(303L);
        when(oqcMapper.selectBySource(QcConstants.SOURCE_PRODUCT_SALES, 5L, null))
            .thenReturn(Arrays.asList(pending, completed, inspecting));

        service.closeBySource(QcConstants.SOURCE_PRODUCT_SALES, 5L);

        ArgumentCaptor<QcOqc> cap = ArgumentCaptor.forClass(QcOqc.class);
        verify(oqcMapper, times(2)).updateQcOqc(cap.capture());
        assertEquals(Long.valueOf(301L), cap.getAllValues().get(0).getOqcId());
        assertEquals(QcConstants.STATUS_CLOSED, cap.getAllValues().get(0).getStatus());
        assertEquals(Long.valueOf(303L), cap.getAllValues().get(1).getOqcId());
        assertEquals(QcConstants.STATUS_CLOSED, cap.getAllValues().get(1).getStatus());
        // IQC 侧不受影响
        verify(iqcMapper, never()).selectBySource(anyString(), any(), any());
    }
}
