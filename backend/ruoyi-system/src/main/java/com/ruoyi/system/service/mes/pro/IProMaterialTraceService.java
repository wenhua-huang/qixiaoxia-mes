package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.pro.ProMaterialTraceChainResult;

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
    /** 正向追溯：查来源的所有去向（单跳） */
    public List<ProMaterialTrace> traceForward(String parentType, Long parentId);
    /** 反向追溯：查去向的所有来源（单跳） */
    public List<ProMaterialTrace> traceBackward(String childType, Long childId);
    /**
     * 深度追溯：一次性递归返回完整链路（替代前端 N+1 查询）
     * @param startType 起始节点类型
     * @param startId   起始节点 ID
     * @param direction forward=正向追去向，backward=反向追来源
     * @return 链路结果（含 chain + endedReason）
     */
    public ProMaterialTraceChainResult traceChain(String startType, Long startId, String direction);
}
