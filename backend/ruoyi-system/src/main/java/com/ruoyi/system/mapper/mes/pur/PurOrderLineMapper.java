package com.ruoyi.system.mapper.mes.pur;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;

/**
 * 采购订单行Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface PurOrderLineMapper 
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
     * 批量查询指定工单+物料集合的有效采购订单行（JOIN 采购单头过滤工单与非终态，消除逐物料 N+1）。
     * @param workorderId 工单ID
     * @param itemIds 物料ID集合
     * @return 采购订单行列表（含 orderId/itemId）
     */
    public List<PurOrderLine> selectPendingByWorkorderAndItems(
            @org.apache.ibatis.annotations.Param("workorderId") Long workorderId,
            @org.apache.ibatis.annotations.Param("itemIds") Collection<Long> itemIds);

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
     * 删除采购订单行
     * 
     * @param lineId 采购订单行主键
     * @return 结果
     */
    public int deletePurOrderLineByLineId(Long lineId);

    /**
     * 批量删除采购订单行
     *
     * @param lineIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePurOrderLineByLineIds(Long[] lineIds);

    /**
     * 按订单ID删除所有行（级联删除）
     *
     * @param orderId 采购订单ID
     * @return 结果
     */
    public int deletePurOrderLineByOrderId(Long orderId);

    /**
     * 原子递增已收货数量（并发安全）
     *
     * @param lineId 采购订单行ID
     * @param delta 增量
     * @return 结果
     */
    public int addQuantityReceived(@org.apache.ibatis.annotations.Param("lineId") Long lineId,
                                   @org.apache.ibatis.annotations.Param("delta") java.math.BigDecimal delta);

    /**
     * 原子递增已退货数量（并发安全）
     *
     * @param lineId 采购订单行ID
     * @param delta 增量
     * @return 结果
     */
    public int addQuantityReturned(@org.apache.ibatis.annotations.Param("lineId") Long lineId,
                                   @org.apache.ibatis.annotations.Param("delta") java.math.BigDecimal delta);
}
