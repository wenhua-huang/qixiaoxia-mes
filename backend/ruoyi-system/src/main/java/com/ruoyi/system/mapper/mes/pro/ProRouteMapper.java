package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRoute;

/**
 * 工艺路线Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProRouteMapper
{
    public ProRoute selectProRouteByRouteId(Long routeId);

    public List<ProRoute> selectProRouteList(ProRoute proRoute);

    public ProRoute selectProRouteByRouteCode(String routeCode);

    public int insertProRoute(ProRoute proRoute);

    public int updateProRoute(ProRoute proRoute);

    public int deleteProRouteByRouteId(Long routeId);

    public int deleteProRouteByRouteIds(Long[] routeIds);
}
