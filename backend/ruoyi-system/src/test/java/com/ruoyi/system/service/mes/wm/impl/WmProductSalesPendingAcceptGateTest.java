package com.ruoyi.system.service.mes.wm.impl;

import com.ruoyi.common.enums.SalOrderStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.service.mes.sal.ISalOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 成品出库"从销售订单生成"待接单兜底闸门（V161 接单前锁执行的后端纵深防御）。
 * 选单弹窗只列已确认/生产中，本测试验证持 orderId 直调 buildFromSaleOrder 同样被拦。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("出库草稿待接单兜底闸门")
class WmProductSalesPendingAcceptGateTest {

    @Mock
    private ISalOrderService salOrderService;
    @InjectMocks
    private WmProductSalesServiceImpl service;

    private SalOrder order(String status) {
        SalOrder o = new SalOrder();
        o.setOrderId(100L);
        o.setOrderCode("SO-100");
        o.setStatus(status);
        o.setLines(Collections.emptyList());
        return o;
    }

    @Test
    @DisplayName("待接单订单直调生成出库单 → 抛业务异常，不建草稿")
    void pendingAccept_rejected() {
        when(salOrderService.getDetail(100L)).thenReturn(order(SalOrderStatus.PENDING_ACCEPT.getCode()));
        assertThatThrownBy(() -> service.buildFromSaleOrder(100L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("待接单");
    }

    @Test
    @DisplayName("已确认订单可生成草稿（闸门不误伤既有可执行状态）")
    void confirmed_allowed() {
        lenient().when(salOrderService.getDetail(100L)).thenReturn(order(SalOrderStatus.CONFIRMED.getCode()));
        WmProductSales draft = service.buildFromSaleOrder(100L);
        assertThat(draft.getSalesOrderId()).isEqualTo(100L);
        assertThat(draft.getStatus()).isNotNull();
    }

    @Test
    @DisplayName("订单不存在 → 维持原 404 语义")
    void notFound_rejected() {
        when(salOrderService.getDetail(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.buildFromSaleOrder(404L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("销售订单不存在");
    }
}
