package com.ruoyi.system.domain.mes.pro.dto;

import java.util.List;

/**
 * 批量默认路线预览请求（头维度对订单多行相同）
 *
 * @author qixiaoxia
 * @date 2026-09-10
 */
public class RouteBatchResolveRequest
{
    private List<Long> itemIds;
    private String orderType;
    private String outsourceFlag;
    private String packageFlag;

    public List<Long> getItemIds() { return itemIds; }
    public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getOutsourceFlag() { return outsourceFlag; }
    public void setOutsourceFlag(String outsourceFlag) { this.outsourceFlag = outsourceFlag; }
    public String getPackageFlag() { return packageFlag; }
    public void setPackageFlag(String packageFlag) { this.packageFlag = packageFlag; }
}
