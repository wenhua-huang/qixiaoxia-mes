package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmPackage;
import com.ruoyi.system.mapper.mes.wm.WmPackageMapper;
import com.ruoyi.system.service.mes.wm.IWmPackageService;

@Service
public class WmPackageServiceImpl implements IWmPackageService
{
    @Autowired
    private WmPackageMapper wmPackageMapper;

    @Override
    public List<WmPackage> selectWmPackageList(WmPackage entity) {
        return wmPackageMapper.selectWmPackageList(entity);
    }

    @Override
    public List<WmPackage> selectWmPackageAll() {
        return wmPackageMapper.selectWmPackageAll();
    }

    @Override
    public WmPackage selectWmPackageByPackageId(Long packageId) {
        return wmPackageMapper.selectWmPackageByPackageId(packageId);
    }

    @Override
    @Transactional
    public int insertWmPackage(WmPackage entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmPackageMapper.insertWmPackage(entity);
    }

    @Override
    @Transactional
    public int updateWmPackage(WmPackage entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmPackageMapper.updateWmPackage(entity);
    }

    @Override
    @Transactional
    public int deleteWmPackageByPackageId(Long packageId) {
        return wmPackageMapper.deleteWmPackageByPackageId(packageId);
    }

    @Override
    @Transactional
    public int deleteWmPackageByPackageIds(Long[] packageIds) {
        return wmPackageMapper.deleteWmPackageByPackageIds(packageIds);
    }
}