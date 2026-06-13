package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;
import com.ruoyi.system.service.mes.wm.IWmProductSalesLineService;

@Service
public class WmProductSalesLineServiceImpl implements IWmProductSalesLineService
{
    @Autowired
    private WmProductSalesLineMapper wmProductSalesLineMapper;

    @Override
    public List<WmProductSalesLine> selectWmProductSalesLineList(WmProductSalesLine entity) {
        return wmProductSalesLineMapper.selectWmProductSalesLineList(entity);
    }

    @Override
    public List<WmProductSalesLine> selectWmProductSalesLineAll() {
        return wmProductSalesLineMapper.selectWmProductSalesLineAll();
    }

    @Override
    public WmProductSalesLine selectWmProductSalesLineByLineId(Long lineId) {
        return wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmProductSalesLine(WmProductSalesLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmProductSalesLineMapper.insertWmProductSalesLine(entity);
    }

    @Override
    @Transactional
    public int updateWmProductSalesLine(WmProductSalesLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmProductSalesLineMapper.updateWmProductSalesLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesLineByLineId(Long lineId) {
        return wmProductSalesLineMapper.deleteWmProductSalesLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesLineByLineIds(Long[] lineIds) {
        return wmProductSalesLineMapper.deleteWmProductSalesLineByLineIds(lineIds);
    }
}