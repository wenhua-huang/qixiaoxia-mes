package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.ruoyi.common.enums.TransactionTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.wm.tx.*;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;
import com.ruoyi.system.service.mes.wm.IWmTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * 库存核心事务服务 — 将 TX Bean 转换为 WmTransaction 并提交处理。
 *
 * 幂等性：所有 processXxx 方法天然幂等。processTransaction 内部通过
 * selectExistingTransaction 按 (sourceDocType + sourceDocId + sourceLineId) 查重，
 * 已处理过的行直接返回已有记录，不会重复扣库存。
 *
 * 调拨重试安全：txOut 成功 + txIn 失败后重试 → txOut 幂等跳过 → txIn 重试成功。
 * 无需业务层补偿，直接重试即可。
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
@Component
public class WmStorageCoreServiceImpl implements IWmStorageCoreService {

    @Autowired
    private IWmTransactionService wmTransactionService;

    // ── 入库类 (+1) ──

    @Override
    public void processItemRecpt(List<ItemRecptTxBean> lines) {
        checkNotEmpty(lines, "入库单行");
        for (ItemRecptTxBean line : lines) {
            WmTransaction tx = buildTx(line, TransactionTypeEnum.ITEM_RECPT.getCode(), 1);
            copyVendor(line, tx);
            wmTransactionService.processTransaction(tx);
        }
    }

    @Override
    public void processProductRecpt(List<ProductRecptTxBean> lines) {
        checkNotEmpty(lines, "入库单行");
        for (ProductRecptTxBean line : lines) {
            wmTransactionService.processTransaction(buildTx(line, TransactionTypeEnum.PRODUCT_RECPT.getCode(), 1));
        }
    }

    @Override
    public void processMiscRecpt(List<MiscRecptTxBean> lines) {
        checkNotEmpty(lines, "入库单行");
        for (MiscRecptTxBean line : lines) {
            wmTransactionService.processTransaction(buildTx(line, TransactionTypeEnum.MISC_RECPT.getCode(), 1));
        }
    }

    @Override
    public void processRtSales(List<RtSalesTxBean> lines) {
        checkNotEmpty(lines, "退货单行");
        for (RtSalesTxBean line : lines) {
            wmTransactionService.processTransaction(buildTx(line, TransactionTypeEnum.PRODUCT_RT.getCode(), 1));
        }
    }

    @Override
    public void processMiscIssue(List<MiscIssueTxBean> lines) {
        checkNotEmpty(lines, "出库单行");
        for (MiscIssueTxBean line : lines) {
            wmTransactionService.processTransaction(buildTx(line, TransactionTypeEnum.MISC_ISSUE.getCode(), -1));
        }
    }

    @Override
    public void processRtVendor(List<RtVendorTxBean> lines) {
        checkNotEmpty(lines, "退货单行");
        for (RtVendorTxBean line : lines) {
            WmTransaction tx = buildTx(line, TransactionTypeEnum.ITEM_RTV.getCode(), -1);
            copyVendor(line, tx);
            wmTransactionService.processTransaction(tx);
        }
    }

    @Override
    public void processProductSales(List<ProductSalesTxBean> lines) {
        checkNotEmpty(lines, "出库单行");
        for (ProductSalesTxBean line : lines) {
            wmTransactionService.processTransaction(buildTx(line, TransactionTypeEnum.PRODUCT_SALES.getCode(), -1));
        }
    }

    @Override
    public void processTransfer(List<TransferTxBean> lines) {
        checkNotEmpty(lines, "调拨单行");
        for (TransferTxBean line : lines) {
            WmTransaction txOut = buildTransferTx(line, TransactionTypeEnum.TRANS_OUT.getCode(), -1,
                    line.getFromWarehouseId(), line.getFromLocationId(), line.getFromAreaId());
            wmTransactionService.processTransaction(txOut);

            WmTransaction txIn = buildTransferTx(line, TransactionTypeEnum.TRANS_IN.getCode(), 1,
                    line.getToWarehouseId(), line.getToLocationId(), line.getToAreaId());
            wmTransactionService.processTransaction(txIn);
        }
    }

    // ── Private helpers ──

    private void checkNotEmpty(List<?> lines, String desc) {
        if (lines == null || lines.isEmpty()) {
            throw new ServiceException("没有需要处理的" + desc);
        }
    }

    /** 通用 TxBean → WmTransaction（6 种普通单据） */
    private WmTransaction buildTx(TxBean bean, String txType, int sign) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(txType);
        copyCommonFields(bean, tx);
        tx.setQuantity(tx.getQuantity().multiply(new BigDecimal(sign)));
        tx.setTransactionTime(new Date());
        return tx;
    }

    /** TransferTxBean → WmTransaction（源/目标仓库不同，手工复制 — 若 TxBean 新增字段需同步更新此处） */
    private WmTransaction buildTransferTx(TransferTxBean line, String txType, int sign,
            Long warehouseId, Long locationId, Long areaId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(txType);
        tx.setSourceDocType(line.getSourceDocType());
        tx.setSourceDocId(line.getSourceDocId());
        tx.setSourceDocCode(line.getSourceDocCode());
        tx.setSourceLineId(line.getSourceDocLineId());
        tx.setMaterialStockId(line.getMaterialStockId());
        tx.setItemId(line.getItemId());
        tx.setItemCode(line.getItemCode());
        tx.setItemName(line.getItemName());
        tx.setSpecification(line.getSpecification());
        tx.setUnitOfMeasure(line.getUnitOfMeasure());
        tx.setUnitName(line.getUnitName());
        tx.setQuantity(line.getTransactionQuantity().multiply(new BigDecimal(sign)));
        tx.setBatchId(line.getBatchId());
        tx.setBatchCode(line.getBatchCode());
        tx.setWarehouseId(warehouseId);
        tx.setLocationId(locationId);
        tx.setAreaId(areaId);
        tx.setVendorId(line.getVendorId() != null ? line.getVendorId() : 0L);
        tx.setTransactionTime(new Date());
        return tx;
    }

    /** 通过 TxBean 接口统一复制公共字段 */
    private void copyCommonFields(TxBean b, WmTransaction tx) {
        tx.setSourceDocType(b.getSourceDocType());
        tx.setSourceDocId(b.getSourceDocId());
        tx.setSourceDocCode(b.getSourceDocCode());
        tx.setSourceLineId(b.getSourceDocLineId());
        tx.setMaterialStockId(b.getMaterialStockId());
        tx.setItemId(b.getItemId());
        tx.setItemCode(b.getItemCode());
        tx.setItemName(b.getItemName());
        tx.setSpecification(b.getSpecification());
        tx.setUnitOfMeasure(b.getUnitOfMeasure());
        tx.setUnitName(b.getUnitName());
        tx.setQuantity(b.getTransactionQuantity());
        tx.setBatchId(b.getBatchId());
        tx.setBatchCode(b.getBatchCode());
        tx.setWarehouseId(b.getWarehouseId());
        tx.setWarehouseCode(b.getWarehouseCode());
        tx.setWarehouseName(b.getWarehouseName());
        tx.setLocationId(b.getLocationId());
        tx.setAreaId(b.getAreaId());
    }

    /** 复制供应商字段（ItemRecptTxBean / RtVendorTxBean 特有） */
    private void copyVendor(ItemRecptTxBean b, WmTransaction tx) {
        tx.setVendorId(b.getVendorId());
    }

    private void copyVendor(RtVendorTxBean b, WmTransaction tx) {
        tx.setVendorId(b.getVendorId());
    }
}
