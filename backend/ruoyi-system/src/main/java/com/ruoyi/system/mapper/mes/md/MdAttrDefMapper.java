package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.system.domain.mes.md.MdAttrDef;

/**
 * 物料扩展属性字典Mapper接口
 * <p>attr_def 为全局表(factory_id 恒为0)，查询方法标注 {@link SkipFactoryId}
 * 跳过工厂隔离，否则不同工厂用户查不到 factory_id=0 的全局属性。
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
public interface MdAttrDefMapper
{
    @SkipFactoryId
    public MdAttrDef selectMdAttrDefByAttrId(Long attrId);

    @SkipFactoryId
    public MdAttrDef checkAttrCodeUnique(MdAttrDef mdAttrDef);

    @SkipFactoryId
    public List<MdAttrDef> selectMdAttrDefList(MdAttrDef mdAttrDef);

    @SkipFactoryId
    public int insertMdAttrDef(MdAttrDef mdAttrDef);

    @SkipFactoryId
    public int updateMdAttrDef(MdAttrDef mdAttrDef);

    @SkipFactoryId
    public int deleteMdAttrDefByAttrId(Long attrId);

    @SkipFactoryId
    public int deleteMdAttrDefByAttrIds(Long[] attrIds);
}
