package com.ruoyi.system.domain.mes.pro.dto;

import com.ruoyi.system.domain.mes.pro.ProRouteProduct;

/**
 * 默认工艺路线解析结果
 *
 * <p>三态：matched=命中默认路线；hardBlocked=外发硬约束不满足(整单阻断)；
 * 两者皆否=未命中且不阻断(留空手选)。
 *
 * @author qixiaoxia
 * @date 2026-09-10
 */
public class RouteResolveResult
{
    private Long routeProductId;
    private Long routeId;
    private boolean matched;
    private boolean hardBlocked;
    private String message;

    public static RouteResolveResult matched(ProRouteProduct rp) {
        RouteResolveResult r = new RouteResolveResult();
        r.routeProductId = rp.getRecordId();
        r.routeId = rp.getRouteId();
        r.matched = true;
        return r;
    }

    public static RouteResolveResult empty() {
        return new RouteResolveResult();
    }

    public static RouteResolveResult blocked(String message) {
        RouteResolveResult r = new RouteResolveResult();
        r.hardBlocked = true;
        r.message = message;
        return r;
    }

    public Long getRouteProductId() { return routeProductId; }
    public Long getRouteId() { return routeId; }
    public boolean isMatched() { return matched; }
    public boolean isHardBlocked() { return hardBlocked; }
    public String getMessage() { return message; }
}
