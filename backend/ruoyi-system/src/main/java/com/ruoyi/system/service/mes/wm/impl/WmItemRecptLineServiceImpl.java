package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptLineMapper;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;

@Service
public class WmItemRecptLineServiceImpl implements IWmItemRecptLineService
{
    @Autowired
    private WmItemRecptLineMapper wmItemRecptLineMapper;

    @Override
    public List<WmItemRecptLine> selectWmItemRecptLineList(WmItemRecptLine entity) {
        return wmItemRecptLineMapper.selectWmItemRecptLineList(entity);
    }

    @Override
    public List<WmItemRecptLine> selectWmItemRecptLineAll() {
        return wmItemRecptLineMapper.selectWmItemRecptLineAll();
    }

    @Override
    public WmItemRecptLine selectWmItemRecptLineByLineId(Long lineId) {
        return wmItemRecptLineMapper.selectWmItemRecptLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmItemRecptLine(WmItemRecptLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmItemRecptLineMapper.insertWmItemRecptLine(entity);
    }

    @Override
    @Transactional
    public int updateWmItemRecptLine(WmItemRecptLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmItemRecptLineMapper.updateWmItemRecptLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptLineByLineId(Long lineId) {
        return wmItemRecptLineMapper.deleteWmItemRecptLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptLineByLineIds(Long[] lineIds) {
        return wmItemRecptLineMapper.deleteWmItemRecptLineByLineIds(lineIds);
    }
}