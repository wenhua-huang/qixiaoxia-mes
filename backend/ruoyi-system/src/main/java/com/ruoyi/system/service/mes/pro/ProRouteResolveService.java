package com.ruoyi.system.service.mes.pro;

import java.util.Collection;
import java.util.Map;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;

/**
 * 按 订单类型 + 是否外发 + 是否包装 在产品绑定路线中解析默认路线。
 *
 * @author qixiaoxia
 * @date 2026-09-10
 */
public interface ProRouteResolveService
{
    /**
     * 解析单个产品的默认路线
     *
     * @param itemId        产品物料ID
     * @param orderType     订单类型(空按 STANDARD)
     * @param outsourceFlag 是否外发 Y/N(空按 N)
     * @param packageFlag   是否包装 Y/N(空按 N)
     */
    RouteResolveResult resolve(Long itemId, String orderType, String outsourceFlag, String packageFlag);

    /** 批量解析，key=itemId（头维度对多行相同） */
    Map<Long, RouteResolveResult> resolveBatch(Collection<Long> itemIds, String orderType,
                                               String outsourceFlag, String packageFlag);
}
