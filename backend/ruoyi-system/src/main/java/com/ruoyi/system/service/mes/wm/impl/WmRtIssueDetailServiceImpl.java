package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.wm.WmRtIssueDetailMapper;
import com.ruoyi.system.domain.mes.wm.WmRtIssueDetail;
import com.ruoyi.system.service.mes.wm.IWmRtIssueDetailService;

/**
 * WmRtIssueDetailService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class WmRtIssueDetailServiceImpl implements IWmRtIssueDetailService
{
    @Autowired
    private WmRtIssueDetailMapper wmRtIssueDetailMapper;

    @Override
    public WmRtIssueDetail selectWmRtIssueDetailByDetailId(Long detailId) { return wmRtIssueDetailMapper.selectWmRtIssueDetailByDetailId(detailId); }

    @Override
    public List<WmRtIssueDetail> selectWmRtIssueDetailList(WmRtIssueDetail e) { return wmRtIssueDetailMapper.selectWmRtIssueDetailList(e); }

    @Override
    public List<WmRtIssueDetail> selectAll() { return wmRtIssueDetailMapper.selectWmRtIssueDetailList(new WmRtIssueDetail()); }

    @Override
    @Transactional
    public int insertWmRtIssueDetail(WmRtIssueDetail e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return wmRtIssueDetailMapper.insertWmRtIssueDetail(e);
    }

    @Override
    public int updateWmRtIssueDetail(WmRtIssueDetail e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmRtIssueDetailMapper.updateWmRtIssueDetail(e);
    }

    @Override
    public int deleteWmRtIssueDetailByDetailIds(Long[] detailIds) { return wmRtIssueDetailMapper.deleteWmRtIssueDetailByDetailIds(detailIds); }

    @Override
    public int deleteWmRtIssueDetailByDetailId(Long detailId) { return wmRtIssueDetailMapper.deleteWmRtIssueDetailByDetailId(detailId); }
}
