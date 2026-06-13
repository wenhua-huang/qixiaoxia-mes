package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmTransfer;
import com.ruoyi.system.mapper.mes.wm.WmTransferMapper;
import com.ruoyi.system.service.mes.wm.IWmTransferService;

@Service
public class WmTransferServiceImpl implements IWmTransferService
{
    @Autowired
    private WmTransferMapper wmTransferMapper;

    @Override
    public List<WmTransfer> selectWmTransferList(WmTransfer entity) {
        return wmTransferMapper.selectWmTransferList(entity);
    }

    @Override
    public List<WmTransfer> selectWmTransferAll() {
        return wmTransferMapper.selectWmTransferAll();
    }

    @Override
    public WmTransfer selectWmTransferByTransferId(Long transferId) {
        return wmTransferMapper.selectWmTransferByTransferId(transferId);
    }

    @Override
    @Transactional
    public int insertWmTransfer(WmTransfer entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmTransferMapper.insertWmTransfer(entity);
    }

    @Override
    @Transactional
    public int updateWmTransfer(WmTransfer entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmTransferMapper.updateWmTransfer(entity);
    }

    @Override
    @Transactional
    public int deleteWmTransferByTransferId(Long transferId) {
        return wmTransferMapper.deleteWmTransferByTransferId(transferId);
    }

    @Override
    @Transactional
    public int deleteWmTransferByTransferIds(Long[] transferIds) {
        return wmTransferMapper.deleteWmTransferByTransferIds(transferIds);
    }
}