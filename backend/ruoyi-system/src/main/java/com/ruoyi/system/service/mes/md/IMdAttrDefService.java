package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdAttrDef;

/**
 * 物料扩展属性字典Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
public interface IMdAttrDefService
{
    public MdAttrDef selectMdAttrDefByAttrId(Long attrId);

    public List<MdAttrDef> selectMdAttrDefList(MdAttrDef mdAttrDef);

    public boolean checkAttrCodeUnique(MdAttrDef mdAttrDef);

    public int insertMdAttrDef(MdAttrDef mdAttrDef);

    public int updateMdAttrDef(MdAttrDef mdAttrDef);

    public int deleteMdAttrDefByAttrIds(Long[] attrIds);
}
