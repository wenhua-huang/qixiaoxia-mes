package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmRtVendorLine;
import com.ruoyi.system.mapper.mes.wm.WmRtVendorLineMapper;
import com.ruoyi.system.service.mes.wm.IWmRtVendorLineService;

@Service
public class WmRtVendorLineServiceImpl implements IWmRtVendorLineService
{
    @Autowired
    private WmRtVendorLineMapper wmRtVendorLineMapper;

    @Override
    public List<WmRtVendorLine> selectWmRtVendorLineList(WmRtVendorLine entity) {
        return wmRtVendorLineMapper.selectWmRtVendorLineList(entity);
    }

    @Override
    public List<WmRtVendorLine> selectWmRtVendorLineAll() {
        return wmRtVendorLineMapper.selectWmRtVendorLineAll();
    }

    @Override
    public WmRtVendorLine selectWmRtVendorLineByLineId(Long lineId) {
        return wmRtVendorLineMapper.selectWmRtVendorLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmRtVendorLine(WmRtVendorLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorLineMapper.insertWmRtVendorLine(entity);
    }

    @Override
    @Transactional
    public int updateWmRtVendorLine(WmRtVendorLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorLineMapper.updateWmRtVendorLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorLineByLineId(Long lineId) {
        return wmRtVendorLineMapper.deleteWmRtVendorLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorLineByLineIds(Long[] lineIds) {
        return wmRtVendorLineMapper.deleteWmRtVendorLineByLineIds(lineIds);
    }
}