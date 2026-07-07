package com.ruoyi.system.mapper.mes.pur;

import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;

/**
 * 采购订单头Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface PurOrderMapper 
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
    public List<PurOrderVO> selectPurOrderList(PurOrder purOrder);

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
     * 删除采购订单头
     * 
     * @param orderId 采购订单头主键
     * @return 结果
     */
    public int deletePurOrderByOrderId(Long orderId);

    /**
     * 批量删除采购订单头
     * 
     * @param orderIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePurOrderByOrderIds(Long[] orderIds);
}
