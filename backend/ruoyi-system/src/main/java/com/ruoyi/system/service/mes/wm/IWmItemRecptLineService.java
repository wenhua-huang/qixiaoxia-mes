package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;

public interface IWmItemRecptLineService
{
    public List<WmItemRecptLine> selectWmItemRecptLineList(WmItemRecptLine entity);
    public List<WmItemRecptLine> selectWmItemRecptLineAll();
    public WmItemRecptLine selectWmItemRecptLineByLineId(Long lineId);
    public int insertWmItemRecptLine(WmItemRecptLine entity);
    public int updateWmItemRecptLine(WmItemRecptLine entity);
    public int deleteWmItemRecptLineByLineId(Long lineId);
    public int deleteWmItemRecptLineByLineIds(Long[] lineIds);
    /** 按入库单头ID批量删除所有行（用于编辑草稿时全量重建行明细） */
    public int deleteWmItemRecptLineByRecptId(Long recptId);
}