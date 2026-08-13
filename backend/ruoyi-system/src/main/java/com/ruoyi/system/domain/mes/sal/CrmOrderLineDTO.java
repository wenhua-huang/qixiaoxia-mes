package com.ruoyi.system.domain.mes.sal;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * CRM 推单明细行 DTO
 *
 * 用 productCode（物料编码）关联物料，后端反查填充 productId/productName/单位等。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
public class CrmOrderLineDTO
{
    /** 物料编码（必填，用于反查 qxx_md_item） */
    @NotBlank(message = "物料编码不能为空")
    private String productCode;

    /** 订单数量（必填） */
    @NotNull(message = "订单数量不能为空")
    private BigDecimal quantity;

    /** 单价（可选） */
    private BigDecimal unitPrice;

    /** 行交期（可选） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date requestDate;

    /** 备注（可选） */
    private String remark;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
