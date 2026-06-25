package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;

/**
 * WmRtIssueService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmRtIssueService
{
    public WmRtIssue selectWmRtIssueByRtId(Long rtId);
    public List<WmRtIssue> selectWmRtIssueList(WmRtIssue e);
    public List<WmRtIssue> selectAll();
    public int insertWmRtIssue(WmRtIssue e);
    public int updateWmRtIssue(WmRtIssue e);
    public int deleteWmRtIssueByRtIds(Long[] rtIds);
    public int deleteWmRtIssueByRtId(Long rtId);

    /** 从领料单创建退料单：复制issue lines → rt lines */
    public Long createFromIssue(Long issueId);

    /** 执行退库：加库存 + 写追溯 + 状态改为POSTED */
    public int executeReturn(Long rtId);
}
