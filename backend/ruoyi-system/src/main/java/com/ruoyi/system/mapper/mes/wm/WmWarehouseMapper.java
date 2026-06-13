package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;

public interface WmWarehouseMapper
{
    public List<WmWarehouse> selectWmWarehouseList(WmWarehouse entity);
    public List<WmWarehouse> selectWmWarehouseAll();
    public WmWarehouse selectWmWarehouseByWarehouseId(Long warehouseId);
    public int insertWmWarehouse(WmWarehouse entity);
    public int updateWmWarehouse(WmWarehouse entity);
    public int deleteWmWarehouseByWarehouseId(Long warehouseId);
    public int deleteWmWarehouseByWarehouseIds(Long[] warehouseIds);
}