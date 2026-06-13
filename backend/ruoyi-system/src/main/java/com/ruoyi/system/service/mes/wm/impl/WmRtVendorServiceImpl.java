package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;
import com.ruoyi.system.mapper.mes.wm.WmRtVendorMapper;
import com.ruoyi.system.service.mes.wm.IWmRtVendorService;

@Service
public class WmRtVendorServiceImpl implements IWmRtVendorService
{
    @Autowired
    private WmRtVendorMapper wmRtVendorMapper;

    @Override
    public List<WmRtVendor> selectWmRtVendorList(WmRtVendor entity) {
        return wmRtVendorMapper.selectWmRtVendorList(entity);
    }

    @Override
    public List<WmRtVendor> selectWmRtVendorAll() {
        return wmRtVendorMapper.selectWmRtVendorAll();
    }

    @Override
    public WmRtVendor selectWmRtVendorByRtId(Long rtId) {
        return wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
    }

    @Override
    @Transactional
    public int insertWmRtVendor(WmRtVendor entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorMapper.insertWmRtVendor(entity);
    }

    @Override
    @Transactional
    public int updateWmRtVendor(WmRtVendor entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorMapper.updateWmRtVendor(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorByRtId(Long rtId) {
        return wmRtVendorMapper.deleteWmRtVendorByRtId(rtId);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorByRtIds(Long[] rtIds) {
        return wmRtVendorMapper.deleteWmRtVendorByRtIds(rtIds);
    }
}