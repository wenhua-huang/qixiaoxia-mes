package com.ruoyi.system.mapper.mes.pro;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /** 按多个路线ID批量拉取（订单保存批量补路线名快照用，避免逐行查询） */
    public List<ProRoute> selectByRouteIds(@Param("routeIds") Collection<Long> routeIds);

    public List<ProRoute> selectProRouteList(ProRoute proRoute);

    public ProRoute selectProRouteByRouteCode(String routeCode);

    public int insertProRoute(ProRoute proRoute);

    public int updateProRoute(ProRoute proRoute);

    public int deleteProRouteByRouteId(Long routeId);

    public int deleteProRouteByRouteIds(Long[] routeIds);
}
