package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;

public interface WmIssueHeaderMapper {
    WmIssueHeader selectWmIssueHeaderByIssueId(Long issueId);
    List<WmIssueHeader> selectWmIssueHeaderList(WmIssueHeader e);
    int insertWmIssueHeader(WmIssueHeader e);
    int updateWmIssueHeader(WmIssueHeader e);
    int deleteWmIssueHeaderByIssueId(Long issueId);
    int deleteWmIssueHeaderByIssueIds(Long[] issueIds);
}
