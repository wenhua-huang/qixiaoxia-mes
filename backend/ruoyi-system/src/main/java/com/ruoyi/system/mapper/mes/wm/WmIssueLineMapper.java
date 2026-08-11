package com.ruoyi.system.mapper.mes.wm;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;

public interface WmIssueLineMapper {
    WmIssueLine selectWmIssueLineByLineId(Long lineId);
    List<WmIssueLine> selectWmIssueLineList(WmIssueLine e);
    /** 按领料单 ID 批量查询明细（消除 N+1） */
    List<WmIssueLine> selectByIssueIds(Collection<Long> issueIds);
    int insertWmIssueLine(WmIssueLine e);
    int updateWmIssueLine(WmIssueLine e);
    int deleteWmIssueLineByLineId(Long lineId);
    int deleteWmIssueLineByLineIds(Long[] lineIds);
    int deleteWmIssueLineByIssueId(Long issueId);
}
