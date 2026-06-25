package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;

/**
 * 工艺路线工序参数Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProRouteProcessParamService
{
    public ProRouteProcessParam selectProRouteProcessParamByRecordId(Long recordId);

    public List<ProRouteProcessParam> selectProRouteProcessParamList(ProRouteProcessParam proRouteProcessParam);

    public List<ProRouteProcessParam> selectProRouteProcessParamByRouteProductId(Long routeProductId);

    public int insertProRouteProcessParam(ProRouteProcessParam proRouteProcessParam);

    public int updateProRouteProcessParam(ProRouteProcessParam proRouteProcessParam);

    public int deleteProRouteProcessParamByRecordIds(Long[] recordIds);

    public int deleteProRouteProcessParamByRecordId(Long recordId);

    public int deleteProRouteProcessParamByRouteProductId(Long routeProductId);

    public int batchInsertFromTemplate(Long routeProductId, Long processId);

    public int batchUpdate(List<ProRouteProcessParam> list);
}
