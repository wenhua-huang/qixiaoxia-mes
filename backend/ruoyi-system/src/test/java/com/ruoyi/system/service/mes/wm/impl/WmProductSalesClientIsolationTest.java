package com.ruoyi.system.service.mes.wm.impl;

import com.ruoyi.common.enums.WmWarehouseConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 销售出库客户仓硬隔离单元测试：validateClientWarehouseIsolation
 * 锁死防错发核心规则 —— 客户专属仓只能发给归属客户。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("销售出库客户仓硬隔离")
class WmProductSalesClientIsolationTest {

    @Mock
    private IWmWarehouseService wmWarehouseService;
    @InjectMocks
    private WmProductSalesServiceImpl service;

    private WmWarehouse warehouse(Long id, String type, Long clientId) {
        WmWarehouse w = new WmWarehouse();
        w.setWarehouseId(id);
        w.setWarehouseType(type);
        w.setClientId(clientId);
        w.setWarehouseName("仓" + id);
        return w;
    }

    private void validate(WmProductSales header, List<WmProductSalesDetail> details) {
        ReflectionTestUtils.invokeMethod(service, "validateClientWarehouseIsolation", header, details);
    }

    @Test
    @DisplayName("客户仓发给归属客户：放行")
    void customerWarehouse_sameClient_passes() {
        WmProductSales header = new WmProductSales();
        header.setClientId(201L);
        header.setWarehouseId(203L);
        when(wmWarehouseService.selectWmWarehouseByWarehouseId(eq(203L)))
                .thenReturn(warehouse(203L, WmWarehouseConstants.TYPE_CUSTOMER, 201L));

        assertThatCode(() -> validate(header, Collections.emptyList())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("客户仓发给其他客户：硬拦截")
    void customerWarehouse_differentClient_blocked() {
        WmProductSales header = new WmProductSales();
        header.setClientId(202L);  // 出库单属于客户B
        header.setWarehouseId(203L);
        when(wmWarehouseService.selectWmWarehouseByWarehouseId(eq(203L)))
                .thenReturn(warehouse(203L, WmWarehouseConstants.TYPE_CUSTOMER, 201L));  // 仓属于客户A

        assertThatThrownBy(() -> validate(header, Collections.emptyList()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("客户专属仓")
                .hasMessageContaining("不能给当前客户发货");
    }

    @Test
    @DisplayName("客户仓 + 出库单无客户：硬拦截")
    void customerWarehouse_nullClient_blocked() {
        WmProductSales header = new WmProductSales();
        header.setClientId(null);
        header.setWarehouseId(203L);
        when(wmWarehouseService.selectWmWarehouseByWarehouseId(eq(203L)))
                .thenReturn(warehouse(203L, WmWarehouseConstants.TYPE_CUSTOMER, 201L));

        assertThatThrownBy(() -> validate(header, Collections.emptyList()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("公共仓发给任意客户：放行")
    void publicWarehouse_anyClient_passes() {
        WmProductSales header = new WmProductSales();
        header.setClientId(201L);
        header.setWarehouseId(206L);
        when(wmWarehouseService.selectWmWarehouseByWarehouseId(eq(206L)))
                .thenReturn(warehouse(206L, "FINISHED", null));

        assertThatCode(() -> validate(header, Collections.emptyList())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("明细行仓库属于其他客户仓：硬拦截（覆盖 detail 路径）")
    void detailWarehouse_differentClient_blocked() {
        WmProductSales header = new WmProductSales();
        header.setClientId(201L);
        header.setWarehouseId(206L);  // 表头公共仓
        // 表头公共仓放行
        lenient().when(wmWarehouseService.selectWmWarehouseByWarehouseId(eq(206L)))
                .thenReturn(warehouse(206L, "FINISHED", null));

        WmProductSalesDetail d = new WmProductSalesDetail();
        d.setWarehouseId(204L);  // 明细选了客户B的仓
        when(wmWarehouseService.selectWmWarehouseByWarehouseId(eq(204L)))
                .thenReturn(warehouse(204L, WmWarehouseConstants.TYPE_CUSTOMER, 202L));

        List<WmProductSalesDetail> details = new ArrayList<>();
        details.add(d);

        assertThatThrownBy(() -> validate(header, details))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("客户专属仓");
    }

    @Test
    @DisplayName("无仓库：放行")
    void noWarehouse_passes() {
        WmProductSales header = new WmProductSales();
        header.setClientId(201L);

        assertThatCode(() -> validate(header, Collections.emptyList())).doesNotThrowAnyException();
    }
}
