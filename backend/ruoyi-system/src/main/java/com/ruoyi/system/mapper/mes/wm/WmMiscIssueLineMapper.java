package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMiscIssueLine;

public interface WmMiscIssueLineMapper
{
    public List<WmMiscIssueLine> selectWmMiscIssueLineList(WmMiscIssueLine entity);
    public List<WmMiscIssueLine> selectWmMiscIssueLineAll();
    public WmMiscIssueLine selectWmMiscIssueLineByLineId(Long lineId);
    public int insertWmMiscIssueLine(WmMiscIssueLine entity);
    public int updateWmMiscIssueLine(WmMiscIssueLine entity);
    public int deleteWmMiscIssueLineByLineId(Long lineId);
    public int deleteWmMiscIssueLineByLineIds(Long[] lineIds);
}