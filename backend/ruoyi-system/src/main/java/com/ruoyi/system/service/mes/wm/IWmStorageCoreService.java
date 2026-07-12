package com.ruoyi.system.service.mes.wm;

import com.ruoyi.system.domain.mes.wm.tx.*;
import java.util.List;

/**
 * 库存核心事务服务接口
 * 处理各类库存变动（入库/出库/调拨），封装事务创建和库存更新逻辑
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
public interface IWmStorageCoreService {

    /** 物料入库 → 库存+1 */
    void processItemRecpt(List<ItemRecptTxBean> lines);

    /** 产品入库（工单完工） -> 库存+1 */
    void processProductRecpt(List<ProductRecptTxBean> lines);

    /** 杂项入库 → 库存+1 */
    void processMiscRecpt(List<MiscRecptTxBean> lines);

    /** 杂项出库 → 库存-1 */
    void processMiscIssue(List<MiscIssueTxBean> lines);

    /** 供应商退货 → 库存-1 */
    void processRtVendor(List<RtVendorTxBean> lines);

    /** 销售出库 → 库存-1 */
    void processProductSales(List<ProductSalesTxBean> lines);

    /** 销售退货 → 库存+1 */
    void processRtSales(List<RtSalesTxBean> lines);

    /** 调拨转移 → 源仓-1 + 目标仓+1 */
    void processTransfer(List<TransferTxBean> lines);
}
