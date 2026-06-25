package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;

/**
 * WmRtIssueLineService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmRtIssueLineService
{
    public WmRtIssueLine selectWmRtIssueLineByLineId(Long lineId);
    public List<WmRtIssueLine> selectWmRtIssueLineList(WmRtIssueLine e);
    public List<WmRtIssueLine> selectAll();
    public int insertWmRtIssueLine(WmRtIssueLine e);
    public int updateWmRtIssueLine(WmRtIssueLine e);
    public int deleteWmRtIssueLineByLineIds(Long[] lineIds);
    public int deleteWmRtIssueLineByLineId(Long lineId);
}
