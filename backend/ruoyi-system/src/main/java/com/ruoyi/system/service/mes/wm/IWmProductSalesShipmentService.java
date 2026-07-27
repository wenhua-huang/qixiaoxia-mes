package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;

/**
 * 销售出库-发运单 Service（多次发运 + 装箱关联 + 签收回单）
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public interface IWmProductSalesShipmentService
{
    /** 列表 */
    public List<WmProductSalesShipment> selectWmProductSalesShipmentList(WmProductSalesShipment entity);

    /** 全量 */
    public List<WmProductSalesShipment> selectWmProductSalesShipmentAll();

    /** 单条详情（聚合关联箱） */
    public WmProductSalesShipment selectWmProductSalesShipmentByShipmentId(Long shipmentId);

    /** 按出库单查全部发运记录 */
    public List<WmProductSalesShipment> selectShipmentsBySalesId(Long salesId);

    /**
     * 新增发运（核心）：SHIPPING → IN_TRANSIT
     * - 校验出库单可发运（已过账、发运子状态未完成）
     * - 勾选关联装箱（boxes），回写 box.shipment_id + status=SHIPPED
     * - 累加头表 shipped_quantity，推导 ship_status
     * - Redis 锁 wm:salesout:lock:{salesId} + TransactionTemplate
     */
    public int createShipment(WmProductSalesShipment entity);

    /** 修改发运（仅 SHIPPING 可改） */
    public int updateWmProductSalesShipment(WmProductSalesShipment entity);

    /** 删除发运（仅 SHIPPING 可删，回滚关联箱状态 + 头表 shipped_quantity） */
    public int deleteWmProductSalesShipmentByShipmentId(Long shipmentId);

    /** 批量删除 */
    public int deleteWmProductSalesShipmentByShipmentIds(Long[] shipmentIds);

    /** 签收：IN_TRANSIT → RECEIVED，写签收时间/人/备注/回单附件 */
    public int receive(Long shipmentId, WmProductSalesShipment info);

    /** 取消发运（仅 SHIPPING 可取消；IN_TRANSIT 之后不可取消，需走销售退货） */
    public int cancel(Long shipmentId);
}
