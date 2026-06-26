package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.mapper.mes.wm.WmWarehouseMapper;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;

@Service
public class WmWarehouseServiceImpl implements IWmWarehouseService
{
    @Autowired
    private WmWarehouseMapper wmWarehouseMapper;

    @Override
    public List<WmWarehouse> selectWmWarehouseList(WmWarehouse entity) {
        return wmWarehouseMapper.selectWmWarehouseList(entity);
    }

    @Override
    public List<WmWarehouse> selectWmWarehouseAll() {
        return wmWarehouseMapper.selectWmWarehouseAll();
    }

    @Override
    public WmWarehouse selectWmWarehouseByWarehouseId(Long warehouseId) {
        return wmWarehouseMapper.selectWmWarehouseByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
    public int insertWmWarehouse(WmWarehouse entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmWarehouseMapper.insertWmWarehouse(entity);
    }    @Override
    @Transactional
    public int updateWmWarehouse(WmWarehouse entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmWarehouseMapper.updateWmWarehouse(entity);
    }

    @Override
    @Transactional
    public int deleteWmWarehouseByWarehouseId(Long warehouseId) {
        return wmWarehouseMapper.deleteWmWarehouseByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
    public int deleteWmWarehouseByWarehouseIds(Long[] warehouseIds) {
        return wmWarehouseMapper.deleteWmWarehouseByWarehouseIds(warehouseIds);
    }
}