package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.wm.WmIssueDetailMapper;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;
import com.ruoyi.system.service.mes.wm.IWmIssueDetailService;

/**
 * WmIssueDetailService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class WmIssueDetailServiceImpl implements IWmIssueDetailService
{
    @Autowired
    private WmIssueDetailMapper wmIssueDetailMapper;

    @Override
    public WmIssueDetail selectWmIssueDetailByDetailId(Long detailId) { return wmIssueDetailMapper.selectWmIssueDetailByDetailId(detailId); }

    @Override
    public List<WmIssueDetail> selectWmIssueDetailList(WmIssueDetail e) { return wmIssueDetailMapper.selectWmIssueDetailList(e); }

    @Override
    public List<WmIssueDetail> selectAll() { return wmIssueDetailMapper.selectWmIssueDetailList(new WmIssueDetail()); }

    @Override
    @Transactional
    public int insertWmIssueDetail(WmIssueDetail e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return wmIssueDetailMapper.insertWmIssueDetail(e);
    }

    @Override
    public int updateWmIssueDetail(WmIssueDetail e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueDetailMapper.updateWmIssueDetail(e);
    }

    @Override
    public int deleteWmIssueDetailByDetailIds(Long[] detailIds) { return wmIssueDetailMapper.deleteWmIssueDetailByDetailIds(detailIds); }

    @Override
    public int deleteWmIssueDetailByDetailId(Long detailId) { return wmIssueDetailMapper.deleteWmIssueDetailByDetailId(detailId); }
}
