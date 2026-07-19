package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.domain.mes.wm.RtVendorFromPurOrderRequest;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;
import com.ruoyi.system.domain.mes.wm.WmRtVendorLine;
import com.ruoyi.system.domain.mes.wm.tx.RtVendorTxBean;
import com.ruoyi.system.domain.mes.wm.vo.ReturnableBatchVO;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmRtVendorMapper;
import com.ruoyi.system.service.mes.wm.IWmRtVendorLineService;
import com.ruoyi.system.service.mes.wm.IWmRtVendorService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;
import jakarta.annotation.PostConstruct;

@Service
public class WmRtVendorServiceImpl implements IWmRtVendorService
{
    private static final String LOCK_PREFIX = "rt_vendor:po:create:";

    @Autowired
    private WmRtVendorMapper wmRtVendorMapper;

    @Autowired
    private IWmRtVendorLineService wmRtVendorLineService;

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @Autowired
    private PurOrderLineMapper purOrderLineMapper;

    @Autowired
    private PurOrderMapper purOrderMapper;

    @Autowired
    private WmItemRecptLineMapper wmItemRecptLineMapper;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public List<WmRtVendor> selectWmRtVendorList(WmRtVendor entity) {
        return wmRtVendorMapper.selectWmRtVendorList(entity);
    }

    @Override
    public List<WmRtVendor> selectWmRtVendorAll() {
        return wmRtVendorMapper.selectWmRtVendorAll();
    }

    @Override
    public WmRtVendor selectWmRtVendorByRtId(Long rtId) {
        return wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
    }

    @Override
    @Transactional
    public int insertWmRtVendor(WmRtVendor entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorMapper.insertWmRtVendor(entity);
    }

