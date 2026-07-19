package com.ruoyi.system.service.mes.pur;

import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderDetailVO;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;

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
    public PurOrderVO selectPurOrderByOrderId(Long orderId);

    /**
     * 按订单编码查询采购订单头+行（移动端扫码/搜索收货一次拿全）
     *
     * @param orderCode 采购订单编码（精确匹配，factory_id 由拦截器注入）
     * @return 头+行封装；未命中返回 null
     */
    public PurOrderDetailVO selectPurOrderDetailByOrderCode(String orderCode);

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

    /**
     * 审批采购订单（DRAFT → APPROVED）
     *
     * @param orderId 采购订单ID
     * @return 结果
     */
    public int approvePurOrder(Long orderId);

    /**
     * 下达采购订单（APPROVED → ORDERED）
     *
     * @param orderId 采购订单ID
     * @return 结果
     */
    public int orderPurOrder(Long orderId);

    /**
     * 关闭采购订单（RECEIVED -> CLOSED 或 RECEIVING -> CLOSED 强制关闭）
     *
     * @param orderId 采购订单ID
     * @param closeReason 关闭原因（强制关闭时必填）
     * @return 结果
     */
    public int closePurOrder(Long orderId, String closeReason);

    /**
     * 取消采购订单（DRAFT/APPROVED/ORDERED -> CANCEL）
     * 校验：所有行已收货数量为0
     *
     * @param orderId 采购订单ID
     * @param cancelReason 取消原因
     * @return 结果
     */
    public int cancelPurOrder(Long orderId, String cancelReason);

    /**
     * 取消采购订单行（ORDERED/RECEIVING -> CANCEL）
     * 校验：已收货数量为0
     *
     * @param lineId 采购订单行ID
     * @param cancelReason 取消原因
     * @return 结果
     */
    public int cancelPurOrderLine(Long lineId, String cancelReason);

    /**
     * 终止收货采购订单行（RECEIVING -> CLOSED）
     * 校验：已收货数量 > 0 且 < 订购数量
     *
     * @param lineId 采购订单行ID
     * @param closeReason 终止原因
     * @return 结果
     */
    public int terminatePurOrderLine(Long lineId, String closeReason);

    /**
     * 联动检查：若订单所有行都进入终态，头自动推进（分三档）。
     * <pre>
     * 全 RECEIVED                 → 头 RECEIVED
     * 全 CANCEL 且所有行未收货   → 头 CANCEL（保留 cancelReason/cancelTime/cancelBy）
     * 其他混合终态（含 CLOSED / 有过实收 + CANCEL 等） → 头 CLOSED
     * </pre>
     * 若存在活跃行（非终态）则不推进。头已在终态时不重复推进。
     *
     * @param orderId 采购订单ID
     * @param reason  可选原因（用于命中 CLOSED/CANCEL 分档时的 close_reason/cancel_reason）
     */
    public void checkAndAutoCloseOrder(Long orderId, String reason);
}
