package com.ruoyi.system.service.mes.pro;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.service.mes.pro.impl.ProRouteResolveServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("默认工艺路线解析服务测试")
class ProRouteResolveServiceTest
{
    @Mock private ProRouteProductMapper routeProductMapper;
    @Mock private ProRouteProcessMapper routeProcessMapper;
    @InjectMocks private ProRouteResolveServiceImpl service;

    private ProRouteProduct rp(long id, long routeId, String type, String out, String pkg, String isDefault) {
        ProRouteProduct p = new ProRouteProduct();
        p.setRecordId(id);
        p.setRouteId(routeId);
        p.setItemId(100L);
        p.setItemName("演示产品");
        p.setApplyOrderType(type);
        p.setApplyOutsource(out);
        p.setApplyPackage(pkg);
        p.setIsDefault(isDefault);
        return p;
    }

    private void stubRoutes(List<ProRouteProduct> rows) {
        when(routeProductMapper.selectProRouteProductList(any())).thenReturn(rows);
    }

    private void stubNodes(long routeId, boolean hasOutsource) {
        ProRouteProcess n = new ProRouteProcess();
        n.setIsOutsource(hasOutsource ? "1" : "0");
        when(routeProcessMapper.selectProRouteProcessByRouteId(routeId)).thenReturn(List.of(n));
    }

    @Test
    @DisplayName("三维精确匹配优先于通配候选")
    void should_pickExact_when_bothWildcardAndExactExist() {
        ProRouteProduct wildcard = rp(1L, 10L, null, null, null, "Y");
        ProRouteProduct exact = rp(2L, 11L, "GIFT", "N", "Y", "N");
        stubRoutes(List.of(wildcard, exact));
        stubNodes(10L, false);
        stubNodes(11L, false);
        RouteResolveResult r = service.resolve(100L, "GIFT", "N", "Y");
        assertThat(r.isMatched()).isTrue();
        assertThat(r.getRouteProductId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("维度不符的候选被淘汰")
    void should_excludeCandidate_when_dimensionMismatch() {
        stubRoutes(List.of(rp(1L, 10L, "PLATE", null, null, "N")));
        stubNodes(10L, false);
        RouteResolveResult r = service.resolve(100L, "STANDARD", "N", "N");
        assertThat(r.isMatched()).isFalse();
        assertThat(r.isHardBlocked()).isFalse();
    }

    @Test
    @DisplayName("同分时 isDefault=Y 胜出")
    void should_pickDefault_when_scoresTie() {
        ProRouteProduct a = rp(1L, 10L, "STANDARD", null, null, "N");
        ProRouteProduct b = rp(2L, 11L, "STANDARD", null, null, "Y");
        stubRoutes(List.of(a, b));
        stubNodes(10L, false);
        stubNodes(11L, false);
        assertThat(service.resolve(100L, "STANDARD", "N", "N").getRouteProductId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("同分时均非默认, recordId 小者稳定胜出")
    void should_pickLowerRecordId_when_tieAndNoDefault() {
        stubRoutes(List.of(rp(2L, 11L, null, null, null, "N"), rp(1L, 10L, null, null, null, "N")));
        stubNodes(10L, false);
        stubNodes(11L, false);
        assertThat(service.resolve(100L, "STANDARD", "N", "N").getRouteProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("外发=Y 且无任何含外发节点路线 -> hardBlocked")
    void should_block_when_outsourceOrderButNoOutsourceRoute() {
        ProRouteProduct tagged = rp(1L, 10L, null, "Y", null, "N");
        stubRoutes(List.of(tagged));
        stubNodes(10L, false);
        RouteResolveResult r = service.resolve(100L, "STANDARD", "Y", "N");
        assertThat(r.isHardBlocked()).isTrue();
        assertThat(r.isMatched()).isFalse();
        assertThat(r.getMessage()).contains("外发");
    }

    @Test
    @DisplayName("外发=Y 命中含外发节点的路线")
    void should_matchOutsourceRoute_when_nodeFlagged() {
        stubRoutes(List.of(rp(1L, 10L, null, "Y", null, "N")));
        stubNodes(10L, true);
        RouteResolveResult r = service.resolve(100L, "STANDARD", "Y", "N");
        assertThat(r.isMatched()).isTrue();
        assertThat(r.getRouteProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("产品无任何绑定 -> 空结果不阻断")
    void should_returnEmpty_when_noBinding() {
        stubRoutes(List.of());
        RouteResolveResult r = service.resolve(100L, "STANDARD", "N", "N");
        assertThat(r.isMatched()).isFalse();
        assertThat(r.isHardBlocked()).isFalse();
    }

    @Test
    @DisplayName("入参空值归一化为 STANDARD/N/N")
    void should_normalizeInputs_when_nullArgs() {
        stubRoutes(List.of(rp(1L, 10L, "STANDARD", "N", "N", "Y")));
        stubNodes(10L, false);
        assertThat(service.resolve(100L, null, null, null).isMatched()).isTrue();
    }

    @Test
    @DisplayName("外发=Y 但产品根本无绑定 -> 空结果(不硬阻断, 允许手选)")
    void should_returnEmpty_when_outsourceOrderButNoBinding() {
        stubRoutes(List.of());
        RouteResolveResult r = service.resolve(100L, null, "Y", null);
        assertThat(r.isHardBlocked()).isFalse();
        assertThat(r.isMatched()).isFalse();
    }

    @Test
    @DisplayName("resolveBatch 为每个 itemId 返回独立结果")
    void should_returnMap_when_resolveBatch() {
        ProRouteProduct p1 = rp(1L, 10L, null, null, null, "Y");
        p1.setItemId(100L);
        ProRouteProduct p2 = rp(2L, 20L, null, null, null, "Y");
        p2.setItemId(200L);
        when(routeProductMapper.selectProRouteProductList(any())).thenAnswer(inv -> {
            ProRouteProduct q = inv.getArgument(0);
            if (q.getItemId() == 100L) return List.of(p1);
            if (q.getItemId() == 200L) return List.of(p2);
            return List.of();
        });
        stubNodes(10L, false);
        stubNodes(20L, false);
        var map = service.resolveBatch(List.of(100L, 200L, 300L), null, null, null);
        assertThat(map.get(100L).isMatched()).isTrue();
        assertThat(map.get(200L).isMatched()).isTrue();
        assertThat(map.get(300L).isMatched()).isFalse();
    }
}
