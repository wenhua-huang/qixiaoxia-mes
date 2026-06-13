package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.wm.WmTransferDetail;
import com.ruoyi.system.mapper.mes.wm.WmTransferDetailMapper;
import com.ruoyi.system.service.mes.wm.IWmTransferDetailService;

@Service
public class WmTransferDetailServiceImpl implements IWmTransferDetailService
{
    @Autowired
    private WmTransferDetailMapper wmTransferDetailMapper;

    @Override
    public List<WmTransferDetail> selectWmTransferDetailList(WmTransferDetail entity) {
        return wmTransferDetailMapper.selectWmTransferDetailList(entity);
    }

    @Override
    public List<WmTransferDetail> selectWmTransferDetailAll() {
        return wmTransferDetailMapper.selectWmTransferDetailAll();
    }

    @Override
    public WmTransferDetail selectWmTransferDetailByDetailId(Long detailId) {
        return wmTransferDetailMapper.selectWmTransferDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int insertWmTransferDetail(WmTransferDetail entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmTransferDetailMapper.insertWmTransferDetail(entity);
    }

    @Override
    @Transactional
    public int updateWmTransferDetail(WmTransferDetail entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmTransferDetailMapper.updateWmTransferDetail(entity);
    }

    @Override
    @Transactional
    public int deleteWmTransferDetailByDetailId(Long detailId) {
        return wmTransferDetailMapper.deleteWmTransferDetailByDetailId(detailId);
    }

    @Override
    @Transactional
    public int deleteWmTransferDetailByDetailIds(Long[] detailIds) {
        return wmTransferDetailMapper.deleteWmTransferDetailByDetailIds(detailIds);
    }
}