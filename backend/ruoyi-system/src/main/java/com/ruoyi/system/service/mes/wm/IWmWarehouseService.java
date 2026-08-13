package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;

public interface IWmWarehouseService
{
    public List<WmWarehouse> selectWmWarehouseList(WmWarehouse entity);
    public List<WmWarehouse> selectWmWarehouseAll();
    public WmWarehouse selectWmWarehouseByWarehouseId(Long warehouseId);
        public int insertWmWarehouse(WmWarehouse entity);    public int updateWmWarehouse(WmWarehouse entity);
    public int deleteWmWarehouseByWarehouseId(Long warehouseId);
    public int deleteWmWarehouseByWarehouseIds(Long[] warehouseIds);

    /** 查某工厂下某客户的专属客户仓（无则返回 null） */
    WmWarehouse findClientWarehouse(Long factoryId, Long clientId);

    /** 查某工厂下某供应商的专属供应商仓（无则返回 null） */
    WmWarehouse findVendorWarehouse(Long factoryId, Long vendorId);
}