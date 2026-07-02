package com.ruoyi.system.service.mes.pur.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

/**
 * 采购订单头Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class PurOrderServiceImpl implements IPurOrderService 
{
    @Autowired
    private PurOrderMapper purOrderMapper;

    @Autowired
    private IPurOrderLineService purOrderLineService;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    /**
     * 查询采购订单头
     * 
     * @param orderId 采购订单头主键
     * @return 采购订单头
     */
    @Override
    public PurOrder selectPurOrderByOrderId(Long orderId)
    {
        return purOrderMapper.selectPurOrderByOrderId(orderId);
    }

    /**
     * 查询采购订单头列表
     * 
     * @param purOrder 采购订单头
     * @return 采购订单头
     */
    @Override
    public List<PurOrder> selectPurOrderList(PurOrder purOrder)
    {
        return purOrderMapper.selectPurOrderList(purOrder);
    }

    /**
     * 新增采购订单头
     * 
     * @param purOrder 采购订单头
     * @return 结果
     */
    @Override
    @Transactional
    public int insertPurOrder(PurOrder purOrder)
    {
        // 订单编码自动生成
        if (StringUtils.isEmpty(purOrder.getOrderCode())) {
            purOrder.setOrderCode(autoCodeGenerator.genSerialCode("PUR_ORDER_CODE", ""));
        }
        purOrder.setCreateTime(DateUtils.getNowDate());
        purOrder.setCreateBy(SecurityUtils.getUsername());
        return purOrderMapper.insertPurOrder(purOrder);
    }

    /**
     * 修改采购订单头
     * 
     * @param purOrder 采购订单头
     * @return 结果
     */
    @Override
    public int updatePurOrder(PurOrder purOrder)
    {
        purOrder.setUpdateTime(DateUtils.getNowDate());
        purOrder.setUpdateBy(SecurityUtils.getUsername());
        return purOrderMapper.updatePurOrder(purOrder);
    }

    /**
     * 批量删除采购订单头（级联删除行）
     *
     * @param orderIds 需要删除的采购订单头主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePurOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds) {
            purOrderLineService.deletePurOrderLineByOrderId(orderId);
        }
        return purOrderMapper.deletePurOrderByOrderIds(orderIds);
    }

    /**
     * 删除采购订单头信息（级联删除行）
     *
     * @param orderId 采购订单头主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePurOrderByOrderId(Long orderId)
    {
        purOrderLineService.deletePurOrderLineByOrderId(orderId);
        return purOrderMapper.deletePurOrderByOrderId(orderId);
    }
}
