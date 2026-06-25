package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;

/**
 * ProMaterialTraceService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProMaterialTraceService
{
    public ProMaterialTrace selectProMaterialTraceByTraceId(Long traceId);
    public List<ProMaterialTrace> selectProMaterialTraceList(ProMaterialTrace e);
    public List<ProMaterialTrace> selectAll();
    public int insertProMaterialTrace(ProMaterialTrace e);
    public int updateProMaterialTrace(ProMaterialTrace e);
    public int deleteProMaterialTraceByTraceIds(Long[] traceIds);
    public int deleteProMaterialTraceByTraceId(Long traceId);
    /** 正向追溯：查来源的所有去向 */
    public List<ProMaterialTrace> traceForward(String parentType, Long parentId);
    /** 反向追溯：查去向的所有来源 */
    public List<ProMaterialTrace> traceBackward(String childType, Long childId);
}
