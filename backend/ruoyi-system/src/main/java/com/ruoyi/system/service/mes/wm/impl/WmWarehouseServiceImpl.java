package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.enums.WmWarehouseConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.mapper.mes.wm.WmWarehouseMapper;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;

@Service
public class WmWarehouseServiceImpl implements IWmWarehouseService
{
    @Autowired
    private WmWarehouseMapper wmWarehouseMapper;

    @Override
    public List<WmWarehouse> selectWmWarehouseList(WmWarehouse entity) {
        return wmWarehouseMapper.selectWmWarehouseList(entity);
    }

    @Override
    public List<WmWarehouse> selectWmWarehouseAll() {
        return wmWarehouseMapper.selectWmWarehouseAll();
    }

    @Override
    public WmWarehouse selectWmWarehouseByWarehouseId(Long warehouseId) {
        return wmWarehouseMapper.selectWmWarehouseByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
    public int insertWmWarehouse(WmWarehouse entity) {
        normalizeWarehouseOwner(entity);
        entity.setCreateTime(DateUtils.getNowDate());
        return wmWarehouseMapper.insertWmWarehouse(entity);
    }    @Override
    @Transactional
    public int updateWmWarehouse(WmWarehouse entity) {
        normalizeWarehouseOwner(entity);
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmWarehouseMapper.updateWmWarehouse(entity);
    }

    @Override
    @Transactional
    public int deleteWmWarehouseByWarehouseId(Long warehouseId) {
        return wmWarehouseMapper.deleteWmWarehouseByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
    public int deleteWmWarehouseByWarehouseIds(Long[] warehouseIds) {
        return wmWarehouseMapper.deleteWmWarehouseByWarehouseIds(warehouseIds);
    }

    @Override
    public WmWarehouse findClientWarehouse(Long factoryId, Long clientId) {
        if (clientId == null) {
            return null;
        }
        WmWarehouse q = new WmWarehouse();
        q.setFactoryId(factoryId);
        q.setWarehouseType(WmWarehouseConstants.TYPE_CUSTOMER);
        q.setClientId(clientId);
        q.setEnableFlag("1");
        List<WmWarehouse> list = wmWarehouseMapper.selectWmWarehouseList(q);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    @Override
    public WmWarehouse findVendorWarehouse(Long factoryId, Long vendorId) {
        if (vendorId == null) {
            return null;
        }
        WmWarehouse q = new WmWarehouse();
        q.setFactoryId(factoryId);
        q.setWarehouseType(WmWarehouseConstants.TYPE_SUPPLIER);
        q.setVendorId(vendorId);
        q.setEnableFlag("1");
        List<WmWarehouse> list = wmWarehouseMapper.selectWmWarehouseList(q);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /** 校验并归一仓库归属：客户仓必填 clientId、供应商仓必填 vendorId、普通仓清空两者。 */
    private void normalizeWarehouseOwner(WmWarehouse w) {
        String t = w.getWarehouseType();
        if (WmWarehouseConstants.TYPE_CUSTOMER.equals(t)) {
            if (w.getClientId() == null) {
                throw new ServiceException("客户仓必须填写归属客户");
            }
            w.setVendorId(null);
        } else if (WmWarehouseConstants.TYPE_SUPPLIER.equals(t)) {
            if (w.getVendorId() == null) {
                throw new ServiceException("供应商仓必须填写归属供应商");
            }
            w.setClientId(null);
        } else {
            w.setClientId(null);
            w.setVendorId(null);
        }
    }
}