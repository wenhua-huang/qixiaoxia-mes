package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.enums.PurOrderStatus;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.wm.ItemRecptReceiveBody;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.domain.mes.wm.tx.ItemRecptTxBean;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.IQcGateService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmBatchService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WmItemRecptServiceImpl implements IWmItemRecptService
{
    @Autowired
    private WmItemRecptMapper wmItemRecptMapper;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IPurOrderService purOrderService;

    @Autowired
    private IPurOrderLineService purOrderLineService;

    @Autowired
    private PurOrderLineMapper purOrderLineMapper;

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @Autowired
    private IWmBatchService wmBatchService;

    @Autowired
    private ProMaterialTraceMapper proMaterialTraceMapper;

    @Autowired
    private WmTransactionMapper wmTransactionMapper;

    @Autowired
    private IWmWarehouseService wmWarehouseService;

    /** 质检生成工厂：入库单创建后生成 IQC 待检单（内部锁+事务，幂等） */
    @Autowired
    private IQcFactoryService qcFactoryService;

    /** 质检拦截门：confirm 即增库存，需检物料的检验必须前置 */
    @Autowired
    private IQcGateService qcGateService;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    private static final Logger log = LoggerFactory.getLogger(WmItemRecptServiceImpl.class);

    /** 入库单号编码规则（RCVT+yyyyMMdd+3位流水，按日循环） */
    private static final String CODE_RULE_RECEIPT = "RECEIPT_NO";
    /** 按单号加锁，防双击/并发重复提交 */
    private static final String LOCK_PREFIX = "wm:itemrecpt:";
    private static final long LOCK_WAIT_SEC = 10;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity) {
        return wmItemRecptMapper.selectWmItemRecptList(entity);
    }

    @Override
    public List<WmItemRecpt> selectWmItemRecptAll() {
        return wmItemRecptMapper.selectWmItemRecptAll();
    }

    @Override
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId) {
        return wmItemRecptMapper.selectWmItemRecptByRecptId(recptId);
    }

    @Override
    public WmItemRecpt selectWmItemRecptDetail(Long recptId) {
        WmItemRecpt header = wmItemRecptMapper.selectWmItemRecptByRecptId(recptId);
        if (header == null) {
            return null;
        }
        header.setLines(loadRecptLines(recptId));
        return header;
    }

    @Override
    public int insertWmItemRecpt(WmItemRecpt entity) {
        // recptCode 为空时服务端兜底生成（前端已取号，此处防御取号失败/未取号）；
        // 按单号加 Redisson 锁串行化同号创建，锁内查重+insert，杜绝双击/并发重复落库。
        String code = ensureRecptCode(entity);
        lockTemplate.execute(LOCK_PREFIX + code, LOCK_WAIT_SEC,
                () -> txTemplate.execute(status -> doInsertWmItemRecpt(entity)));
        return 1;
    }

    private int doInsertWmItemRecpt(WmItemRecpt entity) {
        assertRecptCodeNotExists(entity.getRecptCode());
        entity.setCreateTime(DateUtils.getNowDate());
        entity.setCreateBy(SecurityUtils.getUsername());
        if (entity.getStatus() == null || entity.getStatus().isEmpty()) {
            entity.setStatus("DRAFT");
        }
        wmItemRecptMapper.insertWmItemRecpt(entity);
        // 携带行时一次性插入（从采购订单生成场景）；无行时仅插头（向后兼容原新增流程）
        if (entity.getLines() != null && !entity.getLines().isEmpty()) {
            saveRecptLines(entity, entity.getLines());
            // IQC 生成 hook：PC 手工新增/从采购订单生成的入库单头+行落库后生成来料待检单
            qcFactoryService.generateIqcForItemRecpt(entity, entity.getLines());
        }
        return 1;
    }

    @Override
    @Transactional
    public int updateWmItemRecpt(WmItemRecpt entity) {
        // 先读 current 做状态守卫，再改 header（confirm/post 同款顺序）；
        // 否则 mapper 动态 <set> 会先把 status 写成客户端传入的值，之后再读就是"自己写的 DRAFT"，
        // 校验穿透 → 已过账单据可被回滚为 DRAFT 并清空行。
        if (entity.getRecptId() == null) {
            throw new ServiceException("入库单主键不能为空");
        }
        WmItemRecpt current = wmItemRecptMapper.selectWmItemRecptByRecptId(entity.getRecptId());
        if (current == null) {
            throw new ServiceException("入库单不存在");
        }
        // 状态、创建人、创建时间不允许通过 edit 接口改写；由生命周期方法(confirm/post/cancel)统一变更
        entity.setStatus(current.getStatus());
        entity.setCreateBy(current.getCreateBy());
        entity.setCreateTime(current.getCreateTime());
        entity.setUpdateTime(DateUtils.getNowDate());
        // 携带行时全量重建（仅 DRAFT 允许改行；非 DRAFT 直接拒绝改行意图）
        boolean editLines = entity.getLines() != null;
        if (editLines && !"DRAFT".equals(current.getStatus())) {
            throw new ServiceException("仅草稿状态可编辑物料行");
        }
        int rows = wmItemRecptMapper.updateWmItemRecpt(entity);
        if (editLines) {
            wmItemRecptLineService.deleteWmItemRecptLineByRecptId(entity.getRecptId());
            if (!entity.getLines().isEmpty()) {
                saveRecptLines(entity, entity.getLines());
            }
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteWmItemRecptByRecptId(Long recptId) {
        return wmItemRecptMapper.deleteWmItemRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptByRecptIds(Long[] recptIds) {
        return wmItemRecptMapper.deleteWmItemRecptByRecptIds(recptIds);
    }

    /**
     * 确认收货（DRAFT → CONFIRMED）— 库存更新 + PO回写在同一事务中。
     *
     * 注：本方法使用 @Transactional；库存变更内部通过 WmTransactionServiceImpl.processTransaction()
     * 使用 Redisson 锁 + TransactionTemplate 保证并发安全。外层事务用于保证 header/lines 读写的原子性。
     */
    @Override
    @Transactional
    public void confirmItemRecpt(Long recptId) {
        WmItemRecpt header = selectWmItemRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new RuntimeException("仅草稿状态可确认收货");
        }

        List<WmItemRecptLine> lines = loadRecptLines(recptId);
        if (lines.isEmpty()) {
            throw new RuntimeException("没有入库行，无法确认");
        }

        doConfirmItemRecpt(header, lines);
    }

    /** 确认收货核心逻辑（不入库查询，由调用方传入已加载的 header + lines） */
    private void doConfirmItemRecpt(WmItemRecpt header, List<WmItemRecptLine> lines) {
        // IQC 拦截 hook：confirm 即增库存，检验必须前置（需检物料必须有 COMPLETED+PASS/CONCESSION 检验单，
        // 否则抛 ServiceException 阻断；confirmItemRecpt 与 receiveWithLines 双入口均经此覆盖）
        qcGateService.assertItemRecptConfirmable(header, lines);

        Long recptId = header.getRecptId();
        List<ItemRecptTxBean> txBeans = new ArrayList<>();
        for (WmItemRecptLine line : lines) {
            ItemRecptTxBean b = new ItemRecptTxBean();
            b.setSourceDocType("wm_item_recpt");
            b.setSourceDocId(recptId);
            b.setSourceDocCode(header.getRecptCode());
            b.setSourceDocLineId(line.getLineId());
            b.setItemId(line.getItemId());
            b.setItemCode(line.getItemCode());
            b.setItemName(line.getItemName());
            b.setSpecification(line.getSpecification());
            b.setUnitOfMeasure(line.getUnitOfMeasure());
            b.setUnitName(line.getUnitName());
            b.setTransactionQuantity(line.getQuantityRecpt());
            b.setBatchId(line.getBatchId());
            b.setBatchCode(line.getBatchCode());
            b.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            b.setWarehouseCode(line.getWarehouseCode());
            b.setWarehouseName(line.getWarehouseName());
            b.setLocationId(line.getLocationId());
            b.setAreaId(line.getAreaId());
            b.setVendorId(header.getVendorId());
            b.setVendorCode(header.getVendorCode());
            txBeans.add(b);
        }

        // 1. 更新库存（内部使用 Redisson 锁 + TransactionTemplate）
        storageCoreService.processItemRecpt(txBeans);

        // 2. 写入物料追溯 RECEIPT（采购入库→原料库存，追溯链起点）
        for (WmItemRecptLine line : lines) {
            try {
                writeReceiptTrace(header, line);
            } catch (Exception e) {
                log.error("入库追溯写入失败, recptId={}, lineId={}", recptId, line.getLineId(), e);
            }
        }

        // 3. 确认收货状态
        header.setStatus("CONFIRMED");
        header.setUpdateTime(DateUtils.getNowDate());
        wmItemRecptMapper.updateWmItemRecpt(header);

        // 4. 回写 PO（采购入库时）
        writebackPoOnConfirm(header);
    }

    /**
     * 写入采购入库 RECEIPT 追溯记录（原料从哪里来 — 追溯链起点）。
     * parent = 采购订单(有PO时) 或 供应商(无PO时)，child = 库存记录(MATERIAL_STOCK)。
     */
    private void writeReceiptTrace(WmItemRecpt header, WmItemRecptLine line) {
        // 查刚写入的 wm_transaction 获取 materialStockId + transactionId
        WmTransaction txQ = new WmTransaction();
        txQ.setSourceDocType("wm_item_recpt");
        txQ.setSourceDocId(header.getRecptId());
        txQ.setSourceLineId(line.getLineId());
        List<WmTransaction> txs = wmTransactionMapper.selectWmTransactionList(txQ);
        if (txs == null || txs.isEmpty()) {
            log.warn("入库追溯-未找到对应transaction, recptId={}, lineId={}", header.getRecptId(), line.getLineId());
            return;
        }
        WmTransaction tx = txs.get(0);
        if (tx.getMaterialStockId() == null) return;

        // 确定 trace_type 和 parent 节点
        ProMaterialTrace trace = new ProMaterialTrace();
        boolean isOutsource = "OUTSOURCE".equals(header.getRecptType());
        trace.setTraceType(isOutsource ? "OUTSOURCE_RECPT" : "RECEIPT");
        if (isOutsource) {
            // 外协入库：parent = VENDOR（加工完从供应商拉回）
            trace.setParentType("VENDOR");
            trace.setParentId(header.getVendorId() != null ? header.getVendorId() : 0L);
        } else if (header.getPurOrderId() != null && header.getPurOrderId() > 0) {
            trace.setParentType("PUR_ORDER");
            trace.setParentId(header.getPurOrderId());
        } else if (header.getVendorId() != null && header.getVendorId() > 0) {
            trace.setParentType("VENDOR");
            trace.setParentId(header.getVendorId());
        } else {
            trace.setParentType("NONE");
            trace.setParentId(0L);
        }
        trace.setChildType("MATERIAL_STOCK");
        trace.setChildId(tx.getMaterialStockId());
        trace.setQuantity(line.getQuantityRecpt());
        trace.setUnitOfMeasure(line.getUnitOfMeasure());
        trace.setVendorId(header.getVendorId());
        trace.setTransactionId(tx.getTransactionId());
        trace.setTraceTime(new Date());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        proMaterialTraceMapper.insertProMaterialTrace(trace);
    }

    /**
     * 过账入库（CONFIRMED → POSTED）
     * 回写 PO 行已收数量 + 判断 PO 是否全部收完。
     *
     * 注：本方法使用 @Transactional；库存数据已在 confirmItemRecpt 阶段通过 Redisson 锁更新完毕，
     * 此处仅做状态变更 + PO 回写，不涉及库存变更。
     */
    @Override
    @Transactional
    public void postItemRecpt(Long recptId) {
        WmItemRecpt header = selectWmItemRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"CONFIRMED".equals(header.getStatus())) {
            throw new RuntimeException("仅已确认的入库单可过账");
        }
        doPostItemRecpt(header);
    }

    /** 过账入库核心逻辑（不入库查询，由调用方传入已加载的 header） */
    private void doPostItemRecpt(WmItemRecpt header) {
        // 过账入库
        header.setStatus("POSTED");
        header.setUpdateTime(DateUtils.getNowDate());
        wmItemRecptMapper.updateWmItemRecpt(header);
        // 回写 PO 已收数量（采购入库时）
        if (header.getPurOrderId() != null && header.getPurOrderId() > 0) {
            writebackPoOnPost(header);
        }
    }

    /**
     * 到货登记（移动端）：创建 DRAFT 入库单头+行，生成批次号与 IQC 待检单，不增库存、不过账。
     *
     * <p>语义对齐 PC 端 insertWmItemRecpt：app 只做"货到登记"，IQC 判定、确认入库（confirm 增库存+推 PO→RECEIVING）、
     * 过账（post 累加已收量）全部在 PC 端完成。这样需检物料登记后生成的 PENDING 检验单不会被同一事务内的 confirm 自相矛盾地拒绝。
     * PO 状态在登记阶段保持 ORDERED，支持分批到货多次登记。
     */
    @Override
    public WmItemRecpt receiveWithLines(ItemRecptReceiveBody body) {
        WmItemRecpt header = body.getHeader();
        List<WmItemRecptLine> lines = body.getLines();
        if (header == null) throw new ServiceException("入库单头信息不能为空");
        if (lines == null || lines.isEmpty()) throw new ServiceException("入库单行不能为空");
        // recptCode 为空时服务端兜底生成（移动端常传 RCP- 临时号，为空则走编码规则）；
        // 按单号加锁串行化，锁内查重+insert，防重复登记。
        String code = ensureRecptCode(header);
        Long recptId = lockTemplate.executeWithResult(LOCK_PREFIX + code, LOCK_WAIT_SEC,
                () -> txTemplate.execute(status -> doReceiveWithLines(header, lines)));
        // 事务提交、锁释放后回读详情（头+行批次码），供 App 登记完成页展示
        return selectWmItemRecptDetail(recptId);
    }

    private Long doReceiveWithLines(WmItemRecpt header, List<WmItemRecptLine> lines) {
        assertRecptCodeNotExists(header.getRecptCode());
        // 1. 创建入库单头 — 若 header 未指定仓库，取第一行仓库
        if (header.getWarehouseId() == null) {
            WmItemRecptLine firstLine = lines.get(0);
            header.setWarehouseId(firstLine.getWarehouseId());
            header.setWarehouseCode(firstLine.getWarehouseCode());
            header.setWarehouseName(firstLine.getWarehouseName());
        }
        // 强制设为草稿，防止客户端传入非 DRAFT 状态
        header.setStatus("DRAFT");
        header.setCreateTime(DateUtils.getNowDate());
        header.setCreateBy(SecurityUtils.getUsername());
        wmItemRecptMapper.insertWmItemRecpt(header);

        // 2. 创建入库单行（公共逻辑，与 PC 端从采购订单生成共享）：回填 PO 行、自动生成批次号
        saveRecptLines(header, lines);

        // 2.5 IQC 生成 hook：需检物料生成待检单，状态 PENDING；PC 端判定合格后再确认入库
        qcFactoryService.generateIqcForItemRecpt(header, lines);
        return header.getRecptId();
    }

    /** recptCode 为空时按编码规则服务端生成，返回最终单号 */
    private String ensureRecptCode(WmItemRecpt entity) {
        if (StringUtils.isEmpty(entity.getRecptCode())) {
            entity.setRecptCode(autoCodeGenerator.genSerialCode(CODE_RULE_RECEIPT, ""));
        }
        return entity.getRecptCode();
    }

    /** 同工厂内单号查重（factory_id 由拦截器自动注入）；DB 唯一索引兜底 */
    private void assertRecptCodeNotExists(String recptCode) {
        WmItemRecpt existing = wmItemRecptMapper.selectByRecptCode(recptCode);
        if (existing != null) {
            throw new ServiceException("入库单号[" + recptCode + "]已存在，请勿重复提交");
        }
    }

    /**
     * 批量保存入库单行（头表已落库）：逐行回填 recptId/仓库/purOrderLineId、
     * 自动生成批次号、累加 totalQuantity 并回写头。
     * 由 insertWmItemRecpt（PC 从采购订单生成）与 receiveWithLines（移动端一键收货）共享。
     */
    private void saveRecptLines(WmItemRecpt header, List<WmItemRecptLine> lines) {
        // 采购入库时,按 itemId 匹配 PO 行,回填 purOrderLineId(用于退货精确回写)
        java.util.Map<Long, PurOrderLine> poLineByItemId = null;
        if (header.getPurOrderId() != null && header.getPurOrderId() > 0) {
            List<PurOrderLine> poLines = loadPoLinesByOrderId(header.getPurOrderId());
            if (poLines != null && !poLines.isEmpty()) {
                poLineByItemId = new java.util.HashMap<>();
                for (PurOrderLine pl : poLines) {
                    if (pl.getItemId() != null && !poLineByItemId.containsKey(pl.getItemId())) {
                        poLineByItemId.put(pl.getItemId(), pl);
                    }
                }
            }
        }
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmItemRecptLine line : lines) {
            line.setRecptId(header.getRecptId());
            if (line.getWarehouseId() == null) line.setWarehouseId(header.getWarehouseId());
            if (line.getWarehouseCode() == null) line.setWarehouseCode(header.getWarehouseCode());
            if (line.getWarehouseName() == null) line.setWarehouseName(header.getWarehouseName());
            // 回填采购订单行ID(历史入库未记录,这里按 itemId 匹配)
            if (line.getPurOrderLineId() == null && poLineByItemId != null && line.getItemId() != null) {
                PurOrderLine matched = poLineByItemId.get(line.getItemId());
                if (matched != null) {
                    line.setPurOrderLineId(matched.getLineId());
                }
            }
            // 自动生成批次号（无 batchCode 时，根据物料+供应商+生产日期+有效期匹配或新建）
            if (line.getBatchCode() == null) {
                WmBatch generated = wmBatchService.getOrGenerateBatchCode(buildBatchParam(header, line));
                if (generated != null) {
                    line.setBatchId(generated.getBatchId());
                    line.setBatchCode(generated.getBatchCode());
                }
            }
            line.setCreateTime(DateUtils.getNowDate());
            line.setCreateBy(SecurityUtils.getUsername());
            wmItemRecptLineService.insertWmItemRecptLine(line);
            if (line.getQuantityRecpt() != null) {
                totalQty = totalQty.add(line.getQuantityRecpt());
            }
        }
        // 回写头部的入库总数量
        header.setTotalQuantity(totalQty);
        header.setUpdateTime(DateUtils.getNowDate());
        wmItemRecptMapper.updateWmItemRecpt(header);
    }

    /** 构造批次匹配参数（供 getOrGenerateBatchCode） */
    private WmBatch buildBatchParam(WmItemRecpt header, WmItemRecptLine line) {
        WmBatch param = new WmBatch();
        param.setItemId(line.getItemId());
        param.setItemCode(line.getItemCode());
        param.setItemName(line.getItemName());
        param.setSpecification(line.getSpecification());
        param.setVendorId(header.getVendorId());
        param.setVendorCode(header.getVendorCode());
        param.setVendorName(header.getVendorName());
        param.setProduceDate(line.getProduceDate());
        param.setExpireDate(line.getExpireDate());
        param.setLotNumber(line.getLotNumber());
        return param;
    }

    /**
     * 确认收货时回写 PO — 标记到货 + 更新 PO 状态 → RECEIVING
     * 使用 itemId→PO行 Map 做 O(1) 查找，避免 O(n×m) 嵌套循环。
     */
    private void writebackPoOnConfirm(WmItemRecpt header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        List<PurOrderLine> allPoLines = loadPoLinesByOrderId(purOrderId);
        List<WmItemRecptLine> recptLines = loadRecptLines(header.getRecptId());

        // 构建 itemId → PurOrderLine Map（O(1) 查找，避免嵌套循环）
        java.util.Map<Long, PurOrderLine> poLineByItemId = buildPoLineItemIdMap(allPoLines);

        String username = SecurityUtils.getUsername();
        // 先推头再推行：checkLineStatusNotExceedOrder 要求行 status ≤ 头 status，
        // 若先推行 → RECEIVING 而头仍是 ORDERED，会被联动校验拦截。
        PurOrder purOrder = purOrderService.selectPurOrderByOrderId(purOrderId);
        if (purOrder != null && PurOrderStatus.ORDERED.is(purOrder.getStatus())) {
            purOrder.setStatus(PurOrderStatus.RECEIVING.getCode());
            purOrder.setUpdateTime(DateUtils.getNowDate());
            purOrderService.updatePurOrder(purOrder);
        }

        for (WmItemRecptLine recptLine : recptLines) {
            PurOrderLine poLine = poLineByItemId.get(recptLine.getItemId());
            if (poLine != null) {
                poLine.setArrivalNoticeId(header.getRecptId());
                poLine.setStatus(PurOrderStatus.RECEIVING.getCode());
                poLine.setUpdateTime(DateUtils.getNowDate());
                poLine.setUpdateBy(username);
                purOrderLineService.updatePurOrderLine(poLine);
            }
        }
    }

    /**
     * 过账时回写 PO — 累加已收数量 + 判断是否全部收完。
     * 使用 itemId→PO行 Map 做 O(1) 查找，track allReceived 避免二次 DB 查询。
     */
    private void writebackPoOnPost(WmItemRecpt header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        List<PurOrderLine> allPoLines = loadPoLinesByOrderId(purOrderId);
        List<WmItemRecptLine> recptLines = loadRecptLines(header.getRecptId());

        // 构建 itemId → PurOrderLine Map（O(1) 查找）
        java.util.Map<Long, PurOrderLine> poLineByItemId = buildPoLineItemIdMap(allPoLines);

        String username = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        // 1. 原子递增 quantityReceived（并发安全：UPDATE SET qty = qty + delta）
        for (WmItemRecptLine recptLine : recptLines) {
            PurOrderLine poLine = poLineByItemId.get(recptLine.getItemId());
            if (poLine != null) {
                BigDecimal recptQty = recptLine.getQuantityRecpt() != null
                    ? recptLine.getQuantityRecpt() : BigDecimal.ZERO;
                if (recptQty.compareTo(BigDecimal.ZERO) > 0) {
                    purOrderLineMapper.addQuantityReceived(poLine.getLineId(), recptQty);
                }
            }
        }
        // 2. 原子递增后重新加载，先更新各行 status，再由 checkAndAutoCloseOrder 分档推进头。
        // 校验白名单已放行 RECEIVED（业务子系统推进），行 RECEIVED 高于头 RECEIVING 不会被拦。
        allPoLines = loadPoLinesByOrderId(purOrderId);
        for (PurOrderLine poLine : allPoLines) {
            // 已终态行（CANCEL/CLOSED）保持不变
            if (PurOrderStatus.CANCEL.is(poLine.getStatus())
                    || PurOrderStatus.CLOSED.is(poLine.getStatus())) {
                continue;
            }
            BigDecimal received = poLine.getQuantityReceived() != null
                ? poLine.getQuantityReceived() : BigDecimal.ZERO;
            BigDecimal ordered = poLine.getQuantityOrdered() != null
                ? poLine.getQuantityOrdered() : BigDecimal.ZERO;
            if (received.compareTo(ordered) >= 0) {
                poLine.setStatus(PurOrderStatus.RECEIVED.getCode());
            }
            poLine.setUpdateTime(now);
            poLine.setUpdateBy(username);
            purOrderLineService.updatePurOrderLine(poLine);
        }
        // 3. 头联动：由 PurOrderService 分档推进（全 RECEIVED→RECEIVED / 混合终态→CLOSED / 有活跃行→不动）
        purOrderService.checkAndAutoCloseOrder(purOrderId, null);
    }

    /** 加载 PO 订单下所有行 */
    private List<PurOrderLine> loadPoLinesByOrderId(Long purOrderId) {
        PurOrderLine query = new PurOrderLine();
        query.setOrderId(purOrderId);
        return purOrderLineService.selectPurOrderLineList(query);
    }

    /** 加载入库单下所有行 */
    private List<WmItemRecptLine> loadRecptLines(Long recptId) {
        WmItemRecptLine query = new WmItemRecptLine();
        query.setRecptId(recptId);
        return wmItemRecptLineService.selectWmItemRecptLineList(query);
    }

    // ════════════════════ 从采购订单生成草稿 ════════════════════

    /**
     * 从采购订单生成入库单草稿（不落库）。仅允许 ORDERED/RECEIVING 状态的 PO 收货。
     * 读取 PO 头+行，1:1 映射，入库数量预填「订购 − 已收」未收量，跳过已收完/取消/关闭行。
     */
    @Override
    public WmItemRecpt buildFromPurOrder(Long orderId) {
        PurOrder order = purOrderService.selectPurOrderByOrderId(orderId);
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        String poStatus = order.getStatus();
        if (!PurOrderStatus.ORDERED.is(poStatus) && !PurOrderStatus.RECEIVING.is(poStatus)) {
            throw new ServiceException("采购订单当前状态[" + poStatus + "]不允许收货，仅已下单/收货中可生成入库单");
        }
        List<PurOrderLine> poLines = loadPoLinesByOrderId(orderId);
        List<WmItemRecptLine> recptLines = mapPoLinesToRecptLines(poLines);
        if (recptLines.isEmpty()) {
            throw new ServiceException("该采购订单已全部收货完成，无可入库行");
        }
        WmItemRecpt draft = new WmItemRecpt();
        draft.setPurOrderId(order.getOrderId());
        draft.setPurOrderCode(order.getOrderCode());
        draft.setVendorId(order.getVendorId());
        draft.setVendorCode(order.getVendorCode());
        draft.setVendorName(order.getVendorName());
        draft.setRecptType("PURCHASE");
        draft.setStatus("DRAFT");
        draft.setLines(recptLines);
        // 定向入供应商仓（决策E1）：命中供应商专属仓则预填头仓库，未命中保持空由用户头表选
        if (order.getVendorId() != null) {
            WmWarehouse vendorWh = wmWarehouseService.findVendorWarehouse(order.getFactoryId(), order.getVendorId());
            if (vendorWh != null) {
                draft.setWarehouseId(vendorWh.getWarehouseId());
                draft.setWarehouseCode(vendorWh.getWarehouseCode());
                draft.setWarehouseName(vendorWh.getWarehouseName());
            }
        }
        return draft;
    }

    /**
     * 采购订单行 → 入库单行映射。跳过终态行（已收满/已取消/已关闭）。
     * 入库数量 = 订购 − 已收（max 0），仓库留空由用户在头表选择后回填。
     */
    private List<WmItemRecptLine> mapPoLinesToRecptLines(List<PurOrderLine> poLines) {
        List<WmItemRecptLine> result = new ArrayList<>();
        if (poLines == null) return result;
        for (PurOrderLine pl : poLines) {
            if (PurOrderStatus.CANCEL.is(pl.getStatus()) || PurOrderStatus.CLOSED.is(pl.getStatus())) {
                continue;
            }
            BigDecimal ordered = pl.getQuantityOrdered() != null ? pl.getQuantityOrdered() : BigDecimal.ZERO;
            BigDecimal received = pl.getQuantityReceived() != null ? pl.getQuantityReceived() : BigDecimal.ZERO;
            BigDecimal remain = ordered.subtract(received);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 已收满
            }
            WmItemRecptLine line = new WmItemRecptLine();
            line.setPurOrderLineId(pl.getLineId());
            line.setItemId(pl.getItemId());
            line.setItemCode(pl.getItemCode());
            line.setItemName(pl.getItemName());
            line.setSpecification(pl.getSpecification());
            line.setUnitOfMeasure(pl.getUnitOfMeasure());
            line.setUnitName(pl.getUnitName());
            line.setUnit2(pl.getUnit2());
            line.setUnit2Name(pl.getUnit2Name());
            line.setConversionRate(pl.getConversionRate());
            line.setQuantityRecpt(remain);
            line.setQuantityOrdered(ordered);     // transient 展示值
            line.setQuantityReceived(received);   // transient 展示值
            result.add(line);
        }
        return result;
    }

    /**
     * 构建 itemId → PurOrderLine Map（O(1) 查找）。
     * DB 层已通过 V49 uk_order_item(order_id, item_id) 保证同一 PO 内物料唯一。
     */
    private java.util.Map<Long, PurOrderLine> buildPoLineItemIdMap(List<PurOrderLine> poLines) {
        java.util.Map<Long, PurOrderLine> map = new java.util.LinkedHashMap<>();
        for (PurOrderLine poLine : poLines) {
            Long itemId = poLine.getItemId();
            if (itemId != null) {
                map.put(itemId, poLine);
            }
        }
        return map;
    }
}