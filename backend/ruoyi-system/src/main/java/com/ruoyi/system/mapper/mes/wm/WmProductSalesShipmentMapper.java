package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;

/**
 * 销售出库-发运单 Mapper
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public interface WmProductSalesShipmentMapper
{
    /** 列表查询 */
    public List<WmProductSalesShipment> selectWmProductSalesShipmentList(WmProductSalesShipment entity);

    /** 全量 */
    public List<WmProductSalesShipment> selectWmProductSalesShipmentAll();

    /** 单条 */
    public WmProductSalesShipment selectWmProductSalesShipmentByShipmentId(Long shipmentId);

    /** 按出库单查全部发运记录 */
    public List<WmProductSalesShipment> selectShipmentsBySalesId(Long salesId);

    /** 新增 */
    public int insertWmProductSalesShipment(WmProductSalesShipment entity);

    /** 修改 */
    public int updateWmProductSalesShipment(WmProductSalesShipment entity);

    /** 删除单条 */
    public int deleteWmProductSalesShipmentByShipmentId(Long shipmentId);

    /** 批量删除 */
    public int deleteWmProductSalesShipmentByShipmentIds(Long[] shipmentIds);

    /** 按出库单删除全部（出库单作废时联动） */
    public int deleteWmProductSalesShipmentBySalesId(Long salesId);
}
