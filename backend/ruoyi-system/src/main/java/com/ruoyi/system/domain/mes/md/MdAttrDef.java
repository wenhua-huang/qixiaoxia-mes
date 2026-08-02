package com.ruoyi.system.domain.mes.md;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料扩展属性字典对象 qxx_md_attr_def
 * <p>全局共享（factory_id 恒为 0），定义可复用的属性项。
 * 分类通过 {@link MdItemTypeAttr} 绑定属性并实现继承。
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
public class MdAttrDef extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 属性ID */
    private Long attrId;

    /** 工厂ID(恒为0,全局共享) */
    private Long factoryId;

    /** 属性编码(唯一),如 PAPER_WIDTH */
    @Excel(name = "属性编码")
    private String attrCode;

    /** 属性显示名,如 门幅 */
    @Excel(name = "属性名称")
    private String attrName;

    /** 类型:TEXT/NUMBER/SELECT/BOOL/DATE */
    @Excel(name = "类型")
    private String attrType;

    /** 单位,如 mm/g */
    @Excel(name = "单位")
    private String attrUnit;

    /** SELECT 类型的可选值(JSON数组字符串) */
    private String optionsJson;

    /** 排序号 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getAttrId() { return attrId; }
    public void setAttrId(Long attrId) { this.attrId = attrId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

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

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }
}
