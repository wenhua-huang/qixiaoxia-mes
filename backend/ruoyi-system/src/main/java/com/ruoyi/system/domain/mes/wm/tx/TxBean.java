package com.ruoyi.system.domain.mes.wm.tx;

import java.math.BigDecimal;

/**
 * TX Bean 公共接口 — 消除 copyCommonFields 中的 instanceof 分支
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
public interface TxBean {

    String getSourceDocType();
    Long getSourceDocId();
    String getSourceDocCode();
    Long getSourceDocLineId();

    Long getMaterialStockId();

    Long getItemId();
    String getItemCode();
    String getItemName();
    String getSpecification();
    String getUnitOfMeasure();
    String getUnitName();

    BigDecimal getTransactionQuantity();

    Long getBatchId();
    String getBatchCode();

    Long getWarehouseId();
    String getWarehouseCode();
    String getWarehouseName();
    Long getLocationId();
    Long getAreaId();
}
