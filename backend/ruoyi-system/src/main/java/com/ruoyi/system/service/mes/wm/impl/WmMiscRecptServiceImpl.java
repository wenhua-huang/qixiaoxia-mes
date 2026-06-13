package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmMiscRecpt;
import com.ruoyi.system.mapper.mes.wm.WmMiscRecptMapper;
import com.ruoyi.system.service.mes.wm.IWmMiscRecptService;

@Service
public class WmMiscRecptServiceImpl implements IWmMiscRecptService
{
    @Autowired
    private WmMiscRecptMapper wmMiscRecptMapper;

    @Override
    public List<WmMiscRecpt> selectWmMiscRecptList(WmMiscRecpt entity) {
        return wmMiscRecptMapper.selectWmMiscRecptList(entity);
    }

    @Override
    public List<WmMiscRecpt> selectWmMiscRecptAll() {
        return wmMiscRecptMapper.selectWmMiscRecptAll();
    }

    @Override
    public WmMiscRecpt selectWmMiscRecptByRecptId(Long recptId) {
        return wmMiscRecptMapper.selectWmMiscRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int insertWmMiscRecpt(WmMiscRecpt entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmMiscRecptMapper.insertWmMiscRecpt(entity);
    }

    @Override
    @Transactional
    public int updateWmMiscRecpt(WmMiscRecpt entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmMiscRecptMapper.updateWmMiscRecpt(entity);
    }

    @Override
    @Transactional
    public int deleteWmMiscRecptByRecptId(Long recptId) {
        return wmMiscRecptMapper.deleteWmMiscRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int deleteWmMiscRecptByRecptIds(Long[] recptIds) {
        return wmMiscRecptMapper.deleteWmMiscRecptByRecptIds(recptIds);
    }
}