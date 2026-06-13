package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRtSalesDetail;
import com.ruoyi.system.mapper.mes.wm.WmRtSalesDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmRtSalesDetailService;

@Service
public class WmRtSalesDetailServiceImpl implements IWmRtSalesDetailService
{
    @Autowired
    private WmRtSalesDetailMapper wmRtSalesDetailMapper;

    @Override
    public List<WmRtSalesDetail> selectWmRtSalesDetailList(WmRtSalesDetail entity) {
        return wmRtSalesDetailMapper.selectWmRtSalesDetailList(entity);
    }

    @Override
    public List<WmRtSalesDetail> selectWmRtSalesDetailAll() {
        return wmRtSalesDetailMapper.selectWmRtSalesDetailAll();
    }

    @Override
    public WmRtSalesDetail selectWmRtSalesDetailByDetailId(Long detailId) {
        return wmRtSalesDetailMapper.selectWmRtSalesDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int insertWmRtSalesDetail(WmRtSalesDetail entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtSalesDetailMapper.insertWmRtSalesDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmRtSalesDetail(WmRtSalesDetail entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtSalesDetailMapper.updateWmRtSalesDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtSalesDetailByDetailId(Long detailId) {
        return wmRtSalesDetailMapper.deleteWmRtSalesDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int deleteWmRtSalesDetailByDetailIds(Long[] detailIds) {
        return wmRtSalesDetailMapper.deleteWmRtSalesDetailByDetailIds(detailIds);
    }
}