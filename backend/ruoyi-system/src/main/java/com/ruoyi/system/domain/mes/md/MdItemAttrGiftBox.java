package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料礼品盒属性对象 qxx_md_item_attr_gift_box
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdItemAttrGiftBox extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long attrId;
    private Long factoryId;
    private Long itemId;
    // 礼品盒属性字段待业务确认后补充

    public Long getAttrId() { return attrId; }
    public void setAttrId(Long attrId) { this.attrId = attrId; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("attrId", getAttrId())
            .append("itemId", getItemId())
            .toString();
    }
}
