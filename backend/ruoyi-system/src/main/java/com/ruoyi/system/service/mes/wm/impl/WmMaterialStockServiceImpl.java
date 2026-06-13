package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.service.mes.wm.IWmMaterialStockService;

@Service
public class WmMaterialStockServiceImpl implements IWmMaterialStockService
{
    @Autowired
    private WmMaterialStockMapper wmMaterialStockMapper;

    @Override
    public List<WmMaterialStock> selectWmMaterialStockList(WmMaterialStock entity) {
        return wmMaterialStockMapper.selectWmMaterialStockList(entity);
    }

    @Override
    public List<WmMaterialStock> selectWmMaterialStockAll() {
        return wmMaterialStockMapper.selectWmMaterialStockAll();
    }

    @Override
    public WmMaterialStock selectWmMaterialStockByMaterialStockId(Long materialStockId) {
        return wmMaterialStockMapper.selectWmMaterialStockByMaterialStockId(materialStockId);
    }

    @Override
    @Transactional
    public int insertWmMaterialStock(WmMaterialStock entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmMaterialStockMapper.insertWmMaterialStock(entity);
    }

    @Override
    @Transactional
    public int updateWmMaterialStock(WmMaterialStock entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmMaterialStockMapper.updateWmMaterialStock(entity);
    }

    @Override
    @Transactional
    public int deleteWmMaterialStockByMaterialStockId(Long materialStockId) {
        return wmMaterialStockMapper.deleteWmMaterialStockByMaterialStockId(materialStockId);
    }

    @Override
    @Transactional
    public int deleteWmMaterialStockByMaterialStockIds(Long[] materialStockIds) {
        return wmMaterialStockMapper.deleteWmMaterialStockByMaterialStockIds(materialStockIds);
    }
}