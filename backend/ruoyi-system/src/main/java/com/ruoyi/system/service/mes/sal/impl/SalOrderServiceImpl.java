package com.ruoyi.system.service.mes.sal.impl;

import java.math.BigDecimal;
import java.util.List;
import jakarta.annotation.PostConstruct;
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
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.sal.ISalOrderService;

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
    @Autowired
    private SalOrderMapper salOrderMapper;

    @Autowired
    private SalOrderLineMapper salOrderLineMapper;

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
        if (order.getStatus() == null) order.setStatus("PREPARE");
        if (order.getOrderType() == null) order.setOrderType("NEW");
        if (order.getSampleFlag() == null) order.setSampleFlag("N");
        order.setCreateBy(SecurityUtils.getUsername());
        order.setCreateTime(DateUtils.getNowDate());
        salOrderMapper.insertSalOrder(order);
        saveLines(order.getOrderId(), req.getLines(), true);
        return order;
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
        if (!"PREPARE".equals(existing.getStatus()))
        {
            throw new ServiceException("已确认/关闭/取消的订单不可修改明细,如需调整请先取消重建");
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
    public int confirmOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!"PREPARE".equals(order.getStatus())) throw new ServiceException("仅待确认订单可确认");
        List<SalOrderLine> lines = salOrderLineMapper.selectSalOrderLineByOrderId(orderId);
        if (lines == null || lines.isEmpty()) throw new ServiceException("订单无明细行,不可确认");
        return updateStatus(orderId, "CONFIRMED");
    }

    @Override
    public int closeOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!"CONFIRMED".equals(order.getStatus())) throw new ServiceException("仅已确认订单可关闭");
        return updateStatus(orderId, "CLOSED");
    }

    @Override
    public int cancelOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if ("CLOSED".equals(order.getStatus())) throw new ServiceException("已关闭订单不可取消");
        if ("CANCEL".equals(order.getStatus())) throw new ServiceException("订单已取消");
        return updateStatus(orderId, "CANCEL");
    }

    @Override
    @Transactional
    public int deleteSalOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            SalOrder order = salOrderMapper.selectSalOrderByOrderId(orderId);
            if (order == null) continue;
            // 仅待确认(PREPARE)可删:CONFIRMED 可能已转工单,删除会使工单 sales_order_line_id 成孤儿
            if (!"PREPARE".equals(order.getStatus()))
            {
                throw new ServiceException("订单 " + order.getOrderCode() + " 非待确认状态,不可删除");
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
        if (!"CONFIRMED".equals(order.getStatus())) throw new ServiceException("仅已确认订单可转工单");

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
}
