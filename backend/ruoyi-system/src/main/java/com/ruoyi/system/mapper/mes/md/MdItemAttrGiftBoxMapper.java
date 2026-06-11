package com.ruoyi.system.mapper.mes.md;

import com.ruoyi.system.domain.mes.md.MdItemAttrGiftBox;

public interface MdItemAttrGiftBoxMapper
{
    public MdItemAttrGiftBox selectByItemId(Long itemId);
    public int insert(MdItemAttrGiftBox attr);
    public int updateByItemId(MdItemAttrGiftBox attr);
    public int deleteByItemId(Long itemId);
}
