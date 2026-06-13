package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;

public interface WmRollDetailMapper
{
    public List<WmRollDetail> selectWmRollDetailList(WmRollDetail entity);
    public List<WmRollDetail> selectWmRollDetailAll();
    public WmRollDetail selectWmRollDetailByRollId(Long rollId);
    public int insertWmRollDetail(WmRollDetail entity);
    public int updateWmRollDetail(WmRollDetail entity);
    public int deleteWmRollDetailByRollId(Long rollId);
    public int deleteWmRollDetailByRollIds(Long[] rollIds);
}