package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeResult;

/**
 * 编码生成记录Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysAutoCodeResultMapper
{
    public SysAutoCodeResult selectSysAutoCodeResultByCodeId(Long codeId);
    public SysAutoCodeResult selectSysAutoCodeResultForUpdate(@Param("ruleId") Long ruleId, @Param("genDate") String genDate, @Param("lastInputChar") String lastInputChar);
    public List<SysAutoCodeResult> selectSysAutoCodeResultList(SysAutoCodeResult sysAutoCodeResult);
    public int insertSysAutoCodeResult(SysAutoCodeResult sysAutoCodeResult);
    public int updateSysAutoCodeResult(SysAutoCodeResult sysAutoCodeResult);
    public int deleteSysAutoCodeResultByCodeId(Long codeId);
    public int deleteSysAutoCodeResultByCodeIds(Long[] codeIds);
}
