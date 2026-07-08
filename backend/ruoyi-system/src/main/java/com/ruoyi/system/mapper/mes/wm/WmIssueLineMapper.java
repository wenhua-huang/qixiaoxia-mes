package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;

public interface WmIssueLineMapper {
    WmIssueLine selectWmIssueLineByLineId(Long lineId);
    List<WmIssueLine> selectWmIssueLineList(WmIssueLine e);
    int insertWmIssueLine(WmIssueLine e);
    int updateWmIssueLine(WmIssueLine e);
    int deleteWmIssueLineByLineId(Long lineId);
    int deleteWmIssueLineByLineIds(Long[] lineIds);
    int deleteWmIssueLineByIssueId(Long issueId);
}
