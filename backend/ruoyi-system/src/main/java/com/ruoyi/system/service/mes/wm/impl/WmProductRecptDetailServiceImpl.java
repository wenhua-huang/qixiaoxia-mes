package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.wm.WmProductRecptDetail;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmProductRecptDetailService;

@Service
public class WmProductRecptDetailServiceImpl implements IWmProductRecptDetailService
{
    @Autowired
    private WmProductRecptDetailMapper wmProductRecptDetailMapper;

    @Override
    public List<WmProductRecptDetail> selectWmProductRecptDetailList(WmProductRecptDetail entity) {
        return wmProductRecptDetailMapper.selectWmProductRecptDetailList(entity);
    }

    @Override
    public List<WmProductRecptDetail> selectWmProductRecptDetailAll() {
        return wmProductRecptDetailMapper.selectWmProductRecptDetailAll();
    }

    @Override
    public WmProductRecptDetail selectWmProductRecptDetailByDetailId(Long detailId) {
        return wmProductRecptDetailMapper.selectWmProductRecptDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int insertWmProductRecptDetail(WmProductRecptDetail entity) {
        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(DateUtils.getNowDate());
        return wmProductRecptDetailMapper.insertWmProductRecptDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmProductRecptDetail(WmProductRecptDetail entity) {
        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmProductRecptDetailMapper.updateWmProductRecptDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmProductRecptDetailByDetailId(Long detailId) {
        return wmProductRecptDetailMapper.deleteWmProductRecptDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int deleteWmProductRecptDetailByDetailIds(Long[] detailIds) {
        return wmProductRecptDetailMapper.deleteWmProductRecptDetailByDetailIds(detailIds);
    }
}
