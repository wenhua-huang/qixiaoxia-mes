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
    /** 按退料单头 rtId 删除全部明细（用于编辑态全量替换明细） */
    int deleteWmRtIssueLineByRtId(Long rtId);
}
