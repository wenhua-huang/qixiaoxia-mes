package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;

public interface IWmProductSalesService
{
    public List<WmProductSales> selectWmProductSalesList(WmProductSales entity);
    public List<WmProductSales> selectWmProductSalesAll();
    public WmProductSales selectWmProductSalesBySalesId(Long salesId);
    public int insertWmProductSales(WmProductSales entity);
    public int updateWmProductSales(WmProductSales entity);
    public int deleteWmProductSalesBySalesId(Long salesId);
    public int deleteWmProductSalesBySalesIds(Long[] salesIds);

    // ════════════════════ 业务生命周期方法 ════════════════════

    /** 详情：聚合头+行+明细 */
    public WmProductSales getDetail(Long salesId);

    /** 按出库单编码查详情（移动端扫码查单）：聚合头+行+明细+发运+装箱，找不到返回 null */
    public WmProductSales getDetailBySalesCode(String salesCode);

    /** 出库确认：DRAFT/PARTIAL_POSTED → POSTED 或 PARTIAL_POSTED，扣减库存 */
    public int postOut(Long salesId, List<WmProductSalesDetail> details);

    /** 关闭：SHIPPED → CLOSED */
    public int close(Long salesId);

    /** 作废：DRAFT/PARTIAL_POSTED → CANCELED（PARTIAL_POSTED 需回滚已扣库存） */
    public int cancel(Long salesId);

    /** 从销售订单生成出库单草稿（不落库，返回前端编辑） */
    public WmProductSales buildFromSaleOrder(Long orderId);
}
