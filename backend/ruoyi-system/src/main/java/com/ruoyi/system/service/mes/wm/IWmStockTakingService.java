package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmStockTaking;

public interface IWmStockTakingService
{
    public List<WmStockTaking> selectWmStockTakingList(WmStockTaking entity);
    public List<WmStockTaking> selectWmStockTakingAll();
    public WmStockTaking selectWmStockTakingByTakingId(Long takingId);
    public int insertWmStockTaking(WmStockTaking entity);
    public int updateWmStockTaking(WmStockTaking entity);
    public int deleteWmStockTakingByTakingId(Long takingId);
    public int deleteWmStockTakingByTakingIds(Long[] takingIds);
}