package com.ruoyi.system.mapper.mes.wm;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;

/**
 * 销售出库-装箱明细 Mapper
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public interface WmProductSalesBoxMapper
{
    /** 列表查询 */
    public List<WmProductSalesBox> selectWmProductSalesBoxList(WmProductSalesBox entity);

    /** 单条 */
    public WmProductSalesBox selectWmProductSalesBoxByBoxId(Long boxId);

    /** 按出库单查全部装箱 */
    public List<WmProductSalesBox> selectBoxesBySalesId(Long salesId);

    /** 按发运单查装箱 */
    public List<WmProductSalesBox> selectBoxesByShipmentId(Long shipmentId);

    /** 新增 */
    public int insertWmProductSalesBox(WmProductSalesBox entity);

    /** 修改 */
    public int updateWmProductSalesBox(WmProductSalesBox entity);

    /** 删除单条 */
    public int deleteWmProductSalesBoxByBoxId(Long boxId);

    /** 批量删除 */
    public int deleteWmProductSalesBoxByBoxIds(Long[] boxIds);

    /** 按出库单删除全部 */
    public int deleteWmProductSalesBoxBySalesId(Long salesId);

    /** 取当前最大箱号序号（按 sales_id，用于自动 BOX-NNN） */
    public Integer selectMaxBoxSeqBySalesId(Long salesId);

    /** 发运时标记箱：shipment_id + status=SHIPPED（强制更新，不走 <if> null 判断） */
    public int markShipped(@Param("boxId") Long boxId, @Param("shipmentId") Long shipmentId,
                           @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /** 删除发运回滚箱：shipment_id=null + status=PACKED（shipment_id 需显式置 null，<if> 无法表达） */
    public int rollbackToPacked(@Param("boxId") Long boxId,
                                @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);
}
