package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 移动端产品入库确认请求体 — 更新行数量 + 确认入库，单接口原子完成。
 *
 * 原则：前端只调一个接口，事务由后端保证。
 */
public class WmProductRecptMobileBody {

    /** 目标仓库ID（可选，覆盖 header 级仓库） */
    private Long warehouseId;

    /** 备注 */
    private String remark;

    /** 行项目（实收数量/箱数/仓库/批次） */
    private List<MobileLineItem> lines;

    // ---- getters & setters ----

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<MobileLineItem> getLines() { return lines; }
    public void setLines(List<MobileLineItem> lines) { this.lines = lines; }

    /**
     * 移动端单行入库数据
     */
    public static class MobileLineItem {
        private Long lineId;
        private BigDecimal quantityRecpt;
        private Integer quantityBox;
        private Long warehouseId;
        private String batchCode;

        public Long getLineId() { return lineId; }
        public void setLineId(Long lineId) { this.lineId = lineId; }

        public BigDecimal getQuantityRecpt() { return quantityRecpt; }
        public void setQuantityRecpt(BigDecimal quantityRecpt) { this.quantityRecpt = quantityRecpt; }

        public Integer getQuantityBox() { return quantityBox; }
        public void setQuantityBox(Integer quantityBox) { this.quantityBox = quantityBox; }

        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

        public String getBatchCode() { return batchCode; }
        public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    }
}
