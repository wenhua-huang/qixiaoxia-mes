package com.ruoyi.system.service.mes.pur;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
import com.ruoyi.system.service.mes.pur.impl.PurOrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 采购单 Service 单元测试
 * 覆盖：级联删除（先删行再删头）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("采购单服务单元测试")
class PurOrderServiceUnitTest {

    @Mock private PurOrderMapper purOrderMapper;
    @Mock private IPurOrderLineService purOrderLineService;
    @InjectMocks private PurOrderServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("1. 级联删除：单个采购单先删行再删头")
    void testCascadeDeleteSingle() {
        when(purOrderLineService.deletePurOrderLineByOrderId(1L)).thenReturn(1);
        when(purOrderMapper.deletePurOrderByOrderId(1L)).thenReturn(1);

        service.deletePurOrderByOrderId(1L);

        // 验证：先删行，后删头
        var inOrder = inOrder(purOrderLineService, purOrderMapper);
        inOrder.verify(purOrderLineService).deletePurOrderLineByOrderId(1L);
        inOrder.verify(purOrderMapper).deletePurOrderByOrderId(1L);
    }

    @Test
    @DisplayName("2. 级联删除：批量删除，每个订单都先删行再删头")
    void testCascadeDeleteBatch() {
        Long[] ids = {1L, 2L, 3L};
        when(purOrderLineService.deletePurOrderLineByOrderId(anyLong())).thenReturn(1);
        when(purOrderMapper.deletePurOrderByOrderIds(ids)).thenReturn(3);

        service.deletePurOrderByOrderIds(ids);

        // 每张订单都调用了 deletePurOrderLineByOrderId
        for (Long id : ids) {
            verify(purOrderLineService).deletePurOrderLineByOrderId(id);
        }
        verify(purOrderMapper).deletePurOrderByOrderIds(ids);
    }

    @Test
    @DisplayName("3. 级联删除：即使没有行数据也能正常删除头")
    void testCascadeDeleteWithNoLines() {
        when(purOrderLineService.deletePurOrderLineByOrderId(1L)).thenReturn(0); // 无行可删
        when(purOrderMapper.deletePurOrderByOrderId(1L)).thenReturn(1);

        int result = service.deletePurOrderByOrderId(1L);

        assertThat(result).isEqualTo(1);
        verify(purOrderLineService).deletePurOrderLineByOrderId(1L);
        verify(purOrderMapper).deletePurOrderByOrderId(1L);
    }
}
