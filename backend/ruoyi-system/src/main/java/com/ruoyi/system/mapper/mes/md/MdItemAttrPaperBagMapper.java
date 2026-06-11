package com.ruoyi.system.mapper.mes.md;

import com.ruoyi.system.domain.mes.md.MdItemAttrPaperBag;

public interface MdItemAttrPaperBagMapper
{
    public MdItemAttrPaperBag selectByItemId(Long itemId);
    public int insert(MdItemAttrPaperBag attr);
    public int updateByItemId(MdItemAttrPaperBag attr);
    public int deleteByItemId(Long itemId);
}
