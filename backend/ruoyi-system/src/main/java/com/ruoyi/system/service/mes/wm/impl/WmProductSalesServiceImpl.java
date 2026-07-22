package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.enums.WmProductSalesConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesDetailMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.service.mes.sal.ISalOrderService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmProductSalesService;

/**
 * 销售出库单业务层（状态机 + 过账扣库存 + FIFO 批次拣货）
 * 参照 WmIssueHeaderServiceImpl 的锁+事务+扣减范式。
 *
 * @author qixiaoxia
 * @date 2026-07-22
 */
@Service
public class WmProductSalesServiceImpl implements IWmProductSalesService
{
    @Autowired private WmProductSalesMapper wmProductSalesMapper;
    @Autowired private WmProductSalesLineMapper wmProductSalesLineMapper;
    @Autowired private WmProductSalesDetailMapper wmProductSalesDetailMapper;
    @Autowired private WmMaterialStockMapper wmMaterialStockMapper;
    @Autowired private WmTransactionMapper wmTransactionMapper;
    @Autowired private ProMaterialTraceMapper proMaterialTraceMapper;
    @Autowired private AutoCodeGenerator autoCodeGenerator;
    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ISalOrderService salOrderService;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    // ════════════════════ 基础 CRUD ════════════════════

    @Override
    public List<WmProductSales> selectWmProductSalesList(WmProductSales entity) {
        return wmProductSalesMapper.selectWmProductSalesList(entity);
    }

    @Override
    public List<WmProductSales> selectWmProductSalesAll() {
        return wmProductSalesMapper.selectWmProductSalesAll();
    }

    @Override
    public WmProductSales selectWmProductSalesBySalesId(Long salesId) {
        return wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
    }

    @Override
    @Transactional
    public int insertWmProductSales(WmProductSales entity) {
        if (entity.getStatus() == null || entity.getStatus().isEmpty()) {
            entity.setStatus(WmProductSalesConstants.STATUS_DRAFT);
        }
        if (entity.getSalesCode() == null || entity.getSalesCode().isEmpty()) {
            entity.setSalesCode(autoCodeGenerator.genSerialCode(WmProductSalesConstants.CODE_RULE_SALES, ""));
        }
        if (entity.getPostedQuantity() == null) {
            entity.setPostedQuantity(BigDecimal.ZERO);
        }
        entity.setCreateTime(DateUtils.getNowDate());
        wmProductSalesMapper.insertWmProductSales(entity);
        saveLines(entity);
        return 1;
    }

    @Override
    @Transactional
    public int updateWmProductSales(WmProductSales entity) {
        WmProductSales exist = wmProductSalesMapper.selectWmProductSalesBySalesId(entity.getSalesId());
        if (exist == null) throw new ServiceException("出库单不存在");
        if (!WmProductSalesConstants.isEditable(exist.getStatus())) {
            throw new ServiceException("当前状态[" + exist.getStatus() + "]不允许修改");
        }
        entity.setUpdateTime(DateUtils.getNowDate());
        wmProductSalesMapper.updateWmProductSales(entity);
        saveLines(entity);
        return 1;
    }

    @Override
    @Transactional
    public int deleteWmProductSalesBySalesId(Long salesId) {
        WmProductSales exist = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (exist != null && !WmProductSalesConstants.isEditable(exist.getStatus())) {
            throw new ServiceException("当前状态[" + exist.getStatus() + "]不允许删除");
        }
        return wmProductSalesMapper.deleteWmProductSalesBySalesId(salesId);
    }

    @Override
    @Transactional
    public int deleteWmProductSalesBySalesIds(Long[] salesIds) {
        for (Long id : salesIds) {
            WmProductSales exist = wmProductSalesMapper.selectWmProductSalesBySalesId(id);
            if (exist != null && !WmProductSalesConstants.isEditable(exist.getStatus())) {
                throw new ServiceException("出库单[" + exist.getSalesCode() + "]当前状态不允许删除");
            }
        }
        return wmProductSalesMapper.deleteWmProductSalesBySalesIds(salesIds);
    }

