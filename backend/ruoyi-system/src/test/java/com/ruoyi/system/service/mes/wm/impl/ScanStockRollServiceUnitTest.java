package com.ruoyi.system.service.mes.wm.impl;

import java.util.Collections;
import java.util.List;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmRollDetailMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次码/卷料码精确反查（扫码用）单元测试
 * 覆盖：WmMaterialStockServiceImpl.scanByBatchCode / WmRollDetailServiceImpl.scanByRollCode
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("批次码/卷料码扫码反查单元测试")
class ScanStockRollServiceUnitTest {

    @Mock private WmMaterialStockMapper wmMaterialStockMapper;
    @Mock private WmRollDetailMapper wmRollDetailMapper;
    @InjectMocks private WmMaterialStockServiceImpl materialStockService;
    @InjectMocks private WmRollDetailServiceImpl rollDetailService;

    @Test
    @DisplayName("1. 批次码为空/空白 → 抛 ServiceException")
    void testScanByBatchCodeBlankThrows() {
        assertThatThrownBy(() -> materialStockService.scanByBatchCode(""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("批次码");
        assertThatThrownBy(() -> materialStockService.scanByBatchCode("   "))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("批次码");
    }

    @Test
    @DisplayName("2. 批次码正常 → 返回 mapper 精确查询列表（入参已 trim）")
    void testScanByBatchCodeReturnsList() {
        WmMaterialStock stock = new WmMaterialStock();
        stock.setMaterialStockId(1L);
        stock.setBatchCode("BAT20260815001");
        List<WmMaterialStock> expected = Collections.singletonList(stock);
        when(wmMaterialStockMapper.selectWmMaterialStockByExactBatchCode("BAT20260815001")).thenReturn(expected);

        List<WmMaterialStock> result = materialStockService.scanByBatchCode(" BAT20260815001 ");

        assertThat(result).isSameAs(expected);
        verify(wmMaterialStockMapper).selectWmMaterialStockByExactBatchCode("BAT20260815001");
    }

    @Test
    @DisplayName("3. 卷料码未找到 → 抛 ServiceException(未找到纸卷：xxx)")
    void testScanByRollCodeNotFoundThrows() {
        when(wmRollDetailMapper.selectWmRollDetailByRollCode("ROLL-404")).thenReturn(null);

        assertThatThrownBy(() -> rollDetailService.scanByRollCode(" ROLL-404 "))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未找到纸卷：ROLL-404");
    }

    @Test
    @DisplayName("4. 卷料码正常 → 返回纸卷实体（入参已 trim）")
    void testScanByRollCodeReturnsEntity() {
        WmRollDetail roll = new WmRollDetail();
        roll.setRollId(10L);
        roll.setRollCode("ROLL-001");
        when(wmRollDetailMapper.selectWmRollDetailByRollCode("ROLL-001")).thenReturn(roll);

        WmRollDetail result = rollDetailService.scanByRollCode(" ROLL-001 ");

        assertThat(result).isSameAs(roll);
        assertThat(result.getRollId()).isEqualTo(10L);
        verify(wmRollDetailMapper).selectWmRollDetailByRollCode("ROLL-001");
    }
}
