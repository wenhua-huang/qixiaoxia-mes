package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMiscRecpt;

public interface WmMiscRecptMapper
{
    public List<WmMiscRecpt> selectWmMiscRecptList(WmMiscRecpt entity);
    public List<WmMiscRecpt> selectWmMiscRecptAll();
    public WmMiscRecpt selectWmMiscRecptByRecptId(Long recptId);
    public int insertWmMiscRecpt(WmMiscRecpt entity);
    public int updateWmMiscRecpt(WmMiscRecpt entity);
    public int deleteWmMiscRecptByRecptId(Long recptId);
    public int deleteWmMiscRecptByRecptIds(Long[] recptIds);
}