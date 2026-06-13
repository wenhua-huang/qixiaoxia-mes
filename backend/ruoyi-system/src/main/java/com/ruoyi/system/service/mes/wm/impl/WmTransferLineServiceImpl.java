package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmTransferLine;
import com.ruoyi.system.mapper.mes.wm.WmTransferLineMapper;
import com.ruoyi.system.service.mes.wm.IWmTransferLineService;

@Service
public class WmTransferLineServiceImpl implements IWmTransferLineService
{
    @Autowired
    private WmTransferLineMapper wmTransferLineMapper;

    @Override
    public List<WmTransferLine> selectWmTransferLineList(WmTransferLine entity) {
        return wmTransferLineMapper.selectWmTransferLineList(entity);
    }

    @Override
    public List<WmTransferLine> selectWmTransferLineAll() {
        return wmTransferLineMapper.selectWmTransferLineAll();
    }

    @Override
    public WmTransferLine selectWmTransferLineByLineId(Long lineId) {
        return wmTransferLineMapper.selectWmTransferLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmTransferLine(WmTransferLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmTransferLineMapper.insertWmTransferLine(entity);
    }

    @Override
    @Transactional
    public int updateWmTransferLine(WmTransferLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmTransferLineMapper.updateWmTransferLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmTransferLineByLineId(Long lineId) {
        return wmTransferLineMapper.deleteWmTransferLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmTransferLineByLineIds(Long[] lineIds) {
        return wmTransferLineMapper.deleteWmTransferLineByLineIds(lineIds);
    }
}