package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmMiscIssue;
import com.ruoyi.system.mapper.mes.wm.WmMiscIssueMapper;
import com.ruoyi.system.service.mes.wm.IWmMiscIssueService;

@Service
public class WmMiscIssueServiceImpl implements IWmMiscIssueService
{
    @Autowired
    private WmMiscIssueMapper wmMiscIssueMapper;

    @Override
    public List<WmMiscIssue> selectWmMiscIssueList(WmMiscIssue entity) {
        return wmMiscIssueMapper.selectWmMiscIssueList(entity);
    }

    @Override
    public List<WmMiscIssue> selectWmMiscIssueAll() {
        return wmMiscIssueMapper.selectWmMiscIssueAll();
    }

    @Override
    public WmMiscIssue selectWmMiscIssueByIssueId(Long issueId) {
        return wmMiscIssueMapper.selectWmMiscIssueByIssueId(issueId);
    }

    @Override
    @Transactional
    public int insertWmMiscIssue(WmMiscIssue entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmMiscIssueMapper.insertWmMiscIssue(entity);
    }

    @Override
    @Transactional
    public int updateWmMiscIssue(WmMiscIssue entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmMiscIssueMapper.updateWmMiscIssue(entity);
    }

    @Override
    @Transactional
    public int deleteWmMiscIssueByIssueId(Long issueId) {
        return wmMiscIssueMapper.deleteWmMiscIssueByIssueId(issueId);
    }

    @Override
    @Transactional
    public int deleteWmMiscIssueByIssueIds(Long[] issueIds) {
        return wmMiscIssueMapper.deleteWmMiscIssueByIssueIds(issueIds);
    }
}