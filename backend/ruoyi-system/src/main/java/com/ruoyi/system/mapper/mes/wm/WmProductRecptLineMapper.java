package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;

public interface WmProductRecptLineMapper
{
    public List<WmProductRecptLine> selectWmProductRecptLineList(WmProductRecptLine entity);
    public List<WmProductRecptLine> selectWmProductRecptLineAll();
    public WmProductRecptLine selectWmProductRecptLineByLineId(Long lineId);
    public int insertWmProductRecptLine(WmProductRecptLine entity);
    public int updateWmProductRecptLine(WmProductRecptLine entity);
    public int deleteWmProductRecptLineByLineId(Long lineId);
    public int deleteWmProductRecptLineByLineIds(Long[] lineIds);
}
