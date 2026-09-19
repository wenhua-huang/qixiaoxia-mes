package com.ruoyi.system.service.mes.pro;

import java.util.List;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProRoute;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.mapper.mes.pro.ProRouteMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessParamMapper;
import com.ruoyi.system.service.mes.pro.impl.ProRouteProductServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("产品路线绑定-默认路线唯一校验测试")
class ProRouteProductServiceImplTest
{
    @Mock private ProRouteProductMapper routeProductMapper;
    @Mock private ProRouteMapper routeMapper;
    @Mock private ProRouteProcessMapper routeProcessMapper;
    @Mock private ProRouteProductBomMapper routeProductBomMapper;
    @Mock private ProRouteProcessParamMapper routeProcessParamMapper;
    @InjectMocks private ProRouteProductServiceImpl service;

    private MockedStatic<SecurityUtils> security;
    private MockedStatic<DateUtils> dates;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getUsername).thenReturn("admin");
        dates = mockStatic(DateUtils.class);
        dates.when(DateUtils::getNowDate).thenReturn(new java.util.Date());
    }

    @AfterEach
    void tearDown() {
        security.close();
        dates.close();
    }

    @Test
    @DisplayName("同产品已有默认路线, 再插一条默认 -> 拒绝")
    void should_reject_when_secondDefaultForSameItem() {
        ProRouteProduct existing = binding(10L, 100L, "Y");
        when(routeProductMapper.selectProRouteProductList(any())).thenReturn(List.of(existing));
        ProRouteProduct second = binding(null, 100L, "Y");
        second.setItemName("演示产品");
        assertThatThrownBy(() -> service.insertProRouteProduct(second))
            .isInstanceOf(ServiceException.class).hasMessageContaining("默认路线");
    }

    @Test
    @DisplayName("更新默认记录自身 -> 通过(排除自身)")
    void should_allowUpdate_when_defaultIsSelf() {
        ProRouteProduct self = binding(10L, 100L, "Y");
        when(routeProductMapper.selectProRouteProductList(any())).thenReturn(List.of(self));
        self.setRemark("改备注");
        assertThatCode(() -> service.updateProRouteProduct(self)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("非默认绑定不触发校验 -> 通过")
    void should_pass_when_notDefault() {
        when(routeProductMapper.selectProRouteProductList(any())).thenReturn(List.of(binding(10L, 100L, "Y")));
        ProRouteProduct another = binding(null, 100L, "N");
        assertThatCode(() -> service.insertProRouteProduct(another)).doesNotThrowAnyException();
    }

    private ProRouteProduct binding(Long recordId, Long itemId, String isDefault) {
        ProRouteProduct p = new ProRouteProduct();
        p.setRecordId(recordId);
        p.setItemId(itemId);
        p.setIsDefault(isDefault);
        return p;
    }

    @Test
    @DisplayName("SKU 变体复制: 继承外发节点(标志+供应商四列)与绑定四维标签")
    void should_copyOutsourceNodesAndDimensions_when_copyForSku() {
        ProRouteProduct parent = binding(1L, 100L, "Y");
        parent.setRouteId(10L);
        parent.setApplyOrderType("GIFT");
        parent.setApplyOutsource("Y");
        parent.setApplyPackage("N");
        when(routeProductMapper.selectProRouteProductList(any())).thenAnswer(inv -> {
            ProRouteProduct q = inv.getArgument(0);
            return q.getItemId() != null && q.getItemId().equals(100L) ? List.of(parent) : List.of();
        });
        ProRoute parentRoute = new ProRoute();
        parentRoute.setRouteId(10L);
        when(routeMapper.selectProRouteByRouteId(10L)).thenReturn(parentRoute);
        when(routeMapper.insertProRoute(any())).thenAnswer(inv -> { ((ProRoute) inv.getArgument(0)).setRouteId(20L); return 1; });
        ProRouteProcess outNode = new ProRouteProcess();
        outNode.setProcessId(7L);
        outNode.setProcessCode("PRC-OUT");
        outNode.setProcessName("外发印刷");
        outNode.setIsOutsource("1");
        outNode.setVendorId(208L);
        outNode.setVendorCode("OUT-WANLONG");
        outNode.setVendorName("万隆");
        outNode.setOutsourceFactoryId(9L);
        when(routeProcessMapper.selectProRouteProcessByRouteId(10L)).thenReturn(List.of(outNode));

        service.copyRouteProductForSku(100L, 200L, "SKU-001", "变体产品");

        ArgumentCaptor<ProRouteProcess> procCap = ArgumentCaptor.forClass(ProRouteProcess.class);
        verify(routeProcessMapper).insertProRouteProcess(procCap.capture());
        ProRouteProcess copiedNode = procCap.getValue();
        assertThat(copiedNode.getRouteId()).isEqualTo(20L);
        assertThat(copiedNode.getIsOutsource()).isEqualTo("1");
        assertThat(copiedNode.getVendorId()).isEqualTo(208L);
        assertThat(copiedNode.getVendorCode()).isEqualTo("OUT-WANLONG");
        assertThat(copiedNode.getVendorName()).isEqualTo("万隆");
        assertThat(copiedNode.getOutsourceFactoryId()).isEqualTo(9L);

        ArgumentCaptor<ProRouteProduct> rpCap = ArgumentCaptor.forClass(ProRouteProduct.class);
        verify(routeProductMapper).insertProRouteProduct(rpCap.capture());
        ProRouteProduct copied = rpCap.getValue();
        assertThat(copied.getItemId()).isEqualTo(200L);
        assertThat(copied.getApplyOrderType()).isEqualTo("GIFT");
        assertThat(copied.getApplyOutsource()).isEqualTo("Y");
        assertThat(copied.getApplyPackage()).isEqualTo("N");
        assertThat(copied.getIsDefault()).isEqualTo("Y");
    }
}
