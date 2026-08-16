package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.QcConstants;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IQC 确认入库拦截门单测（Mockito 全 Mock，禁止连库）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IQC 确认入库拦截门")
class QcGateServiceImplTest {

    @Mock IQcFactoryService factoryService;
    @Mock QcIqcMapper iqcMapper;
    @InjectMocks QcGateServiceImpl service;

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
        return o;
    }

    /** 物料 1 绑定了 IQC 模板（需检） */
    private void needInspect() {
        when(factoryService.resolveTemplate(eq(QcConstants.TYPE_IQC), eq(1L), any()))
            .thenReturn(new QcTemplateProduct());
    }

    @Test
    @DisplayName("需检物料无检验单时抛异常且消息含物料编码与提示")
    void should_throw_when_no_order_for_inspected_item() {
        needInspect();
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
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
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_PASS)));

        assertDoesNotThrow(
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));
    }

    @Test
    @DisplayName("仅有 FAIL 判定单时抛异常且消息含检验单编码与状态")
    void should_throw_when_only_fail_order_exists() {
        needInspect();
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
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
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_COMPLETED, QcConstants.RESULT_CONCESSION)));

        assertDoesNotThrow(
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));
    }

    @Test
    @DisplayName("未绑定模板的物料免检放行且不查检验单")
    void should_pass_when_item_not_bound() {
        when(factoryService.resolveTemplate(eq(QcConstants.TYPE_IQC), eq(1L), any()))
            .thenReturn(null);

        assertDoesNotThrow(
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));

        verify(iqcMapper, never()).selectBySource(anyString(), any(), any());
    }

    @Test
    @DisplayName("待检单仍在 PENDING 未完成时抛异常")
    void should_throw_when_order_not_completed() {
        needInspect();
        when(iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, 1L, 1L))
            .thenReturn(Collections.singletonList(order(QcConstants.STATUS_PENDING, null)));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.assertItemRecptConfirmable(header(1L), Collections.singletonList(line(1L))));

        assertTrue(ex.getMessage().contains("IQC001"));
        assertTrue(ex.getMessage().contains(QcConstants.STATUS_PENDING));
    }
}
