package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProductBom;

/**
 * 工艺路线产品BOMMapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProRouteProductBomMapper
{
    public ProRouteProductBom selectProRouteProductBomByRecordId(Long recordId);

    public List<ProRouteProductBom> selectProRouteProductBomList(ProRouteProductBom proRouteProductBom);

    public List<ProRouteProductBom> selectProRouteProductBomByRouteId(Long routeId);

    public List<ProRouteProductBom> selectProRouteProductBomByRouteIdAndProcessId(Long routeId, Long processId);

    public int insertProRouteProductBom(ProRouteProductBom proRouteProductBom);

    public int updateProRouteProductBom(ProRouteProductBom proRouteProductBom);

    public int deleteProRouteProductBomByRecordId(Long recordId);

    public int deleteProRouteProductBomByRecordIds(Long[] recordIds);

    public int deleteProRouteProductBomByRouteId(Long routeId);
}
