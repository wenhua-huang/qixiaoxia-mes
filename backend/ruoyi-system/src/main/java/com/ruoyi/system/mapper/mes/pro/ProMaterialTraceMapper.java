package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;

public interface ProMaterialTraceMapper {
    ProMaterialTrace selectProMaterialTraceByTraceId(Long traceId);
    List<ProMaterialTrace> selectProMaterialTraceList(ProMaterialTrace e);
    /** 正向追溯：查某个来源的所有去向（走 idx_parent 索引） */
    List<ProMaterialTrace> selectByParent(@Param("parentType") String parentType, @Param("parentId") Long parentId);
    /** 反向追溯：查某个去向的所有来源（走 idx_child 索引） */
    List<ProMaterialTrace> selectByChild(@Param("childType") String childType, @Param("childId") Long childId);
    int insertProMaterialTrace(ProMaterialTrace e);
    int updateProMaterialTrace(ProMaterialTrace e);
    int deleteProMaterialTraceByTraceId(Long traceId);
    int deleteProMaterialTraceByTraceIds(Long[] traceIds);
}
