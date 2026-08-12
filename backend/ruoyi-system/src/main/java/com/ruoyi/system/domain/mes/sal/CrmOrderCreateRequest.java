package com.ruoyi.system.domain.mes.sal;

import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * CRM 系统推单请求 DTO
 *
 * 专为 CRM 等外部系统设计，字段精简：
 * - 不暴露 factoryId（由 API Key 绑定的工厂决定）
 * - orderCode 可选（不传则后端用 ORDER_NO 规则生成）
 * - 明细行用 productCode 关联物料，CRM 无需知道内部 productId
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
public class CrmOrderCreateRequest
{
    /** 销售订单号（可选，不传则后端按 ORDER_NO 规则生成） */
    private String orderCode;

    /** 订单名称（必填） */
    @NotBlank(message = "订单名称不能为空")
    private String orderName;

    /** 客户名称（必填） */
    @NotBlank(message = "客户名称不能为空")
    private String clientName;

    /** 客户编码（可选） */
    private String clientCode;

    /** 客户PO号（可选） */
    private String clientOrderCode;

    /** 业务员（可选） */
    private String salesperson;

    /** 订单日期（可选，默认当前时间） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date orderDate;

    /** 需求交期（可选） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date requestDate;

    /** 明细行（必填，至少一行） */
    @NotEmpty(message = "明细行不能为空")
    private List<CrmOrderLineDTO> lines;

    /** 备注（可选） */
    private String remark;

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public String getOrderName() { return orderName; }
    public void setOrderName(String orderName) { this.orderName = orderName; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }
    public String getClientOrderCode() { return clientOrderCode; }
    public void setClientOrderCode(String clientOrderCode) { this.clientOrderCode = clientOrderCode; }
    public String getSalesperson() { return salesperson; }
    public void setSalesperson(String salesperson) { this.salesperson = salesperson; }
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }
    public List<CrmOrderLineDTO> getLines() { return lines; }
    public void setLines(List<CrmOrderLineDTO> lines) { this.lines = lines; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
