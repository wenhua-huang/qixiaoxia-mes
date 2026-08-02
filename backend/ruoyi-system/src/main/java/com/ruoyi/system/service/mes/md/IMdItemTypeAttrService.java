package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdAttrDef;
import com.ruoyi.system.domain.mes.md.MdItemTypeAttr;

/**
 * 物料分类-扩展属性绑定Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
public interface IMdItemTypeAttrService
{
    /** 查某分类直接绑定（配置页用，不含继承） */
    public List<MdItemTypeAttr> selectBindByTypeId(Long itemTypeId);

    /**
     * 查某分类有效属性（含继承，沿父链聚合；前端动态表单渲染用）。
     * factoryId 由调用方（Controller）从登录上下文填入。
     */
    public List<MdItemTypeAttr> selectEffAttrSchema(Long factoryId, Long itemTypeId);

    /** 全量替换某分类的绑定（先删后插） */
    public int saveBind(Long factoryId, Long itemTypeId, List<MdItemTypeAttr> binds);

    /**
     * 新建属性并绑定到分类（隐式字典：attr_code 已存在则复用该字典记录，不重复创建；不存在则建 attr_def）。
     * 返回绑定后的完整信息（含 attrId）。factoryId/itemTypeId 由调用方填入。
     */
    public MdItemTypeAttr createAttrAndBind(Long factoryId, Long itemTypeId, MdAttrDef attrDef, boolean required);
}
