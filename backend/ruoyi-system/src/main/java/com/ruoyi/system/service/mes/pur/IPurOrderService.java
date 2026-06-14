package com.ruoyi.system.service.mes.pur;

import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrder;

/**
 * 采购订单头Service接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface IPurOrderService 
{
    /**
     * 查询采购订单头
     * 
     * @param orderId 采购订单头主键
     * @return 采购订单头
     */
    public PurOrder selectPurOrderByOrderId(Long orderId);

    /**
     * 查询采购订单头列表
     * 
     * @param purOrder 采购订单头
     * @return 采购订单头集合
     */
    public List<PurOrder> selectPurOrderList(PurOrder purOrder);

    /**
     * 新增采购订单头
     * 
     * @param purOrder 采购订单头
     * @return 结果
     */
    public int insertPurOrder(PurOrder purOrder);

    /**
     * 修改采购订单头
     * 
     * @param purOrder 采购订单头
     * @return 结果
     */
    public int updatePurOrder(PurOrder purOrder);

    /**
     * 批量删除采购订单头
     * 
     * @param orderIds 需要删除的采购订单头主键集合
     * @return 结果
     */
    public int deletePurOrderByOrderIds(Long[] orderIds);

    /**
     * 删除采购订单头信息
     * 
     * @param orderId 采购订单头主键
     * @return 结果
     */
    public int deletePurOrderByOrderId(Long orderId);
}
