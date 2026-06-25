package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.wm.WmIssueLineMapper;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.service.mes.wm.IWmIssueLineService;

/**
 * WmIssueLineService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class WmIssueLineServiceImpl implements IWmIssueLineService
{
    @Autowired
    private WmIssueLineMapper wmIssueLineMapper;

    @Override
    public WmIssueLine selectWmIssueLineByLineId(Long lineId) { return wmIssueLineMapper.selectWmIssueLineByLineId(lineId); }

    @Override
    public List<WmIssueLine> selectWmIssueLineList(WmIssueLine e) { return wmIssueLineMapper.selectWmIssueLineList(e); }

    @Override
    public List<WmIssueLine> selectAll() { return wmIssueLineMapper.selectWmIssueLineList(new WmIssueLine()); }

    @Override
    @Transactional
    public int insertWmIssueLine(WmIssueLine e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return wmIssueLineMapper.insertWmIssueLine(e);
    }

    @Override
    public int updateWmIssueLine(WmIssueLine e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueLineMapper.updateWmIssueLine(e);
    }

    @Override
    public int deleteWmIssueLineByLineIds(Long[] lineIds) { return wmIssueLineMapper.deleteWmIssueLineByLineIds(lineIds); }

    @Override
    public int deleteWmIssueLineByLineId(Long lineId) { return wmIssueLineMapper.deleteWmIssueLineByLineId(lineId); }
}
