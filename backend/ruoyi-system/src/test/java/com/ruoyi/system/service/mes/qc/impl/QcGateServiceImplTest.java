package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import com.ruoyi.system.domain.mes.qc.QcRqc;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.mapper.mes.qc.QcIpqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcRqcMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.QcConstants;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 质检拦截门单测（Mockito 全 Mock，禁止连库）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IQC/IPQC/OQC/RQC 拦截门")
class QcGateServiceImplTest {

    @Mock IQcFactoryService factoryService;
    @Mock QcIqcMapper iqcMapper;
    @Mock QcOqcMapper oqcMapper;
    @Mock QcIpqcMapper ipqcMapper;
    @Mock QcRqcMapper rqcMapper;
    @Mock WmProductSalesLineMapper wmProductSalesLineMapper;
    @Mock WmProductRecptLineMapper wmProductRecptLineMapper;
    @InjectMocks QcGateServiceImpl service;

    // ── 通用夹具 ──

    private WmItemRecpt header(Long recptId) {
        WmItemRecpt h = new WmItemRecpt();
        h.setRecptId(recptId);
        h.setRecptCode("RECPT001");
        return h;
    }

    private WmItemRecptLine line(Long itemId) {
        WmItemRecptLine l = new WmItemRecptLine();
        l.setItemId(itemId);
        l.setItemCode("ITEM-" + itemId);
        l.setItemName("物料" + itemId);
        l.setQuantityRecpt(BigDecimal.TEN);
        return l;
    }

    private QcIqc order(String status, String result) {
        QcIqc o = new QcIqc();
        o.setIqcId(100L);
        o.setIqcCode("IQC001");
        o.setStatus(status);
        o.setCheckResult(result);
        o.setItemId(1L);
        return o;
    }

    @SuppressWarnings("unchecked")
    private void stubBind(String qcType, Long itemId, QcTemplateProduct bind) {
        when(factoryService.resolveTemplates(eq(qcType), anyCollection(), any()))
            .thenAnswer(inv -> {
                Collection<Long> ids = inv.getArgument(1);
                Map<Long, QcTemplateProduct> map = new HashMap<>();
                if (bind != null) {
                    for (Long id : ids) {
                        if (id.equals(itemId)) {
                            map.put(id, bind);
                        }
                    }
                }
                return map;
            });
    }

    /** 物料 1 绑定了 IQC 模板（需检） */
    private void needInspect() {
        stubBind(QcConstants.TYPE_IQC, 1L, new QcTemplateProduct());
    }

    // ==================== IQC 确认入库拦截 ====================

    @Test
    @DisplayName("需检物料无检验单时抛异常且消息含物料编码与提示")
    void should_throw_when_no_order_for_inspected_item() {
        needInspect();
        when(iqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_ITEM_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));

