package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.service.mes.wm.IWmTransactionService;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 库存事务Service（先锁后事务：Redisson 分布式锁 → TransactionTemplate 显式开事务）
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
@Service
public class WmTransactionServiceImpl implements IWmTransactionService
{
    @Autowired
    private WmTransactionMapper wmTransactionMapper;

    @Autowired
    private WmMaterialStockMapper wmMaterialStockMapper;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 缓存单例，线程安全 — 锁内事务超时 30s，防止 DB 死锁时长时间阻塞 */
    private TransactionTemplate txTemplate;

    private static final String LOCK_PREFIX = "wm:stock:lock:";

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);  // DB 事务最长 30s
    }

    /**
     * 处理库存事务（先锁后事务 — 锁内显式开 TX，确保快照在锁之后）
     *
     * 顺序：validate → initStock → 🔒拿锁 → BEGIN TX → stock UPSERT → insert tx → COMMIT → 🔓放锁
     */
    @Override
    public WmTransaction processTransaction(WmTransaction transaction) {
        validate(transaction);
        WmMaterialStock stock = initStock(transaction);
        return lockTemplate.execute(buildLockKey(stock),
                () -> txTemplate.execute(status -> doProcessTransaction(transaction, stock)));
    }

    private WmTransaction doProcessTransaction(WmTransaction transaction, WmMaterialStock stock) {
        // 幂等检查：同一来源单据行已处理过 → 直接返回已有记录
        WmTransaction existingTx = wmTransactionMapper.selectExistingTransaction(transaction);
        if (existingTx != null) {
            return existingTx;
        }

        WmMaterialStock existing = wmMaterialStockMapper.loadMaterialStockForUpdate(stock);
        BigDecimal delta = transaction.getQuantity();

        if (StringUtils.isNotNull(existing)) {
            BigDecimal result = existing.getQuantityOnhand().add(delta);
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("库存数量不足！当前库存：" + existing.getQuantityOnhand()
                        + "，变动：" + delta);
            }
            stock.setQuantityOnhand(result);
            // 入库时同步增加可用库存，出库时不减（由领料 confirm 时扣减）
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                stock.setQuantityAvailable(
                    (existing.getQuantityAvailable() != null ? existing.getQuantityAvailable() : BigDecimal.ZERO)
                        .add(delta));
            }
            stock.setMaterialStockId(existing.getMaterialStockId());
            stock.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(stock);
        } else {
            if (delta.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("库存记录不存在，不能执行出库操作");
            }
            stock.setQuantityOnhand(delta);
            // 新建库存记录时，入库数量即为可用库存
            stock.setQuantityAvailable(delta);
            stock.setCreateTime(new Date());
            stock.setUpdateTime(new Date());
            wmMaterialStockMapper.insertWmMaterialStock(stock);
        }

        transaction.setMaterialStockId(stock.getMaterialStockId());
        transaction.setCreateTime(DateUtils.getNowDate());
        wmTransactionMapper.insertWmTransaction(transaction);
        return transaction;
    }

    private String buildLockKey(WmMaterialStock stock) {
        return LOCK_PREFIX + stock.getItemId() + ":" + stock.getBatchId() + ":"
                + stock.getWarehouseId() + ":" + stock.getVendorId() + ":"
                + stock.getWorkorderId() + ":" + stock.getQualityStatus();
    }

    private void validate(WmTransaction tx) {
        if (StringUtils.isNull(tx.getTransactionType())) {
            throw new ServiceException("库存事务类型不能为空");
        }
        if (StringUtils.isNull(tx.getQuantity())) {
            throw new ServiceException("事务数量不能为空");
        }
        if (StringUtils.isNull(tx.getSourceDocCode())) {
            throw new ServiceException("来源单据号不能为空");
        }
        if (StringUtils.isNull(tx.getTransactionTime())) {
            tx.setTransactionTime(new Date());
        }
    }

    private WmMaterialStock initStock(WmTransaction tx) {
        WmMaterialStock stock = new WmMaterialStock();
        stock.setItemId(tx.getItemId());
        stock.setItemCode(tx.getItemCode());
        stock.setItemName(tx.getItemName());
        stock.setSpecification(tx.getSpecification());
        stock.setUnitOfMeasure(tx.getUnitOfMeasure());
        stock.setUnitName(tx.getUnitName());
        stock.setBatchId(tx.getBatchId() != null ? tx.getBatchId() : 0L);
        stock.setBatchCode(tx.getBatchCode());
        stock.setWarehouseId(tx.getWarehouseId());
        stock.setWarehouseCode(tx.getWarehouseCode());
        stock.setWarehouseName(tx.getWarehouseName());
        stock.setLocationId(tx.getLocationId());
        if (StringUtils.isNotNull(tx.getAreaId())) {
            stock.setAreaId(tx.getAreaId());
        }
        stock.setVendorId(tx.getVendorId() != null ? tx.getVendorId() : 0L);
        stock.setWorkorderId(tx.getWorkorderId() != null ? tx.getWorkorderId() : 0L);
        stock.setQualityStatus("NORMAL");
        return stock;
    }

    // ── 标准 CRUD ──

    @Override
    public List<WmTransaction> selectWmTransactionList(WmTransaction entity) {
        return wmTransactionMapper.selectWmTransactionList(entity);
    }

    @Override
    public List<WmTransaction> selectWmTransactionAll() {
        return wmTransactionMapper.selectWmTransactionAll();
    }

    @Override
    public WmTransaction selectWmTransactionByTransactionId(Long transactionId) {
        return wmTransactionMapper.selectWmTransactionByTransactionId(transactionId);
    }

    @Override
    @Transactional
    public int insertWmTransaction(WmTransaction entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmTransactionMapper.insertWmTransaction(entity);
    }

    @Override
    @Transactional
    public int updateWmTransaction(WmTransaction entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmTransactionMapper.updateWmTransaction(entity);
    }

    @Override
    @Transactional
    public int deleteWmTransactionByTransactionId(Long transactionId) {
        return wmTransactionMapper.deleteWmTransactionByTransactionId(transactionId);
    }

    @Override
    @Transactional
    public int deleteWmTransactionByTransactionIds(Long[] transactionIds) {
        return wmTransactionMapper.deleteWmTransactionByTransactionIds(transactionIds);
    }
}
