package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRtSalesLine;
import com.ruoyi.system.mapper.mes.wm.WmRtSalesLineMapper;
import com.ruoyi.system.service.mes.wm.IWmRtSalesLineService;

@Service
public class WmRtSalesLineServiceImpl implements IWmRtSalesLineService
{
    @Autowired
    private WmRtSalesLineMapper wmRtSalesLineMapper;

    @Override
    public List<WmRtSalesLine> selectWmRtSalesLineList(WmRtSalesLine entity) {
        return wmRtSalesLineMapper.selectWmRtSalesLineList(entity);
    }

    @Override
    public List<WmRtSalesLine> selectWmRtSalesLineAll() {
        return wmRtSalesLineMapper.selectWmRtSalesLineAll();
    }

    @Override
    public WmRtSalesLine selectWmRtSalesLineByLineId(Long lineId) {
        return wmRtSalesLineMapper.selectWmRtSalesLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmRtSalesLine(WmRtSalesLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtSalesLineMapper.insertWmRtSalesLine(entity);
    }

    @Override
    @Transactional
    public int updateWmRtSalesLine(WmRtSalesLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtSalesLineMapper.updateWmRtSalesLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtSalesLineByLineId(Long lineId) {
        return wmRtSalesLineMapper.deleteWmRtSalesLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmRtSalesLineByLineIds(Long[] lineIds) {
        return wmRtSalesLineMapper.deleteWmRtSalesLineByLineIds(lineIds);
    }
}