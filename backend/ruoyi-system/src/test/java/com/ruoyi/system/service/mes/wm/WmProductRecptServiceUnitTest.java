package com.ruoyi.system.service.mes.wm;

import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.domain.mes.wm.tx.ProductRecptTxBean;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptMapper;
import com.ruoyi.system.service.mes.wm.impl.WmProductRecptServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 产品入库单收货确认 + 过账 单元测试
 * 覆盖：confirmProductRecpt / postProductRecpt + TxBean 构建
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("产品入库单收货确认单元测试")
class WmProductRecptServiceUnitTest {

    @Mock private WmProductRecptMapper wmProductRecptMapper;
    @Mock private IWmProductRecptLineService wmProductRecptLineService;
    @Mock private IWmStorageCoreService storageCoreService;
    @InjectMocks private WmProductRecptServiceImpl service;

    @Test
    @DisplayName("1. 确认收货：DRAFT -> CONFIRMED，调 processProductRecpt 更新库存")
    void testConfirmProductRecpt() {
        WmProductRecpt header = draftRecpt();
        header.setRecptCode("PR-001");
        WmProductRecptLine line = newLine(201L, "ITEM-001", "产品A", new BigDecimal("100"));
        line.setWarehouseId(1L);

        when(wmProductRecptMapper.selectWmProductRecptByRecptId(1L)).thenReturn(header);
        when(wmProductRecptLineService.selectWmProductRecptLineList(any())).thenReturn(Collections.singletonList(line));
        when(wmProductRecptMapper.updateWmProductRecpt(any())).thenReturn(1);

        service.confirmProductRecpt(1L);

        ArgumentCaptor<List<ProductRecptTxBean>> captor = ArgumentCaptor.forClass(List.class);
        verify(storageCoreService).processProductRecpt(captor.capture());
        ProductRecptTxBean bean = captor.getValue().get(0);
        assertThat(bean.getSourceDocType()).isEqualTo("wm_product_recpt");
        assertThat(bean.getSourceDocId()).isEqualTo(1L);
        assertThat(bean.getSourceDocCode()).isEqualTo("PR-001");
        assertThat(bean.getItemId()).isEqualTo(201L);
        assertThat(bean.getTransactionQuantity()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(header.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("2. 确认收货：行无仓库时取 header 仓库")
    void testConfirmUsesHeaderWarehouseWhenLineEmpty() {
        WmProductRecpt header = draftRecpt();
        header.setWarehouseId(99L);
        WmProductRecptLine line = newLine(201L, "ITEM-001", "产品A", new BigDecimal("10"));
        line.setWarehouseId(null);

        when(wmProductRecptMapper.selectWmProductRecptByRecptId(1L)).thenReturn(header);
        when(wmProductRecptLineService.selectWmProductRecptLineList(any())).thenReturn(Collections.singletonList(line));
        when(wmProductRecptMapper.updateWmProductRecpt(any())).thenReturn(1);

        service.confirmProductRecpt(1L);

        ArgumentCaptor<List<ProductRecptTxBean>> captor = ArgumentCaptor.forClass(List.class);
        verify(storageCoreService).processProductRecpt(captor.capture());
        assertThat(captor.getValue().get(0).getWarehouseId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("3. 确认收货：非DRAFT状态拒绝")
    void testConfirmRejectsNonDraft() {
        WmProductRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        when(wmProductRecptMapper.selectWmProductRecptByRecptId(1L)).thenReturn(header);

        assertThatThrownBy(() -> service.confirmProductRecpt(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("草稿状态");
        verify(storageCoreService, never()).processProductRecpt(any());
    }

    @Test
    @DisplayName("4. 确认收货：无入库行拒绝")
    void testConfirmRejectsEmptyLines() {
        WmProductRecpt header = draftRecpt();
        when(wmProductRecptMapper.selectWmProductRecptByRecptId(1L)).thenReturn(header);
        when(wmProductRecptLineService.selectWmProductRecptLineList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.confirmProductRecpt(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("没有入库行");
    }

    @Test
    @DisplayName("5. 确认收货：入库单不存在")
    void testConfirmNotFound() {
        when(wmProductRecptMapper.selectWmProductRecptByRecptId(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.confirmProductRecpt(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("6. 过账入库：CONFIRMED -> POSTED")
    void testPostProductRecpt() {
        WmProductRecpt header = draftRecpt();
        header.setStatus("CONFIRMED");
        when(wmProductRecptMapper.selectWmProductRecptByRecptId(1L)).thenReturn(header);
        when(wmProductRecptMapper.updateWmProductRecpt(any())).thenReturn(1);

        service.postProductRecpt(1L);

        assertThat(header.getStatus()).isEqualTo("POSTED");
        verify(wmProductRecptMapper).updateWmProductRecpt(header);
    }

    @Test
    @DisplayName("7. 过账入库：非CONFIRMED状态拒绝")
    void testPostRejectsNonConfirmed() {
        WmProductRecpt header = draftRecpt();
        header.setStatus("DRAFT");
        when(wmProductRecptMapper.selectWmProductRecptByRecptId(1L)).thenReturn(header);

        assertThatThrownBy(() -> service.postProductRecpt(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("已确认");
    }

    @Test
    @DisplayName("8. 过账入库：入库单不存在")
    void testPostNotFound() {
        when(wmProductRecptMapper.selectWmProductRecptByRecptId(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.postProductRecpt(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("不存在");
    }

    private WmProductRecpt draftRecpt() {
        WmProductRecpt recpt = new WmProductRecpt();
        recpt.setRecptId(1L);
        recpt.setRecptCode("PR-001");
        recpt.setStatus("DRAFT");
        recpt.setWarehouseId(1L);
        return recpt;
    }

    private WmProductRecptLine newLine(Long itemId, String code, String name, BigDecimal qty) {
        WmProductRecptLine line = new WmProductRecptLine();
        line.setLineId(1L);
        line.setRecptId(1L);
        line.setItemId(itemId);
        line.setItemCode(code);
        line.setItemName(name);
        line.setQuantityRecpt(qty);
        return line;
    }
}
