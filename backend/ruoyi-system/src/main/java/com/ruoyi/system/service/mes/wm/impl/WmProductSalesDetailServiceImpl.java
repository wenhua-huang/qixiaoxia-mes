package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmProductSalesDetailService;

@Service
public class WmProductSalesDetailServiceImpl implements IWmProductSalesDetailService
{
    @Autowired
    private WmProductSalesDetailMapper wmProductSalesDetailMapper;

    @Override
    public List<WmProductSalesDetail> selectWmProductSalesDetailList(WmProductSalesDetail entity) {
        return wmProductSalesDetailMapper.selectWmProductSalesDetailList(entity);
    }

    @Override
    public List<WmProductSalesDetail> selectWmProductSalesDetailAll() {
        return wmProductSalesDetailMapper.selectWmProductSalesDetailAll();
    }

    @Override
    public WmProductSalesDetail selectWmProductSalesDetailByDetailId(Long detailId) {
        return wmProductSalesDetailMapper.selectWmProductSalesDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int insertWmProductSalesDetail(WmProductSalesDetail entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmProductSalesDetailMapper.insertWmProductSalesDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmProductSalesDetail(WmProductSalesDetail entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmProductSalesDetailMapper.updateWmProductSalesDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesDetailByDetailId(Long detailId) {
        return wmProductSalesDetailMapper.deleteWmProductSalesDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesDetailByDetailIds(Long[] detailIds) {
        return wmProductSalesDetailMapper.deleteWmProductSalesDetailByDetailIds(detailIds);
    }
}