    @Override
    @Transactional
    public int updateWmRtVendor(WmRtVendor entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorMapper.updateWmRtVendor(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorByRtId(Long rtId) {
        return wmRtVendorMapper.deleteWmRtVendorByRtId(rtId);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorByRtIds(Long[] rtIds) {
        return wmRtVendorMapper.deleteWmRtVendorByRtIds(rtIds);
    }

    /**
     * 确认退货单（DRAFT -> CONFIRMED）
     * 执行库存扣减（processRtVendor，内部 Redisson 锁+幂等）
     */
    @Override
    @Transactional
    public void confirmRtVendor(Long rtId) {
        WmRtVendor header = wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
        if (header == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new RuntimeException("仅草稿状态的退货单可确认，当前状态：" + header.getStatus());
        }
        WmRtVendorLine queryLine = new WmRtVendorLine();
        queryLine.setRtId(rtId);
        List<WmRtVendorLine> lines = wmRtVendorLineService.selectWmRtVendorLineList(queryLine);
        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("没有退货行，无法确认");
        }
        doConfirmRtVendor(header, lines);
    }

    /**
     * 确认核心逻辑（参数由调用者提供，避免重复 DB 查询）
     */
    private void doConfirmRtVendor(WmRtVendor header, List<WmRtVendorLine> lines) {
        // 1. 构建 TX beans
        List<RtVendorTxBean> txBeans = new ArrayList<>();
        for (WmRtVendorLine line : lines) {
            RtVendorTxBean bean = new RtVendorTxBean();
            bean.setSourceDocType("wm_rt_vendor");
            bean.setSourceDocId(header.getRtId());
            bean.setSourceDocCode(header.getRtCode());
            bean.setSourceDocLineId(line.getLineId());
            bean.setItemId(line.getItemId());
            bean.setItemCode(line.getItemCode());
            bean.setItemName(line.getItemName());
            bean.setSpecification(line.getSpecification());
            bean.setUnitOfMeasure(line.getUnitOfMeasure());
            bean.setUnitName(line.getUnitName());
            BigDecimal qty = line.getQuantityRt() != null ? line.getQuantityRt() : BigDecimal.ZERO;
            bean.setTransactionQuantity(qty);
            bean.setBatchId(line.getBatchId());
            bean.setBatchCode(line.getBatchCode());
            bean.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            bean.setWarehouseCode(line.getWarehouseCode() != null ? line.getWarehouseCode() : header.getWarehouseCode());
            bean.setWarehouseName(line.getWarehouseName() != null ? line.getWarehouseName() : header.getWarehouseName());
            bean.setLocationId(line.getLocationId());
            bean.setAreaId(line.getAreaId());
            bean.setVendorId(header.getVendorId());
            bean.setVendorCode(header.getVendorCode());
            bean.setVendorName(header.getVendorName());
            txBeans.add(bean);
        }
        // 2. 扣减库存（库存-1，内部 Redisson 锁+幂等检查）
        storageCoreService.processRtVendor(txBeans);
        // 3. 更新头状态 -> CONFIRMED
        header.setStatus("CONFIRMED");
        header.setConfirmTime(DateUtils.getNowDate());
        header.setConfirmBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmRtVendorMapper.updateWmRtVendor(header);
    }

    /**
     * 过账退货单（CONFIRMED -> POSTED）
     * 回写采购订单行已退货数量（quantity_returned 原子递增）
     */
    @Override
    @Transactional
    public void postRtVendor(Long rtId) {
        WmRtVendor header = wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
        if (header == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (!"CONFIRMED".equals(header.getStatus())) {
            throw new RuntimeException("仅已确认的退货单可过账，当前状态：" + header.getStatus());
        }
        // 更新头 -> POSTED
        header.setStatus("POSTED");
        header.setPostTime(DateUtils.getNowDate());
        header.setPostBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmRtVendorMapper.updateWmRtVendor(header);
        // 回写采购订单行已退货数量
        writebackPoOnRtVendorPost(header);
    }

    /**
     * 回写采购订单：原子递增 quantity_returned
     * 优先用退货行的 purOrderLineId 精确匹配 PO 行(V78 新字段);
     * 历史数据无该字段时 fallback 到 itemId 匹配(依赖 V49 uk_order_item 唯一约束)。
     */
    private void writebackPoOnRtVendorPost(WmRtVendor header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        // 加载 PO 行列表，构建 lineId -> PO 行 / itemId -> PO 行 两个 Map
        PurOrderLine query = new PurOrderLine();
        query.setOrderId(purOrderId);
        List<PurOrderLine> poLines = purOrderLineMapper.selectPurOrderLineList(query);
        if (poLines == null || poLines.isEmpty()) return;
        Map<Long, PurOrderLine> poLineByLineId = poLines.stream()
            .collect(Collectors.toMap(PurOrderLine::getLineId, Function.identity(), (a, b) -> a));
        Map<Long, PurOrderLine> poLineByItemId = poLines.stream()
            .collect(Collectors.toMap(PurOrderLine::getItemId, Function.identity(), (a, b) -> a));

        // 加载退货行
        WmRtVendorLine queryLine = new WmRtVendorLine();
        queryLine.setRtId(header.getRtId());
        List<WmRtVendorLine> rtLines = wmRtVendorLineService.selectWmRtVendorLineList(queryLine);
        if (rtLines == null || rtLines.isEmpty()) return;

        // 逐行原子递增 quantity_returned,lineId 优先 + itemId fallback
        for (WmRtVendorLine rtLine : rtLines) {
            BigDecimal qty = rtLine.getQuantityRt() != null ? rtLine.getQuantityRt() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            PurOrderLine poLine = null;
            if (rtLine.getPurOrderLineId() != null) {
                poLine = poLineByLineId.get(rtLine.getPurOrderLineId());
            }
            if (poLine == null && rtLine.getItemId() != null) {
                poLine = poLineByItemId.get(rtLine.getItemId());
            }
            if (poLine != null) {
                purOrderLineMapper.addQuantityReturned(poLine.getLineId(), qty);
            }
        }
    }

    /**
     * 从采购订单生成退货单(DRAFT)
     * 数据源是"可退入库批次"（按 item+warehouse+batch 聚合），不是 PO 行。
     * 仓库下沉到行：每个退货行带自己的仓库/批次（来自入库批次）。
     * 校验：按 (itemId, warehouseId, COALESCE(batchId,0)) 匹配可退批次，数量 ≤ 可退量。
     * 并发：先锁后事务(Redisson key=rt_vendor:po:create:{purOrderId}),
     *      防止两人同时建同 PO 的 DRAFT 单导致可退量校验 TOCTOU 超退。
     */
    @Override
    public WmRtVendor createRtVendorFromPurOrder(RtVendorFromPurOrderRequest req) {
        validateFromPurOrder(req);
        String lockKey = LOCK_PREFIX + req.getPurOrderId();
        return lockTemplate.executeWithResult(lockKey, 5,
                () -> txTemplate.execute(status -> doCreateRtVendorFromPurOrder(req)));
    }

    private WmRtVendor doCreateRtVendorFromPurOrder(RtVendorFromPurOrderRequest req) {
        PurOrderVO po = purOrderMapper.selectPurOrderByOrderId(req.getPurOrderId());
        if (po == null) {
            throw new ServiceException("采购订单不存在");
        }
        if ("CANCEL".equals(po.getStatus()) || "CLOSED".equals(po.getStatus())) {
            throw new ServiceException("采购订单状态为[" + po.getStatus() + "]，不允许生成退货单");
        }

        // 查可退批次聚合,构建 (itemId, warehouseId, batchKey) -> ReturnableBatchVO Map
        List<ReturnableBatchVO> returnableBatches = wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(req.getPurOrderId());
        if (returnableBatches == null || returnableBatches.isEmpty()) {
            throw new ServiceException("该采购订单没有可退的入库批次");
        }
        Map<String, ReturnableBatchVO> batchMap = new HashMap<>();
        for (ReturnableBatchVO b : returnableBatches) {
            batchMap.put(batchKey(b.getItemId(), b.getWarehouseId(), b.getBatchId()), b);
        }

        // 校验每个退货行的可退量并构建 WmRtVendorLine。
        // 注意:同一请求内可能有多条 dto 落同一 (item, warehouse, batch),必须累加占用量校验,
        // 否则单条都 ≤ returnable 但累计会超退。SQL 已扣减其他 DRAFT 单的占用,本循环只防本单内的累加。
        List<WmRtVendorLine> linesToInsert = new ArrayList<>();
        Map<String, BigDecimal> usedInThisReq = new HashMap<>();
        for (RtVendorFromPurOrderRequest.RtVendorLineDto dto : req.getLines()) {
            String key = batchKey(dto.getItemId(), dto.getWarehouseId(), dto.getBatchId());
            ReturnableBatchVO batch = batchMap.get(key);
            if (batch == null) {
                throw new ServiceException(String.format(
                    "物料[%s]在指定仓库/批次下没有可退批次", dto.getItemId()));
            }
            BigDecimal used = usedInThisReq.getOrDefault(key, BigDecimal.ZERO).add(dto.getQuantityRt());
            usedInThisReq.put(key, used);
            if (used.compareTo(batch.getQuantityReturnable()) > 0) {
                throw new ServiceException(String.format(
                    "物料[%s]退货数量累计(%s)超过可退量(%s=入库%s-已退/草稿占用%s)",
                    batch.getItemCode(), used, batch.getQuantityReturnable(),
                    batch.getQuantityRecptTotal(), batch.getQuantityReturned()));
            }
            linesToInsert.add(buildRtLineFromBatch(batch, dto));
        }

        // 构建头表:仓库取第一个退货行的仓库（兼容 doConfirmRtVendor 的 fallback 逻辑）
        WmRtVendorLine firstLine = linesToInsert.get(0);
        WmRtVendor header = new WmRtVendor();
        header.setRtCode(req.getRtCode());
        header.setRtName(StringUtils.isNotEmpty(req.getRtName()) ? req.getRtName() : "退货-" + po.getOrderCode());
        header.setVendorId(po.getVendorId());
        header.setVendorCode(po.getVendorCode());
        header.setVendorName(po.getVendorName());
        header.setWarehouseId(firstLine.getWarehouseId());
        header.setWarehouseCode(firstLine.getWarehouseCode());
        header.setWarehouseName(firstLine.getWarehouseName());
        header.setRtDate(DateUtils.getNowDate());
        header.setStatus("DRAFT");
        header.setPurOrderId(po.getOrderId());
        header.setPurOrderCode(po.getOrderCode());
        header.setRemark(req.getRemark());
        header.setCreateBy(SecurityUtils.getUsername());
        header.setCreateTime(DateUtils.getNowDate());
        wmRtVendorMapper.insertWmRtVendor(header);

        // 插入退货行
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmRtVendorLine line : linesToInsert) {
            line.setRtId(header.getRtId());
            line.setCreateBy(SecurityUtils.getUsername());
            line.setCreateTime(DateUtils.getNowDate());
            wmRtVendorLineService.insertWmRtVendorLine(line);
            totalQty = totalQty.add(line.getQuantityRt());
        }
        header.setTotalQuantity(totalQty);
        wmRtVendorMapper.updateWmRtVendor(header);
        return header;
    }

    @Override
    public List<ReturnableBatchVO> selectReturnableBatches(Long purOrderId) {
        if (purOrderId == null) {
            return new ArrayList<>();
        }
        return wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(purOrderId);
    }

    private void validateFromPurOrder(RtVendorFromPurOrderRequest req) {
        if (req.getPurOrderId() == null) {
            throw new ServiceException("采购订单ID不能为空");
        }
        if (StringUtils.isEmpty(req.getRtCode())) {
            throw new ServiceException("退货单编码不能为空");
        }
        if (req.getLines() == null || req.getLines().isEmpty()) {
            throw new ServiceException("退货行不能为空");
        }
        for (RtVendorFromPurOrderRequest.RtVendorLineDto dto : req.getLines()) {
            if (dto.getItemId() == null) {
                throw new ServiceException("退货行缺少物料ID");
            }
            if (dto.getWarehouseId() == null) {
                throw new ServiceException("退货行缺少仓库ID");
            }
            if (dto.getQuantityRt() == null || dto.getQuantityRt().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("退货数量必须大于0");
            }
        }
    }

    /** (itemId, warehouseId, COALESCE(batchId,0)) 三元组的归一化 key，null 批次归一为 0 */
    private String batchKey(Long itemId, Long warehouseId, Long batchId) {
        long bk = batchId == null ? 0L : batchId;
        return itemId + ":" + warehouseId + ":" + bk;
    }

    /** 从可退批次构建退货行(物料字段后端回填,仓库/批次/PO行引用取请求) */
    private WmRtVendorLine buildRtLineFromBatch(ReturnableBatchVO batch, RtVendorFromPurOrderRequest.RtVendorLineDto dto) {
        WmRtVendorLine line = new WmRtVendorLine();
        line.setItemId(batch.getItemId());
        line.setItemCode(batch.getItemCode());
        line.setItemName(batch.getItemName());
        line.setSpecification(batch.getSpecification());
        line.setUnitOfMeasure(batch.getUnitOfMeasure());
        line.setUnitName(batch.getUnitName());
        line.setQuantityRt(dto.getQuantityRt());
        line.setBatchId(dto.getBatchId());
        line.setBatchCode(dto.getBatchCode());
        line.setWarehouseId(dto.getWarehouseId());
        line.setWarehouseCode(dto.getWarehouseCode());
        line.setWarehouseName(dto.getWarehouseName());
        line.setPurOrderLineId(dto.getPurOrderLineId() != null ? dto.getPurOrderLineId() : batch.getPurOrderLineId());
        line.setPurOrderLineNo(dto.getPurOrderLineId() != null ? String.valueOf(dto.getPurOrderLineId())
            : (batch.getPurOrderLineId() != null ? String.valueOf(batch.getPurOrderLineId()) : null));
        line.setQuantityOrdered(batch.getQuantityRecptTotal());
        return line;
    }
}
