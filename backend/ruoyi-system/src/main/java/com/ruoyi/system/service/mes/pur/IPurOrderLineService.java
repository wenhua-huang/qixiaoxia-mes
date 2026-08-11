package com.ruoyi.system.service.mes.pur;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;

/**
 * 采购订单行Service接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface IPurOrderLineService 
{
    /**
     * 查询采购订单行
     * 
     * @param lineId 采购订单行主键
     * @return 采购订单行
     */
    public PurOrderLine selectPurOrderLineByLineId(Long lineId);

    /**
     * 查询采购订单行列表
     * 
     * @param purOrderLine 采购订单行
     * @return 采购订单行集合
     */
    public List<PurOrderLine> selectPurOrderLineList(PurOrderLine purOrderLine);

    /**
     * 批量查询工单+物料集合的有效采购订单行（非取消/关闭），消除齐套看板逐物料 N+1。
     */
    public List<PurOrderLine> selectPendingByWorkorderAndItems(Long workorderId, Collection<Long> itemIds);

    /**
     * 新增采购订单行
     * 
     * @param purOrderLine 采购订单行
     * @return 结果
     */
    public int insertPurOrderLine(PurOrderLine purOrderLine);

    /**
     * 修改采购订单行
     * 
     * @param purOrderLine 采购订单行
     * @return 结果
     */
    public int updatePurOrderLine(PurOrderLine purOrderLine);

    /**
     * 批量删除采购订单行
     * 
     * @param lineIds 需要删除的采购订单行主键集合
     * @return 结果
     */
    public int deletePurOrderLineByLineIds(Long[] lineIds);

    /**
     * 删除采购订单行信息
     *
     * @param lineId 采购订单行主键
     * @return 结果
     */
    public int deletePurOrderLineByLineId(Long lineId);

    /**
     * 按订单ID删除所有行（级联删除）
     *
     * @param orderId 采购订单ID
     * @return 结果
     */
    public int deletePurOrderLineByOrderId(Long orderId);
}
