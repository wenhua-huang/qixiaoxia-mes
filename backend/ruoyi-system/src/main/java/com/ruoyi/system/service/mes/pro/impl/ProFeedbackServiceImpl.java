package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        autoFillCodes(proFeedback);
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

    /** 从工单BOM构建默认物料消耗列表 */
    private List<ProFeedbackConsume> buildConsumeFromBom(Long workorderId) {
        try {
            List<ProWorkorderBom> boms = proWorkorderBomMapper.selectProWorkorderBomByWorkorderId(workorderId);
            if (boms == null || boms.isEmpty()) return null;
            return boms.stream().map(bom -> {
                ProFeedbackConsume c = new ProFeedbackConsume();
                c.setWorkorderId(workorderId);
                c.setItemId(bom.getItemId());
                c.setItemCode(bom.getItemCode());
                c.setItemName(bom.getItemName());
                c.setQuantity(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : bom.getQuantity());
                c.setBatchCode("");
                return c;
            }).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    private void writeMaterialTrace(ProFeedback fb) {
        try {
            if (fb.getProcessId() == null) return;
            String traceType = "PRODUCE";
            try {
                ProProcess proc = proProcessMapper.selectProProcessByProcessId(fb.getProcessId());
                if (proc != null && "SLITTING".equals(proc.getProcessType())) {
                    traceType = "SLIT";
                }
            } catch (Exception e) {
                log.warn("物料追溯-工序类型判定失败, processId={}, 降级为 PRODUCE", fb.getProcessId(), e);
            }
            Long cardId = null;
            Long cardProcessId = null;
            try {
                ProCard cardQ = new ProCard();
                cardQ.setWorkorderId(fb.getWorkorderId());
                List<ProCard> cards = proCardMapper.selectProCardList(cardQ);
                if (cards != null && !cards.isEmpty()) {
                    cardId = cards.get(0).getCardId();
                    ProCardProcess cpQuery = new ProCardProcess();
                    cpQuery.setCardId(cardId);
                    cpQuery.setProcessId(fb.getProcessId());
                    List<ProCardProcess> cps = proCardProcessMapper.selectProCardProcessList(cpQuery);
                    if (cps != null && !cps.isEmpty()) {
                        ProCardProcess cp = cps.get(0);
                        cardId = cp.getCardId();
                        cardProcessId = cp.getRecordId();
                    }
                }
            } catch (Exception e) {
                log.warn("物料追溯-流转卡查找失败, workorderId={}, cardId/cardProcessId 将为 null", fb.getWorkorderId(), e);
            }
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType(traceType);
            trace.setParentType("CARD");
            trace.setParentId(cardId);
            trace.setChildType("BATCH");
            trace.setChildId(fb.getRecordId());
            trace.setQuantity(fb.getQuantityQualified());
            trace.setUnitOfMeasure(fb.getUnitOfMeasure());
            trace.setWorkorderId(fb.getWorkorderId());
            trace.setFeedbackId(fb.getRecordId());
            trace.setCardId(cardId);
            trace.setCardProcessId(cardProcessId);
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
            }

            // 增量更新生产工单已生产数量（末工序法：仅工艺路线最后一道工序报工才累加，避免多工序重复计数）
            if (fb.getWorkorderId() != null) {
                BigDecimal deltaProduced = nvl(fb.getQuantityFeedback());
                if (fb.getRouteId() != null && fb.getProcessId() != null) {
                    // 有工艺路线信息：仅末工序报工才更新工单已生产数
                    if (isLastProcessOfRoute(fb.getRouteId(), fb.getProcessId())) {
                        proWorkorderMapper.addQuantityProduced(fb.getWorkorderId(), deltaProduced);
                        // 末工序报工审核后：自动生成入库单 + 退料单
                        try {
                            proWorkorderDocService.onFeedbackAudited(recordId);
                        } catch (Exception e) {
                            log.error("自动生成入库单/退料单失败, feedbackId={}, workorderId={}, err={}",
                                    recordId, fb.getWorkorderId(), e.getMessage());
                        }
                    }
                } else {
                    // 无工艺路线信息（直接从工单报工，不绑定具体工序）：保持原有逻辑
                    proWorkorderMapper.addQuantityProduced(fb.getWorkorderId(), deltaProduced);
                }
            }
        });
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
}
