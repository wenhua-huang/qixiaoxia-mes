package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmTransferLine;

public interface IWmTransferLineService
{
    public List<WmTransferLine> selectWmTransferLineList(WmTransferLine entity);
    public List<WmTransferLine> selectWmTransferLineAll();
    public WmTransferLine selectWmTransferLineByLineId(Long lineId);
    public int insertWmTransferLine(WmTransferLine entity);
    public int updateWmTransferLine(WmTransferLine entity);
    public int deleteWmTransferLineByLineId(Long lineId);
    public int deleteWmTransferLineByLineIds(Long[] lineIds);
}