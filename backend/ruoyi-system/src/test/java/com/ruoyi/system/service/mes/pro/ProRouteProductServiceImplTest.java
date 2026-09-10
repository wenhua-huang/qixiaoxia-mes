package com.ruoyi.system.service.mes.pro;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.service.mes.pro.impl.ProRouteProductServiceImpl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("产品路线绑定-默认路线唯一校验测试")
class ProRouteProductServiceImplTest
{
    @Mock private ProRouteProductMapper routeProductMapper;
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
}
