package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtSalesLine;

public interface IWmRtSalesLineService
{
    public List<WmRtSalesLine> selectWmRtSalesLineList(WmRtSalesLine entity);
    public List<WmRtSalesLine> selectWmRtSalesLineAll();
    public WmRtSalesLine selectWmRtSalesLineByLineId(Long lineId);
    public int insertWmRtSalesLine(WmRtSalesLine entity);
    public int updateWmRtSalesLine(WmRtSalesLine entity);
    public int deleteWmRtSalesLineByLineId(Long lineId);
    public int deleteWmRtSalesLineByLineIds(Long[] lineIds);
}