package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmStorageArea;

public interface IWmStorageAreaService
{
    public List<WmStorageArea> selectWmStorageAreaList(WmStorageArea entity);
    public List<WmStorageArea> selectWmStorageAreaAll();
    public WmStorageArea selectWmStorageAreaByAreaId(Long areaId);
    public int insertWmStorageArea(WmStorageArea entity);
    public int updateWmStorageArea(WmStorageArea entity);
    public int deleteWmStorageAreaByAreaId(Long areaId);
    public int deleteWmStorageAreaByAreaIds(Long[] areaIds);
}