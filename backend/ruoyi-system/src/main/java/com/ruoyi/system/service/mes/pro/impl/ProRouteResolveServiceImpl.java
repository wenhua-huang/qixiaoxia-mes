package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.enums.SalOrderType;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.service.mes.pro.ProRouteResolveService;

/**
 * 默认工艺路线解析：维度通配淘汰 → 外发节点校验 → 精确维度打分 → isDefault → recordId。
 *
 * @author qixiaoxia
 * @date 2026-09-10
 */
@Service
public class ProRouteResolveServiceImpl implements ProRouteResolveService
{
    private static final String YES = "Y";
    private static final String NO = "N";
    /** qxx_pro_route_process.is_outsource 外发节点取值为 1/0（与订单 Y/N 标志不同源） */
    private static final String OUTSOURCE_NODE = "1";

    @Autowired private ProRouteProductMapper routeProductMapper;
    @Autowired private ProRouteProcessMapper routeProcessMapper;

    @Override
    public RouteResolveResult resolve(Long itemId, String orderType, String outsourceFlag, String packageFlag) {
        if (itemId == null) return RouteResolveResult.empty();
        String type = normalizeType(orderType);
        String out = normalizeYn(outsourceFlag);
        String pkg = normalizeYn(packageFlag);

        List<ProRouteProduct> all = bindingsOf(itemId);
        Set<Long> outsourceRoutes = YES.equals(out) ? routesWithOutsourceNode(routeIds(all)) : Set.of();
        return pick(itemId, type, out, pkg, all, outsourceRoutes);
    }

    @Override
    public Map<Long, RouteResolveResult> resolveBatch(Collection<Long> itemIds, String orderType,
                                                      String outsourceFlag, String packageFlag) {
        Map<Long, RouteResolveResult> map = new LinkedHashMap<>();
        if (itemIds == null || itemIds.isEmpty()) return map;
        String type = normalizeType(orderType);
        String out = normalizeYn(outsourceFlag);
        String pkg = normalizeYn(packageFlag);

        // 去重后两次 IN 查询拉齐全部绑定与工序节点, 内存分组, 避免逐行 N+1
        List<Long> distinctItemIds = itemIds.stream().distinct().collect(Collectors.toList());
        List<ProRouteProduct> bindings = routeProductMapper.selectByItemIds(distinctItemIds);
        if (bindings == null) bindings = List.of();
        Map<Long, List<ProRouteProduct>> byItem = bindings.stream()
                .collect(Collectors.groupingBy(ProRouteProduct::getItemId));
        Set<Long> outsourceRoutes = YES.equals(out)
                ? routesWithOutsourceNode(routeIds(bindings)) : Set.of();
        for (Long itemId : itemIds) {
            map.put(itemId, pick(itemId, type, out, pkg, byItem.getOrDefault(itemId, List.of()), outsourceRoutes));
        }
        return map;
    }

    /** 单个物料在已预取绑定/节点集合上的选择逻辑 */
    private RouteResolveResult pick(Long itemId, String type, String out, String pkg,
                                    List<ProRouteProduct> all, Set<Long> outsourceRoutes) {
        if (all.isEmpty()) return RouteResolveResult.empty();

        boolean sawOutsourceIneligible = false;
        List<ProRouteProduct> survivors = new ArrayList<>();
        for (ProRouteProduct rp : all) {
            if (dimensionMismatch(rp, type, out, pkg)) continue;
            if (YES.equals(out) && !outsourceRoutes.contains(rp.getRouteId())) {
                sawOutsourceIneligible = true;
                continue;
            }
            survivors.add(rp);
        }
        // 有维度匹配的候选、但全部因不含外发节点出局才硬阻断；无任何绑定时留空手选
        if (survivors.isEmpty() && YES.equals(out) && sawOutsourceIneligible) {
            return RouteResolveResult.blocked(
                    "产品[" + itemId + "]未配置含外发工序的工艺路线，请先在工艺路线主数据配置");
        }
        if (survivors.isEmpty()) return RouteResolveResult.empty();

        survivors.sort(comparator());
        return RouteResolveResult.matched(survivors.get(0));
    }

    private List<ProRouteProduct> bindingsOf(Long itemId) {
        ProRouteProduct query = new ProRouteProduct();
        query.setItemId(itemId);
        List<ProRouteProduct> rows = routeProductMapper.selectProRouteProductList(query);
        return rows == null ? List.of() : rows;
    }

    /** 含 is_outsource=1 节点的路线ID集合（一次 IN 查询） */
    private Set<Long> routesWithOutsourceNode(Set<Long> routeIds) {
        if (routeIds.isEmpty()) return Set.of();
        List<ProRouteProcess> nodes = routeProcessMapper.selectByRouteIds(routeIds);
        if (nodes == null || nodes.isEmpty()) return Set.of();
        return nodes.stream()
                .filter(n -> OUTSOURCE_NODE.equals(n.getIsOutsource()))
                .map(ProRouteProcess::getRouteId)
                .collect(Collectors.toSet());
    }

    private Set<Long> routeIds(List<ProRouteProduct> bindings) {
        Set<Long> ids = new HashSet<>();
        for (ProRouteProduct rp : bindings) {
            if (rp.getRouteId() != null) ids.add(rp.getRouteId());
        }
        return ids;
    }

    private boolean dimensionMismatch(ProRouteProduct rp, String type, String out, String pkg) {
        return rp.getApplyOrderType() != null && !rp.getApplyOrderType().equals(type)
            || rp.getApplyOutsource() != null && !rp.getApplyOutsource().equals(out)
            || rp.getApplyPackage() != null && !rp.getApplyPackage().equals(pkg);
    }

    private String normalizeType(String orderType) {
        return orderType == null ? SalOrderType.STANDARD.getCode() : orderType;
    }

    private String normalizeYn(String flag) {
        return flag == null ? NO : flag;
    }

    /** 精确维度多者优先；同分 isDefault=Y；再同分 recordId 升序稳定 */
    private Comparator<ProRouteProduct> comparator() {
        return Comparator
            .comparingInt((ProRouteProduct rp) -> exactDimensionCount(rp)).reversed()
            .thenComparing(rp -> YES.equals(rp.getIsDefault()) ? 0 : 1)
            .thenComparing(ProRouteProduct::getRecordId);
    }

    private int exactDimensionCount(ProRouteProduct rp) {
        int score = 0;
        if (rp.getApplyOrderType() != null) score++;
        if (rp.getApplyOutsource() != null) score++;
        if (rp.getApplyPackage() != null) score++;
        return score;
    }
}
