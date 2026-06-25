package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProWorkrecordMapper;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;
import com.ruoyi.system.service.mes.pro.IProWorkrecordService;

/**
 * ProWorkrecordService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProWorkrecordServiceImpl implements IProWorkrecordService
{
    @Autowired
    private ProWorkrecordMapper proWorkrecordMapper;

    @Override
    public ProWorkrecord selectProWorkrecordByRecordId(Long recordId) { return proWorkrecordMapper.selectProWorkrecordByRecordId(recordId); }

    @Override
    public List<ProWorkrecord> selectProWorkrecordList(ProWorkrecord e) { return proWorkrecordMapper.selectProWorkrecordList(e); }

    @Override
    public List<ProWorkrecord> selectAll() { return proWorkrecordMapper.selectProWorkrecordList(new ProWorkrecord()); }

    @Override
    @Transactional
    public int insertProWorkrecord(ProWorkrecord e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return proWorkrecordMapper.insertProWorkrecord(e);
    }

    @Override
    public int updateProWorkrecord(ProWorkrecord e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proWorkrecordMapper.updateProWorkrecord(e);
    }

    @Override
    public int deleteProWorkrecordByRecordIds(Long[] recordIds) { return proWorkrecordMapper.deleteProWorkrecordByRecordIds(recordIds); }

    @Override
    public int deleteProWorkrecordByRecordId(Long recordId) { return proWorkrecordMapper.deleteProWorkrecordByRecordId(recordId); }
}
