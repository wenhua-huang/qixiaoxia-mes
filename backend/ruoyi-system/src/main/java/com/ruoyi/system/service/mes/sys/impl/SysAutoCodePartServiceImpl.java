package com.ruoyi.system.service.mes.sys.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import com.ruoyi.system.mapper.mes.sys.SysAutoCodePartMapper;
import com.ruoyi.system.service.mes.sys.ISysAutoCodePartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 编码生成规则组成Service实现
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Service
public class SysAutoCodePartServiceImpl implements ISysAutoCodePartService
{
    @Autowired
    private SysAutoCodePartMapper sysAutoCodePartMapper;

    @Override
    public SysAutoCodePart selectSysAutoCodePartByPartId(Long partId) {
        return sysAutoCodePartMapper.selectSysAutoCodePartByPartId(partId);
    }

    @Override
    public List<SysAutoCodePart> selectSysAutoCodePartList(SysAutoCodePart sysAutoCodePart) {
        return sysAutoCodePartMapper.selectSysAutoCodePartList(sysAutoCodePart);
    }

    @Override
    public List<SysAutoCodePart> selectSysAutoCodePartByRuleId(Long ruleId) {
        return sysAutoCodePartMapper.selectSysAutoCodePartByRuleId(ruleId);
    }

    @Override
    public int insertSysAutoCodePart(SysAutoCodePart sysAutoCodePart) {
        sysAutoCodePart.setCreateTime(DateUtils.getNowDate());
        return sysAutoCodePartMapper.insertSysAutoCodePart(sysAutoCodePart);
    }

    @Override
    public int updateSysAutoCodePart(SysAutoCodePart sysAutoCodePart) {
        sysAutoCodePart.setUpdateTime(DateUtils.getNowDate());
        return sysAutoCodePartMapper.updateSysAutoCodePart(sysAutoCodePart);
    }

    @Override
    public int deleteSysAutoCodePartByPartIds(Long[] partIds) {
        return sysAutoCodePartMapper.deleteSysAutoCodePartByPartIds(partIds);
    }

    @Override
    public int deleteSysAutoCodePartByPartId(Long partId) {
        return sysAutoCodePartMapper.deleteSysAutoCodePartByPartId(partId);
    }

    @Override
    public int deleteSysAutoCodePartByRuleId(Long ruleId) {
        return sysAutoCodePartMapper.deleteSysAutoCodePartByRuleId(ruleId);
    }
}
