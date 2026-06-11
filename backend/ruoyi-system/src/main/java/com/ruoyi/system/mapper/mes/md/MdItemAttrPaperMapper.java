package com.ruoyi.system.mapper.mes.md;

import com.ruoyi.system.domain.mes.md.MdItemAttrPaper;

public interface MdItemAttrPaperMapper
{
    public MdItemAttrPaper selectByItemId(Long itemId);
    public int insert(MdItemAttrPaper attr);
    public int updateByItemId(MdItemAttrPaper attr);
    public int deleteByItemId(Long itemId);
}
