package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeRule;

/**
 * 编码生成规则Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysAutoCodeRuleMapper
{
    public SysAutoCodeRule selectSysAutoCodeRuleByRuleId(Long ruleId);
    public SysAutoCodeRule selectSysAutoCodeRuleByRuleCode(String ruleCode);
    public SysAutoCodeRule checkRuleCodeUnique(SysAutoCodeRule sysAutoCodeRule);
    public List<SysAutoCodeRule> selectSysAutoCodeRuleList(SysAutoCodeRule sysAutoCodeRule);
    public int insertSysAutoCodeRule(SysAutoCodeRule sysAutoCodeRule);
    public int updateSysAutoCodeRule(SysAutoCodeRule sysAutoCodeRule);
    public int deleteSysAutoCodeRuleByRuleId(Long ruleId);
    public int deleteSysAutoCodeRuleByRuleIds(Long[] ruleIds);
}
