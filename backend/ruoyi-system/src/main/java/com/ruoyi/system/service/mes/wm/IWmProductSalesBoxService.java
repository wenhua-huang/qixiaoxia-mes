package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;

/**
 * 销售出库-装箱明细 Service
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public interface IWmProductSalesBoxService
{
    /** 列表查询 */
    public List<WmProductSalesBox> selectWmProductSalesBoxList(WmProductSalesBox entity);

    /** 单条 */
    public WmProductSalesBox selectWmProductSalesBoxByBoxId(Long boxId);

    /** 按出库单查全部装箱 */
    public List<WmProductSalesBox> selectBoxesBySalesId(Long salesId);

    /** 按发运单查装箱 */
    public List<WmProductSalesBox> selectBoxesByShipmentId(Long shipmentId);

    /** 新增装箱（自动算体积、自动箱号） */
    public int insertWmProductSalesBox(WmProductSalesBox entity);

    /** 修改装箱 */
    public int updateWmProductSalesBox(WmProductSalesBox entity);

    /** 删除单条（仅 PACKED 可删） */
    public int deleteWmProductSalesBoxByBoxId(Long boxId);

    /** 批量删除 */
    public int deleteWmProductSalesBoxByBoxIds(Long[] boxIds);
}
