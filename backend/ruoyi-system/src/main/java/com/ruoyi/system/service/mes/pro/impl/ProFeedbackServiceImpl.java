package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackConsumeMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackParamMapper;
import com.ruoyi.system.mapper.mes.pro.ProParamTemplateMapper;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProFeedbackConsume;
import com.ruoyi.system.domain.mes.pro.ProFeedbackParam;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.service.mes.pro.IProFeedbackService;
import com.ruoyi.system.service.mes.pro.IProWorkorderDocService;

/**
 * 报工记录Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProFeedbackServiceImpl implements IProFeedbackService {

    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private ProFeedbackMapper qxxProFeedbackMapper;
    @Autowired private ProFeedbackConsumeMapper consumeMapper;
    @Autowired private ProFeedbackParamMapper feedbackParamMapper;
    @Autowired private ProParamTemplateMapper proParamTemplateMapper;
    @Autowired private ProTaskMapper proTaskMapper;
    @Autowired private ProWorkorderMapper proWorkorderMapper;
    @Autowired private ProWorkorderBomMapper proWorkorderBomMapper;
    @Autowired private ProProcessMapper proProcessMapper;
    @Autowired private ProRouteProcessMapper proRouteProcessMapper;
    @Autowired private ProCardProcessMapper proCardProcessMapper;
    @Autowired private ProCardMapper proCardMapper;
    @Autowired private ProMaterialTraceMapper proMaterialTraceMapper;
    @Autowired private MdItemMapper mdItemMapper;
    @Autowired private com.ruoyi.system.mapper.mes.wm.WmIssueHeaderMapper wmIssueHeaderMapper;
    @Autowired private com.ruoyi.system.mapper.mes.wm.WmIssueDetailMapper wmIssueDetailMapper;

    @Override
    public ProFeedback selectProFeedbackByRecordId(Long recordId) {
        ProFeedback fb = qxxProFeedbackMapper.selectProFeedbackByRecordId(recordId);
        if (fb != null) {
            fb.setConsumeList(consumeMapper.selectByFeedbackId(recordId));
            fb.setParamList(feedbackParamMapper.selectProFeedbackParamByFeedbackId(recordId));
        }
        return fb;
    }

    @Override
    public List<ProFeedback> selectProFeedbackList(ProFeedback proFeedback) {
        return qxxProFeedbackMapper.selectProFeedbackList(proFeedback);
    }

    @Override
    public List<ProFeedback> selectAll() {
        return qxxProFeedbackMapper.selectProFeedbackList(new ProFeedback());
    }

    @Override
    public boolean checkFeedbackCodeUnique(ProFeedback proFeedback) {
        ProFeedback existing = qxxProFeedbackMapper.selectProFeedbackByFeedbackCode(proFeedback.getFeedbackCode());
        if (existing == null) return true;
        if (existing.getRecordId().equals(proFeedback.getRecordId())) return true;
        throw new ServiceException("报工编码[" + proFeedback.getFeedbackCode() + "]已存在");
    }

    private void autoFillCodes(ProFeedback fb) {
        if (fb.getWorkorderId() != null && fb.getWorkorderCode() == null) {
            try {
                ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(fb.getWorkorderId());
                if (wo != null) {
                    if (fb.getWorkorderCode() == null) fb.setWorkorderCode(wo.getWorkorderCode());
                    if (fb.getWorkorderName() == null) fb.setWorkorderName(wo.getWorkorderName());
                    if (fb.getItemId() == null) fb.setItemId(wo.getProductId());
                    if (fb.getItemName() == null) fb.setItemName(wo.getProductName());
                }
            } catch (Exception ignored) {}
        }
        if (fb.getProcessId() != null && fb.getProcessCode() == null) {
            try {
                ProProcess proc = proProcessMapper.selectProProcessByProcessId(fb.getProcessId());
                if (proc != null && fb.getProcessCode() == null) fb.setProcessCode(proc.getProcessCode());
            } catch (Exception ignored) {}
        }
        if (fb.getTaskId() != null) {
            try {
                ProTask task = proTaskMapper.selectProTaskByTaskId(fb.getTaskId());
                if (task != null) {
                    if (fb.getWorkorderId() == null) fb.setWorkorderId(task.getWorkorderId());
                    if (fb.getWorkorderCode() == null) fb.setWorkorderCode(task.getWorkorderCode());
                    if (fb.getWorkorderName() == null) fb.setWorkorderName(task.getWorkorderName());
                    if (fb.getTaskCode() == null) fb.setTaskCode(task.getTaskCode());
                    if (fb.getProcessId() == null) fb.setProcessId(task.getProcessId());
                    if (fb.getProcessCode() == null) fb.setProcessCode(task.getProcessCode());
                    if (fb.getProcessName() == null) fb.setProcessName(task.getProcessName());
                    if (fb.getRouteId() == null) fb.setRouteId(task.getRouteId());
                    if (fb.getWorkstationId() == null) fb.setWorkstationId(task.getWorkstationId());
                    if (fb.getWorkstationCode() == null) fb.setWorkstationCode(task.getWorkstationCode());
                    if (fb.getWorkstationName() == null) fb.setWorkstationName(task.getWorkstationName());
                    if (fb.getItemId() == null) fb.setItemId(task.getItemId());
                    if (fb.getItemCode() == null) fb.setItemCode(task.getItemCode());
                    if (fb.getItemName() == null) fb.setItemName(task.getItemName());
                    if (fb.getUnitOfMeasure() == null) fb.setUnitOfMeasure(task.getUnitOfMeasure());
                    if (fb.getUnitName() == null) fb.setUnitName(task.getUnitName());
                }
            } catch (Exception ignored) {}
        }
        if (fb.getItemId() != null && fb.getItemCode() == null) {
            try {
                MdItem item = mdItemMapper.selectMdItemById(fb.getItemId());
                if (item != null) {
                    fb.setItemCode(item.getItemCode());
                    if (fb.getItemName() == null) fb.setItemName(item.getItemName());
                    if (fb.getUnitOfMeasure() == null) fb.setUnitOfMeasure(item.getUnitOfMeasure());
                    if (fb.getUnitName() == null) fb.setUnitName(item.getUnitName());
                    if (fb.getSpecification() == null) fb.setSpecification(item.getSpecification());
                }
            } catch (Exception ignored) {}
        }
        if (fb.getItemCode() == null || fb.getItemCode().isEmpty()) fb.setItemCode("-");
        if (fb.getWorkorderCode() == null || fb.getWorkorderCode().isEmpty()) fb.setWorkorderCode("-");
        if (fb.getProcessCode() == null || fb.getProcessCode().isEmpty()) fb.setProcessCode("-");
        if (fb.getTaskCode() == null || fb.getTaskCode().isEmpty()) fb.setTaskCode("-");
        if (fb.getUnitOfMeasure() == null || fb.getUnitOfMeasure().isEmpty()) fb.setUnitOfMeasure("PCS");
        if (fb.getQuantity() == null) fb.setQuantity(BigDecimal.ZERO);
        if (fb.getQuantityFeedback() == null) fb.setQuantityFeedback(BigDecimal.ZERO);
        if (fb.getQuantityQualified() == null) fb.setQuantityQualified(BigDecimal.ZERO);
        if (fb.getQuantityUnqualified() == null) fb.setQuantityUnqualified(BigDecimal.ZERO);
        if (fb.getQuantityUncheck() == null) fb.setQuantityUncheck(BigDecimal.ZERO);
        if (fb.getQuantityLaborScrap() == null) fb.setQuantityLaborScrap(BigDecimal.ZERO);
        if (fb.getQuantityMaterialScrap() == null) fb.setQuantityMaterialScrap(BigDecimal.ZERO);
        if (fb.getQuantityOtherScrap() == null) fb.setQuantityOtherScrap(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public int insertProFeedback(ProFeedback proFeedback) {
        proFeedback.setCreateTime(DateUtils.getNowDate());
        proFeedback.setCreateBy(SecurityUtils.getUsername());
        if (proFeedback.getStatus() == null) proFeedback.setStatus("PREPARE");
        if (proFeedback.getFeedbackTime() == null) proFeedback.setFeedbackTime(DateUtils.getNowDate());
        // 自动填充报工人（当前登录用户），前端不传时由后端兜底
        if (proFeedback.getUserName() == null || proFeedback.getUserName().isEmpty()) {
            proFeedback.setUserName(SecurityUtils.getUsername());
            try {
                proFeedback.setNickName(SecurityUtils.getLoginUser().getUser().getNickName());
            } catch (Exception ignored) {}
        }
        autoFillCodes(proFeedback);
        // 自动关联流转卡：用户没传 cardId 时，按工单查活跃卡取第一张
        if (proFeedback.getCardId() == null && proFeedback.getWorkorderId() != null) {
            proFeedback.setCardId(resolveActiveCardId(proFeedback.getWorkorderId()));
        }
        // 物料消耗默认值：若未传 consumeList 但有工单ID，从工单BOM自动填充
        if ((proFeedback.getConsumeList() == null || proFeedback.getConsumeList().isEmpty())
                && proFeedback.getWorkorderId() != null) {
            proFeedback.setConsumeList(buildConsumeFromBom(proFeedback.getWorkorderId()));
        }
        int rows = qxxProFeedbackMapper.insertProFeedback(proFeedback);
        // 持久化物料消耗
        if (proFeedback.getConsumeList() != null && !proFeedback.getConsumeList().isEmpty()) {
            for (ProFeedbackConsume c : proFeedback.getConsumeList()) {
                c.setFeedbackId(proFeedback.getRecordId());
                if (c.getWorkorderId() == null) c.setWorkorderId(proFeedback.getWorkorderId());
            }
            consumeMapper.insertBatch(proFeedback.getConsumeList());
        }
        // 持久化报工参数（自动判定偏差）
        if (proFeedback.getParamList() != null && !proFeedback.getParamList().isEmpty()) {
            for (ProFeedbackParam p : proFeedback.getParamList()) {
                p.setFeedbackId(proFeedback.getRecordId());
                p.setIsDeviation(calcDeviation(p));
                feedbackParamMapper.insertProFeedbackParam(p);
            }
        }
        writeMaterialTrace(proFeedback);
        return rows;
    }

    /**
     * 计算参数偏差：actualValue 超出 template 的 minValue/maxValue 范围则返回 "Y"，范围内返回 "N"。
     * 无 min/max 约束、actualValue 为空、或非数值型参数（无法解析为 BigDecimal）时返回 null（未判定）。
     */
    private String calcDeviation(ProFeedbackParam p) {
        if (p.getActualValue() == null || p.getActualValue().isEmpty() || p.getTemplateId() == null) {
            return null;
        }
        ProParamTemplate tpl = proParamTemplateMapper.selectProParamTemplateByTemplateId(p.getTemplateId());
        if (tpl == null || (tpl.getMinValue() == null && tpl.getMaxValue() == null)) {
            return null;
        }
        try {
            BigDecimal val = new BigDecimal(p.getActualValue().trim());
            if (tpl.getMinValue() != null && val.compareTo(tpl.getMinValue()) < 0) return "Y";
            if (tpl.getMaxValue() != null && val.compareTo(tpl.getMaxValue()) > 0) return "Y";
            return "N";
        } catch (NumberFormatException e) {
            // 非数值型参数（VARCHAR/ENUM/DATE 等），无法做范围比较
            return null;
        }
    }

    /**
     * 报工审核后检查任务是否产够，产够则自动完成（PRODUCING → COMPLETED）。
     * 避免任务永远停在 PRODUCING，需人工点"完成"按钮。
     */
    private void tryAutoCompleteTask(Long taskId) {
        ProTask task = proTaskMapper.selectProTaskByTaskId(taskId);
        if (task == null || !ProConstants.TASK_STATUS_PRODUCING.equals(task.getStatus())) return;
        BigDecimal produced = task.getQuantityProduced() != null ? task.getQuantityProduced() : BigDecimal.ZERO;
        BigDecimal planned = task.getQuantity() != null ? task.getQuantity() : BigDecimal.ZERO;
        if (planned.compareTo(BigDecimal.ZERO) > 0 && produced.compareTo(planned) >= 0) {
            task.setStatus(ProConstants.TASK_STATUS_COMPLETED);
            task.setUpdateTime(DateUtils.getNowDate());
            task.setUpdateBy(SecurityUtils.getUsername());
            proTaskMapper.updateProTask(task);
        }
    }

    /** 从工单BOM构建默认物料消耗列表，batch_code 反查该工单最近领料的真实批次 */
    private List<ProFeedbackConsume> buildConsumeFromBom(Long workorderId) {
        try {
            List<ProWorkorderBom> boms = proWorkorderBomMapper.selectProWorkorderBomByWorkorderId(workorderId);
            if (boms == null || boms.isEmpty()) return null;
            // 预查该工单所有领料明细（按 detail_id desc，取最近的批次）
            Map<Long, String> itemBatchMap = loadLatestIssueBatchByWorkorder(workorderId);
            return boms.stream().map(bom -> {
                ProFeedbackConsume c = new ProFeedbackConsume();
                c.setWorkorderId(workorderId);
                c.setItemId(bom.getItemId());
                c.setItemCode(bom.getItemCode());
                c.setItemName(bom.getItemName());
                c.setQuantity(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : bom.getQuantity());
                c.setBatchCode(itemBatchMap.getOrDefault(bom.getItemId(), ""));
                return c;
            }).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 加载工单下各物料最近一次领料的批次号（workorder→issue_header→issue_detail）。
     * @return itemId → batchCode 映射
     */
    private Map<Long, String> loadLatestIssueBatchByWorkorder(Long workorderId) {
        Map<Long, String> result = new java.util.HashMap<>();
        try {
            com.ruoyi.system.domain.mes.wm.WmIssueHeader hq = new com.ruoyi.system.domain.mes.wm.WmIssueHeader();
            hq.setWorkorderId(workorderId);
            List<com.ruoyi.system.domain.mes.wm.WmIssueHeader> headers = wmIssueHeaderMapper.selectWmIssueHeaderList(hq);
            if (headers == null || headers.isEmpty()) return result;
            for (com.ruoyi.system.domain.mes.wm.WmIssueHeader h : headers) {
                com.ruoyi.system.domain.mes.wm.WmIssueDetail dq = new com.ruoyi.system.domain.mes.wm.WmIssueDetail();
                dq.setIssueId(h.getIssueId());
                List<com.ruoyi.system.domain.mes.wm.WmIssueDetail> details = wmIssueDetailMapper.selectWmIssueDetailList(dq);
                if (details == null) continue;
                for (com.ruoyi.system.domain.mes.wm.WmIssueDetail d : details) {
                    // detail_id desc 已排序，putIfAbsent 保留最近（最大 detail_id）的批次
                    if (d.getItemId() != null && d.getBatchCode() != null && !d.getBatchCode().isEmpty()) {
                        result.putIfAbsent(d.getItemId(), d.getBatchCode());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("反查工单领料批次失败, workorderId={}", workorderId, e);
        }
        return result;
    }

    /**
     * 解析报工对应的活跃流转卡ID（用户没选卡时自动带出工单的第一张活跃卡）
     */
    private Long resolveActiveCardId(Long workorderId) {
        try {
            ProCard q = new ProCard();
            q.setWorkorderId(workorderId);
            q.setStatus("ACTIVE");
            List<ProCard> cards = proCardMapper.selectProCardList(q);
            return (cards != null && !cards.isEmpty()) ? cards.get(0).getCardId() : null;
        } catch (Exception e) {
            log.warn("报工-流转卡解析失败, workorderId={}", workorderId, e);
            return null;
        }
    }

    /**
     * 写入工序追溯：每道工序报工都写一条 CARD→FEEDBACK 边，保证全工序可追溯。
     * trace_type 统一策略（不硬编码 process_type 映射，适配工序类型可扩展）：
     *   - 末工序 = PRODUCE（成品产出）
     *   - 外协工序 = OUTSOURCE_PROCESS
     *   - 其余工序 = PROCESS
     * parent=CARD（投料/产出的枢纽），child=报工记录（工序产出标识）。
     */
    private void writeMaterialTrace(ProFeedback fb) {
        try {
            if (fb.getProcessId() == null) return;
            // trace_type 按工序角色动态判定：末工序优先（动态查 route 的 max order_num，工序增删自动跟随）
            ProProcess proc = proProcessMapper.selectProProcessByProcessId(fb.getProcessId());
            boolean isLast = fb.getRouteId() != null && isLastProcessOfRoute(fb.getRouteId(), fb.getProcessId());
            String traceType;
            if (isLast) {
                traceType = "PRODUCE";
            } else if (proc != null && "OUTSOURCE".equals(proc.getProcessType())) {
                traceType = "OUTSOURCE_PROCESS";
            } else {
                traceType = "PROCESS";
            }
            Long cardId = fb.getCardId();
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType(traceType);
            // 无流转卡的工单（工单级生产），产出来源记为工单本身，避免无效的 CARD:0 断链
            if (cardId != null) {
                trace.setParentType("CARD");
                trace.setParentId(cardId);
            } else {
                trace.setParentType("WORKORDER");
                trace.setParentId(fb.getWorkorderId() != null ? fb.getWorkorderId() : 0L);
            }
            trace.setChildType("FEEDBACK");
            trace.setChildId(fb.getRecordId());
            trace.setQuantity(fb.getQuantityQualified());
            trace.setUnitOfMeasure(fb.getUnitOfMeasure());
            trace.setWorkorderId(fb.getWorkorderId());
            trace.setFeedbackId(fb.getRecordId());
            trace.setCardId(cardId);
            trace.setProcessId(fb.getProcessId());
            trace.setTraceTime(new Date());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(SecurityUtils.getUsername());
            proMaterialTraceMapper.insertProMaterialTrace(trace);
        } catch (Exception e) {
            // 物料追溯失败不阻断主报工流程（报工主记录已提交），但必须留痕便于排查追溯断链
            log.error("写入物料追溯失败, feedbackId={}", fb.getRecordId(), e);
        }
    }

    @Override
    @Transactional
    public int updateProFeedback(ProFeedback proFeedback) {
        proFeedback.setUpdateTime(DateUtils.getNowDate());
        proFeedback.setUpdateBy(SecurityUtils.getUsername());
        int rows = qxxProFeedbackMapper.updateProFeedback(proFeedback);
        // 物料消耗：先删后插
        if (proFeedback.getConsumeList() != null) {
            consumeMapper.deleteByFeedbackId(proFeedback.getRecordId());
            if (!proFeedback.getConsumeList().isEmpty()) {
                for (ProFeedbackConsume c : proFeedback.getConsumeList()) {
                    c.setFeedbackId(proFeedback.getRecordId());
                    if (c.getWorkorderId() == null) c.setWorkorderId(proFeedback.getWorkorderId());
                }
                consumeMapper.insertBatch(proFeedback.getConsumeList());
            }
        }
        return rows;
    }

    @Autowired
    private IProWorkorderDocService proWorkorderDocService;

    /**
     * 自注入代理：批量方法内需要调用 confirmFeedback / auditFeedback，
     * 直接 this::foo 走裸方法会绕过 CGLIB 代理导致 @Transactional 失效，
     * 尤其 auditFeedback 内部调 onFeedbackAudited(REQUIRES_NEW)，外层事务必须真实存在。
     */
    @Autowired
    @Lazy
    private IProFeedbackService self;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProFeedbackServiceImpl.class);

    @Override
    @Transactional
    public void auditFeedback(Long recordId) {
        // Redis 分布式锁 + DB 行级锁，双重防并发重复审核
        lockTemplate.execute("feedback:audit:" + recordId, () -> {
            ProFeedback fb = qxxProFeedbackMapper.selectProFeedbackByRecordIdForUpdate(recordId);
            if (fb == null) throw new ServiceException("报工记录不存在");
            if (!"CONFIRMED".equals(fb.getStatus())) throw new ServiceException("只有已确认状态的报工才能审核");

            // 更新报工状态
            fb.setStatus("AUDITED");
            fb.setUpdateTime(DateUtils.getNowDate());
            fb.setUpdateBy(SecurityUtils.getUsername());
            qxxProFeedbackMapper.updateProFeedback(fb);

            // 增量更新排产任务已生产数量
            if (fb.getTaskId() != null) {
                BigDecimal deltaProduced = nvl(fb.getQuantityFeedback());
                BigDecimal deltaQualified = nvl(fb.getQuantityQualified());
                BigDecimal deltaUnqualified = nvl(fb.getQuantityUnqualified());
                proTaskMapper.addQuantityProduced(fb.getTaskId(), deltaProduced, deltaQualified, deltaUnqualified);
                // 任务已生产 ≥ 计划量 → 自动标记 COMPLETED（否则任务永远停在 PRODUCING）
                tryAutoCompleteTask(fb.getTaskId());
            }

            // 增量更新生产工单已生产数量（末工序法：仅工艺路线最后一道工序报工才累加，避免多工序重复计数）
            if (fb.getWorkorderId() != null) {
                BigDecimal deltaProduced = nvl(fb.getQuantityFeedback());
                if (fb.getRouteId() != null && fb.getProcessId() != null) {
                    // 有工艺路线信息：仅末工序报工才更新工单已生产数
                    if (isLastProcessOfRoute(fb.getRouteId(), fb.getProcessId())) {
                        // 【Fix #1/#2/#4】末工序场景：quantity_produced 更新 + 完工判定 都在外层本事务完成，
                        // 与单据生成 (REQUIRES_NEW) 解耦 —— 单据失败回滚不影响 quantity_produced / 审核提交。
                        proWorkorderMapper.addQuantityProduced(fb.getWorkorderId(), deltaProduced);
                        // 完工判定先于单据生成：若 autoComplete 失败，外层事务整体回滚，避免
                        // REQUIRES_NEW 已提交的入库单成孤儿 (autoComplete 失败时 onFeedbackAudited 未执行)。
                        proWorkorderDocService.autoCompleteWorkorderIfQualified(fb.getWorkorderId());
                        // 【Fix REQUIRES_NEW 可见性】同事务重读 workorder 拿到 addQuantityProduced 后的
                        // quantity_produced（InnoDB 同事务内可见自己未提交的写），传给 onFeedbackAudited。
                        // 不传的话 REQUIRES_NEW 子事务在 MySQL REPEATABLE READ 下读到的是旧值（produced=0），
                        // 会导致 qtyToRecpt=min(合格数, produced-alreadyRecpt)=0，跳过入库单生成。
                        ProWorkorder woAfter = proWorkorderMapper.selectProWorkorderByWorkorderId(fb.getWorkorderId());
                        BigDecimal producedAfter = woAfter != null && woAfter.getQuantityProduced() != null
                                ? woAfter.getQuantityProduced() : BigDecimal.ZERO;
                        // 末工序报工审核后：自动生成入库单 + 退料单 (独立事务, 失败不影响审核)
                        try {
                            proWorkorderDocService.onFeedbackAudited(recordId, producedAfter);
                        } catch (Exception e) {
                            // 单据生成失败：审核仍提交，用户需手动补录入库单
                            log.warn("自动生成入库单/退料单失败, 需手动补录. feedbackId={}, workorderId={}, err={}",
                                    recordId, fb.getWorkorderId(), e.getMessage());
                        }
                        // 推进流转卡：末工序完工 → 卡置 COMPLETED
                        advanceCardStatus(fb, true);
                    } else {
                        // 中间工序报工审核 → 更新流转卡当前工序（status 保持 ACTIVE）
                        advanceCardStatus(fb, false);
                    }
                } else {
                    // 无工艺路线信息（直接从工单报工，不绑定具体工序）：保持原有逻辑
                    proWorkorderMapper.addQuantityProduced(fb.getWorkorderId(), deltaProduced);
                }
            }
        });
    }

    /**
     * 推进流转卡工序状态。
     * @param isLastProcess true=末工序完工（卡置 COMPLETED），false=中间工序（更新 currentProcessId，保持 ACTIVE）
     */
    private void advanceCardStatus(ProFeedback fb, boolean isLastProcess) {
        if (fb.getCardId() == null) return;
        try {
            ProCard cardUpd = new ProCard();
            cardUpd.setCardId(fb.getCardId());
            cardUpd.setCurrentProcessId(fb.getProcessId());
            cardUpd.setCurrentProcessName(fb.getProcessName());
            if (isLastProcess) {
                // 末工序完工判定：累计审核合格量 >= 计划量才置 COMPLETED（对齐 autoCompleteWorkorderIfQualified 的工单完工逻辑）
                // 避免多批次末工序审核时，首批就把卡误判完工（破坏 resolveActiveCardId 的 ACTIVE 过滤 + 状态一致性）
                ProCard card = proCardMapper.selectProCardByCardId(fb.getCardId());
                if (card != null) {
                    BigDecimal produced = nvl(qxxProFeedbackMapper.sumAuditedQualifiedByCardAndProcess(
                            fb.getCardId(), fb.getProcessId()));
                    BigDecimal planned = nvl(card.getQuantityTransfered());
                    if (produced.compareTo(planned) >= 0) {
                        cardUpd.setStatus("COMPLETED");
                    }
                }
            }
            cardUpd.setUpdateBy(SecurityUtils.getUsername());
            cardUpd.setUpdateTime(DateUtils.getNowDate());
            proCardMapper.updateProCard(cardUpd);
        } catch (Exception e) {
            log.error("流转卡状态推进失败, cardId={}, isLastProcess={}", fb.getCardId(), isLastProcess, e);
        }
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    /**
     * 判断指定工序是否为工艺路线上的最后一道工序
     */
    private boolean isLastProcessOfRoute(Long routeId, Long processId) {
        ProRouteProcess lastProcess = proRouteProcessMapper.selectLastProcessByRouteId(routeId);
        return lastProcess != null && lastProcess.getProcessId().equals(processId);
    }

    @Override
    public int deleteProFeedbackByRecordIds(Long[] recordIds) {
        return qxxProFeedbackMapper.deleteProFeedbackByRecordIds(recordIds);
    }

    @Override
    public int deleteProFeedbackByRecordId(Long recordId) {
        return qxxProFeedbackMapper.deleteProFeedbackByRecordId(recordId);
    }

    // ════════════════════════════════════════════════════════════════
    // 批量流转：确认(PREPARE→CONFIRMED) / 审核(CONFIRMED→AUDITED)
    // 单条方法各自事务/幂等，单张失败不影响其他张。
    // ════════════════════════════════════════════════════════════════

    /** 确认报工：PREPARE → CONFIRMED。纯状态翻转，无库存/单据副作用。 */
    @Override
    @Transactional
    public int confirmFeedback(Long recordId) {
        ProFeedback fb = qxxProFeedbackMapper.selectProFeedbackByRecordId(recordId);
        if (fb == null) throw new ServiceException("报工记录不存在");
        if (!"PREPARE".equals(fb.getStatus())) {
            throw new ServiceException("只有待确认状态的报工才能确认,当前状态:" + fb.getStatus());
        }
        fb.setStatus("CONFIRMED");
        fb.setUpdateTime(DateUtils.getNowDate());
        fb.setUpdateBy(SecurityUtils.getUsername());
        return qxxProFeedbackMapper.updateProFeedback(fb);
    }

    /**
     * 批量执行通用骨架:逐条调用单条动作,尽力执行,失败收集到 failures。
     * 通过 self 代理调用保持每条独立事务(与 REQUIRES_NEW 子事务协同)。
     */
    private Map<String, Object> executeBatch(Long[] recordIds, java.util.function.Consumer<Long> action) {
        if (recordIds == null || recordIds.length == 0) {
            throw new ServiceException("未选择报工记录");
        }
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        for (Long id : recordIds) {
            try {
                action.accept(id);
                success++;
            } catch (Exception e) {
                if (!(e instanceof ServiceException)) {
                    log.warn("批量流转失败(非业务异常), recordId={}", id, e);
                }
                ProFeedback fb = qxxProFeedbackMapper.selectProFeedbackByRecordId(id);
                Map<String, Object> f = new HashMap<>();
                f.put("recordId", id);
                f.put("feedbackCode", fb != null ? fb.getFeedbackCode() : null);
                f.put("workorderName", fb != null ? fb.getWorkorderName() : null);
                String msg = e.getMessage();
                f.put("reason", (msg != null && !msg.isEmpty()) ? msg : e.getClass().getSimpleName());
                failures.add(f);
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("total", recordIds.length);
        r.put("successCount", success);
        r.put("failedCount", failures.size());
        r.put("failures", failures);
        return r;
    }

    @Override
    public Map<String, Object> batchConfirmFeedback(Long[] recordIds) {
        return executeBatch(recordIds, self::confirmFeedback);
    }

    @Override
    public Map<String, Object> batchAuditFeedback(Long[] recordIds) {
        return executeBatch(recordIds, self::auditFeedback);
    }
}
