package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmItemRecptDetail;

public interface WmItemRecptDetailMapper
{
    public List<WmItemRecptDetail> selectWmItemRecptDetailList(WmItemRecptDetail entity);
    public List<WmItemRecptDetail> selectWmItemRecptDetailAll();
    public WmItemRecptDetail selectWmItemRecptDetailByDetailId(Long detailId);
    public int insertWmItemRecptDetail(WmItemRecptDetail entity);
    public int updateWmItemRecptDetail(WmItemRecptDetail entity);
    public int deleteWmItemRecptDetailByDetailId(Long detailId);
    public int deleteWmItemRecptDetailByDetailIds(Long[] detailIds);
}