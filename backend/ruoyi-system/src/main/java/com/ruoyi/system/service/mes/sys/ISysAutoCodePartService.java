package com.ruoyi.system.service.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;

/**
 * 编码生成规则组成Service接口
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface ISysAutoCodePartService
{
    public SysAutoCodePart selectSysAutoCodePartByPartId(Long partId);
    public List<SysAutoCodePart> selectSysAutoCodePartList(SysAutoCodePart sysAutoCodePart);
    public List<SysAutoCodePart> selectSysAutoCodePartByRuleId(Long ruleId);
    public int insertSysAutoCodePart(SysAutoCodePart sysAutoCodePart);
    public int updateSysAutoCodePart(SysAutoCodePart sysAutoCodePart);
    public int deleteSysAutoCodePartByPartIds(Long[] partIds);
    public int deleteSysAutoCodePartByPartId(Long partId);
    public int deleteSysAutoCodePartByRuleId(Long ruleId);
}
