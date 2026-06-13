package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmMiscRecptLine;
import com.ruoyi.system.mapper.mes.wm.WmMiscRecptLineMapper;
import com.ruoyi.system.service.mes.wm.IWmMiscRecptLineService;

@Service
public class WmMiscRecptLineServiceImpl implements IWmMiscRecptLineService
{
    @Autowired
    private WmMiscRecptLineMapper wmMiscRecptLineMapper;

    @Override
    public List<WmMiscRecptLine> selectWmMiscRecptLineList(WmMiscRecptLine entity) {
        return wmMiscRecptLineMapper.selectWmMiscRecptLineList(entity);
    }

    @Override
    public List<WmMiscRecptLine> selectWmMiscRecptLineAll() {
        return wmMiscRecptLineMapper.selectWmMiscRecptLineAll();
    }

    @Override
    public WmMiscRecptLine selectWmMiscRecptLineByLineId(Long lineId) {
        return wmMiscRecptLineMapper.selectWmMiscRecptLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmMiscRecptLine(WmMiscRecptLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmMiscRecptLineMapper.insertWmMiscRecptLine(entity);
    }

    @Override
    @Transactional
    public int updateWmMiscRecptLine(WmMiscRecptLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmMiscRecptLineMapper.updateWmMiscRecptLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmMiscRecptLineByLineId(Long lineId) {
        return wmMiscRecptLineMapper.deleteWmMiscRecptLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmMiscRecptLineByLineIds(Long[] lineIds) {
        return wmMiscRecptLineMapper.deleteWmMiscRecptLineByLineIds(lineIds);
    }
}