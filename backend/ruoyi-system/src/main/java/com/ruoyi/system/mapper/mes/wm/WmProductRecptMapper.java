package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;

public interface WmProductRecptMapper
{
    public List<WmProductRecpt> selectWmProductRecptList(WmProductRecpt entity);
    public List<WmProductRecpt> selectWmProductRecptAll();
    public WmProductRecpt selectWmProductRecptByRecptId(Long recptId);
    public int insertWmProductRecpt(WmProductRecpt entity);
    public int updateWmProductRecpt(WmProductRecpt entity);
    public int deleteWmProductRecptByRecptId(Long recptId);
    public int deleteWmProductRecptByRecptIds(Long[] recptIds);
}
