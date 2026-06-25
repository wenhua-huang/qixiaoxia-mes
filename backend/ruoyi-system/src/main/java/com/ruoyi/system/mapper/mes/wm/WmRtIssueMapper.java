package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;

public interface WmRtIssueMapper {
    WmRtIssue selectWmRtIssueByRtId(Long rtId);
    List<WmRtIssue> selectWmRtIssueList(WmRtIssue e);
    int insertWmRtIssue(WmRtIssue e);
    int updateWmRtIssue(WmRtIssue e);
    int deleteWmRtIssueByRtId(Long rtId);
    int deleteWmRtIssueByRtIds(Long[] rtIds);
}
