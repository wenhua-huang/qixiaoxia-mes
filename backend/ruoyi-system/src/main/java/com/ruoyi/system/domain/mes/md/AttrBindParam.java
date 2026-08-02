package com.ruoyi.system.domain.mes.md;

import java.util.List;

/**
 * 全量保存某分类属性绑定的请求体（先删后插）。
 * 替代旧 Map<String,Object> 入参，提供类型安全与校验。
 */
public class AttrBindParam
{
    /** 绑定的分类 ID */
    private Long typeId;

    /** 绑定列表：attrId / required / sortOrder / enableFlag */
    private List<MdItemTypeAttr> binds;

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public List<MdItemTypeAttr> getBinds() { return binds; }
    public void setBinds(List<MdItemTypeAttr> binds) { this.binds = binds; }
}
