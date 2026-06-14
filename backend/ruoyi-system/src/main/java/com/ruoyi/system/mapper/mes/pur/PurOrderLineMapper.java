package com.ruoyi.system.mapper.mes.pur;

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
}
