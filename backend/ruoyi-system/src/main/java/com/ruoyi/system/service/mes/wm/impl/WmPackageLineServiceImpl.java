package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmPackageLine;
import com.ruoyi.system.mapper.mes.wm.WmPackageLineMapper;
import com.ruoyi.system.service.mes.wm.IWmPackageLineService;

@Service
public class WmPackageLineServiceImpl implements IWmPackageLineService
{
    @Autowired
    private WmPackageLineMapper wmPackageLineMapper;

    @Override
    public List<WmPackageLine> selectWmPackageLineList(WmPackageLine entity) {
        return wmPackageLineMapper.selectWmPackageLineList(entity);
    }

    @Override
    public List<WmPackageLine> selectWmPackageLineAll() {
        return wmPackageLineMapper.selectWmPackageLineAll();
    }

    @Override
    public WmPackageLine selectWmPackageLineByLineId(Long lineId) {
        return wmPackageLineMapper.selectWmPackageLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int insertWmPackageLine(WmPackageLine entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmPackageLineMapper.insertWmPackageLine(entity);
    }

    @Override
    @Transactional
    public int updateWmPackageLine(WmPackageLine entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmPackageLineMapper.updateWmPackageLine(entity);
    }

    @Override
    @Transactional
    public int deleteWmPackageLineByLineId(Long lineId) {
        return wmPackageLineMapper.deleteWmPackageLineByLineId(lineId);
    }

    @Override
    @Transactional
    public int deleteWmPackageLineByLineIds(Long[] lineIds) {
        return wmPackageLineMapper.deleteWmPackageLineByLineIds(lineIds);
    }
}