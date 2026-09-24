package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.ProExceptionParty;
import com.ruoyi.common.enums.ProExceptionResolve;
import com.ruoyi.common.enums.ProExceptionStatus;
import com.ruoyi.common.enums.ProExceptionType;
import com.ruoyi.common.enums.PurOrderStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProException;
import com.ruoyi.system.domain.mes.pro.ProExceptionConstants;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.pro.ProExceptionMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.service.mes.pro.IProExceptionService;
import com.ruoyi.system.service.mes.pro.IProTaskService;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 生产异常单 Service 实现：手机上报 → PC 补全定责 → 七出口处理 → 回流手动收口。
 *
 * <p>出口处理先锁后事务（锁 key pro:exception:resolve:{id}），锁内完成建异常任务/
 * DRAFT 采购单/顺延/关闭，避免并发重复开单。factory_id 由 FactoryIdInterceptor 注入。
 *
 * @author qixiaoxia
 */
@Service
public class ProExceptionServiceImpl implements IProExceptionService
{
    @Autowired
    private ProExceptionMapper proExceptionMapper;

    @Autowired
    private ProTaskMapper proTaskMapper;

    @Autowired
    private IProTaskService proTaskService;

    @Autowired
    private IPurOrderService purOrderService;

    @Autowired
    private IPurOrderLineService purOrderLineService;

    @Autowired
    private MdItemMapper mdItemMapper;

