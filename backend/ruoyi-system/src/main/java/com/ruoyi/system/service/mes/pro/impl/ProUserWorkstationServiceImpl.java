package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.service.mes.pro.IProUserWorkstationService;

/**
 * ProUserWorkstationService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProUserWorkstationServiceImpl implements IProUserWorkstationService
{
    @Autowired
    private ProUserWorkstationMapper proUserWorkstationMapper;

    @Override
    public ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId) { return proUserWorkstationMapper.selectProUserWorkstationByRecordId(recordId); }

    @Override
    public List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e) { return proUserWorkstationMapper.selectProUserWorkstationList(e); }

    @Override
    public List<ProUserWorkstation> selectAll() { ProUserWorkstation cond = new ProUserWorkstation(); cond.setEnableFlag("1"); return proUserWorkstationMapper.selectProUserWorkstationList(cond); }

    @Override
    @Transactional
    public int insertProUserWorkstation(ProUserWorkstation e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return proUserWorkstationMapper.insertProUserWorkstation(e);
    }    @Override
    public int updateProUserWorkstation(ProUserWorkstation e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proUserWorkstationMapper.updateProUserWorkstation(e);
    }

    @Override
    public int deleteProUserWorkstationByRecordIds(Long[] recordIds) { return proUserWorkstationMapper.deleteProUserWorkstationByRecordIds(recordIds); }

    @Override
    public int deleteProUserWorkstationByRecordId(Long recordId) { return proUserWorkstationMapper.deleteProUserWorkstationByRecordId(recordId); }
}
