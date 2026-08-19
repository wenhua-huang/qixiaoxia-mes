package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 按路线+工序唯一定位路线工序组成行（IPQC 工序检验 isCheck 判定用）
     *
     * @param routeId   工艺路线ID
     * @param processId 工序ID
     * @return 路线工序行；null = 该路线未配置此工序
     */
    public ProRouteProcess selectByRouteAndProcess(@Param("routeId") Long routeId,
                                                   @Param("processId") Long processId);

    public int insertProRouteProcess(ProRouteProcess proRouteProcess);

    public int updateProRouteProcess(ProRouteProcess proRouteProcess);

    public int deleteProRouteProcessByRecordId(Long recordId);

    public int deleteProRouteProcessByRecordIds(Long[] recordIds);

    public int deleteProRouteProcessByRouteId(Long routeId);
}