    @Autowired(required = false)
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTxTemplate()
    {
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<ProException> selectProExceptionList(ProException query)
    {
        return proExceptionMapper.selectProExceptionList(query);
    }

    @Override
    public ProException selectProExceptionByExceptionId(Long exceptionId)
    {
        ProException ex = proExceptionMapper.selectProExceptionByExceptionId(exceptionId);
        fillTargetDocStatus(ex);
        return ex;
    }

    @Override
    public ProTask reportContext(Long taskId)
    {
        ProTask task = proTaskMapper.selectProTaskByTaskId(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已被删除");
        }
        return task;
    }

    @Override
    @Transactional
    public Long reportException(ProException input)
    {
        ProExceptionType type = ProExceptionType.fromCode(input.getExceptionType());
        if (type == null)
        {
            throw new ServiceException("请选择异常类型");
        }
        if (input.getTaskId() == null)
        {
            throw new ServiceException("缺少关联工序任务");
        }
        if (StringUtils.isBlank(input.getDescription()))
        {
            throw new ServiceException("请填写异常说明");
        }
        ProTask task = proTaskMapper.selectProTaskByTaskId(input.getTaskId());
        if (task == null)
        {
            throw new ServiceException("关联任务不存在或已被删除");
        }

        ProException ex = new ProException();
        ex.setExceptionCode(generateExceptionCode());
        ex.setExceptionType(type.getCode());
        ex.setStatus(ProExceptionStatus.OPEN.getCode());
        ex.setResponsibleParty(ProExceptionParty.PENDING.getCode());
        fillSnapshotFromTask(ex, task);
        ex.setReporterId(SecurityUtils.getUserId());
        ex.setReporterName(currentUserName());
        ex.setOccurTime(new Date());
        ex.setImpactQuantity(input.getImpactQuantity());
        ex.setDescription(StringUtils.trim(input.getDescription()));
        ex.setSceneImages(StringUtils.trimToNull(input.getSceneImages()));
        ex.setRemark(StringUtils.trimToNull(input.getRemark()));
        ex.setCreateBy(SecurityUtils.getUsername());
        ex.setCreateTime(new Date());
        proExceptionMapper.insertProException(ex);
        return ex.getExceptionId();
    }

    @Override
    @Transactional
    public int updateProException(ProException input)
    {
        ProException ex = mustGet(input.getExceptionId());
        if (!ProExceptionStatus.OPEN.is(ex.getStatus()))
        {
            throw new ServiceException("仅待处理状态的异常单可补全编辑");
        }
        // 异常类型建单后不可变（四类专属字段按类型切换，改类型会导致快照语义错乱）
        if (StringUtils.isNotBlank(input.getExceptionType())
                && !input.getExceptionType().equals(ex.getExceptionType()))
        {
            throw new ServiceException("异常类型不可修改");
        }
        applyParty(ex, input.getResponsibleParty());
        applyEditableFields(ex, input);
        validateTypeSpecific(ex);
        ex.setUpdateBy(SecurityUtils.getUsername());
        ex.setUpdateTime(new Date());
        int rows = proExceptionMapper.updateProException(ex);
        // 动态 UPDATE 无法置空：可空列（数量/到货日/备注）清空由专用语句落库
        proExceptionMapper.updateExceptionEditOptional(ex);
        return rows;
    }

    @Override
    public ProException resolveException(Long exceptionId, ProException input)
    {
        ProExceptionResolve resolve = ProExceptionResolve.fromCode(input.getResolveType());
        if (resolve == null)
        {
            throw new ServiceException("请选择处理出口");
        }
        // 先锁后事务：防并发重复开返工任务/采购单
        lockTemplate.execute(ProExceptionConstants.LOCK_RESOLVE_PREFIX + exceptionId,
                (Runnable) () -> txTemplate.execute(tx ->
                {
                    doResolve(exceptionId, input, resolve);
                    return null;
                }));
        return proExceptionMapper.selectProExceptionByExceptionId(exceptionId);
    }

    @Override
    @Transactional
    public int closeException(Long exceptionId, String conclusion)
    {
        ProException ex = mustGet(exceptionId);
        if (!ProExceptionStatus.PROCESSING.is(ex.getStatus()))
        {
            throw new ServiceException("仅处理中的异常单可关闭");
        }
        if (StringUtils.isBlank(conclusion))
        {
            throw new ServiceException("请填写处理结论后再关闭");
        }
        ex.setConclusion(StringUtils.trim(conclusion));
        stampClosed(ex, SecurityUtils.getUsername(), new Date());
        ex.setUpdateBy(SecurityUtils.getUsername());
        ex.setUpdateTime(new Date());
        return proExceptionMapper.updateProException(ex);
    }

    @Override
    @Transactional
    public int voidException(Long exceptionId, String reason)
    {
        ProException ex = mustGet(exceptionId);
        if (!ProExceptionStatus.OPEN.is(ex.getStatus()))
        {
            throw new ServiceException("仅待处理状态的异常单可作废（已产生处理单据请先收口）");
        }
        if (StringUtils.isBlank(reason))
        {
            throw new ServiceException("请填写作废原因");
        }
        String operator = SecurityUtils.getUsername();
        Date now = new Date();
        ex.setConclusion(ProExceptionConstants.VOID_CONCLUSION_PREFIX + StringUtils.trim(reason));
        ex.setStatus(ProExceptionStatus.VOID.getCode());
        ex.setCloseBy(operator);
        ex.setCloseTime(now);
        ex.setUpdateBy(operator);
        ex.setUpdateTime(now);
        return proExceptionMapper.updateProException(ex);
    }

    // ======================== 出口处理 ========================

    private void doResolve(Long exceptionId, ProException input, ProExceptionResolve resolve)
    {
        ProException ex = mustGet(exceptionId);
        if (!ProExceptionStatus.OPEN.is(ex.getStatus()))
        {
            throw new ServiceException("异常单已处理或已关闭，请勿重复操作");
        }
        if (ProExceptionParty.PENDING.is(ex.getResponsibleParty()))
        {
            throw new ServiceException("请先确定责任方后再处理");
        }
        if (StringUtils.isNotBlank(input.getConclusion()))
        {
            ex.setConclusion(StringUtils.trim(input.getConclusion()));
        }
        DocInfo doc = switch (resolve)
        {
            case REWORK, REMAKE -> createExceptionTask(ex, input, resolve);
            case PURCHASE -> createDraftPurchase(ex, input);
            case RESCHEDULE -> rescheduleTask(ex, input);
            case SCRAP -> prepareScrap(ex, input);
            case CONCESSION, REFUND -> prepareTerminalConclusion(ex, input, resolve);
        };
        String operator = SecurityUtils.getUsername();
        Date now = new Date();
        ex.setResolveType(resolve.getCode());
        ex.setResolveBy(currentUserName());
        ex.setResolveTime(now);
        if (doc != null)
        {
            ex.setTargetDocType(doc.type);
            ex.setTargetDocId(doc.id);
            ex.setTargetDocCode(doc.code);
        }
        if (resolve.isFlowback())
        {
            ex.setStatus(ProExceptionStatus.PROCESSING.getCode());
        }
        else
        {
            stampClosed(ex, operator, now);
        }
        ex.setUpdateBy(operator);
        ex.setUpdateTime(now);
        proExceptionMapper.updateProException(ex);
    }

    /** 开返工/补做异常任务：同工单/路线/工序/机台/产品，编号 原码-E{n}，状态 NORMAL 待排产 */
    private DocInfo createExceptionTask(ProException ex, ProException input, ProExceptionResolve resolve)
    {
        ProTask origin = proTaskMapper.selectProTaskByTaskId(ex.getTaskId());
        if (origin == null)
        {
            throw new ServiceException("原工序任务不存在，无法开返工/补做任务");
        }
        BigDecimal qty = firstPositive(input.getResolveQuantity(), ex.getImpactQuantity());
        if (qty == null)
        {
            throw new ServiceException("请填写有效的返工/补做数量");
        }
        int seq = proExceptionMapper.countExceptionTasksByOrigin(origin.getTaskId()) + 1;
        String suffix = ProExceptionResolve.REWORK == resolve
                ? ProExceptionConstants.TASK_SUFFIX_REWORK : ProExceptionConstants.TASK_SUFFIX_REMAKE;

        ProTask task = copyTaskForException(origin, suffix, qty, ex, seq);
        proTaskService.insertProTask(task);
        return new DocInfo(ProExceptionConstants.DOC_TYPE_TASK, task.getTaskId(), task.getTaskCode());
    }

    private ProTask copyTaskForException(ProTask origin, String suffix, BigDecimal qty,
                                         ProException ex, int seq)
    {
        ProTask t = new ProTask();
        t.setWorkorderId(origin.getWorkorderId());
        t.setWorkorderCode(origin.getWorkorderCode());
        t.setWorkorderName(origin.getWorkorderName());
        t.setWorkstationId(origin.getWorkstationId());
        t.setWorkstationCode(origin.getWorkstationCode());
        t.setWorkstationName(origin.getWorkstationName());
        t.setRouteId(origin.getRouteId());
        t.setRouteCode(origin.getRouteCode());
        t.setProcessId(origin.getProcessId());
        t.setProcessCode(origin.getProcessCode());
        if (StringUtils.isNotBlank(origin.getProcessName()))
        {
            t.setProcessName(origin.getProcessName() + suffix);
        }
        t.setItemId(origin.getItemId());
        t.setItemCode(origin.getItemCode());
        t.setItemName(origin.getItemName());
        t.setSpecification(origin.getSpecification());
        t.setUnitOfMeasure(origin.getUnitOfMeasure());
        t.setUnitName(origin.getUnitName());
        t.setClientId(origin.getClientId());
        t.setClientCode(origin.getClientCode());
        t.setClientName(origin.getClientName());
        t.setClientNick(origin.getClientNick());
        t.setMachineCode(origin.getMachineCode());
        t.setSetupDuration(origin.getSetupDuration());
        t.setUnitDuration(origin.getUnitDuration());
        t.setDuration(origin.getDuration());
        t.setColorCode(origin.getColorCode());
        t.setRequestDate(origin.getRequestDate());
        t.setVendorId(origin.getVendorId());
        t.setVendorCode(origin.getVendorCode());
        t.setOutsourceFactoryId(origin.getOutsourceFactoryId());
        t.setWorkerId(origin.getWorkerId());
        t.setLeaderId(origin.getLeaderId());

        t.setTaskCode(origin.getTaskCode() + ProExceptionConstants.TASK_CODE_EXCEPTION_SEP + seq);
        String baseName = StringUtils.defaultIfBlank(origin.getTaskName(), origin.getTaskCode());
        t.setTaskName(baseName + suffix);
        t.setQuantity(qty);
        t.setStatus(ProConstants.TASK_STATUS_NORMAL);
        t.setIsException(ProExceptionConstants.YES);
        t.setOriginTaskId(origin.getTaskId());
        t.setExceptionId(ex.getExceptionId());
        t.setExceptionCode(ex.getExceptionCode());
        t.setRemark("来源异常单 " + ex.getExceptionCode());
        return t;
    }

    /** 开补料出口：建 DRAFT 采购单（供应商待定、一行缺料），双向以异常单号挂链，不下发 */
    private DocInfo createDraftPurchase(ProException ex, ProException input)
    {
        BigDecimal qty = firstPositive(input.getResolveQuantity(), ex.getShortageQuantity());
        if (qty == null)
        {
            throw new ServiceException("请填写有效的补料数量");
        }
        if (StringUtils.isBlank(ex.getItemName()))
        {
            throw new ServiceException("缺料物料信息不完整，请先在异常单补全物料");
        }
        PurOrder po = buildDraftPurchaseOrder(ex);
        purOrderService.insertPurOrder(po);

        PurOrderLine line = buildDraftPurchaseLine(po.getOrderId(), ex, qty);
        purOrderLineService.insertPurOrderLine(line);
        po.setTotalQuantity(qty);
        purOrderService.updatePurOrder(po);
        return new DocInfo(ProExceptionConstants.DOC_TYPE_PUR_ORDER, po.getOrderId(), po.getOrderCode());
    }

    private PurOrder buildDraftPurchaseOrder(ProException ex)
    {
        PurOrder po = new PurOrder();
        po.setOrderName(StringUtils.defaultIfBlank(ex.getWorkorderName(), ex.getWorkorderCode())
                + "-异常补料-" + ex.getExceptionCode());
        po.setVendorId(0L);
        po.setOrderDate(new Date());
        po.setExpectedDate(ex.getExpectedArrivalDate());
        po.setStatus(PurOrderStatus.DRAFT.getCode());
        po.setCurrency(ProExceptionConstants.PO_CURRENCY_CNY);
        po.setSourceOrderCode(ex.getExceptionCode());
        po.setWorkorderId(ex.getWorkorderId());
        po.setWorkorderCode(ex.getWorkorderCode());
        return po;
    }

    private PurOrderLine buildDraftPurchaseLine(Long orderId, ProException ex, BigDecimal qty)
    {
        PurOrderLine line = new PurOrderLine();
        line.setOrderId(orderId);
        line.setItemId(ex.getItemId());
        line.setItemCode(ex.getItemCode());
        line.setItemName(ex.getItemName());
        fillItemUnit(line, ex);
        line.setQuantityOrdered(qty);
        line.setExpectedDate(ex.getExpectedArrivalDate());
        line.setSourceOrderCode(ex.getExceptionCode());
        line.setStatus(PurOrderStatus.ORDERED.getCode());
        return line;
    }

    /** 单位取物料主数据；物料主数据缺失时退化为原任务快照单位 */
    private void fillItemUnit(PurOrderLine line, ProException ex)
    {
        MdItem item = ex.getItemId() == null ? null : mdItemMapper.selectMdItemById(ex.getItemId());
        if (item != null)
        {
            line.setUnitOfMeasure(item.getUnitOfMeasure());
            line.setUnitName(item.getUnitName());
            return;
        }
        ProTask task = proTaskMapper.selectProTaskByTaskId(ex.getTaskId());
        if (task != null)
        {
            line.setUnitOfMeasure(task.getUnitOfMeasure());
            line.setUnitName(task.getUnitName());
        }
    }

    /** 顺延改期：精准更新原任务计划完成时间，回写原计划时间（全实体 update 会覆盖并发状态） */
    private DocInfo rescheduleTask(ProException ex, ProException input)
    {
        ProTask origin = proTaskMapper.selectProTaskByTaskId(ex.getTaskId());
        if (origin == null)
        {
            throw new ServiceException("原工序任务不存在，无法顺延改期");
        }
        Date newTime = input.getNewExpectedTime() != null ? input.getNewExpectedTime() : ex.getNewExpectedTime();
        if (newTime == null)
        {
            throw new ServiceException("请选择新的计划完成时间");
        }
        if (ex.getOriginalPlanTime() == null && origin.getEndTime() != null)
        {
            ex.setOriginalPlanTime(origin.getEndTime());
        }
        ex.setNewExpectedTime(newTime);
        proTaskMapper.updateTaskEndTime(origin.getTaskId(), newTime, SecurityUtils.getUsername());
        return null;
    }

    private DocInfo prepareScrap(ProException ex, ProException input)
    {
        // 报废数量必须在出口弹窗显式确认，不静默取影响数量兜底
        if (!isPositive(input.getScrapQuantity()))
        {
            throw new ServiceException("请填写有效的报废数量");
        }
        ex.setScrapQuantity(input.getScrapQuantity());
        return null;
    }

    private DocInfo prepareTerminalConclusion(ProException ex, ProException input, ProExceptionResolve resolve)
    {
        if (StringUtils.isBlank(input.getConclusion()))
        {
            throw new ServiceException(resolve == ProExceptionResolve.CONCESSION
                    ? "请填写让步接收结论" : "请填写退款结单说明");
        }
        return null;
    }

    // ======================== 补全/上报辅助 ========================

    private void fillSnapshotFromTask(ProException ex, ProTask task)
    {
        ex.setWorkorderId(task.getWorkorderId());
        ex.setWorkorderCode(task.getWorkorderCode());
        ex.setWorkorderName(task.getWorkorderName());
        ex.setTaskId(task.getTaskId());
        ex.setTaskCode(task.getTaskCode());
        ex.setProcessId(task.getProcessId());
        ex.setProcessCode(task.getProcessCode());
        ex.setProcessName(task.getProcessName());
        ex.setTargetType(ProExceptionConstants.TARGET_TYPE_TASK);
        ex.setTargetId(task.getTaskId());
        ex.setTargetCode(task.getTaskCode());
    }

    /** 责任方锁定：首次选定（PENDING→具体方）后不可再改 */
    private void applyParty(ProException ex, String newParty)
    {
        if (StringUtils.isBlank(newParty))
        {
            return;
        }
        if (ProExceptionParty.fromCode(newParty) == null)
        {
            throw new ServiceException("责任方取值非法");
        }
        String oldParty = ex.getResponsibleParty();
        if (!ProExceptionParty.PENDING.is(oldParty) && !oldParty.equals(newParty))
        {
            throw new ServiceException("责任方已确定，不可修改");
        }
        ex.setResponsibleParty(newParty);
    }

    private void applyEditableFields(ProException ex, ProException input)
    {
        ex.setImpactQuantity(input.getImpactQuantity());
        if (StringUtils.isNotBlank(input.getDescription()))
        {
            ex.setDescription(StringUtils.trim(input.getDescription()));
        }
        if (input.getSceneImages() != null)
        {
            ex.setSceneImages(StringUtils.trimToNull(input.getSceneImages()));
        }
        if (input.getOccurTime() != null)
        {
            ex.setOccurTime(input.getOccurTime());
        }
        ex.setQualitySubclass(StringUtils.trimToNull(input.getQualitySubclass()));
        ex.setUsableQuantity(input.getUsableQuantity());
        ex.setNeedRework(StringUtils.trimToNull(input.getNeedRework()));
        ex.setItemId(input.getItemId());
        ex.setItemCode(StringUtils.trimToNull(input.getItemCode()));
        ex.setItemName(StringUtils.trimToNull(input.getItemName()));
        ex.setShortageQuantity(input.getShortageQuantity());
        ex.setExpectedArrivalDate(input.getExpectedArrivalDate());
        ex.setOriginalPlanTime(input.getOriginalPlanTime());
        ex.setNewExpectedTime(input.getNewExpectedTime());
        ex.setDelayReason(StringUtils.trimToNull(input.getDelayReason()));
        ex.setReturnQuantity(input.getReturnQuantity());
        ex.setReturnReason(StringUtils.trimToNull(input.getReturnReason()));
        ex.setCustomerAcceptRework(StringUtils.trimToNull(input.getCustomerAcceptRework()));
        if (input.getRemark() != null)
        {
            ex.setRemark(StringUtils.trimToNull(input.getRemark()));
        }
    }

    private void validateTypeSpecific(ProException ex)
    {
        ProExceptionType type = ProExceptionType.fromCode(ex.getExceptionType());
        if (type == ProExceptionType.QUALITY && StringUtils.isBlank(ex.getQualitySubclass()))
        {
            throw new ServiceException("请选择质量异常细分（做坏了/做少了）");
        }
        if (type == ProExceptionType.MATERIAL)
        {
            if (StringUtils.isBlank(ex.getItemName()) || !isPositive(ex.getShortageQuantity()))
            {
                throw new ServiceException("请补全缺料物料与缺料数量");
            }
        }
        if (type == ProExceptionType.DELAY)
        {
            if (ex.getOriginalPlanTime() == null || ex.getNewExpectedTime() == null
                    || StringUtils.isBlank(ex.getDelayReason()))
            {
                throw new ServiceException("请补全原计划时间、新预计时间与延迟原因");
            }
        }
        if (type == ProExceptionType.RETURN
                && (!isPositive(ex.getReturnQuantity()) || StringUtils.isBlank(ex.getReturnReason())))
        {
            throw new ServiceException("请补全退货数量与退货原因");
        }
    }

    private void stampClosed(ProException ex, String operator, Date now)
    {
        ex.setStatus(ProExceptionStatus.CLOSED.getCode());
        ex.setCloseBy(operator);
        ex.setCloseTime(now);
    }

    /** 详情回填处理单据（异常任务/补料采购单）当前状态，供 PC 展示与跳转（E3/E4 验收） */
    private void fillTargetDocStatus(ProException ex)
    {
        if (ex == null || ex.getTargetDocId() == null || ex.getTargetDocType() == null)
        {
            return;
        }
        if (ProExceptionConstants.DOC_TYPE_TASK.equals(ex.getTargetDocType()))
        {
            ProTask doc = proTaskMapper.selectProTaskByTaskId(ex.getTargetDocId());
            if (doc != null)
            {
                ex.setTargetDocStatus(doc.getStatus());
            }
        }
        else if (ProExceptionConstants.DOC_TYPE_PUR_ORDER.equals(ex.getTargetDocType()))
        {
            PurOrderVO doc = purOrderService.selectPurOrderByOrderId(ex.getTargetDocId());
            if (doc != null)
            {
                ex.setTargetDocStatus(doc.getStatus());
            }
        }
    }

    private ProException mustGet(Long exceptionId)
    {
        ProException ex = proExceptionMapper.selectProExceptionByExceptionId(exceptionId);
        if (ex == null)
        {
            throw new ServiceException("异常单不存在或已被删除");
        }
        return ex;
    }

    private String generateExceptionCode()
    {
        if (autoCodeGenerator != null)
        {
            try
            {
                return autoCodeGenerator.genSerialCode(ProExceptionConstants.RULE_CODE, null);
            }
            catch (Exception e)
            {
                // 规则缺失/序列故障降级，保证上报不被编码阻断（与任务/报工编码同策略）
            }
        }
        return "EX" + System.currentTimeMillis();
    }

    /** 操作人快照：nickName(userName)，取不到姓名退化为账号 */
    private String currentUserName()
    {
        String userName = SecurityUtils.getUsername();
        try
        {
            String nickName = SecurityUtils.getLoginUser().getUser().getNickName();
            if (StringUtils.isNotBlank(nickName))
            {
                return nickName + "(" + userName + ")";
            }
        }
        catch (Exception ignored)
        {
            // 上下文取不到用户实体时退化为账号
        }
        return userName;
    }

    private static BigDecimal firstPositive(BigDecimal a, BigDecimal b)
    {
        if (isPositive(a)) return a;
        return isPositive(b) ? b : null;
    }

    private static boolean isPositive(BigDecimal v)
    {
        return v != null && v.signum() > 0;
    }

    /** 出口产生单据挂链信息 */
    private record DocInfo(String type, Long id, String code) {}
}
