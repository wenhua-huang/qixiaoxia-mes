package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;

public interface IWmItemRecptService
{
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity);
    public List<WmItemRecpt> selectWmItemRecptAll();
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId);
    public int insertWmItemRecpt(WmItemRecpt entity);
    public int updateWmItemRecpt(WmItemRecpt entity);
    public int deleteWmItemRecptByRecptId(Long recptId);
    public int deleteWmItemRecptByRecptIds(Long[] recptIds);
}