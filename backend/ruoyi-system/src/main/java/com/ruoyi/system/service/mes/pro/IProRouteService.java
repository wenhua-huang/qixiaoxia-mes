package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRoute;

/**
 * 工艺路线Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProRouteService
{
    public ProRoute selectProRouteByRouteId(Long routeId);

    public List<ProRoute> selectProRouteList(ProRoute proRoute);

    public List<ProRoute> selectProRouteAll();

        public int insertProRoute(ProRoute proRoute);
    public int updateProRoute(ProRoute proRoute);

    public int deleteProRouteByRouteIds(Long[] routeIds);

    public int deleteProRouteByRouteId(Long routeId);

    public boolean checkRouteCodeUnique(ProRoute proRoute);
}
