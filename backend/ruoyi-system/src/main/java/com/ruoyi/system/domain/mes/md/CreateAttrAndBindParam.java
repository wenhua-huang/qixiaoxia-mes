package com.ruoyi.system.domain.mes.md;

/**
 * 新建属性并绑定到分类的请求体（隐式字典：attr_code 存在则复用，不重复创建）。
 * 替代旧 Map<String,Object> 入参，提供类型安全与校验。
 */
public class CreateAttrAndBindParam
{
    /** 绑定的分类 ID */
    private Long typeId;

    /** 属性定义（attrCode/attrName/attrType/attrUnit/optionsJson） */
    private MdAttrDef attrDef;

    /** 是否必填 */
    private Boolean required;

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public MdAttrDef getAttrDef() { return attrDef; }
    public void setAttrDef(MdAttrDef attrDef) { this.attrDef = attrDef; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
}
