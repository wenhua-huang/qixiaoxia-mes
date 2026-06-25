package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;

/**
 * 工艺路线工序参数Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProRouteProcessParamMapper
{
    public ProRouteProcessParam selectProRouteProcessParamByRecordId(Long recordId);

    public List<ProRouteProcessParam> selectProRouteProcessParamList(ProRouteProcessParam proRouteProcessParam);

    public List<ProRouteProcessParam> selectProRouteProcessParamByRouteProductId(Long routeProductId);

    public int insertProRouteProcessParam(ProRouteProcessParam proRouteProcessParam);

    public int updateProRouteProcessParam(ProRouteProcessParam proRouteProcessParam);

    public int deleteProRouteProcessParamByRecordId(Long recordId);

    public int deleteProRouteProcessParamByRecordIds(Long[] recordIds);

    public int deleteProRouteProcessParamByRouteProductId(Long routeProductId);
}
