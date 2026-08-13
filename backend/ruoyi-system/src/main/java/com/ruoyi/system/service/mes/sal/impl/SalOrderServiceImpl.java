package com.ruoyi.system.service.mes.sal.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.SalOrderStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.CrmOrderLineDTO;
import com.ruoyi.system.domain.mes.sal.SalConstants;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.sal.ISalOrderService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

/**
 * 销售订单Service实现
 *
 * 转工单遵循"先锁后事务":Redisson 锁 lineId -> TransactionTemplate 显式开 TX ->
 * 校验可转量 -> 构建 ProWorkorder(回填+来源) -> createWorkorderWithBom。
 * 已转量/可转量查询时按 sales_order_line_id 聚合 qxx_pro_workorder,不存计数列,
 * 取消工单靠状态过滤自动排除,无需回滚。
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
@Service
public class SalOrderServiceImpl implements ISalOrderService
{
    private static final Logger log = LoggerFactory.getLogger(SalOrderServiceImpl.class);

    @Autowired
    private SalOrderMapper salOrderMapper;

    @Autowired
    private SalOrderLineMapper salOrderLineMapper;

    @Autowired
    private MdItemMapper mdItemMapper;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    private static final String LOCK_PREFIX = "sal:order:line:toWorkorder:";

    @PostConstruct
    void initTx()
    {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public SalOrder selectSalOrderByOrderId(Long orderId)
    {
        return salOrderMapper.selectSalOrderByOrderId(orderId);
    }

    @Override
    public List<SalOrder> selectSalOrderList(SalOrder salOrder)
    {
        return salOrderMapper.selectSalOrderList(salOrder);
    }

    @Override
    public List<SalOrder> selectAllConvertible()
    {
        return salOrderMapper.selectSalOrderAllConvertible();
    }

    @Override
    public boolean checkOrderCodeUnique(SalOrder salOrder)
    {
        Long orderId = salOrder.getOrderId();
        SalOrder info = salOrderMapper.checkOrderCodeUnique(salOrder);
        return info != null && !info.getOrderId().equals(orderId);
    }

    @Override
    @Transactional
    public SalOrder createWithLines(SalOrderCreateRequest req)
    {
        SalOrder order = req.getOrder();
        validateOrderCode(order);
        if (order.getStatus() == null) order.setStatus(SalOrderStatus.PREPARE.getCode());
        if (order.getOrderType() == null) order.setOrderType("NEW");
        if (order.getSampleFlag() == null) order.setSampleFlag("N");
        if (order.getSource() == null) order.setSource(SalConstants.SOURCE_DIRECT);
        order.setCreateBy(SecurityUtils.getUsername());
        order.setCreateTime(DateUtils.getNowDate());
        salOrderMapper.insertSalOrder(order);
        saveLines(order.getOrderId(), req.getLines(), true);
        return order;
    }

    @Override
    @Transactional
    public SalOrder createFromCrm(CrmOrderCreateRequest req)
    {
        SalOrder order = new SalOrder();
        order.setOrderCode(StringUtils.isEmpty(req.getOrderCode())
                ? autoCodeGenerator.genSerialCode("ORDER_NO", "")
                : req.getOrderCode());
        order.setOrderName(req.getOrderName());
        order.setClientName(req.getClientName());
        order.setClientCode(req.getClientCode());
        order.setClientOrderCode(req.getClientOrderCode());
        order.setSalesperson(req.getSalesperson());
        order.setOrderDate(req.getOrderDate() != null ? req.getOrderDate() : DateUtils.getNowDate());
        order.setRequestDate(req.getRequestDate());
        order.setRemark(req.getRemark());
        order.setOrderType("NEW");
        order.setSampleFlag("N");
        // CRM 推单无 MES 内"提交"动作，到 MES 即待审核
        order.setStatus(SalOrderStatus.PENDING.getCode());
        order.setSource(SalConstants.SOURCE_CRM);

        if (req.getLines() == null || req.getLines().isEmpty())
        {
            throw new ServiceException("CRM 推单至少需要一行明细");
        }
        List<SalOrderLine> lines = new java.util.ArrayList<>();
        for (CrmOrderLineDTO dto : req.getLines())
        {
            lines.add(buildLineFromCrm(dto));
        }
        SalOrderCreateRequest payload = new SalOrderCreateRequest();
        payload.setOrder(order);
        payload.setLines(lines);
        return createWithLines(payload);
    }

    /** CRM 明细行 productCode -> 反查物料填充 productId/name/单位 */
    private SalOrderLine buildLineFromCrm(CrmOrderLineDTO dto)
    {
        MdItem query = new MdItem();
        query.setItemCode(dto.getProductCode());
        List<MdItem> items = mdItemMapper.selectMdItemList(query);
        if (items == null || items.isEmpty())
        {
            throw new ServiceException("物料编码不存在:" + dto.getProductCode());
        }
        MdItem item = items.get(0);
        SalOrderLine line = new SalOrderLine();
        line.setProductId(item.getItemId());
        line.setProductCode(item.getItemCode());
        line.setProductName(item.getItemName());
        line.setProductSpc(item.getSpecification());
        line.setUnitOfMeasure(item.getUnitOfMeasure());
        line.setQuantity(dto.getQuantity());
        line.setUnitPrice(dto.getUnitPrice());
        line.setRequestDate(dto.getRequestDate());
        line.setRemark(dto.getRemark());
        return line;
    }

    @Override
    @Transactional
    public SalOrder updateWithLines(SalOrderCreateRequest req)
    {
        SalOrder order = req.getOrder();
        if (order.getOrderId() == null) throw new ServiceException("订单ID不能为空");
        validateOrderCode(order);
        SalOrder existing = salOrderMapper.selectSalOrderByOrderId(order.getOrderId());
        if (existing == null) throw new ServiceException("销售订单不存在");
        if (!SalOrderStatus.PREPARE.is(existing.getStatus()))
        {
            throw new ServiceException("仅待提交(PREPARE)订单可修改,审核中/已确认订单不可改,如需调整请先驳回或取消");
        }
        order.setUpdateBy(SecurityUtils.getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        salOrderMapper.updateSalOrder(order);
        // PREPARE 状态无转工单,可安全全量替换行(line_id 重置不影响 FK)
        salOrderLineMapper.deleteSalOrderLineByOrderId(order.getOrderId());
        saveLines(order.getOrderId(), req.getLines(), true);
        return order;
    }

    @Override
    public SalOrder getDetail(Long orderId)
    {
        SalOrder order = salOrderMapper.selectSalOrderByOrderId(orderId);
        if (order == null) return null;
        List<SalOrderLine> lines = salOrderLineMapper.selectSalOrderLineByOrderId(orderId);
        if (lines != null)
        {
            for (SalOrderLine line : lines)
            {
                fillConvertible(line);
            }
        }
        order.setLines(lines);
        return order;
    }

    @Override
    public int submitOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.PREPARE.is(order.getStatus()))
        {
            throw new ServiceException("仅待提交(PREPARE)订单可提交审核,当前状态:" + order.getStatus());
        }
        List<SalOrderLine> lines = salOrderLineMapper.selectSalOrderLineByOrderId(orderId);
        if (lines == null || lines.isEmpty()) throw new ServiceException("订单无明细行,不可提交审核");
        // 重新提交清空上次驳回原因
        SalOrder update = new SalOrder();
        update.setOrderId(orderId);
        update.setStatus(SalOrderStatus.PENDING.getCode());
        update.setApproveRemark("");
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return salOrderMapper.updateSalOrder(update);
    }

    @Override
    public int approveOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.PENDING.is(order.getStatus()))
        {
            throw new ServiceException("仅待审核(PENDING)订单可审核,当前状态:" + order.getStatus());
        }
        List<SalOrderLine> lines = salOrderLineMapper.selectSalOrderLineByOrderId(orderId);
        if (lines == null || lines.isEmpty()) throw new ServiceException("订单无明细行,不可审核通过");
        SalOrder update = new SalOrder();
        update.setOrderId(orderId);
        update.setStatus(SalOrderStatus.CONFIRMED.getCode());
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(DateUtils.getNowDate());
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return salOrderMapper.updateSalOrder(update);
    }

    @Override
    public int rejectOrder(Long orderId, String remark)
    {
        if (StringUtils.isEmpty(remark)) throw new ServiceException("驳回必须填写审核意见");
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.PENDING.is(order.getStatus()))
        {
            throw new ServiceException("仅待审核(PENDING)订单可驳回,当前状态:" + order.getStatus());
        }
        SalOrder update = new SalOrder();
        update.setOrderId(orderId);
        update.setStatus(SalOrderStatus.PREPARE.getCode());
        update.setApproveRemark(remark);
        // PENDING 态尚未审核通过,approveBy/approveTime 仍为 null,无需清空;保留驳回意见供提交人查看
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return salOrderMapper.updateSalOrder(update);
    }

    @Override
    public Map<String, Object> batchSubmit(Long[] orderIds)
    {
        return executeBatch(orderIds, this::submitOrder);
    }

    @Override
    public Map<String, Object> batchApprove(Long[] orderIds)
    {
        return executeBatch(orderIds, this::approveOrder);
    }

    @Override
    public int closeOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.CONFIRMED.is(order.getStatus())) throw new ServiceException("仅已确认订单可关闭");
        return updateStatus(orderId, SalOrderStatus.CLOSED.getCode());
    }

    @Override
    public int cancelOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (SalOrderStatus.CLOSED.is(order.getStatus())) throw new ServiceException("已关闭订单不可取消");
        if (SalOrderStatus.CANCEL.is(order.getStatus())) throw new ServiceException("订单已取消");
        return updateStatus(orderId, SalOrderStatus.CANCEL.getCode());
    }

    @Override
    @Transactional
    public int deleteSalOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            SalOrder order = salOrderMapper.selectSalOrderByOrderId(orderId);
            if (order == null) continue;
            // 仅待提交(PREPARE)可删:CONFIRMED 可能已转工单,删除会使工单 sales_order_line_id 成孤儿
            if (!SalOrderStatus.PREPARE.is(order.getStatus()))
            {
                throw new ServiceException("订单 " + order.getOrderCode() + " 非待提交状态,不可删除");
            }
            salOrderLineMapper.deleteSalOrderLineByOrderId(orderId);
        }
        return salOrderMapper.deleteSalOrderByOrderIds(orderIds);
    }

    @Override
    public ProWorkorder toWorkorder(SalOrderToWorkorderRequest req)
    {
        validateToWorkorder(req);
        String lockKey = LOCK_PREFIX + req.getLineId();
        // 先锁后事务:锁内显式开 TX,确保可转量快照在锁之后
        return lockTemplate.executeWithResult(lockKey, 5,
                () -> txTemplate.execute(status -> doToWorkorder(req)));
    }

    // ==================== 私有辅助 ====================

    private ProWorkorder doToWorkorder(SalOrderToWorkorderRequest req)
    {
        SalOrderLine line = salOrderLineMapper.selectSalOrderLineByLineId(req.getLineId());
        if (line == null) throw new ServiceException("销售订单明细行不存在");
        SalOrder order = salOrderMapper.selectSalOrderByOrderId(line.getOrderId());
        if (order == null) throw new ServiceException("销售订单不存在");
        if (!SalOrderStatus.CONFIRMED.is(order.getStatus())) throw new ServiceException("仅已确认订单可转工单");

        BigDecimal produced = salOrderLineMapper.sumProducedQtyByLineId(req.getLineId());
        if (produced == null) produced = BigDecimal.ZERO;
        BigDecimal convertible = (line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity()).subtract(produced);
        if (req.getQuantity().compareTo(convertible) > 0)
        {
            throw new ServiceException("转工单数量超过可转数量(" + convertible + ")");
        }
        ProWorkorder wo = buildWorkorderFromLine(order, line, req);
        // 路线 BOM 不存 itemOrProduct；按物料编码前缀推导默认值
        if (req.getBomList() != null) {
            for (ProWorkorderBom bom : req.getBomList()) {
                if (bom.getItemOrProduct() == null) {
                    String code = bom.getItemCode();
                    if (code != null && code.startsWith("AUX-")) bom.setItemOrProduct("AUXILIARY");
                    else if (code != null && code.startsWith("PACK-")) bom.setItemOrProduct("PACK");
                    else if (code != null && code.startsWith("SEMI-")) bom.setItemOrProduct("SEMI");
                    else bom.setItemOrProduct("RAW");
                }
            }
        }
        return proWorkorderService.createWorkorderWithBom(wo, req.getBomList(), req.getParamList());
    }

    private ProWorkorder buildWorkorderFromLine(SalOrder order, SalOrderLine line, SalOrderToWorkorderRequest req)
    {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderCode(req.getWorkorderCode());
        wo.setWorkorderName(StringUtils.isNotEmpty(req.getWorkorderName())
                ? req.getWorkorderName()
                : (StringUtils.isNotEmpty(line.getProductName()) ? line.getProductName() : order.getOrderName())
                        + "-" + order.getOrderCode());
        wo.setWorkorderType("SELF");
        wo.setOrderSource("SALES_ORDER");
        wo.setSourceCode(order.getOrderCode());
        wo.setSalesOrderLineId(line.getLineId());
        wo.setProductId(line.getProductId());
        wo.setProductCode(line.getProductCode());
        wo.setProductName(line.getProductName());
        wo.setProductSpc(line.getProductSpc());
        wo.setUnitOfMeasure(line.getUnitOfMeasure());
        wo.setUnitName(line.getUnitName());
        wo.setQuantity(req.getQuantity());
        wo.setClientId(order.getClientId());
        wo.setClientCode(order.getClientCode());
        wo.setClientName(order.getClientName());
        wo.setClientOrderCode(order.getClientOrderCode());
        wo.setProductSize(line.getProductSize());
        wo.setPrintingReq(line.getPrintingReq());
        wo.setRopeSpec(line.getRopeSpec());
        wo.setPackageReq(line.getPackageReq());
        wo.setShippingReq(line.getShippingReq());
        // 扩展属性（分类驱动的动态属性）从销售明细继承到工单（深拷贝避免共享引用）
        wo.setLineAttrs(line.getLineAttrs() == null ? null : new java.util.HashMap<>(line.getLineAttrs()));
        wo.setOrderType(StringUtils.isNotEmpty(order.getOrderType()) ? order.getOrderType() : "NEW");
        wo.setRequestDate(req.getRequestDate() != null ? req.getRequestDate()
                : (line.getRequestDate() != null ? line.getRequestDate() : order.getRequestDate()));
        wo.setRouteProductId(req.getRouteProductId());
        wo.setCreateSkuVariant(req.getCreateSkuVariant());
        wo.setSkuCode(req.getSkuCode());
        wo.setSkuName(req.getSkuName());
        wo.setStatus("PREPARE");
        wo.setRemark(req.getRemark());
        return wo;
    }

    private void saveLines(Long orderId, List<SalOrderLine> lines, boolean isCreate)
    {
        if (lines == null || lines.isEmpty()) return;
        int lineNo = 1;
        for (SalOrderLine line : lines)
        {
            if (!isCreate) line.setLineId(null);
            line.setOrderId(orderId);
            line.setLineNo(lineNo++);
            if (line.getQuantity() == null) throw new ServiceException("明细行订单数量不能为空");
            if (line.getLineAmount() == null && line.getUnitPrice() != null)
            {
                line.setLineAmount(line.getUnitPrice().multiply(line.getQuantity()));
            }
            line.setCreateBy(SecurityUtils.getUsername());
            line.setCreateTime(DateUtils.getNowDate());
            salOrderLineMapper.insertSalOrderLine(line);
        }
    }

    private void fillConvertible(SalOrderLine line)
    {
        BigDecimal produced = salOrderLineMapper.sumProducedQtyByLineId(line.getLineId());
        if (produced == null) produced = BigDecimal.ZERO;
        line.setQuantityProduced(produced);
        BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
        line.setQuantityConvertible(qty.subtract(produced));
    }

    private void validateOrderCode(SalOrder order)
    {
        if (StringUtils.isEmpty(order.getOrderCode())) throw new ServiceException("销售订单号不能为空");
        if (checkOrderCodeUnique(order)) throw new ServiceException("销售订单号已存在:" + order.getOrderCode());
    }

    private void validateToWorkorder(SalOrderToWorkorderRequest req)
    {
        if (req.getLineId() == null) throw new ServiceException("明细行ID不能为空");
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new ServiceException("转工单数量必须大于0");
        if (StringUtils.isEmpty(req.getWorkorderCode())) throw new ServiceException("工单编码不能为空");
    }

    private SalOrder mustExist(Long orderId)
    {
        SalOrder order = salOrderMapper.selectSalOrderByOrderId(orderId);
        if (order == null) throw new ServiceException("销售订单不存在");
        return order;
    }

    private int updateStatus(Long orderId, String status)
    {
        SalOrder update = new SalOrder();
        update.setOrderId(orderId);
        update.setStatus(status);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return salOrderMapper.updateSalOrder(update);
    }

    /** 批量流转骨架:逐张调用单条动作,失败不中断,收集到 failures(仿 WmIssueHeaderServiceImpl) */
    private Map<String, Object> executeBatch(Long[] orderIds, Consumer<Long> action)
    {
        if (orderIds == null || orderIds.length == 0)
        {
            throw new ServiceException("未选择销售订单");
        }
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        for (Long id : orderIds)
        {
            try
            {
                action.accept(id);
                success++;
            }
            catch (Exception e)
            {
                // ServiceException 是预期业务失败,仅收集;非 ServiceException 留 warn 日志
                if (!(e instanceof ServiceException))
                {
                    log.warn("批量流转失败(非业务异常), orderId={}", id, e);
                }
                SalOrder o = salOrderMapper.selectSalOrderByOrderId(id);
                Map<String, Object> f = new HashMap<>();
                f.put("orderId", id);
                f.put("orderCode", o != null ? o.getOrderCode() : null);
                f.put("orderName", o != null ? o.getOrderName() : null);
                String msg = e.getMessage();
                f.put("reason", (msg != null && !msg.isEmpty()) ? msg : e.getClass().getSimpleName());
                failures.add(f);
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("total", orderIds.length);
        r.put("successCount", success);
        r.put("failedCount", failures.size());
        r.put("failures", failures);
        return r;
    }
}
