package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMiscRecpt;

public interface IWmMiscRecptService
{
    public List<WmMiscRecpt> selectWmMiscRecptList(WmMiscRecpt entity);
    public List<WmMiscRecpt> selectWmMiscRecptAll();
    public WmMiscRecpt selectWmMiscRecptByRecptId(Long recptId);
    public int insertWmMiscRecpt(WmMiscRecpt entity);
    public int updateWmMiscRecpt(WmMiscRecpt entity);
    public int deleteWmMiscRecptByRecptId(Long recptId);
    public int deleteWmMiscRecptByRecptIds(Long[] recptIds);
}