package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProProcessMapper;
import com.ruoyi.system.domain.mes.pro.ProProcess;
import com.ruoyi.system.service.mes.pro.IProProcessService;

/**
 * 生产工序Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProProcessServiceImpl implements IProProcessService
{
    @Autowired
    private ProProcessMapper qxxProProcessMapper;

    @Override
    public ProProcess selectProProcessByProcessId(Long processId)
    {
        return qxxProProcessMapper.selectProProcessByProcessId(processId);
    }

    @Override
    public List<ProProcess> selectProProcessList(ProProcess proProcess)
    {
        return qxxProProcessMapper.selectProProcessList(proProcess);
    }

    @Override
    public List<ProProcess> selectProProcessAll()
    {
        ProProcess cond = new ProProcess();
        cond.setEnableFlag("1");
        return qxxProProcessMapper.selectProProcessList(cond);
    }

    @Override
    @Transactional
    public int insertProProcess(ProProcess proProcess)
    {
        proProcess.setCreateTime(DateUtils.getNowDate());
        proProcess.setCreateBy(SecurityUtils.getUsername());
        return qxxProProcessMapper.insertProProcess(proProcess);
    }    @Override
    public int updateProProcess(ProProcess proProcess)
    {
        proProcess.setUpdateTime(DateUtils.getNowDate());
        proProcess.setUpdateBy(SecurityUtils.getUsername());
        return qxxProProcessMapper.updateProProcess(proProcess);
    }

    @Override
    public int deleteProProcessByProcessIds(Long[] processIds)
    {
        return qxxProProcessMapper.deleteProProcessByProcessIds(processIds);
    }

    @Override
    public int deleteProProcessByProcessId(Long processId)
    {
        return qxxProProcessMapper.deleteProProcessByProcessId(processId);
    }

    @Override
    public boolean checkProcessCodeUnique(ProProcess proProcess)
    {
        ProProcess existing = qxxProProcessMapper.selectProProcessByProcessCode(proProcess.getProcessCode());
        if (existing == null) return true;
        if (existing.getProcessId().equals(proProcess.getProcessId())) return true;
        throw new ServiceException("工序编码[" + proProcess.getProcessCode() + "]已存在");
    }
}
