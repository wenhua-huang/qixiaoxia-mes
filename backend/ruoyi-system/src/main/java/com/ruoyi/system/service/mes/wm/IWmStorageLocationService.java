package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmStorageLocation;

public interface IWmStorageLocationService
{
    public List<WmStorageLocation> selectWmStorageLocationList(WmStorageLocation entity);
    public List<WmStorageLocation> selectWmStorageLocationAll();
    public WmStorageLocation selectWmStorageLocationByLocationId(Long locationId);
        public int insertWmStorageLocation(WmStorageLocation entity);    public int updateWmStorageLocation(WmStorageLocation entity);
    public int deleteWmStorageLocationByLocationId(Long locationId);
    public int deleteWmStorageLocationByLocationIds(Long[] locationIds);
}