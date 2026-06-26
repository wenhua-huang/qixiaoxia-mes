package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmBarcode;
import com.ruoyi.system.mapper.mes.wm.WmBarcodeMapper;
import com.ruoyi.system.service.mes.wm.IWmBarcodeService;

@Service
public class WmBarcodeServiceImpl implements IWmBarcodeService
{
    @Autowired
    private WmBarcodeMapper wmBarcodeMapper;

    @Override
    public List<WmBarcode> selectWmBarcodeList(WmBarcode entity) {
        return wmBarcodeMapper.selectWmBarcodeList(entity);
    }

    @Override
    public List<WmBarcode> selectWmBarcodeAll() {
        return wmBarcodeMapper.selectWmBarcodeAll();
    }

    @Override
    public WmBarcode selectWmBarcodeByBarcodeId(Long barcodeId) {
        return wmBarcodeMapper.selectWmBarcodeByBarcodeId(barcodeId);
    }

    @Override
    @Transactional
    public int insertWmBarcode(WmBarcode entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmBarcodeMapper.insertWmBarcode(entity);
    }    @Override
    @Transactional
    public int updateWmBarcode(WmBarcode entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmBarcodeMapper.updateWmBarcode(entity);
    }

    @Override
    @Transactional
    public int deleteWmBarcodeByBarcodeId(Long barcodeId) {
        return wmBarcodeMapper.deleteWmBarcodeByBarcodeId(barcodeId);
    }

    @Override
    @Transactional
    public int deleteWmBarcodeByBarcodeIds(Long[] barcodeIds) {
        return wmBarcodeMapper.deleteWmBarcodeByBarcodeIds(barcodeIds);
    }
}