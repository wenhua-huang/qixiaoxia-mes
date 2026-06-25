package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProCardProcessMapper;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;
import com.ruoyi.system.service.mes.pro.IProCardProcessService;

/**
 * ProCardProcessService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProCardProcessServiceImpl implements IProCardProcessService
{
    @Autowired
    private ProCardProcessMapper proCardProcessMapper;

    @Override
    public ProCardProcess selectProCardProcessByRecordId(Long recordId) { return proCardProcessMapper.selectProCardProcessByRecordId(recordId); }

    @Override
    public List<ProCardProcess> selectProCardProcessList(ProCardProcess e) { return proCardProcessMapper.selectProCardProcessList(e); }

    @Override
    public List<ProCardProcess> selectAll() { return proCardProcessMapper.selectProCardProcessList(new ProCardProcess()); }

    @Override
    @Transactional
    public int insertProCardProcess(ProCardProcess e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return proCardProcessMapper.insertProCardProcess(e);
    }

    @Override
    public int updateProCardProcess(ProCardProcess e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proCardProcessMapper.updateProCardProcess(e);
    }

    @Override
    public int deleteProCardProcessByRecordIds(Long[] recordIds) { return proCardProcessMapper.deleteProCardProcessByRecordIds(recordIds); }

    @Override
    public int deleteProCardProcessByRecordId(Long recordId) { return proCardProcessMapper.deleteProCardProcessByRecordId(recordId); }
}
