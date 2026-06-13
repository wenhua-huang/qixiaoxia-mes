package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRtSales;
import com.ruoyi.system.mapper.mes.wm.WmRtSalesMapper;
import com.ruoyi.system.service.mes.wm.IWmRtSalesService;

@Service
public class WmRtSalesServiceImpl implements IWmRtSalesService
{
    @Autowired
    private WmRtSalesMapper wmRtSalesMapper;

    @Override
    public List<WmRtSales> selectWmRtSalesList(WmRtSales entity) {
        return wmRtSalesMapper.selectWmRtSalesList(entity);
    }

    @Override
    public List<WmRtSales> selectWmRtSalesAll() {
        return wmRtSalesMapper.selectWmRtSalesAll();
    }

    @Override
    public WmRtSales selectWmRtSalesByRtId(Long rtId) {
        return wmRtSalesMapper.selectWmRtSalesByRtId(rtId);
    }

    @Override
    @Transactional
    public int insertWmRtSales(WmRtSales entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtSalesMapper.insertWmRtSales(entity);
    }

    @Override
    @Transactional
    public int updateWmRtSales(WmRtSales entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtSalesMapper.updateWmRtSales(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtSalesByRtId(Long rtId) {
        return wmRtSalesMapper.deleteWmRtSalesByRtId(rtId);
    }

    @Override
    @Transactional
    public int deleteWmRtSalesByRtIds(Long[] rtIds) {
        return wmRtSalesMapper.deleteWmRtSalesByRtIds(rtIds);
    }
}