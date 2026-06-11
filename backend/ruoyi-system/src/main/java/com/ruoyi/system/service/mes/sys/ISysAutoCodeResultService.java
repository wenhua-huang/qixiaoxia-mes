package com.ruoyi.system.service.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeResult;

/**
 * 编码生成记录Service接口
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface ISysAutoCodeResultService
{
    public SysAutoCodeResult selectSysAutoCodeResultByCodeId(Long codeId);
    public SysAutoCodeResult selectSysAutoCodeResultForUpdate(Long ruleId, String genDate, String lastInputChar);
    public List<SysAutoCodeResult> selectSysAutoCodeResultList(SysAutoCodeResult sysAutoCodeResult);
    public int insertSysAutoCodeResult(SysAutoCodeResult sysAutoCodeResult);
    public int updateSysAutoCodeResult(SysAutoCodeResult sysAutoCodeResult);
    public int deleteSysAutoCodeResultByCodeIds(Long[] codeIds);
    public int deleteSysAutoCodeResultByCodeId(Long codeId);
}
