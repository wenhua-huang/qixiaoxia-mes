package com.ruoyi.system.domain.mes.sal;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;

/**
 * 销售订单转工单请求
 * 用途:从销售订单明细行一键生成生产工单。支持选工艺路线(自动装载BOM+参数)和创建SKU变体。
 *      前端只调此一次,后端事务内完成回填+来源+调 createWorkorderWithBom。
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public class SalOrderToWorkorderRequest
{
    /** 销售订单明细行ID(转工单来源行) */
    private Long lineId;

    /** 本次转工单数量(≤ 可转数量) */
    private BigDecimal quantity;

    /** 工单编码(前端 genSerialCode WORKORDER_NO 生成) */
    private String workorderCode;

    /** 工单名称(为空则用 产品名+订单号 自动生成) */
    private String workorderName;

    /** 需求交期(前端传 yyyy-MM-dd HH:mm:ss，对齐 ProWorkorder + checkDeviation) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestDate;

    /** 工艺路线产品ID(可选,留空则工单后续在工单页补路线/BOM) */
    private Long routeProductId;

    /** 是否创建SKU变体(偏离标准时由前端决定) */
    private Boolean createSkuVariant;

    /** SKU变体编码(createSkuVariant=true时必填) */
    private String skuCode;

    /** SKU变体名称(createSkuVariant=true时必填) */
    private String skuName;

    /** 从路线加载的BOM列表(前端选路线后自动装载,后端透传给createWorkorderWithBom) */
    private List<ProWorkorderBom> bomList;

    /** 从路线加载的工序参数列表(前端选路线后自动装载,后端透传) */
    private List<ProWorkorderParam> paramList;

    /** 备注 */
    private String remark;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public Long getRouteProductId() { return routeProductId; }
    public void setRouteProductId(Long v) { this.routeProductId = v; }
    public Boolean getCreateSkuVariant() { return createSkuVariant; }
    public void setCreateSkuVariant(Boolean v) { this.createSkuVariant = v; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String v) { this.skuCode = v; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String v) { this.skuName = v; }
    public List<ProWorkorderBom> getBomList() { return bomList; }
    public void setBomList(List<ProWorkorderBom> v) { this.bomList = v; }
    public List<ProWorkorderParam> getParamList() { return paramList; }
    public void setParamList(List<ProWorkorderParam> v) { this.paramList = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
}