    // ════════════════════ 详情 ════════════════════

    @Override
    public WmProductSales getDetail(Long salesId) {
        WmProductSales header = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (header == null) throw new ServiceException("出库单不存在");
        header.setLines(wmProductSalesLineMapper.selectLinesBySalesId(salesId));
        header.setDetails(wmProductSalesDetailMapper.selectDetailsBySalesId(salesId));
        return header;
    }

    // ════════════════════ 过账出库（核心） ════════════════════

    @Override
    public int postOut(Long salesId, List<WmProductSalesDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new ServiceException("出库明细不能为空");
        }
        lockTemplate.execute("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doPostOut(salesId, details)));
        return 1;
    }

    private Long doPostOut(Long salesId, List<WmProductSalesDetail> details) {
        WmProductSales header = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (header == null) throw new ServiceException("出库单不存在");
        if (!WmProductSalesConstants.isPostable(header.getStatus())) {
            throw new ServiceException("当前状态[" + header.getStatus() + "]不允许过账");
        }
        Map<Long, WmProductSalesLine> lineMap = buildLineMap(salesId);
        BigDecimal postedThisTime = BigDecimal.ZERO;
        for (WmProductSalesDetail d : details) {
            BigDecimal qty = d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            WmProductSalesLine line = lineMap.get(d.getLineId());
            validateLinePostedQty(line, d, qty);
            postOutSingleBatch(header, d, qty, line);
            accumulateLinePosted(line, qty);
            postedThisTime = postedThisTime.add(qty);
        }
        updateHeaderAfterPost(header, postedThisTime);
        return salesId;
    }

    private Map<Long, WmProductSalesLine> buildLineMap(Long salesId) {
        List<WmProductSalesLine> lines = wmProductSalesLineMapper.selectLinesBySalesId(salesId);
        Map<Long, WmProductSalesLine> map = new HashMap<>();
        for (WmProductSalesLine l : lines) map.put(l.getLineId(), l);
        return map;
    }

    private void validateLinePostedQty(WmProductSalesLine line, WmProductSalesDetail d, BigDecimal qty) {
        if (line == null) throw new ServiceException("出库行[" + d.getLineId() + "]不存在");
        BigDecimal need = line.getQuantitySales() != null ? line.getQuantitySales() : BigDecimal.ZERO;
        BigDecimal posted = line.getQuantityPosted() != null ? line.getQuantityPosted() : BigDecimal.ZERO;
        BigDecimal remain = need.subtract(posted);
        if (qty.compareTo(remain) > 0) {
            throw new ServiceException("物料[" + line.getItemCode() + "]本次出库量(" + qty
                    + ")超过未出库量(" + remain + ")");
        }
    }

    private void accumulateLinePosted(WmProductSalesLine line, BigDecimal qty) {
        BigDecimal posted = line.getQuantityPosted() != null ? line.getQuantityPosted() : BigDecimal.ZERO;
        line.setQuantityPosted(posted.add(qty));
        line.setUpdateTime(DateUtils.getNowDate());
        line.setUpdateBy(SecurityUtils.getUsername());
        wmProductSalesLineMapper.updateWmProductSalesLine(line);
    }

