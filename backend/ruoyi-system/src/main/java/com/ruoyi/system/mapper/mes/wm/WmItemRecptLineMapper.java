package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;

public interface WmItemRecptLineMapper
{
    public List<WmItemRecptLine> selectWmItemRecptLineList(WmItemRecptLine entity);
    public List<WmItemRecptLine> selectWmItemRecptLineAll();
    public WmItemRecptLine selectWmItemRecptLineByLineId(Long lineId);
    public int insertWmItemRecptLine(WmItemRecptLine entity);
    public int updateWmItemRecptLine(WmItemRecptLine entity);
    public int deleteWmItemRecptLineByLineId(Long lineId);
    public int deleteWmItemRecptLineByLineIds(Long[] lineIds);
}