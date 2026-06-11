package com.ruoyi.system.service.mes.sys.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeResult;
import com.ruoyi.system.mapper.mes.sys.SysAutoCodeResultMapper;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 编码生成记录Service实现
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Service
public class SysAutoCodeResultServiceImpl implements ISysAutoCodeResultService
{
    @Autowired
    private SysAutoCodeResultMapper sysAutoCodeResultMapper;

    @Override
    public SysAutoCodeResult selectSysAutoCodeResultByCodeId(Long codeId) {
        return sysAutoCodeResultMapper.selectSysAutoCodeResultByCodeId(codeId);
    }

    @Override
    public SysAutoCodeResult selectSysAutoCodeResultForUpdate(Long ruleId, String genDate, String lastInputChar) {
        return sysAutoCodeResultMapper.selectSysAutoCodeResultForUpdate(ruleId, genDate, lastInputChar);
    }

    @Override
    public List<SysAutoCodeResult> selectSysAutoCodeResultList(SysAutoCodeResult sysAutoCodeResult) {
        return sysAutoCodeResultMapper.selectSysAutoCodeResultList(sysAutoCodeResult);
    }

    @Override
    public int insertSysAutoCodeResult(SysAutoCodeResult sysAutoCodeResult) {
        sysAutoCodeResult.setCreateTime(DateUtils.getNowDate());
        return sysAutoCodeResultMapper.insertSysAutoCodeResult(sysAutoCodeResult);
    }

    @Override
    public int updateSysAutoCodeResult(SysAutoCodeResult sysAutoCodeResult) {
        sysAutoCodeResult.setUpdateTime(DateUtils.getNowDate());
        return sysAutoCodeResultMapper.updateSysAutoCodeResult(sysAutoCodeResult);
    }

    @Override
    public int deleteSysAutoCodeResultByCodeIds(Long[] codeIds) {
        return sysAutoCodeResultMapper.deleteSysAutoCodeResultByCodeIds(codeIds);
    }

    @Override
    public int deleteSysAutoCodeResultByCodeId(Long codeId) {
        return sysAutoCodeResultMapper.deleteSysAutoCodeResultByCodeId(codeId);
    }
}
