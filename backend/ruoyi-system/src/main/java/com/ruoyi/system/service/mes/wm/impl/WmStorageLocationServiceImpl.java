package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmStorageLocation;
import com.ruoyi.system.mapper.mes.wm.WmStorageLocationMapper;
import com.ruoyi.system.service.mes.wm.IWmStorageLocationService;

@Service
public class WmStorageLocationServiceImpl implements IWmStorageLocationService
{
    @Autowired
    private WmStorageLocationMapper wmStorageLocationMapper;

    @Override
    public List<WmStorageLocation> selectWmStorageLocationList(WmStorageLocation entity) {
        return wmStorageLocationMapper.selectWmStorageLocationList(entity);
    }

    @Override
    public List<WmStorageLocation> selectWmStorageLocationAll() {
        return wmStorageLocationMapper.selectWmStorageLocationAll();
    }

    @Override
    public WmStorageLocation selectWmStorageLocationByLocationId(Long locationId) {
        return wmStorageLocationMapper.selectWmStorageLocationByLocationId(locationId);
    }

    @Override
    @Transactional
    public int insertWmStorageLocation(WmStorageLocation entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmStorageLocationMapper.insertWmStorageLocation(entity);
    }

    @Override
    @Transactional
    public int updateWmStorageLocation(WmStorageLocation entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmStorageLocationMapper.updateWmStorageLocation(entity);
    }

    @Override
    @Transactional
    public int deleteWmStorageLocationByLocationId(Long locationId) {
        return wmStorageLocationMapper.deleteWmStorageLocationByLocationId(locationId);
    }

    @Override
    @Transactional
    public int deleteWmStorageLocationByLocationIds(Long[] locationIds) {
        return wmStorageLocationMapper.deleteWmStorageLocationByLocationIds(locationIds);
    }
}