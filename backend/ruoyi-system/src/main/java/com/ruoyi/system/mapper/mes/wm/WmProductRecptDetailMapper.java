package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductRecptDetail;

public interface WmProductRecptDetailMapper
{
    public List<WmProductRecptDetail> selectWmProductRecptDetailList(WmProductRecptDetail entity);
    public List<WmProductRecptDetail> selectWmProductRecptDetailAll();
    public WmProductRecptDetail selectWmProductRecptDetailByDetailId(Long detailId);
    public int insertWmProductRecptDetail(WmProductRecptDetail entity);
    public int updateWmProductRecptDetail(WmProductRecptDetail entity);
    public int deleteWmProductRecptDetailByDetailId(Long detailId);
    public int deleteWmProductRecptDetailByDetailIds(Long[] detailIds);
}
