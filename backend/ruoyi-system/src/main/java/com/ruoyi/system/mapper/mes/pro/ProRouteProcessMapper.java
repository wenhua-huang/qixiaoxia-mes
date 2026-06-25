package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;

/**
 * 工艺路线-工序组成Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProRouteProcessMapper
{
    public ProRouteProcess selectProRouteProcessByRecordId(Long recordId);

    public List<ProRouteProcess> selectProRouteProcessList(ProRouteProcess proRouteProcess);

    public List<ProRouteProcess> selectProRouteProcessByRouteId(Long routeId);

    public ProRouteProcess selectLastProcessByRouteId(Long routeId);

    public int insertProRouteProcess(ProRouteProcess proRouteProcess);

    public int updateProRouteProcess(ProRouteProcess proRouteProcess);

    public int deleteProRouteProcessByRecordId(Long recordId);

    public int deleteProRouteProcessByRecordIds(Long[] recordIds);

    public int deleteProRouteProcessByRouteId(Long routeId);
}
