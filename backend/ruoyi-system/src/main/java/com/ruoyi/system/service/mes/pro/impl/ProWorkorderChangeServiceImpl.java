package com.ruoyi.system.service.mes.pro.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderChangeMapper;
import com.ruoyi.system.domain.mes.pro.ProWorkorderChange;
import com.ruoyi.system.service.mes.pro.IProWorkorderChangeService;

/**
 * 工单变更记录Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProWorkorderChangeServiceImpl implements IProWorkorderChangeService
{
    @Autowired
    private ProWorkorderChangeMapper qxxProWorkorderChangeMapper;

    /**
     * 查询工单变更记录
     *
     * @param changeId 工单变更记录主键
     * @return 工单变更记录
     */
    @Override
    public ProWorkorderChange selectProWorkorderChangeByChangeId(Long changeId)
    {
        return qxxProWorkorderChangeMapper.selectProWorkorderChangeByChangeId(changeId);
    }

    /**
     * 查询工单变更记录列表
     *
     * @param proWorkorderChange 工单变更记录
     * @return 工单变更记录
     */
    @Override
    public List<ProWorkorderChange> selectProWorkorderChangeList(ProWorkorderChange proWorkorderChange)
    {
        return qxxProWorkorderChangeMapper.selectProWorkorderChangeList(proWorkorderChange);
    }

    /**
     * 根据工单ID查询变更记录列表
     *
     * @param workorderId 工单ID
     * @return 工单变更记录集合
     */
    @Override
    public List<ProWorkorderChange> selectProWorkorderChangeByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderChangeMapper.selectProWorkorderChangeByWorkorderId(workorderId);
    }

    /**
     * 审批变更记录
     *
     * @param changeId 变更记录ID
     * @return 结果
     */
    @Override
    @Transactional
    public int approve(Long changeId)
    {
        ProWorkorderChange change = qxxProWorkorderChangeMapper.selectProWorkorderChangeByChangeId(changeId);
        change.setApprover(SecurityUtils.getUsername());
        change.setApproveTime(new Date());
        change.setStatus("APPROVED");
        change.setUpdateTime(DateUtils.getNowDate());
        change.setUpdateBy(SecurityUtils.getUsername());
        return qxxProWorkorderChangeMapper.updateProWorkorderChange(change);
    }

    /**
     * 新增工单变更记录
     *
     * @param proWorkorderChange 工单变更记录
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProWorkorderChange(ProWorkorderChange proWorkorderChange)
    {
        proWorkorderChange.setCreateTime(DateUtils.getNowDate());
        proWorkorderChange.setCreateBy(SecurityUtils.getUsername());
        if (proWorkorderChange.getStatus() == null) proWorkorderChange.setStatus("PENDING");
        return qxxProWorkorderChangeMapper.insertProWorkorderChange(proWorkorderChange);
    }

    /**
     * 修改工单变更记录
     *
     * @param proWorkorderChange 工单变更记录
     * @return 结果
     */
    @Override
    public int updateProWorkorderChange(ProWorkorderChange proWorkorderChange)
    {
        proWorkorderChange.setUpdateTime(DateUtils.getNowDate());
        proWorkorderChange.setUpdateBy(SecurityUtils.getUsername());
        return qxxProWorkorderChangeMapper.updateProWorkorderChange(proWorkorderChange);
    }

    /**
     * 批量删除工单变更记录
     *
     * @param changeIds 需要删除的工单变更记录主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderChangeByChangeIds(Long[] changeIds)
    {
        return qxxProWorkorderChangeMapper.deleteProWorkorderChangeByChangeIds(changeIds);
    }

    /**
     * 删除工单变更记录信息
     *
     * @param changeId 工单变更记录主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderChangeByChangeId(Long changeId)
    {
        return qxxProWorkorderChangeMapper.deleteProWorkorderChangeByChangeId(changeId);
    }
}
