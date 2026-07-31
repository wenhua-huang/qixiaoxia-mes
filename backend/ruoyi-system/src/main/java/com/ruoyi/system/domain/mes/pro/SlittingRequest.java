package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分切作业请求 DTO（库存驱动 + 报工自动建卷）
 *
 * <p>流程：选母卷物料 → 领料出库(扣 material_stock) → 报工事务内自动建母卷+子卷 roll_detail。
 * <p>母卷/子卷 roll_detail 不再要求预存，由本接口在执行时创建。
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
public class SlittingRequest
{
    /** 生产工单ID（可选，分切可独立于工单执行） */
    private Long workorderId;
    private String workorderCode;
    /** 工序ID（工单关联的分切工序） */
    private Long processId;
    private String processCode;
    private String processName;
    /** 流转卡ID（可选） */
    private Long cardId;
    /** 工艺路线ID（可选，用于判断末工序） */
    private Long routeId;

    // ── 领料（库存驱动）──
    /** 领料母卷物料ID（必填，从 material_stock 扣减） */
    private Long sourceItemId;
    private String sourceItemCode;
    private String sourceItemName;
    /** 领料出库仓库ID（必填） */
    private Long sourceWarehouseId;
    private String sourceWarehouseCode;
    private String sourceWarehouseName;
    /** 领料数量（吨，必填） */
    private BigDecimal pickQty;
    /** 领料批次号（可选，指定则扣该批次；不指定由 processTransaction 按 materialStock 自动归集） */
    private Long sourceBatchId;
    private String sourceBatchCode;

    /** 子卷规格列表（至少1条） */
    private List<ChildRollSpec> childRolls;

    /** 纸边物料ID（可选，不填则不产生纸边入库） */
    private Long edgeItemId;
    private String edgeItemCode;
    private String edgeItemName;
    /** 纸边重量(kg) */
    private BigDecimal edgeWeight;

    /** 工作站ID（分切设备，可选） */
    private Long workstationId;
    /** 备注 */
    private String remark;

    /**
     * 子卷规格（前端录入，后端据此创建 roll_detail）
     */
    public static class ChildRollSpec
    {
        /** 目标物料ID（默认继承母卷物料，可改为窄幅物料） */
        private Long itemId;
        private String itemCode;
        private String itemName;
        /** 实际门幅(mm) */
        private String actualWidth;
        /** 实际长度(米) */
        private BigDecimal actualLength;
        /** 实际重量(吨) */
        private BigDecimal actualWeight;
        /** 实际克重(g/㎡) */
        private String actualWeightGsm;

        public Long getItemId() { return itemId; }
        public void setItemId(Long v) { this.itemId = v; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String v) { this.itemCode = v; }
        public String getItemName() { return itemName; }
        public void setItemName(String v) { this.itemName = v; }
        public String getActualWidth() { return actualWidth; }
        public void setActualWidth(String v) { this.actualWidth = v; }
        public BigDecimal getActualLength() { return actualLength; }
        public void setActualLength(BigDecimal v) { this.actualLength = v; }
        public BigDecimal getActualWeight() { return actualWeight; }
        public void setActualWeight(BigDecimal v) { this.actualWeight = v; }
        public String getActualWeightGsm() { return actualWeightGsm; }
        public void setActualWeightGsm(String v) { this.actualWeightGsm = v; }
    }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public Long getSourceItemId() { return sourceItemId; }
    public void setSourceItemId(Long v) { this.sourceItemId = v; }
    public String getSourceItemCode() { return sourceItemCode; }
    public void setSourceItemCode(String v) { this.sourceItemCode = v; }
    public String getSourceItemName() { return sourceItemName; }
    public void setSourceItemName(String v) { this.sourceItemName = v; }
    public Long getSourceWarehouseId() { return sourceWarehouseId; }
    public void setSourceWarehouseId(Long v) { this.sourceWarehouseId = v; }
    public String getSourceWarehouseCode() { return sourceWarehouseCode; }
    public void setSourceWarehouseCode(String v) { this.sourceWarehouseCode = v; }
    public String getSourceWarehouseName() { return sourceWarehouseName; }
    public void setSourceWarehouseName(String v) { this.sourceWarehouseName = v; }
    public BigDecimal getPickQty() { return pickQty; }
    public void setPickQty(BigDecimal v) { this.pickQty = v; }
    public Long getSourceBatchId() { return sourceBatchId; }
    public void setSourceBatchId(Long v) { this.sourceBatchId = v; }
    public String getSourceBatchCode() { return sourceBatchCode; }
    public void setSourceBatchCode(String v) { this.sourceBatchCode = v; }
    public List<ChildRollSpec> getChildRolls() { return childRolls; }
    public void setChildRolls(List<ChildRollSpec> v) { this.childRolls = v; }
    public Long getEdgeItemId() { return edgeItemId; }
    public void setEdgeItemId(Long v) { this.edgeItemId = v; }
    public String getEdgeItemCode() { return edgeItemCode; }
    public void setEdgeItemCode(String v) { this.edgeItemCode = v; }
    public String getEdgeItemName() { return edgeItemName; }
    public void setEdgeItemName(String v) { this.edgeItemName = v; }
    public BigDecimal getEdgeWeight() { return edgeWeight; }
    public void setEdgeWeight(BigDecimal v) { this.edgeWeight = v; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
}
