package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;

/**
 * 工艺路线-工序组成Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProRouteProcessService
{
    public ProRouteProcess selectProRouteProcessByRecordId(Long recordId);

    public List<ProRouteProcess> selectProRouteProcessList(ProRouteProcess proRouteProcess);

    public List<ProRouteProcess> selectProRouteProcessByRouteId(Long routeId);

    public int insertProRouteProcess(ProRouteProcess proRouteProcess);

    public int updateProRouteProcess(ProRouteProcess proRouteProcess);

    public int deleteProRouteProcessByRecordIds(Long[] recordIds);

    public int deleteProRouteProcessByRecordId(Long recordId);

    public int deleteProRouteProcessByRouteId(Long routeId);
}
