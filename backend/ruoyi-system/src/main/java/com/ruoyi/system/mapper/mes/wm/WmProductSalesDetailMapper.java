package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;

public interface WmProductSalesDetailMapper
{
    public List<WmProductSalesDetail> selectWmProductSalesDetailList(WmProductSalesDetail entity);
    public List<WmProductSalesDetail> selectWmProductSalesDetailAll();
    public WmProductSalesDetail selectWmProductSalesDetailByDetailId(Long detailId);
    /** 按出库单ID查全部出库明细（历史出库记录） */
    public List<WmProductSalesDetail> selectDetailsBySalesId(Long salesId);
    public int insertWmProductSalesDetail(WmProductSalesDetail entity);
    public int updateWmProductSalesDetail(WmProductSalesDetail entity);
    public int deleteWmProductSalesDetailByDetailId(Long detailId);
    public int deleteWmProductSalesDetailByDetailIds(Long[] detailIds);
}