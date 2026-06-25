package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorderChange;

/**
 * 工单变更记录Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProWorkorderChangeService
{
    /**
     * 查询工单变更记录
     *
     * @param changeId 工单变更记录主键
     * @return 工单变更记录
     */
    public ProWorkorderChange selectProWorkorderChangeByChangeId(Long changeId);

    /**
     * 查询工单变更记录列表
     *
     * @param proWorkorderChange 工单变更记录
     * @return 工单变更记录集合
     */
    public List<ProWorkorderChange> selectProWorkorderChangeList(ProWorkorderChange proWorkorderChange);

    /**
     * 根据工单ID查询变更记录列表
     *
     * @param workorderId 工单ID
     * @return 工单变更记录集合
     */
    public List<ProWorkorderChange> selectProWorkorderChangeByWorkorderId(Long workorderId);

    /**
     * 审批变更记录
     *
     * @param changeId 变更记录ID
     * @return 结果
     */
    public int approve(Long changeId);

    /**
     * 新增工单变更记录
     *
     * @param proWorkorderChange 工单变更记录
     * @return 结果
     */
    public int insertProWorkorderChange(ProWorkorderChange proWorkorderChange);

    /**
     * 修改工单变更记录
     *
     * @param proWorkorderChange 工单变更记录
     * @return 结果
     */
    public int updateProWorkorderChange(ProWorkorderChange proWorkorderChange);

    /**
     * 批量删除工单变更记录
     *
     * @param changeIds 需要删除的工单变更记录主键集合
     * @return 结果
     */
    public int deleteProWorkorderChangeByChangeIds(Long[] changeIds);

    /**
     * 删除工单变更记录信息
     *
     * @param changeId 工单变更记录主键
     * @return 结果
     */
    public int deleteProWorkorderChangeByChangeId(Long changeId);
}
