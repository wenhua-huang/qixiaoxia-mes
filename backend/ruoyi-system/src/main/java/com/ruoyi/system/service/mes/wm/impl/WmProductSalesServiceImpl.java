package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.service.mes.wm.IWmProductSalesService;

@Service
public class WmProductSalesServiceImpl implements IWmProductSalesService
{
    @Autowired
    private WmProductSalesMapper wmProductSalesMapper;

    @Override
    public List<WmProductSales> selectWmProductSalesList(WmProductSales entity) {
        return wmProductSalesMapper.selectWmProductSalesList(entity);
    }

    @Override
    public List<WmProductSales> selectWmProductSalesAll() {
        return wmProductSalesMapper.selectWmProductSalesAll();
    }

    @Override
    public WmProductSales selectWmProductSalesBySalesId(Long salesId) {
        return wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
    }

    @Override
    @Transactional
    public int insertWmProductSales(WmProductSales entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmProductSalesMapper.insertWmProductSales(entity);
    }

    @Override
    @Transactional
    public int updateWmProductSales(WmProductSales entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmProductSalesMapper.updateWmProductSales(entity);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesBySalesId(Long salesId) {
        return wmProductSalesMapper.deleteWmProductSalesBySalesId(salesId);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesBySalesIds(Long[] salesIds) {
        return wmProductSalesMapper.deleteWmProductSalesBySalesIds(salesIds);
    }
}