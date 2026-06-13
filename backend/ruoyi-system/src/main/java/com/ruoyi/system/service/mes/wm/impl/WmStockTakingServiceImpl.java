package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmStockTaking;
import com.ruoyi.system.mapper.mes.wm.WmStockTakingMapper;
import com.ruoyi.system.service.mes.wm.IWmStockTakingService;

@Service
public class WmStockTakingServiceImpl implements IWmStockTakingService
{
    @Autowired
    private WmStockTakingMapper wmStockTakingMapper;

    @Override
    public List<WmStockTaking> selectWmStockTakingList(WmStockTaking entity) {
        return wmStockTakingMapper.selectWmStockTakingList(entity);
    }

    @Override
    public List<WmStockTaking> selectWmStockTakingAll() {
        return wmStockTakingMapper.selectWmStockTakingAll();
    }

    @Override
    public WmStockTaking selectWmStockTakingByTakingId(Long takingId) {
        return wmStockTakingMapper.selectWmStockTakingByTakingId(takingId);
    }

    @Override
    @Transactional
    public int insertWmStockTaking(WmStockTaking entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmStockTakingMapper.insertWmStockTaking(entity);
    }

    @Override
    @Transactional
    public int updateWmStockTaking(WmStockTaking entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmStockTakingMapper.updateWmStockTaking(entity);
    }

    @Override
    @Transactional
    public int deleteWmStockTakingByTakingId(Long takingId) {
        return wmStockTakingMapper.deleteWmStockTakingByTakingId(takingId);
    }

    @Override
    @Transactional
    public int deleteWmStockTakingByTakingIds(Long[] takingIds) {
        return wmStockTakingMapper.deleteWmStockTakingByTakingIds(takingIds);
    }
}