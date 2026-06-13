package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;

public interface IWmProductSalesLineService
{
    public List<WmProductSalesLine> selectWmProductSalesLineList(WmProductSalesLine entity);
    public List<WmProductSalesLine> selectWmProductSalesLineAll();
    public WmProductSalesLine selectWmProductSalesLineByLineId(Long lineId);
    public int insertWmProductSalesLine(WmProductSalesLine entity);
    public int updateWmProductSalesLine(WmProductSalesLine entity);
    public int deleteWmProductSalesLineByLineId(Long lineId);
    public int deleteWmProductSalesLineByLineIds(Long[] lineIds);
}