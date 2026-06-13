package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmStorageArea;
import com.ruoyi.system.mapper.mes.wm.WmStorageAreaMapper;
import com.ruoyi.system.service.mes.wm.IWmStorageAreaService;

@Service
public class WmStorageAreaServiceImpl implements IWmStorageAreaService
{
    @Autowired
    private WmStorageAreaMapper wmStorageAreaMapper;

    @Override
    public List<WmStorageArea> selectWmStorageAreaList(WmStorageArea entity) {
        return wmStorageAreaMapper.selectWmStorageAreaList(entity);
    }

    @Override
    public List<WmStorageArea> selectWmStorageAreaAll() {
        return wmStorageAreaMapper.selectWmStorageAreaAll();
    }

    @Override
    public WmStorageArea selectWmStorageAreaByAreaId(Long areaId) {
        return wmStorageAreaMapper.selectWmStorageAreaByAreaId(areaId);
    }

    @Override
    @Transactional
    public int insertWmStorageArea(WmStorageArea entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmStorageAreaMapper.insertWmStorageArea(entity);
    }

    @Override
    @Transactional
    public int updateWmStorageArea(WmStorageArea entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmStorageAreaMapper.updateWmStorageArea(entity);
    }

    @Override
    @Transactional
    public int deleteWmStorageAreaByAreaId(Long areaId) {
        return wmStorageAreaMapper.deleteWmStorageAreaByAreaId(areaId);
    }

    @Override
    @Transactional
    public int deleteWmStorageAreaByAreaIds(Long[] areaIds) {
        return wmStorageAreaMapper.deleteWmStorageAreaByAreaIds(areaIds);
    }
}