package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMiscRecptLine;

public interface WmMiscRecptLineMapper
{
    public List<WmMiscRecptLine> selectWmMiscRecptLineList(WmMiscRecptLine entity);
    public List<WmMiscRecptLine> selectWmMiscRecptLineAll();
    public WmMiscRecptLine selectWmMiscRecptLineByLineId(Long lineId);
    public int insertWmMiscRecptLine(WmMiscRecptLine entity);
    public int updateWmMiscRecptLine(WmMiscRecptLine entity);
    public int deleteWmMiscRecptLineByLineId(Long lineId);
    public int deleteWmMiscRecptLineByLineIds(Long[] lineIds);
}