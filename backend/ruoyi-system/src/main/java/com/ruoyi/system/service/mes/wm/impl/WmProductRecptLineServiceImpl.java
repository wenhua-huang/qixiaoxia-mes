package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptLineMapper;
import com.ruoyi.system.service.mes.wm.IWmProductRecptLineService;

@Service
public class WmProductRecptLineServiceImpl implements IWmProductRecptLineService
{
    @Autowired
    private WmProductRecptLineMapper wmProductRecptLineMapper;

    @Override
    public List<WmProductRecptLine> selectWmProductRecptLineList(WmProductRecptLine entity) {
        return wmProductRecptLineMapper.selectWmProductRecptLineList(entity);
    }

    @Override
    public List<WmProductRecptLine> selectWmProductRecptLineAll() {
        return wmProductRecptLineMapper.selectWmProductRecptLineAll();
    }

    @Override
    public WmProductRecptLine selectWmProductRecptLineByLineId(Long lineId) {
        return wmProductRecptLineMapper.selectWmProductRecptLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmProductRecptLine(WmProductRecptLine entity) {
        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(DateUtils.getNowDate());
        return wmProductRecptLineMapper.insertWmProductRecptLine(entity);
    }

    @Override
    @Transactional
    public int updateWmProductRecptLine(WmProductRecptLine entity) {
        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmProductRecptLineMapper.updateWmProductRecptLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmProductRecptLineByLineId(Long lineId) {
        return wmProductRecptLineMapper.deleteWmProductRecptLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmProductRecptLineByLineIds(Long[] lineIds) {
        return wmProductRecptLineMapper.deleteWmProductRecptLineByLineIds(lineIds);
    }
}
