package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 从采购订单生成退货单请求
 * 数据源是"可退入库批次"（按 item+warehouse+batch 聚合），不是 PO 行本身。
 * 仓库下沉到行：每个退货行带自己的仓库/批次（从入库批次带出）。
 * 前端选 PO + 勾可退批次 + 填退货数量，一次性提交。
 * 后端事务内：校验可退量 → 构建头+行 → 写库。
 *
 * @author qixiaoxia
 * @date 2026-07-18
 */
public class RtVendorFromPurOrderRequest
{
    /** 采购订单ID */
    private Long purOrderId;

    /** 退货单编码(前端 genSerialCode('RT_VENDOR_NO') 生成) */
    private String rtCode;

    /** 退货单名称(可空,默认"退货-"+purOrderCode) */
    private String rtName;

    /** 备注 */
    private String remark;

    /** 退货行列表(至少1行) */
    private List<RtVendorLineDto> lines;

    /**
     * 退货行（从可退批次生成）。
     * 物料编码/名称/单位等由后端按 (itemId, warehouseId, batchId) 查可退批次回填，避免前端篡改。
     * 仓库和批次下沉到行（不再由表头统一）。
     */
    public static class RtVendorLineDto
    {
        /** 物料ID(与 warehouseId+batchId 一起定位可退批次) */
        private Long itemId;

        /** 仓库ID(行级,来自入库批次) */
        private Long warehouseId;

        /** 仓库编码 */
        private String warehouseCode;

        /** 仓库名称 */
        private String warehouseName;

        /** 批次ID(可空=无批次) */
        private Long batchId;

        /** 批次编码 */
        private String batchCode;

        /** 采购订单行ID(来自入库批次,精确回写退货量;历史数据可空) */
        private Long purOrderLineId;

        /** 本次退货数量(≤ 可退量 = 入库量 − 已退量) */
        private BigDecimal quantityRt;

        public Long getItemId() { return itemId; }
        public void setItemId(Long v) { this.itemId = v; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long v) { this.warehouseId = v; }
        public String getWarehouseCode() { return warehouseCode; }
        public void setWarehouseCode(String v) { this.warehouseCode = v; }
        public String getWarehouseName() { return warehouseName; }
        public void setWarehouseName(String v) { this.warehouseName = v; }
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long v) { this.batchId = v; }
        public String getBatchCode() { return batchCode; }
        public void setBatchCode(String v) { this.batchCode = v; }
        public Long getPurOrderLineId() { return purOrderLineId; }
        public void setPurOrderLineId(Long v) { this.purOrderLineId = v; }
        public BigDecimal getQuantityRt() { return quantityRt; }
        public void setQuantityRt(BigDecimal v) { this.quantityRt = v; }
    }

    public Long getPurOrderId() { return purOrderId; }
    public void setPurOrderId(Long v) { this.purOrderId = v; }
    public String getRtCode() { return rtCode; }
    public void setRtCode(String v) { this.rtCode = v; }
    public String getRtName() { return rtName; }
    public void setRtName(String v) { this.rtName = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public List<RtVendorLineDto> getLines() { return lines; }
    public void setLines(List<RtVendorLineDto> v) { this.lines = v; }
}