    private void updateHeaderAfterPost(WmProductSales header, BigDecimal postedThisTime) {
        BigDecimal totalPosted = (header.getPostedQuantity() != null ? header.getPostedQuantity() : BigDecimal.ZERO)
                .add(postedThisTime);
        header.setPostedQuantity(totalPosted);
        boolean allPosted = totalPosted.compareTo(
                header.getTotalQuantity() != null ? header.getTotalQuantity() : BigDecimal.ZERO) >= 0;
        header.setStatus(allPosted ? WmProductSalesConstants.STATUS_POSTED
                : WmProductSalesConstants.STATUS_PARTIAL_POSTED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmProductSalesMapper.updateWmProductSales(header);
    }

    /**
     * 过账出库执行（支持跨批次 FIFO）。
     * detail 指定 batchId → 精确批次扣减；未指定 → 按 FIFO 跨批次逐条扣减直至满足 qty。
     * 每扣一个批次生成一条 detail/tx/trace，保持审计粒度。
     */
    private void postOutSingleBatch(WmProductSales header, WmProductSalesDetail d, BigDecimal qty, WmProductSalesLine line) {
        Long wh = d.getWarehouseId() != null ? d.getWarehouseId() : header.getWarehouseId();
        if (d.getBatchId() != null) {
            // 精确批次：单条扣减
            WmMaterialStock stock = loadStockForUpdate(d.getItemId(), d.getBatchId(), wh);
            if (stock == null) {
                throw new ServiceException("物料[" + d.getItemCode() + "]批次[" + d.getBatchId() + "]库存记录不存在，无法出库");
            }
            deductAndRecord(header, d, line, stock, qty);
            return;
        }
        // FIFO 跨批次扣减
        List<WmMaterialStock> fifo = wmMaterialStockMapper.selectAvailableStocksForFifo(
                d.getItemId(), wh, WmProductSalesConstants.QUALITY_NORMAL);
        if (fifo == null || fifo.isEmpty()) {
            throw new ServiceException("物料[" + d.getItemCode() + "]无可用库存，无法出库");
        }
        BigDecimal remaining = qty;
        for (WmMaterialStock stock : fifo) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal onhand = stock.getQuantityOnhand() != null ? stock.getQuantityOnhand() : BigDecimal.ZERO;
            if (onhand.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal take = remaining.min(onhand);
            // 复制 detail 避免多次 insert 共享同一对象状态
            WmProductSalesDetail split = cloneDetail(d);
            deductAndRecord(header, split, line, stock, take);
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException("物料[" + d.getItemCode() + "]库存不足！仍需出库：" + remaining);
        }
    }

    /** 单批次扣减并写 detail/tx/trace（行锁已在 load 时由 for update 取得） */
    private void deductAndRecord(WmProductSales header, WmProductSalesDetail d,
                                 WmProductSalesLine line, WmMaterialStock stock, BigDecimal qty) {
        BigDecimal newOnhand = (stock.getQuantityOnhand() != null ? stock.getQuantityOnhand() : BigDecimal.ZERO)
                .subtract(qty);
        if (newOnhand.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("物料[" + d.getItemCode() + "]批次[" + stock.getBatchCode()
                    + "]库存不足！当前：" + stock.getQuantityOnhand() + "，需出库：" + qty);
        }
        stock.setQuantityOnhand(newOnhand);
        stock.setQuantityAvailable(clampAvailableToOnhand(stock.getQuantityAvailable(), newOnhand));
        stock.setUpdateTime(new Date());
        wmMaterialStockMapper.updateWmMaterialStock(stock);

        d.setDetailId(null);
        d.setSalesId(header.getSalesId());
        d.setMaterialStockId(stock.getMaterialStockId());
        d.setBatchId(stock.getBatchId());
        d.setBatchCode(stock.getBatchCode());
        d.setWarehouseId(stock.getWarehouseId());
        d.setQuantity(qty);
        d.setCreateTime(DateUtils.getNowDate());
        d.setCreateBy(SecurityUtils.getUsername());
        wmProductSalesDetailMapper.insertWmProductSalesDetail(d);

        writeTransactionAndTrace(header, d, line, qty, stock);
    }

    /** 克隆 detail（跨批次拆分时每次 insert 用独立对象） */
    private WmProductSalesDetail cloneDetail(WmProductSalesDetail src) {
        WmProductSalesDetail c = new WmProductSalesDetail();
        c.setSalesId(src.getSalesId());
        c.setLineId(src.getLineId());
        c.setItemId(src.getItemId());
        c.setItemCode(src.getItemCode());
        c.setItemName(src.getItemName());
        c.setSpecification(src.getSpecification());
        c.setUnitOfMeasure(src.getUnitOfMeasure());
        c.setUnitName(src.getUnitName());
        c.setWarehouseId(src.getWarehouseId());
        return c;
    }

    /** 按 itemId+batchId+warehouseId 精确匹配查库存并锁定（vendor=0/workorder=0/quality=NORMAL） */
    private WmMaterialStock loadStockForUpdate(Long itemId, Long batchId, Long warehouseId) {
        WmMaterialStock q = new WmMaterialStock();
        q.setItemId(itemId);
        q.setBatchId(batchId != null ? batchId : 0L);
        q.setWarehouseId(warehouseId);
        q.setVendorId(0L);
        q.setWorkorderId(0L);
        q.setQualityStatus(WmProductSalesConstants.QUALITY_NORMAL);
        return wmMaterialStockMapper.loadMaterialStockForUpdate(q);
    }

    /** 出库后钳制 available ≤ onhand */
    private static BigDecimal clampAvailableToOnhand(BigDecimal oldAvailable, BigDecimal newOnhand) {
        BigDecimal avail = oldAvailable != null ? oldAvailable : BigDecimal.ZERO;
        BigDecimal onh = newOnhand != null ? newOnhand : BigDecimal.ZERO;
        return avail.min(onh);
    }

    private void writeTransactionAndTrace(WmProductSales header, WmProductSalesDetail d,
                                          WmProductSalesLine line, BigDecimal qty, WmMaterialStock stock) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(WmProductSalesConstants.TX_SALES_OUT);
        tx.setSourceDocType(WmProductSalesConstants.SOURCE_SALES_OUT);
        tx.setSourceDocId(header.getSalesId());
        tx.setSourceDocCode(header.getSalesCode());
        tx.setSourceLineId(d.getLineId());
        tx.setMaterialStockId(stock.getMaterialStockId());
        tx.setItemId(d.getItemId());
        tx.setItemCode(d.getItemCode());
        tx.setItemName(d.getItemName());
        tx.setSpecification(d.getSpecification());
        tx.setUnitOfMeasure(d.getUnitOfMeasure());
        tx.setUnitName(d.getUnitName());
        tx.setQuantity(qty.negate());
        tx.setBatchId(d.getBatchId() != null ? d.getBatchId() : 0L);
        tx.setBatchCode(d.getBatchCode());
        tx.setWarehouseId(d.getWarehouseId());
        tx.setClientId(header.getClientId());
        tx.setTransactionTime(new Date());
        tx.setCreateTime(DateUtils.getNowDate());
        tx.setCreateBy(SecurityUtils.getUsername());
        wmTransactionMapper.insertWmTransaction(tx);

        ProMaterialTrace trace = new ProMaterialTrace();
        trace.setTraceType("SALES_OUT");
        trace.setParentType("MATERIAL_STOCK");
        trace.setParentId(stock.getMaterialStockId());
        trace.setChildType("SALES_OUT");
        trace.setChildId(header.getSalesId());
        trace.setQuantity(qty);
        trace.setUnitOfMeasure(d.getUnitOfMeasure());
        trace.setTransactionId(tx.getTransactionId());
        trace.setTraceTime(new Date());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        proMaterialTraceMapper.insertProMaterialTrace(trace);
    }