        assertTrue(ex.getMessage().contains("ITEM-1"));
        assertTrue(ex.getMessage().contains("未生成检验单"));
    }

    @Test
    @DisplayName("存在 COMPLETED 且 PASS 的检验单时放行")
    void should_pass_when_completed_pass_order_exists() {
        needInspect();
        when(iqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_ITEM_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_PASS)));

        assertDoesNotThrow(
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));
    }

    @Test
    @DisplayName("仅有 FAIL 判定单时抛异常且消息含检验单编码与状态")
    void should_throw_when_only_fail_order_exists() {
        needInspect();
        when(iqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_ITEM_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_FAIL)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));

        assertTrue(ex.getMessage().contains("IQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.RESULT_FAIL));
    }

    @Test
    @DisplayName("COMPLETED 且 CONCESSION 让步接收单放行")
    void should_pass_when_concession_order_exists() {
        needInspect();
        when(iqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_ITEM_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_CONCESSION)));

        assertDoesNotThrow(
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));
    }

    @Test
    @DisplayName("未绑定模板的物料免检放行且不查检验单")
    void should_pass_when_item_not_bound() {
        stubBind(QcConstants.TYPE_IQC, 1L, null);

        assertDoesNotThrow(
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));

        verify(iqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(iqcMapper, never()).selectBySourceItems(anyString(), any(), anyCollection());
    }

    @Test
    @DisplayName("待检单仍在 PENDING 未完成时抛异常")
    void should_throw_when_order_not_completed() {
        needInspect();
        when(iqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_ITEM_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_PENDING, null)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));

        assertTrue(ex.getMessage().contains("IQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.STATUS_PENDING));
    }

    // ==================== OQC 出库确认拦截 ====================

    private WmProductSales salesHeader(Long salesId) {
        WmProductSales h = new WmProductSales();
        h.setSalesId(salesId);
        h.setSalesCode("SAL001");
        return h;
    }

    private WmProductSalesLine salesLine(Long itemId) {
        WmProductSalesLine l = new WmProductSalesLine();
        l.setItemId(itemId);
        l.setItemCode("ITEM-" + itemId);
        l.setQuantitySales(BigDecimal.TEN);
        return l;
    }

    private QcOqc oqcOrder(String status, String result) {
        QcOqc o = new QcOqc();
        o.setOqcId(300L);
        o.setOqcCode("OQC001");
        o.setStatus(status);
        o.setCheckResult(result);
        o.setItemId(1L);
        return o;
    }

    private void needOqcInspect() {
        when(wmProductSalesLineMapper.selectLinesBySalesId(1L))
            .thenReturn(Collections.singletonList(salesLine(1L)));
        stubBind(QcConstants.TYPE_OQC, 1L, new QcTemplateProduct());
    }

    @Test
    @DisplayName("OQC：需检物料无检验单时抛异常且消息含物料编码与出货检验提示")
    void should_throw_oqc_when_no_order_for_inspected_item() {
        needOqcInspect();
        when(oqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_SALES), eq(1L), anyCollection()))
            .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertProductSalesPostable(salesHeader(1L)));

        assertTrue(ex.getMessage().contains("ITEM-1"));
        assertTrue(ex.getMessage().contains("需出货检验合格后方可出库确认"));
        assertTrue(ex.getMessage().contains("未生成检验单"));
    }

    @Test
    @DisplayName("OQC：存在 COMPLETED 且 PASS 的检验单时放行")
    void should_pass_oqc_when_completed_pass_order_exists() {
        needOqcInspect();
        when(oqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_SALES), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(oqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_PASS)));

        assertDoesNotThrow(() -> service.assertProductSalesPostable(salesHeader(1L)));
    }

    @Test
    @DisplayName("OQC：仅有 FAIL 判定单时抛异常且消息含检验单编码与结果")
    void should_throw_oqc_when_only_fail_order_exists() {
        needOqcInspect();
        when(oqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_SALES), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(oqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_FAIL)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertProductSalesPostable(salesHeader(1L)));

        assertTrue(ex.getMessage().contains("OQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.RESULT_FAIL));
    }

    @Test
    @DisplayName("OQC：COMPLETED 且 CONCESSION 让步接收单放行")
    void should_pass_oqc_when_concession_order_exists() {
        needOqcInspect();
        when(oqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_SALES), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(oqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_CONCESSION)));

        assertDoesNotThrow(() -> service.assertProductSalesPostable(salesHeader(1L)));
    }

    @Test
    @DisplayName("OQC：未绑定模板的物料免检放行且不查检验单")
    void should_pass_oqc_when_item_not_bound() {
        when(wmProductSalesLineMapper.selectLinesBySalesId(1L))
            .thenReturn(Collections.singletonList(salesLine(1L)));
        stubBind(QcConstants.TYPE_OQC, 1L, null);

        assertDoesNotThrow(() -> service.assertProductSalesPostable(salesHeader(1L)));

        verify(oqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(oqcMapper, never()).selectBySourceItems(anyString(), any(), anyCollection());
    }

    @Test
    @DisplayName("OQC：待检单仍在 PENDING 未完成时抛异常")
    void should_throw_oqc_when_order_not_completed() {
        needOqcInspect();
        when(oqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_SALES), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(oqcOrder(QcConstants.STATUS_PENDING, null)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertProductSalesPostable(salesHeader(1L)));

        assertTrue(ex.getMessage().contains("OQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.STATUS_PENDING));
    }

    // ==================== IPQC 成品入库确认拦截 ====================

    private WmProductRecpt recptHeader(Long recptId) {
        WmProductRecpt h = new WmProductRecpt();
        h.setRecptId(recptId);
        h.setRecptCode("PR001");
        h.setProduceId(2L);
        return h;
    }

    private WmProductRecptLine recptLine(Long itemId) {
        WmProductRecptLine l = new WmProductRecptLine();
        l.setRecptId(1L);
        l.setItemId(itemId);
        l.setItemCode("ITEM-" + itemId);
        l.setQuantityRecpt(BigDecimal.TEN);
        return l;
    }

    private QcIpqc ipqcOrder(String status, String result) {
        QcIpqc o = new QcIpqc();
        o.setIpqcId(400L);
        o.setIpqcCode("IPQC001");
        o.setStatus(status);
        o.setCheckResult(result);
        o.setItemId(1L);
        return o;
    }

    private void needIpqcInspect() {
        when(wmProductRecptLineMapper.selectWmProductRecptLineList(any()))
            .thenReturn(Collections.singletonList(recptLine(1L)));
        stubBind(QcConstants.TYPE_IPQC, 1L, new QcTemplateProduct());
    }

    @Test
    @DisplayName("IPQC：需检产品无完工检验单时抛异常且消息含物料编码与完工检验提示")
    void should_throw_ipqc_when_no_order_for_inspected_product() {
        needIpqcInspect();
        when(ipqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertProductRecptConfirmable(recptHeader(1L)));

        assertTrue(ex.getMessage().contains("ITEM-1"));
        assertTrue(ex.getMessage().contains("需完工检验合格后方可确认入库"));
        assertTrue(ex.getMessage().contains("未生成检验单"));
    }

    @Test
    @DisplayName("IPQC：存在 COMPLETED 且 PASS 的完工检验单时放行")
    void should_pass_ipqc_when_completed_pass_order_exists() {
        needIpqcInspect();
        when(ipqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(ipqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_PASS)));

        assertDoesNotThrow(() -> service.assertProductRecptConfirmable(recptHeader(1L)));
    }

    @Test
    @DisplayName("IPQC：COMPLETED 且 CONCESSION 让步接收单放行")
    void should_pass_ipqc_when_concession_order_exists() {
        needIpqcInspect();
        when(ipqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(ipqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_CONCESSION)));

        assertDoesNotThrow(() -> service.assertProductRecptConfirmable(recptHeader(1L)));
    }

    @Test
    @DisplayName("IPQC：仅有 FAIL 判定单时抛异常且消息含检验单编码与结果")
    void should_throw_ipqc_when_only_fail_order_exists() {
        needIpqcInspect();
        when(ipqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(ipqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_FAIL)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertProductRecptConfirmable(recptHeader(1L)));

        assertTrue(ex.getMessage().contains("IPQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.RESULT_FAIL));
    }

    @Test
    @DisplayName("IPQC：待检单仍在 PENDING 未完成时抛异常")
    void should_throw_ipqc_when_order_not_completed() {
        needIpqcInspect();
        when(ipqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_PRODUCT_RECPT), eq(1L), anyCollection()))
            .thenReturn(Collections.singletonList(ipqcOrder(QcConstants.STATUS_PENDING, null)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertProductRecptConfirmable(recptHeader(1L)));

        assertTrue(ex.getMessage().contains("IPQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.STATUS_PENDING));
    }

    @Test
    @DisplayName("IPQC：未绑定模板的产品免检放行且不查检验单")
    void should_pass_ipqc_when_product_not_bound() {
        when(wmProductRecptLineMapper.selectWmProductRecptLineList(any()))
            .thenReturn(Collections.singletonList(recptLine(1L)));
        stubBind(QcConstants.TYPE_IPQC, 1L, null);

        assertDoesNotThrow(() -> service.assertProductRecptConfirmable(recptHeader(1L)));

        verify(ipqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(ipqcMapper, never()).selectBySourceItems(anyString(), any(), anyCollection());
    }

    // ==================== RQC 退料执行拦截 ====================

    private com.ruoyi.system.domain.mes.wm.WmRtIssue rtHeader(Long rtId) {
        com.ruoyi.system.domain.mes.wm.WmRtIssue h = new com.ruoyi.system.domain.mes.wm.WmRtIssue();
        h.setRtId(rtId);
        h.setRtCode("RT001");
        return h;
    }

    private com.ruoyi.system.domain.mes.wm.WmRtIssueLine rtLine(Long itemId) {
        com.ruoyi.system.domain.mes.wm.WmRtIssueLine l = new com.ruoyi.system.domain.mes.wm.WmRtIssueLine();
        l.setItemId(itemId);
        l.setItemCode("ITEM-" + itemId);
        l.setQuantityRt(BigDecimal.TEN);
        return l;
    }

    private QcRqc rqcOrder(String status, String result) {
        QcRqc o = new QcRqc();
        o.setRqcId(500L);
        o.setRqcCode("RQC001");
        o.setStatus(status);
        o.setCheckResult(result);
        o.setItemId(1L);
        return o;
    }

    @Test
    @DisplayName("RQC：需检物料无退料检验单时抛异常")
    void should_throw_rqc_when_no_order_for_inspected_item() {
        stubBind(QcConstants.TYPE_RQC, 1L, new QcTemplateProduct());
        when(rqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_RT_ISSUE), eq(7L), anyCollection()))
            .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertRtIssueExecutable(rtHeader(7L), Collections.singletonList(rtLine(1L))));

        assertTrue(ex.getMessage().contains("ITEM-1"));
        assertTrue(ex.getMessage().contains("需退料检验合格后方可执行退料"));
        assertTrue(ex.getMessage().contains("未生成检验单"));
    }

    @Test
    @DisplayName("RQC：存在 COMPLETED 且 PASS 的检验单时放行")
    void should_pass_rqc_when_completed_pass_order_exists() {
        stubBind(QcConstants.TYPE_RQC, 1L, new QcTemplateProduct());
        when(rqcMapper.selectBySourceItems(eq(QcConstants.SOURCE_RT_ISSUE), eq(7L), anyCollection()))
            .thenReturn(Collections.singletonList(rqcOrder(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_PASS)));

        assertDoesNotThrow(
            () -> service.assertRtIssueExecutable(rtHeader(7L), Collections.singletonList(rtLine(1L))));
    }

    @Test
    @DisplayName("RQC：未绑定模板的物料免检放行且不查检验单")
    void should_pass_rqc_when_item_not_bound() {
        stubBind(QcConstants.TYPE_RQC, 1L, null);

        assertDoesNotThrow(
            () -> service.assertRtIssueExecutable(rtHeader(7L), Collections.singletonList(rtLine(1L))));

        verify(rqcMapper, never()).selectBySource(anyString(), any(), any());
        verify(rqcMapper, never()).selectBySourceItems(anyString(), any(), anyCollection());
    }
}
