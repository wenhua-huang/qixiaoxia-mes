package com.ruoyi.system.domain.mes.md;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料分类-扩展属性绑定对象 qxx_md_item_type_attr
 * <p>实现继承：查询某分类有效属性时，沿 parent_type_id 递归向上聚合所有祖先的绑定，
 * 子类对同一 attr 的绑定覆盖父类（取最近本类）。
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
public class MdItemTypeAttr extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 工厂ID */
    private Long factoryId;

    /** 分类ID */
    private Long itemTypeId;

    /** 属性ID */
    private Long attrId;

    /** 是否必填(1-是,0-否) */
    private String required;

    /** 本分类内排序 */
    private Integer sortOrder;

    /** 是否启用(1-是,0-否) */
    private String enableFlag;

    // ---- 关联冗余字段（JOIN attr_def 带出，非本表列） ----

    /** 属性编码（关联 attr_def） */
    private String attrCode;

    /** 属性显示名 */
    private String attrName;

    /** 属性类型 */
    private String attrType;

    /** 属性单位 */
    private String attrUnit;

    /** SELECT 选项 JSON */
    private String optionsJson;

    /** 继承深度（0=本类绑定，>0=祖先继承，越大越远） */
    private Integer inheritDepth;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getItemTypeId() { return itemTypeId; }
    public void setItemTypeId(Long itemTypeId) { this.itemTypeId = itemTypeId; }

    public Long getAttrId() { return attrId; }
    public void setAttrId(Long attrId) { this.attrId = attrId; }

    public String getRequired() { return required; }
    public void setRequired(String required) { this.required = required; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }

    public String getAttrCode() { return attrCode; }
    public void setAttrCode(String attrCode) { this.attrCode = attrCode; }

    public String getAttrName() { return attrName; }
    public void setAttrName(String attrName) { this.attrName = attrName; }

    public String getAttrType() { return attrType; }
    public void setAttrType(String attrType) { this.attrType = attrType; }

    public String getAttrUnit() { return attrUnit; }
    public void setAttrUnit(String attrUnit) { this.attrUnit = attrUnit; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public Integer getInheritDepth() { return inheritDepth; }
    public void setInheritDepth(Integer inheritDepth) { this.inheritDepth = inheritDepth; }
}