    // ════════════════════ 发货 ════════════════════

    @Override
    public int ship(Long salesId, WmProductSales info) {
        return lockTemplate.executeWithResult("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doShip(salesId, info)));
    }

    private Integer doShip(Long salesId, WmProductSales info) {
        WmProductSales header = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (header == null) throw new ServiceException("出库单不存在");
        if (!WmProductSalesConstants.isShippable(header.getStatus())) {
            throw new ServiceException("当前状态[" + header.getStatus() + "]不允许发货");
        }
        header.setStatus(WmProductSalesConstants.STATUS_SHIPPED);
        copyShipFields(header, info);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmProductSalesMapper.updateWmProductSales(header);
    }

    private void copyShipFields(WmProductSales target, WmProductSales src) {
        if (src.getLogisticsCompany() != null) target.setLogisticsCompany(src.getLogisticsCompany());
        if (src.getTrackingNo() != null) target.setTrackingNo(src.getTrackingNo());
        if (src.getLogisticsFee() != null) target.setLogisticsFee(src.getLogisticsFee());
        if (src.getShippingAddress() != null) target.setShippingAddress(src.getShippingAddress());
        if (src.getReceiverName() != null) target.setReceiverName(src.getReceiverName());
        if (src.getReceiverTel() != null) target.setReceiverTel(src.getReceiverTel());
    }

    // ════════════════════ 关闭 ════════════════════

    @Override
    public int close(Long salesId) {
        WmProductSales header = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (header == null) throw new ServiceException("出库单不存在");
        if (!WmProductSalesConstants.STATUS_SHIPPED.equals(header.getStatus())) {
            throw new ServiceException("只有已发货状态才能关闭");
        }
        header.setStatus(WmProductSalesConstants.STATUS_CLOSED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmProductSalesMapper.updateWmProductSales(header);
    }

    // ════════════════════ 作废（PARTIAL_POSTED 需回滚库存） ════════════════════

    @Override
    public int cancel(Long salesId) {
        lockTemplate.execute("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doCancel(salesId)));
        return 1;
    }

    private Long doCancel(Long salesId) {
        WmProductSales header = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (header == null) throw new ServiceException("出库单不存在");
        if (WmProductSalesConstants.isTerminal(header.getStatus())) {
            throw new ServiceException("当前状态[" + header.getStatus() + "]不允许作废");
        }
        if (WmProductSalesConstants.STATUS_POSTED.equals(header.getStatus())
                || WmProductSalesConstants.STATUS_SHIPPED.equals(header.getStatus())) {
            throw new ServiceException("已过账/已发货的出库单不能直接作废，请通过销售退货回库");
        }
        // PARTIAL_POSTED 需回滚已扣库存
        if (WmProductSalesConstants.STATUS_PARTIAL_POSTED.equals(header.getStatus())) {
            rollbackPostedStock(header);
        }
        header.setStatus(WmProductSalesConstants.STATUS_CANCELED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmProductSalesMapper.updateWmProductSales(header);
        return salesId;
    }

    /** 作废回滚：遍历已出库明细，逐条恢复 onhand + 写 PRODUCT_SALES 正数冲回 */
    private void rollbackPostedStock(WmProductSales header) {
        List<WmProductSalesDetail> posted = wmProductSalesDetailMapper.selectDetailsBySalesId(header.getSalesId());
        for (WmProductSalesDetail d : posted) {
            if (d.getMaterialStockId() == null || d.getQuantity() == null) continue;
            WmMaterialStock stock = wmMaterialStockMapper.selectWmMaterialStockByMaterialStockId(d.getMaterialStockId());
            if (stock == null) continue;
            BigDecimal newOnhand = (stock.getQuantityOnhand() != null ? stock.getQuantityOnhand() : BigDecimal.ZERO)
                    .add(d.getQuantity());
            // 作废回滚：available 亦需回加（否则被扣的量长期冻结），再钳制 ≤ onhand
            BigDecimal newAvailable = (stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : BigDecimal.ZERO)
                    .add(d.getQuantity());
            stock.setQuantityOnhand(newOnhand);
            stock.setQuantityAvailable(clampAvailableToOnhand(newAvailable, newOnhand));
            stock.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(stock);
            writeRollbackTransaction(header, d, d.getQuantity(), stock);
        }
        // 清零已出库量
        header.setPostedQuantity(BigDecimal.ZERO);
    }

    private void writeRollbackTransaction(WmProductSales header, WmProductSalesDetail d,
                                          BigDecimal qty, WmMaterialStock stock) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(WmProductSalesConstants.TX_SALES_OUT);
        tx.setSourceDocType(WmProductSalesConstants.SOURCE_SALES_OUT);
        tx.setSourceDocId(header.getSalesId());
        tx.setSourceDocCode(header.getSalesCode());
        tx.setSourceLineId(d.getLineId());
        tx.setMaterialStockId(stock.getMaterialStockId());
        tx.setItemId(d.getItemId());
        tx.setItemCode(d.getItemCode());
        tx.setItemName(d.getItemName());
        tx.setUnitOfMeasure(d.getUnitOfMeasure());
        tx.setUnitName(d.getUnitName());
        tx.setQuantity(qty); // 正数 = 回库
        tx.setBatchId(d.getBatchId() != null ? d.getBatchId() : 0L);
        tx.setBatchCode(d.getBatchCode());
        tx.setWarehouseId(d.getWarehouseId());
        tx.setClientId(header.getClientId());
        tx.setTransactionTime(new Date());
        tx.setCreateTime(DateUtils.getNowDate());
        tx.setCreateBy(SecurityUtils.getUsername());
        wmTransactionMapper.insertWmTransaction(tx);
    }

    // ════════════════════ 从销售订单生成 ════════════════════

    @Override
    public WmProductSales buildFromSaleOrder(Long orderId) {
        SalOrder order = salOrderService.getDetail(orderId);
        if (order == null) throw new ServiceException("销售订单不存在");
        WmProductSales draft = new WmProductSales();
        draft.setSalesOrderId(order.getOrderId());
        draft.setSalesOrderCode(order.getOrderCode());
        draft.setClientId(order.getClientId());
        draft.setClientCode(order.getClientCode());
        draft.setClientName(order.getClientName());
        draft.setClientOrderCode(order.getClientOrderCode());
        draft.setSalesperson(order.getSalesperson());
        draft.setStatus(WmProductSalesConstants.STATUS_DRAFT);
        draft.setPostedQuantity(BigDecimal.ZERO);
        draft.setLines(mapOrderLinesToSalesLines(order.getLines()));
        return draft;
    }

    private List<WmProductSalesLine> mapOrderLinesToSalesLines(List<SalOrderLine> orderLines) {
        List<WmProductSalesLine> result = new ArrayList<>();
        if (orderLines == null) return result;
        for (SalOrderLine ol : orderLines) {
            WmProductSalesLine sl = new WmProductSalesLine();
            sl.setSalesOrderLineId(ol.getLineId());
            sl.setItemId(ol.getProductId());
            sl.setItemCode(ol.getProductCode());
            sl.setItemName(ol.getProductName());
            sl.setSpecification(ol.getProductSpc());
            sl.setUnitOfMeasure(ol.getUnitOfMeasure());
            sl.setUnitName(ol.getUnitName());
            sl.setQuantitySales(ol.getQuantity());
            sl.setQuantityPosted(BigDecimal.ZERO);
            result.add(sl);
        }
        return result;
    }

    // ════════════════════ 行保存（头保存时全量替换行） ════════════════════

    private void saveLines(WmProductSales entity) {
        List<WmProductSalesLine> lines = entity.getLines();
        if (lines == null) return;
        WmProductSales existing = wmProductSalesMapper.selectWmProductSalesBySalesId(entity.getSalesId());
        boolean editable = existing == null || WmProductSalesConstants.isEditable(existing.getStatus());
        if (!editable) return;
        // 全量替换：先删旧行（仅草稿态），再插新行
        wmProductSalesLineMapper.deleteWmProductSalesLineBySalesId(entity.getSalesId());
        for (WmProductSalesLine l : lines) {
            l.setLineId(null);
            l.setSalesId(entity.getSalesId());
            // 行的仓库默认从头表继承（warehouse_id NOT NULL）
            if (l.getWarehouseId() == null) l.setWarehouseId(entity.getWarehouseId());
            if (l.getQuantityPosted() == null) l.setQuantityPosted(BigDecimal.ZERO);
            l.setCreateTime(DateUtils.getNowDate());
            l.setCreateBy(SecurityUtils.getUsername());
            wmProductSalesLineMapper.insertWmProductSalesLine(l);
        }
        recalcTotals(entity);
    }

    private void recalcTotals(WmProductSales entity) {
        if (entity.getLines() == null) return;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmProductSalesLine l : entity.getLines()) {
            if (l.getQuantitySales() != null) totalQty = totalQty.add(l.getQuantitySales());
        }
        entity.setTotalQuantity(totalQty);
        entity.setUpdateTime(DateUtils.getNowDate());
        wmProductSalesMapper.updateWmProductSales(entity);
    }
}
