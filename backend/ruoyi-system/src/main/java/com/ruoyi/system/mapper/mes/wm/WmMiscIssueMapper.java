package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMiscIssue;

public interface WmMiscIssueMapper
{
    public List<WmMiscIssue> selectWmMiscIssueList(WmMiscIssue entity);
    public List<WmMiscIssue> selectWmMiscIssueAll();
    public WmMiscIssue selectWmMiscIssueByIssueId(Long issueId);
    public int insertWmMiscIssue(WmMiscIssue entity);
    public int updateWmMiscIssue(WmMiscIssue entity);
    public int deleteWmMiscIssueByIssueId(Long issueId);
    public int deleteWmMiscIssueByIssueIds(Long[] issueIds);
}