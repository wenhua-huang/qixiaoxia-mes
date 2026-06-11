package com.ruoyi.system.service.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeRule;

/**
 * 编码生成规则Service接口
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface ISysAutoCodeRuleService
{
    public SysAutoCodeRule selectSysAutoCodeRuleByRuleId(Long ruleId);
    public SysAutoCodeRule selectSysAutoCodeRuleByRuleCode(String ruleCode);
    public boolean checkRuleCodeUnique(SysAutoCodeRule sysAutoCodeRule);
    public List<SysAutoCodeRule> selectSysAutoCodeRuleList(SysAutoCodeRule sysAutoCodeRule);
    public int insertSysAutoCodeRule(SysAutoCodeRule sysAutoCodeRule);
    public int updateSysAutoCodeRule(SysAutoCodeRule sysAutoCodeRule);
    public int deleteSysAutoCodeRuleByRuleIds(Long[] ruleIds);
    public int deleteSysAutoCodeRuleByRuleId(Long ruleId);
}
