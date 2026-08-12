package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工单外协发料信息 VO — App 端"外协按工单发料"入口：扫码/输工单号后，
 * 一次返回该工单所有外协工序(is_outsource=1)及其 BOM 发料行、默认厂商、
 * 以及是否已存在外协单（防重复建单）。
 *
 * @author qixiaoxia
 */
public class ProOutsourceWorkorderInfoVO
{
    private Long workorderId;
    private String workorderCode;
    private String workorderName;
    private String productName;
    private Long routeId;
    /** 活跃流转卡ID（无卡时为 null，建单后用于标记 OUTSOURCING） */
    private Long activeCardId;

    private List<OutsourceProcessItem> outsourceProcesses;

    /** 外协工序节点：含默认厂商、已有外协单、该工序 BOM 发料行 */
    public static class OutsourceProcessItem
    {
        private Long processId;
        private String processCode;
        private String processName;
        private Integer orderNum;
        private Long vendorId;
        private String vendorCode;
        private String vendorName;

        /** 该(工单,工序)已存在的外协单（含草稿/已发料等任意状态），无则 null，前端据此防重 */
        private Long existingOrderId;
        private String existingOrderCode;
        private String existingStatus;

        private List<OutsourceBomLine> bomLines;

        public Long getProcessId() { return processId; }
        public void setProcessId(Long v) { this.processId = v; }
        public String getProcessCode() { return processCode; }
        public void setProcessCode(String v) { this.processCode = v; }
        public String getProcessName() { return processName; }
        public void setProcessName(String v) { this.processName = v; }
        public Integer getOrderNum() { return orderNum; }
        public void setOrderNum(Integer v) { this.orderNum = v; }
        public Long getVendorId() { return vendorId; }
        public void setVendorId(Long v) { this.vendorId = v; }
        public String getVendorCode() { return vendorCode; }
        public void setVendorCode(String v) { this.vendorCode = v; }
        public String getVendorName() { return vendorName; }
        public void setVendorName(String v) { this.vendorName = v; }
        public Long getExistingOrderId() { return existingOrderId; }
        public void setExistingOrderId(Long v) { this.existingOrderId = v; }
        public String getExistingOrderCode() { return existingOrderCode; }
        public void setExistingOrderCode(String v) { this.existingOrderCode = v; }
        public String getExistingStatus() { return existingStatus; }
        public void setExistingStatus(String v) { this.existingStatus = v; }
        public List<OutsourceBomLine> getBomLines() { return bomLines; }
        public void setBomLines(List<OutsourceBomLine> v) { this.bomLines = v; }
    }

    /** BOM 发料行（数量取预计总用量 totalQuantity，为空回退单位用量 quantity） */
    public static class OutsourceBomLine
    {
        private Long itemId;
        private String itemCode;
        private String itemName;
        private String specification;
        private String unitOfMeasure;
        private String unitName;
        private BigDecimal quantity;

        public Long getItemId() { return itemId; }
        public void setItemId(Long v) { this.itemId = v; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String v) { this.itemCode = v; }
        public String getItemName() { return itemName; }
        public void setItemName(String v) { this.itemName = v; }
        public String getSpecification() { return specification; }
        public void setSpecification(String v) { this.specification = v; }
        public String getUnitOfMeasure() { return unitOfMeasure; }
        public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String v) { this.unitName = v; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal v) { this.quantity = v; }
    }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public Long getActiveCardId() { return activeCardId; }
    public void setActiveCardId(Long v) { this.activeCardId = v; }
    public List<OutsourceProcessItem> getOutsourceProcesses() { return outsourceProcesses; }
    public void setOutsourceProcesses(List<OutsourceProcessItem> v) { this.outsourceProcesses = v; }
}
