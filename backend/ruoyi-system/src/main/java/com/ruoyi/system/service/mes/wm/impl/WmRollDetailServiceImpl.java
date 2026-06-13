package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;
import com.ruoyi.system.mapper.mes.wm.WmRollDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmRollDetailService;

@Service
public class WmRollDetailServiceImpl implements IWmRollDetailService
{
    @Autowired
    private WmRollDetailMapper wmRollDetailMapper;

    @Override
    public List<WmRollDetail> selectWmRollDetailList(WmRollDetail entity) {
        return wmRollDetailMapper.selectWmRollDetailList(entity);
    }

    @Override
    public List<WmRollDetail> selectWmRollDetailAll() {
        return wmRollDetailMapper.selectWmRollDetailAll();
    }

    @Override
    public WmRollDetail selectWmRollDetailByRollId(Long rollId) {
        return wmRollDetailMapper.selectWmRollDetailByRollId(rollId);
    }

    @Override
    @Transactional
    public int insertWmRollDetail(WmRollDetail entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRollDetailMapper.insertWmRollDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmRollDetail(WmRollDetail entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRollDetailMapper.updateWmRollDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmRollDetailByRollId(Long rollId) {
        return wmRollDetailMapper.deleteWmRollDetailByRollId(rollId);
    }

    @Override
    @Transactional
    public int deleteWmRollDetailByRollIds(Long[] rollIds) {
        return wmRollDetailMapper.deleteWmRollDetailByRollIds(rollIds);
    }
}