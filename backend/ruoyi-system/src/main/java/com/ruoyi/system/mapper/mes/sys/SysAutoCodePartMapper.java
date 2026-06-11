package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;

/**
 * 编码生成规则组成Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysAutoCodePartMapper
{
    public SysAutoCodePart selectSysAutoCodePartByPartId(Long partId);
    public List<SysAutoCodePart> selectSysAutoCodePartList(SysAutoCodePart sysAutoCodePart);
    public List<SysAutoCodePart> selectSysAutoCodePartByRuleId(Long ruleId);
    public int insertSysAutoCodePart(SysAutoCodePart sysAutoCodePart);
    public int updateSysAutoCodePart(SysAutoCodePart sysAutoCodePart);
    public int deleteSysAutoCodePartByPartId(Long partId);
    public int deleteSysAutoCodePartByPartIds(Long[] partIds);
    public int deleteSysAutoCodePartByRuleId(Long ruleId);
}
