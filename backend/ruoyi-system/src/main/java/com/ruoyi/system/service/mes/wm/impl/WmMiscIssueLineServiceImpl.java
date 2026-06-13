package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmMiscIssueLine;
import com.ruoyi.system.mapper.mes.wm.WmMiscIssueLineMapper;
import com.ruoyi.system.service.mes.wm.IWmMiscIssueLineService;

@Service
public class WmMiscIssueLineServiceImpl implements IWmMiscIssueLineService
{
    @Autowired
    private WmMiscIssueLineMapper wmMiscIssueLineMapper;

    @Override
    public List<WmMiscIssueLine> selectWmMiscIssueLineList(WmMiscIssueLine entity) {
        return wmMiscIssueLineMapper.selectWmMiscIssueLineList(entity);
    }

    @Override
    public List<WmMiscIssueLine> selectWmMiscIssueLineAll() {
        return wmMiscIssueLineMapper.selectWmMiscIssueLineAll();
    }

    @Override
    public WmMiscIssueLine selectWmMiscIssueLineByLineId(Long lineId) {
        return wmMiscIssueLineMapper.selectWmMiscIssueLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmMiscIssueLine(WmMiscIssueLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmMiscIssueLineMapper.insertWmMiscIssueLine(entity);
    }

    @Override
    @Transactional
    public int updateWmMiscIssueLine(WmMiscIssueLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmMiscIssueLineMapper.updateWmMiscIssueLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmMiscIssueLineByLineId(Long lineId) {
        return wmMiscIssueLineMapper.deleteWmMiscIssueLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmMiscIssueLineByLineIds(Long[] lineIds) {
        return wmMiscIssueLineMapper.deleteWmMiscIssueLineByLineIds(lineIds);
    }
}