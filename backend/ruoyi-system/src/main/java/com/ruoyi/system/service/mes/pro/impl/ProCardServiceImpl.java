package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.domain.mes.pro.CardScanResultVO;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProFeedbackConsume;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.service.mes.pro.IProCardService;
import com.ruoyi.system.service.mes.pro.IProFeedbackService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

/**
 * ProCardService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProCardServiceImpl implements IProCardService
{
    private static final Logger log = LoggerFactory.getLogger(ProCardServiceImpl.class);

    @Autowired
    private ProCardMapper proCardMapper;
    @Autowired
    private RedisLockTemplate lockTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private AutoCodeGenerator autoCodeGenerator;
    @Autowired
    private ProTaskMapper proTaskMapper;
    @Autowired
    private ProFeedbackMapper proFeedbackMapper;
    @Autowired
    private ProRouteProcessMapper routeProcessMapper;
    @Autowired
    private IProFeedbackService proFeedbackService;
    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public ProCard selectProCardByCardId(Long cardId) { return proCardMapper.selectProCardByCardId(cardId); }

    @Override
    public List<ProCard> selectProCardList(ProCard e) { return proCardMapper.selectProCardList(e); }

    @Override
    public List<ProCard> selectAll() { return proCardMapper.selectProCardList(new ProCard()); }

    @Override
    @Transactional
    public int insertProCard(ProCard e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        return proCardMapper.insertProCard(e);
    }

    @Override
    public int updateProCard(ProCard e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proCardMapper.updateProCard(e);
    }

    @Override
    public int deleteProCardByCardIds(Long[] cardIds) { return proCardMapper.deleteProCardByCardIds(cardIds); }

    @Override
    public int deleteProCardByCardId(Long cardId) { return proCardMapper.deleteProCardByCardId(cardId); }

    /**
     * 拆卡：从原卡拆出 splitQty 生成新卡。
     * Redis 锁 + 事务模板保证原子性（仿 WmIssueHeaderServiceImpl.confirmIssue：先锁后事务，不加 @Transactional）。
     * 失败（余量不足/状态非 ACTIVE/编码生成失败）整体回滚，原卡数量不变。
     */
    @Override
    public ProCard splitCard(Long cardId, BigDecimal splitQty) {
        if (cardId == null || splitQty == null || splitQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("拆卡参数无效");
        }
        String lockKey = "pro:card:split:" + cardId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> txTemplate.execute(status -> doSplitCard(cardId, splitQty)));
    }

    /** 拆卡事务体：查原卡 → 校验 → 原子扣减 → 生成新卡编码 → 建新卡（复制原卡业务字段） */
    private ProCard doSplitCard(Long cardId, BigDecimal splitQty) {
        ProCard src = proCardMapper.selectProCardByCardId(cardId);
        if (src == null) throw new ServiceException("流转卡不存在");
        if (!"ACTIVE".equals(src.getStatus())) throw new ServiceException("仅流转中的卡可拆分");
        BigDecimal originQty = src.getQuantityTransfered() != null ? src.getQuantityTransfered() : BigDecimal.ZERO;
        if (splitQty.compareTo(originQty) >= 0) throw new ServiceException("拆出数量必须小于原卡数量");
        // 原子扣减（带 status=ACTIVE + 余量守卫，并发安全）
        int affected = proCardMapper.decrementQuantity(cardId, splitQty, src.getFactoryId());
        if (affected == 0) throw new ServiceException("拆卡失败：卡状态或余量已变化，请刷新重试");
        // 生成新卡编码（事务内，失败回滚序号）
        String newCode = autoCodeGenerator.genSerialCode("PRO_CARD_CODE", null);
        ProCard newCard = buildSplitCard(src, newCode, splitQty);
        insertProCard(newCard);
        log.info("拆卡成功: 原卡 {} 扣减 {}，新卡 {} {}", cardId, splitQty, newCard.getCardId(), newCode);
        return newCard;
    }

    /** 从原卡复制业务字段构建新卡（数量=splitQty，状态=ACTIVE） */
    private ProCard buildSplitCard(ProCard src, String newCode, BigDecimal splitQty) {
        ProCard c = new ProCard();
        c.setCardCode(newCode);
        c.setWorkorderId(src.getWorkorderId());
        c.setWorkorderCode(src.getWorkorderCode());
        c.setWorkorderName(src.getWorkorderName());
        c.setTaskId(src.getTaskId());
        c.setTaskCode(src.getTaskCode());
        c.setBatchCode(src.getBatchCode());
        c.setItemId(src.getItemId());
        c.setItemCode(src.getItemCode());
        c.setItemName(src.getItemName());
        c.setSpecification(src.getSpecification());
        c.setUnitOfMeasure(src.getUnitOfMeasure());
        c.setUnitName(src.getUnitName());
        c.setQuantityTransfered(splitQty);
        c.setCurrentProcessId(src.getCurrentProcessId());
        c.setCurrentProcessName(src.getCurrentProcessName());
        c.setStatus("ACTIVE");
        return c;
    }

    /** 扫流转卡码反查报工上下文（设计文档 §6.2，App 扫码报工入口） */
    @Override
    public CardScanResultVO scanForReport(String cardCode) {
        CardScanResultVO vo = new CardScanResultVO();
        // 1. 按 cardCode 查卡（factory_id 由拦截器自动注入）
        ProCard q = new ProCard();
        q.setCardCode(cardCode);
        List<ProCard> list = proCardMapper.selectProCardList(q);
        if (list == null || list.isEmpty()) {
            vo.setCanReport(false);
            vo.setReason("CARD_NOT_FOUND");
            return vo;
        }
        ProCard card = list.get(0);
        vo.setCard(card);

        // 2. 卡状态校验（非 ACTIVE 不可报）
        if (!"ACTIVE".equals(card.getStatus())) {
            vo.setCanReport(false);
            vo.setReason(reasonForCardStatus(card.getStatus()));
            return vo;
        }

        // 3-5. 加载报工上下文（任务/BOM/已审核合格数）
        loadReportContext(card, vo);

        // 6. 可报判定（当前工序是外协工序时给出明确原因，而非笼统的无可报任务）
        if (vo.getReportableTasks().isEmpty()) {
            vo.setCanReport(false);
            vo.setReason(vo.getOutsourceTasks().isEmpty() ? "NO_REPORTABLE_TASK" : "PROCESS_OUTSOURCED");
        } else {
            vo.setCanReport(true);
        }
        return vo;
    }

    /** 加载报工上下文：按路线位置筛「当前工序(可续报)+下一波」的任务、BOM 消耗默认值、已审核合格数 */
    private void loadReportContext(ProCard card, CardScanResultVO vo) {
        // 3. 加载工单任务，按路线位置筛应展示的工序（游标语义=最近完成的工序，见 advanceCardStatus）
        ProTask tq = new ProTask();
        tq.setWorkorderId(card.getWorkorderId());
        List<ProTask> all = proTaskMapper.selectProTaskList(tq);
        Long routeId = all.isEmpty() ? null : all.get(0).getRouteId();
        Set<Long> dueProcessIds = resolveDueProcessIds(routeId, card.getCurrentProcessId());
        List<ProTask> candidates = (dueProcessIds == null) ? all
                : all.stream().filter(t -> dueProcessIds.contains(t.getProcessId())).collect(Collectors.toList());
        List<ProTask> reportable = candidates.stream()
                .filter(t -> ProConstants.TASK_STATUS_PRODUCING.equals(t.getStatus()))
                .filter(t -> !ProConstants.WS_CODE_VENDOR.equals(t.getWorkstationCode()))
                .collect(Collectors.toList());
        fillPendingCount(reportable);
        vo.setReportableTasks(reportable);
        // 外协任务不限状态：进行中/已完成均展示（供 App 呈现外协进度）
        List<ProTask> outsource = candidates.stream()
                .filter(t -> ProConstants.WS_CODE_VENDOR.equals(t.getWorkstationCode()))
                .collect(Collectors.toList());
        vo.setOutsourceTasks(outsource);

        // 4. BOM 消耗默认值（失败不阻断，置空列表）
        try {
            vo.setConsumeDefaults(proFeedbackService.getDefaultConsume(card.getWorkorderId()));
        } catch (Exception e) {
            log.warn("扫码反查 getDefaultConsume 失败 workorderId={}", card.getWorkorderId(), e);
            vo.setConsumeDefaults(Collections.emptyList());
        }

        // 5. 已审核合格数（防超报）
        if (card.getCurrentProcessId() != null) {
            vo.setReportedQualifiedSum(
                    proFeedbackMapper.sumAuditedQualifiedByCardAndProcess(card.getCardId(), card.getCurrentProcessId()));
        } else {
            vo.setReportedQualifiedSum(BigDecimal.ZERO);
        }
    }

    /**
     * 计算扫码应展示的工序集合：当前工序（支持同工序多批续报）+ 路线上位置更靠后的最近一波工序。
     * currentProcessId 语义为「最近完成的工序」（报工/外协收货后置为该工序，见 advanceCardStatus），
     * 故下一波 = orderNum 严格更大且最小的节点（并行路线同一波 orderNum 相同会全部纳入）。
     * 新卡（currentProcessId 为空或不在路线上）取第一波工序。
     * 无法定位路线时返回 null（不过滤，由报工提交侧 validateProcessSequence 兜底）。
     */
    private Set<Long> resolveDueProcessIds(Long routeId, Long currentProcessId) {
        if (routeId == null) {
            return null;
        }
        List<ProRouteProcess> nodes = routeProcessMapper.selectProRouteProcessByRouteId(routeId);
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        List<ProRouteProcess> ordered = new ArrayList<>(nodes);
        ordered.sort(Comparator.comparing(rp -> rp.getOrderNum() != null ? rp.getOrderNum() : Integer.MAX_VALUE));
        int currentIdx = -1;
        if (currentProcessId != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (currentProcessId.equals(ordered.get(i).getProcessId())) {
                    currentIdx = i;
                    break;
                }
            }
        }
        int waveIdx = (currentIdx >= 0) ? currentIdx + 1 : 0;
        Set<Long> due = new HashSet<>();
        if (currentIdx >= 0) {
            due.add(currentProcessId);
        }
        if (waveIdx < ordered.size()) {
            Integer waveOrder = ordered.get(waveIdx).getOrderNum();
            for (int i = waveIdx; i < ordered.size(); i++) {
                ProRouteProcess rp = ordered.get(i);
                if (rp.getOrderNum() != null && !rp.getOrderNum().equals(waveOrder)) {
                    break;
                }
                if (rp.getProcessId() != null) {
                    due.add(rp.getProcessId());
                }
            }
        }
        return due;
    }

    /** 卡状态 → 不可报原因码 */
    private String reasonForCardStatus(String status) {
        if (status == null) {
            return "CARD_NOT_ACTIVE";
        }
        switch (status) {
            case "COMPLETED": return "CARD_COMPLETED";
            case "OUTSOURCING": return "CARD_OUTSOURCING";
            case "SCRAPPED": return "CARD_SCRAPPED";
            default: return "CARD_NOT_ACTIVE";
        }
    }

    /** 回填任务的待审核报工数（用于前端状态展示，>0 即 1） */
    private void fillPendingCount(List<ProTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> ids = tasks.stream().map(ProTask::getTaskId).collect(Collectors.toList());
        List<Long> pending = proFeedbackMapper.selectPendingTaskIds(ids);
        Set<Long> pendingSet = (pending == null) ? Collections.emptySet() : new HashSet<>(pending);
        for (ProTask t : tasks) {
            t.setPendingFeedbackCount(pendingSet.contains(t.getTaskId()) ? 1 : 0);
        }
    }
}
