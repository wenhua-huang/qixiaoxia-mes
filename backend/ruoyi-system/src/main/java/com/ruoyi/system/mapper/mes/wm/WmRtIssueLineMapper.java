package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;

public interface WmRtIssueLineMapper {
    WmRtIssueLine selectWmRtIssueLineByLineId(Long lineId);
    List<WmRtIssueLine> selectWmRtIssueLineList(WmRtIssueLine e);
    int insertWmRtIssueLine(WmRtIssueLine e);
    int updateWmRtIssueLine(WmRtIssueLine e);
    int deleteWmRtIssueLineByLineId(Long lineId);
    int deleteWmRtIssueLineByLineIds(Long[] lineIds);
}
