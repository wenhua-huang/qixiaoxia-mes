package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;

@Service
public class WmItemRecptServiceImpl implements IWmItemRecptService
{
    @Autowired
    private WmItemRecptMapper wmItemRecptMapper;

    @Override
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity) {
        return wmItemRecptMapper.selectWmItemRecptList(entity);
    }

    @Override
    public List<WmItemRecpt> selectWmItemRecptAll() {
        return wmItemRecptMapper.selectWmItemRecptAll();
    }

    @Override
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId) {
        return wmItemRecptMapper.selectWmItemRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int insertWmItemRecpt(WmItemRecpt entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmItemRecptMapper.insertWmItemRecpt(entity);
    }

    @Override
    @Transactional
    public int updateWmItemRecpt(WmItemRecpt entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmItemRecptMapper.updateWmItemRecpt(entity);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptByRecptId(Long recptId) {
        return wmItemRecptMapper.deleteWmItemRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptByRecptIds(Long[] recptIds) {
        return wmItemRecptMapper.deleteWmItemRecptByRecptIds(recptIds);
    }
}