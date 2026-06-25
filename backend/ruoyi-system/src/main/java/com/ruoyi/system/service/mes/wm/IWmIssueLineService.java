package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;

/**
 * WmIssueLineService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmIssueLineService
{
    public WmIssueLine selectWmIssueLineByLineId(Long lineId);
    public List<WmIssueLine> selectWmIssueLineList(WmIssueLine e);
    public List<WmIssueLine> selectAll();
    public int insertWmIssueLine(WmIssueLine e);
    public int updateWmIssueLine(WmIssueLine e);
    public int deleteWmIssueLineByLineIds(Long[] lineIds);
    public int deleteWmIssueLineByLineId(Long lineId);
}
