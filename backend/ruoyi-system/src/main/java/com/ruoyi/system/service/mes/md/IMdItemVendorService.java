package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdItemVendor;

/**
 * 物料供应商关系Service接口
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
public interface IMdItemVendorService
{
    MdItemVendor selectByRecordId(Long recordId);
    List<MdItemVendor> selectList(MdItemVendor query);
    List<MdItemVendor> selectByVendorId(Long vendorId);
    List<MdItemVendor> selectByItemId(Long itemId);
    int insert(MdItemVendor entity);
    int update(MdItemVendor entity);
    int deleteByRecordIds(Long[] recordIds);
}
