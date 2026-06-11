package com.ruoyi.system.service.mes.sys.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeRule;
import com.ruoyi.system.mapper.mes.sys.SysAutoCodeRuleMapper;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 编码生成规则Service实现
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Service
public class SysAutoCodeRuleServiceImpl implements ISysAutoCodeRuleService
{
    @Autowired
    private SysAutoCodeRuleMapper sysAutoCodeRuleMapper;

    @Override
    public SysAutoCodeRule selectSysAutoCodeRuleByRuleId(Long ruleId) {
        return sysAutoCodeRuleMapper.selectSysAutoCodeRuleByRuleId(ruleId);
    }

    @Override
    public SysAutoCodeRule selectSysAutoCodeRuleByRuleCode(String ruleCode) {
        return sysAutoCodeRuleMapper.selectSysAutoCodeRuleByRuleCode(ruleCode);
    }

    @Override
    public boolean checkRuleCodeUnique(SysAutoCodeRule sysAutoCodeRule) {
        Long ruleId = sysAutoCodeRule.getRuleId() == null ? -1L : sysAutoCodeRule.getRuleId();
        SysAutoCodeRule rule = sysAutoCodeRuleMapper.checkRuleCodeUnique(sysAutoCodeRule);
        if (StringUtils.isNotNull(rule) && rule.getRuleId().longValue() != ruleId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<SysAutoCodeRule> selectSysAutoCodeRuleList(SysAutoCodeRule sysAutoCodeRule) {
        return sysAutoCodeRuleMapper.selectSysAutoCodeRuleList(sysAutoCodeRule);
    }

    @Override
    public int insertSysAutoCodeRule(SysAutoCodeRule sysAutoCodeRule) {
        sysAutoCodeRule.setCreateTime(DateUtils.getNowDate());
        return sysAutoCodeRuleMapper.insertSysAutoCodeRule(sysAutoCodeRule);
    }

    @Override
    public int updateSysAutoCodeRule(SysAutoCodeRule sysAutoCodeRule) {
        sysAutoCodeRule.setUpdateTime(DateUtils.getNowDate());
        return sysAutoCodeRuleMapper.updateSysAutoCodeRule(sysAutoCodeRule);
    }

    @Override
    public int deleteSysAutoCodeRuleByRuleIds(Long[] ruleIds) {
        return sysAutoCodeRuleMapper.deleteSysAutoCodeRuleByRuleIds(ruleIds);
    }

    @Override
    public int deleteSysAutoCodeRuleByRuleId(Long ruleId) {
        return sysAutoCodeRuleMapper.deleteSysAutoCodeRuleByRuleId(ruleId);
    }
}
