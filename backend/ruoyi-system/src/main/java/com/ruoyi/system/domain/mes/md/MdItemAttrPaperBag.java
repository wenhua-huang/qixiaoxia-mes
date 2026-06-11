package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料纸袋成品属性对象 qxx_md_item_attr_paper_bag
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdItemAttrPaperBag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long attrId;
    private Long factoryId;
    private Long itemId;
    private String ropeSpec;
    private String mouthType;
    private String bottomType;

    public Long getAttrId() { return attrId; }
    public void setAttrId(Long attrId) { this.attrId = attrId; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getRopeSpec() { return ropeSpec; }
    public void setRopeSpec(String ropeSpec) { this.ropeSpec = ropeSpec; }
    public String getMouthType() { return mouthType; }
    public void setMouthType(String mouthType) { this.mouthType = mouthType; }
    public String getBottomType() { return bottomType; }
    public void setBottomType(String bottomType) { this.bottomType = bottomType; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("attrId", getAttrId())
            .append("itemId", getItemId())
            .append("ropeSpec", getRopeSpec())
            .toString();
    }
}
