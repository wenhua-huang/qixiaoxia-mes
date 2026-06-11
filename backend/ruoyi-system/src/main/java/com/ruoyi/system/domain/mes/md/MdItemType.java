package com.ruoyi.system.domain.mes.md;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料产品分类对象 qxx_md_item_type
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdItemType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Long itemTypeId;

    /** 工厂ID */
    private Long factoryId;

    /** 分类编码 */
    @Excel(name = "分类编码")
    private String itemTypeCode;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String itemTypeName;

    /** 父类型ID(0=根节点) */
    private Long parentTypeId;

    /** 产品物料标识:RAW-原料,SEMI-半成品,FINISHED-成品,AUXILIARY-辅料,PACK-包材 */
    @Excel(name = "产品物料标识", readConverterExp = "RAW=原料,SEMI=半成品,FINISHED=成品,AUXILIARY=辅料,PACK=包材")
    private String itemOrProduct;

    /** 同级排序号 */
    @Excel(name = "排序号")
    private Integer orderNum;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    /** 子分类列表（内存树结构） */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MdItemType> children = new ArrayList<>();

    public Long getItemTypeId()
    {
        return itemTypeId;
    }

    public void setItemTypeId(Long itemTypeId)
    {
        this.itemTypeId = itemTypeId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getItemTypeCode()
    {
        return itemTypeCode;
    }

    public void setItemTypeCode(String itemTypeCode)
    {
        this.itemTypeCode = itemTypeCode;
    }

    public String getItemTypeName()
    {
        return itemTypeName;
    }

    public void setItemTypeName(String itemTypeName)
    {
        this.itemTypeName = itemTypeName;
    }

    public Long getParentTypeId()
    {
        return parentTypeId;
    }

    public void setParentTypeId(Long parentTypeId)
    {
        this.parentTypeId = parentTypeId;
    }

    public String getItemOrProduct()
    {
        return itemOrProduct;
    }

    public void setItemOrProduct(String itemOrProduct)
    {
        this.itemOrProduct = itemOrProduct;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getEnableFlag()
    {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag)
    {
        this.enableFlag = enableFlag;
    }

    public List<MdItemType> getChildren()
    {
        return children;
    }

    public void setChildren(List<MdItemType> children)
    {
        this.children = children;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemTypeId", getItemTypeId())
            .append("factoryId", getFactoryId())
            .append("itemTypeCode", getItemTypeCode())
            .append("itemTypeName", getItemTypeName())
            .append("parentTypeId", getParentTypeId())
            .append("itemOrProduct", getItemOrProduct())
            .append("orderNum", getOrderNum())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
