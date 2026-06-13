package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmItemRecptDetail;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmItemRecptDetailService;

@Service
public class WmItemRecptDetailServiceImpl implements IWmItemRecptDetailService
{
    @Autowired
    private WmItemRecptDetailMapper wmItemRecptDetailMapper;

    @Override
    public List<WmItemRecptDetail> selectWmItemRecptDetailList(WmItemRecptDetail entity) {
        return wmItemRecptDetailMapper.selectWmItemRecptDetailList(entity);
    }

    @Override
    public List<WmItemRecptDetail> selectWmItemRecptDetailAll() {
        return wmItemRecptDetailMapper.selectWmItemRecptDetailAll();
    }

    @Override
    public WmItemRecptDetail selectWmItemRecptDetailByDetailId(Long detailId) {
        return wmItemRecptDetailMapper.selectWmItemRecptDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int insertWmItemRecptDetail(WmItemRecptDetail entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmItemRecptDetailMapper.insertWmItemRecptDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmItemRecptDetail(WmItemRecptDetail entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmItemRecptDetailMapper.updateWmItemRecptDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptDetailByDetailId(Long detailId) {
        return wmItemRecptDetailMapper.deleteWmItemRecptDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptDetailByDetailIds(Long[] detailIds) {
        return wmItemRecptDetailMapper.deleteWmItemRecptDetailByDetailIds(detailIds);
    }
}