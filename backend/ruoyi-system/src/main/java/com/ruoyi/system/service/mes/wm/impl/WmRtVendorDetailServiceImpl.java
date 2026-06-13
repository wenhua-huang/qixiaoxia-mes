package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRtVendorDetail;
import com.ruoyi.system.mapper.mes.wm.WmRtVendorDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmRtVendorDetailService;

@Service
public class WmRtVendorDetailServiceImpl implements IWmRtVendorDetailService
{
    @Autowired
    private WmRtVendorDetailMapper wmRtVendorDetailMapper;

    @Override
    public List<WmRtVendorDetail> selectWmRtVendorDetailList(WmRtVendorDetail entity) {
        return wmRtVendorDetailMapper.selectWmRtVendorDetailList(entity);
    }

    @Override
    public List<WmRtVendorDetail> selectWmRtVendorDetailAll() {
        return wmRtVendorDetailMapper.selectWmRtVendorDetailAll();
    }

    @Override
    public WmRtVendorDetail selectWmRtVendorDetailByDetailId(Long detailId) {
        return wmRtVendorDetailMapper.selectWmRtVendorDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int insertWmRtVendorDetail(WmRtVendorDetail entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorDetailMapper.insertWmRtVendorDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmRtVendorDetail(WmRtVendorDetail entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorDetailMapper.updateWmRtVendorDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorDetailByDetailId(Long detailId) {
        return wmRtVendorDetailMapper.deleteWmRtVendorDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorDetailByDetailIds(Long[] detailIds) {
        return wmRtVendorDetailMapper.deleteWmRtVendorDetailByDetailIds(detailIds);
    }
}