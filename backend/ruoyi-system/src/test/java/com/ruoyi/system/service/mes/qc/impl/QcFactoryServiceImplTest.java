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

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.domain.mes.qc.QcTemplateIndex;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOrderLineMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateIndexMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateProductMapper;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock QcTemplateProductMapper bindMapper;
    @Mock QcIqcMapper iqcMapper;
    @Mock QcOrderLineMapper lineMapper;
    @Mock QcTemplateIndexMapper templateIndexMapper;
    @Mock AutoCodeGenerator autoCodeGenerator;
    @Mock RedisLockTemplate lockTemplate;
    @Mock WmItemRecptMapper wmItemRecptMapper;
    @InjectMocks QcFactoryServiceImpl service;

    @Captor ArgumentCaptor<QcIqc> iqcCaptor;
    @Captor ArgumentCaptor<List<QcOrderLine>> linesCaptor;

    @BeforeEach
    void stubLockDirectRun() {
        // 生产代码走 execute(String, Runnable) 重载，等效 stub：锁内逻辑直接执行
        lenient().doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(lockTemplate).execute(anyString(), any(Runnable.class));
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
        when(bindMapper.selectEnabledBindExact("IQC", 1L, null)).thenReturn(null);
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(null);

        service.generateIqcForItemRecpt(header(1L), Collections.singletonList(line(1L, 10)));

        verify(iqcMapper, never()).insertQcIqc(any());
    }

    @Test
    @DisplayName("同来源同物料已有未关闭检验单时不重复生成")
    void should_skip_when_active_order_exists() {
        when(bindMapper.selectEnabledBindExact("IQC", 1L, null)).thenReturn(bind(10L));
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
        when(bindMapper.selectEnabledBindExact("IQC", 1L, null)).thenReturn(bind(10L));
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
        when(bindMapper.selectEnabledBindExact("IQC", 1L, null)).thenReturn(bind(10L));
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
        service.closeBySource(QcConstants.SOURCE_PRODUCT_SALES, 5L);

        verify(iqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(iqcMapper, never()).updateQcIqc(any());
    }
}
