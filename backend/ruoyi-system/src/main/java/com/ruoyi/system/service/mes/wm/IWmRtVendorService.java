package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;

public interface IWmRtVendorService
{
    public List<WmRtVendor> selectWmRtVendorList(WmRtVendor entity);
    public List<WmRtVendor> selectWmRtVendorAll();
    public WmRtVendor selectWmRtVendorByRtId(Long rtId);
    public int insertWmRtVendor(WmRtVendor entity);
    public int updateWmRtVendor(WmRtVendor entity);
    public int deleteWmRtVendorByRtId(Long rtId);
    public int deleteWmRtVendorByRtIds(Long[] rtIds);

    /** 确认退货单（DRAFT -> CONFIRMED），执行库存扣减 */
    void confirmRtVendor(Long rtId);

    /** 过账退货单（CONFIRMED -> POSTED），回写采购订单已退货数量 */
    void postRtVendor(Long rtId);
}