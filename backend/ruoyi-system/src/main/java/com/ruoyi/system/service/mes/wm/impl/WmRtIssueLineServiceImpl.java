package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.wm.WmRtIssueLineMapper;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.service.mes.wm.IWmRtIssueLineService;

/**
 * WmRtIssueLineService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class WmRtIssueLineServiceImpl implements IWmRtIssueLineService
{
    @Autowired
    private WmRtIssueLineMapper wmRtIssueLineMapper;

    @Override
    public WmRtIssueLine selectWmRtIssueLineByLineId(Long lineId) { return wmRtIssueLineMapper.selectWmRtIssueLineByLineId(lineId); }

    @Override
    public List<WmRtIssueLine> selectWmRtIssueLineList(WmRtIssueLine e) { return wmRtIssueLineMapper.selectWmRtIssueLineList(e); }

    @Override
    public List<WmRtIssueLine> selectAll() { return wmRtIssueLineMapper.selectWmRtIssueLineList(new WmRtIssueLine()); }

    @Override
    @Transactional
    public int insertWmRtIssueLine(WmRtIssueLine e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return wmRtIssueLineMapper.insertWmRtIssueLine(e);
    }

    @Override
    public int updateWmRtIssueLine(WmRtIssueLine e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmRtIssueLineMapper.updateWmRtIssueLine(e);
    }

    @Override
    public int deleteWmRtIssueLineByLineIds(Long[] lineIds) { return wmRtIssueLineMapper.deleteWmRtIssueLineByLineIds(lineIds); }

    @Override
    public int deleteWmRtIssueLineByLineId(Long lineId) { return wmRtIssueLineMapper.deleteWmRtIssueLineByLineId(lineId); }
}
