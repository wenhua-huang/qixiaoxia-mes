package com.ruoyi.system.service.mes.sal.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.SalOrderStatus;
import com.ruoyi.common.enums.SalOrderType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.domain.mes.pro.ProRoute;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;
import com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.CrmOrderLineDTO;
import com.ruoyi.system.domain.mes.sal.SalConstants;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;
import com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow;
import com.ruoyi.system.domain.mes.sal.vo.SalOrderWorkorderCountRow;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.pro.ProRouteResolveService;
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
    private ProRouteResolveService proRouteResolveService;

    @Autowired
    private ProRouteProductMapper proRouteProductMapper;

    @Autowired
    private ProRouteMapper proRouteMapper;

    @Autowired
    private ProRouteProcessMapper proRouteProcessMapper;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    private static final String LOCK_PREFIX = "sal:order:line:toWorkorder:";
    private static final String YES = "Y";
    private static final String NO = "N";
    /** qxx_pro_route_process.is_outsource 外发节点取值为 1/0（与订单 Y/N 标志不同源） */
    private static final String OUTSOURCE_NODE = "1";

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
        // 建单即待接单（V161），忽略前端可能传入的状态；人工接单后才 CONFIRMED
        order.setStatus(SalOrderStatus.PENDING_ACCEPT.getCode());
        normalizeOrderDimensions(order);
        if (order.getSampleFlag() == null) order.setSampleFlag("N");
        if (order.getSource() == null) order.setSource(SalConstants.SOURCE_DIRECT);
        order.setCreateBy(SecurityUtils.getUsername());
        order.setCreateTime(DateUtils.getNowDate());
        salOrderMapper.insertSalOrder(order);
        saveLines(order, req.getLines(), true);
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
        order.setOrderType(SalOrderType.STANDARD.getCode());
        order.setSampleFlag(NO);
        order.setOutsourceFlag(NO);
        order.setPackageFlag(NO);
        // CRM 推单同样落待接单，需 MES 内人工接单（createWithLines 还会再强制一次）
        order.setStatus(SalOrderStatus.PENDING_ACCEPT.getCode());
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
        if (!SalOrderStatus.PENDING_ACCEPT.is(existing.getStatus())
                && !SalOrderStatus.CONFIRMED.is(existing.getStatus())) {
            throw new ServiceException("仅待接单/已确认订单可修改,生产中已派生工单不可改,如需调整请取消后重建");
        }
        assertNoDerivedWorkorder(order.getOrderId(), "修改");
        normalizeOrderDimensions(order);
        order.setStatus(existing.getStatus()); // 状态不允许经编辑接口篡改
        // 审核历史列为留存字段,不允许经编辑接口覆写,强制以库中值为准
        order.setApproveBy(existing.getApproveBy());
        order.setApproveTime(existing.getApproveTime());
        order.setApproveRemark(existing.getApproveRemark());
        order.setUpdateBy(SecurityUtils.getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        salOrderMapper.updateSalOrder(order);
        // CONFIRMED 且未派生工单才可整单改单,全量替换行（派生工单即使未开工也被上面闸门拦截，避免工单引用成孤儿）
        salOrderLineMapper.deleteSalOrderLineByOrderId(order.getOrderId());
        saveLines(order, req.getLines(), true);
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
        enrichOrderProgress(List.of(order));
        order.setLines(lines);
        return order;
    }

    @Override
    public void enrichOrderProgress(List<SalOrder> list)
    {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(SalOrder::getOrderId).collect(Collectors.toList());
        Map<Long, Integer> pmap = new HashMap<>();
        for (SalOrderProgressRow r : salOrderMapper.selectProgressByOrderIds(ids))
        {
            pmap.put(r.getOrderId(), r.getProgressPercent());
        }
        Map<Long, Integer> wmap = new HashMap<>();
        List<SalOrderWorkorderCountRow> crows = salOrderMapper.selectWorkorderCountsByOrderIds(ids);
        if (crows != null)
        {
            for (SalOrderWorkorderCountRow r : crows)
            {
                wmap.put(r.getOrderId(), r.getWorkorderCount() == null ? 0 : r.getWorkorderCount());
            }
        }
        // 无聚合行（无未取消任务/工单）的订单在 SQL 结果中缺席，回填 0
        for (SalOrder o : list)
        {
            o.setProgressPercent(pmap.getOrDefault(o.getOrderId(), 0));
            o.setWorkorderCount(wmap.getOrDefault(o.getOrderId(), 0));
        }
    }

    @Override
    public int closeOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.SHIPPED.is(order.getStatus())) throw new ServiceException("仅已出货订单可结单");
        return updateStatus(orderId, SalOrderStatus.CLOSED.getCode());
    }

    // PENDING_ACCEPT/CONFIRMED/PRODUCING 均可取消（SHIPPED/CLOSED/CANCEL 拦截）
    @Override
    public int cancelOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (SalOrderStatus.SHIPPED.is(order.getStatus())
                || SalOrderStatus.CLOSED.is(order.getStatus())
                || SalOrderStatus.CANCEL.is(order.getStatus())) {
            throw new ServiceException("已出货/已结单/已取消订单不可取消，关联工单需另行处理");
        }
        return updateStatus(orderId, SalOrderStatus.CANCEL.getCode());
    }

    @Override
    public int acceptOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.PENDING_ACCEPT.is(order.getStatus())) {
            throw new ServiceException("仅待接单订单可接单");
        }
        return updateStatus(orderId, SalOrderStatus.CONFIRMED.getCode());
    }

    @Override
    @Transactional
    public int deleteSalOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            SalOrder order = salOrderMapper.selectSalOrderByOrderId(orderId);
            if (order == null) continue;
            // 仅待接单/已确认可删:转工单后(PRODUCING 起)删除会使工单 sales_order_line_id 成孤儿
            if (!SalOrderStatus.PENDING_ACCEPT.is(order.getStatus())
                    && !SalOrderStatus.CONFIRMED.is(order.getStatus()))
            {
                throw new ServiceException("订单 " + order.getOrderCode() + " 非待接单/已确认状态,不可删除");
            }
            assertNoDerivedWorkorder(orderId, "删除");
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
        if (!SalOrderStatus.CONFIRMED.is(order.getStatus())
                && !SalOrderStatus.PRODUCING.is(order.getStatus())) {
            throw new ServiceException("仅已确认/生产中订单可转工单");
        }

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
        wo.setOrderType(StringUtils.isNotEmpty(order.getOrderType()) ? order.getOrderType() : SalOrderType.STANDARD.getCode());
        wo.setRequestDate(req.getRequestDate() != null ? req.getRequestDate()
                : (line.getRequestDate() != null ? line.getRequestDate() : order.getRequestDate()));
        wo.setRouteProductId(resolveWorkorderRoute(order, line, req));
        wo.setCreateSkuVariant(req.getCreateSkuVariant());
        wo.setSkuCode(req.getSkuCode());
        wo.setSkuName(req.getSkuName());
        wo.setStatus("PREPARE"); // ProConstants 工单初态
        wo.setRemark(req.getRemark());
        return wo;
    }

    /**
     * 转工单项路线: 请求显式指定 → 订单行存值 → 现场按头维度解析, 三级兜底;
     * 最终路线必须属于该产品, 外发订单还必须含实际外发节点, 否则阻断。
     */
    private Long resolveWorkorderRoute(SalOrder order, SalOrderLine line, SalOrderToWorkorderRequest req)
    {
        Long routeProductId = req.getRouteProductId() != null
                ? req.getRouteProductId() : line.getRouteProductId();
        boolean outsource = YES.equals(order.getOutsourceFlag());
        if (routeProductId == null)
        {
            RouteResolveResult result = proRouteResolveService.resolve(line.getProductId(),
                    order.getOrderType(), order.getOutsourceFlag(), order.getPackageFlag());
            if (result.isHardBlocked()) throw new ServiceException(result.getMessage());
            if (result.isMatched()) return result.getRouteProductId();
            if (outsource)
            {
                throw new ServiceException("外发订单必须选择含外发工序的工艺路线后再转工单: " + line.getProductName());
            }
            return null;
        }
        ProRouteProduct binding = proRouteProductMapper.selectProRouteProductByRecordId(routeProductId);
        if (binding == null || !Objects.equals(binding.getItemId(), line.getProductId()))
        {
            throw new ServiceException("选择的工艺路线不属于该产品: " + line.getProductName());
        }
        if (outsource && !routeHasOutsourceNode(binding.getRouteId()))
        {
            throw new ServiceException("订单标记外发，但所选路线不含外发工序: " + line.getProductName());
        }
        return routeProductId;
    }

    private void saveLines(SalOrder order, List<SalOrderLine> lines, boolean isCreate)
    {
        if (lines == null || lines.isEmpty()) return;
        int lineNo = 1;
        for (SalOrderLine line : lines)
        {
            if (!isCreate) line.setLineId(null);
            line.setOrderId(order.getOrderId());
            line.setLineNo(lineNo++);
            if (line.getQuantity() == null) throw new ServiceException("明细行订单数量不能为空");
            if (line.getLineAmount() == null && line.getUnitPrice() != null)
            {
                line.setLineAmount(line.getUnitPrice().multiply(line.getQuantity()));
            }
            line.setCreateBy(SecurityUtils.getUsername());
            line.setCreateTime(DateUtils.getNowDate());
        }
        resolveLinesRoute(order, lines);
        for (SalOrderLine line : lines)
        {
            salOrderLineMapper.insertSalOrderLine(line);
        }
    }

    /**
     * 批量解析/校验全部明细行路线: 未手选的走一次 resolveBatch, 手选的一次 IN 拉绑定,
     * 外发节点与路线名快照同样批量预取, 全程无逐行查询。
     */
    private void resolveLinesRoute(SalOrder order, List<SalOrderLine> lines)
    {
        Map<Long, ProRouteProduct> manualBindings = loadManualBindings(lines);
        Map<Long, RouteResolveResult> autoResults = resolveAutoLines(order, lines);
        Set<Long> outsourceRoutes = YES.equals(order.getOutsourceFlag())
                ? outsourceNodeRoutes(manualBindings.values()) : Set.of();
        Map<Long, ProRoute> routeMap = loadRouteSnapshots(lines, manualBindings, autoResults);

        for (SalOrderLine line : lines)
        {
            Long routeId = assignLineRoute(order, line, manualBindings, autoResults, outsourceRoutes);
            ProRoute route = routeId == null ? null : routeMap.get(routeId);
            if (route != null)
            {
                line.setRouteCode(route.getRouteCode());
                line.setRouteName(route.getRouteName());
            }
        }
    }

    private Map<Long, ProRouteProduct> loadManualBindings(List<SalOrderLine> lines)
    {
        Set<Long> recordIds = lines.stream().map(SalOrderLine::getRouteProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (recordIds.isEmpty()) return Map.of();
        return proRouteProductMapper.selectByRecordIds(recordIds).stream()
                .collect(Collectors.toMap(ProRouteProduct::getRecordId, b -> b, (a, b) -> a));
    }

    private Map<Long, RouteResolveResult> resolveAutoLines(SalOrder order, List<SalOrderLine> lines)
    {
        List<Long> itemIds = lines.stream()
                .filter(l -> l.getRouteProductId() == null)
                .map(SalOrderLine::getProductId).filter(Objects::nonNull).distinct().toList();
        if (itemIds.isEmpty()) return Map.of();
        return proRouteResolveService.resolveBatch(itemIds, order.getOrderType(),
                order.getOutsourceFlag(), order.getPackageFlag());
    }

    /** 含 is_outsource='1' 节点的路线ID集合（一次 IN 查询） */
    private Set<Long> outsourceNodeRoutes(java.util.Collection<ProRouteProduct> bindings)
    {
        Set<Long> routeIds = bindings.stream().map(ProRouteProduct::getRouteId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (routeIds.isEmpty()) return Set.of();
        return proRouteProcessMapper.selectByRouteIds(routeIds).stream()
                .filter(n -> OUTSOURCE_NODE.equals(n.getIsOutsource()))
                .map(ProRouteProcess::getRouteId).collect(Collectors.toSet());
    }

    private Map<Long, ProRoute> loadRouteSnapshots(List<SalOrderLine> lines,
            Map<Long, ProRouteProduct> manualBindings, Map<Long, RouteResolveResult> autoResults)
    {
        Set<Long> routeIds = new HashSet<>(manualBindings.values().stream()
                .map(ProRouteProduct::getRouteId).filter(Objects::nonNull).collect(Collectors.toSet()));
        autoResults.values().stream().filter(RouteResolveResult::isMatched)
                .map(RouteResolveResult::getRouteId).forEach(routeIds::add);
        if (routeIds.isEmpty()) return Map.of();
        return proRouteMapper.selectByRouteIds(routeIds).stream()
                .collect(Collectors.toMap(ProRoute::getRouteId, r -> r, (a, b) -> a));
    }

    /** 落定单行路线: 自动解析(阻断即抛)或手选归属/外发节点校验, 返回最终 routeId(无则 null) */
    private Long assignLineRoute(SalOrder order, SalOrderLine line,
            Map<Long, ProRouteProduct> manualBindings, Map<Long, RouteResolveResult> autoResults,
            Set<Long> outsourceRoutes)
    {
        if (line.getRouteProductId() == null)
        {
            RouteResolveResult result = autoResults.get(line.getProductId());
            if (result != null && result.isHardBlocked()) throw new ServiceException(result.getMessage());
            if (result == null || !result.isMatched()) return null;
            line.setRouteProductId(result.getRouteProductId());
            return result.getRouteId();
        }
        ProRouteProduct binding = manualBindings.get(line.getRouteProductId());
        if (binding == null || !Objects.equals(binding.getItemId(), line.getProductId()))
        {
            throw new ServiceException("明细行选择的工艺路线不属于该产品: " + line.getProductName());
        }
        if (YES.equals(order.getOutsourceFlag()) && !outsourceRoutes.contains(binding.getRouteId()))
        {
            throw new ServiceException("订单标记外发，但所选路线不含外发工序: " + line.getProductName());
        }
        return binding.getRouteId();
    }

    private boolean routeHasOutsourceNode(Long routeId)
    {
        List<ProRouteProcess> nodes = proRouteProcessMapper.selectProRouteProcessByRouteId(routeId);
        return nodes != null && nodes.stream().anyMatch(n -> OUTSOURCE_NODE.equals(n.getIsOutsource()));
    }

    private void fillConvertible(SalOrderLine line)
    {
        BigDecimal produced = salOrderLineMapper.sumProducedQtyByLineId(line.getLineId());
        if (produced == null) produced = BigDecimal.ZERO;
        line.setQuantityProduced(produced);
        BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
        line.setQuantityConvertible(qty.subtract(produced));
    }

    /**
     * 头维度归一化：null 给默认值；订单类型必须是 5 值枚举之一；两标志只接受 Y/N。
     * 不信任前端值域，防止 "y"/"YES" 之类绕过 "Y".equals 外发硬阻断。
     */
    private void normalizeOrderDimensions(SalOrder order)
    {
        String type = order.getOrderType();
        if (type == null)
        {
            order.setOrderType(SalOrderType.STANDARD.getCode());
        }
        else if (SalOrderType.fromCode(type) == null)
        {
            throw new ServiceException("非法订单类型: " + type);
        }
        order.setOutsourceFlag(normalizeYn(order.getOutsourceFlag(), "是否外发"));
        order.setPackageFlag(normalizeYn(order.getPackageFlag(), "是否包装"));
    }

    private String normalizeYn(String value, String field)
    {
        if (value == null) return NO;
        if (YES.equals(value) || NO.equals(value)) return value;
        throw new ServiceException(field + "只接受 Y/N, 实际: " + value);
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

    /** 改/删闸门：订单 CONFIRMED 但已派生工单（含未开工 PREPARE）时拒绝，避免行硬删使工单引用成孤儿+可转量重复占用 */
    private void assertNoDerivedWorkorder(Long orderId, String action)
    {
        List<SalOrderWorkorderCountRow> rows =
                salOrderMapper.selectWorkorderCountsByOrderIds(List.of(orderId));
        if (rows != null && rows.stream().anyMatch(r -> r.getWorkorderCount() != null && r.getWorkorderCount() > 0))
        {
            throw new ServiceException("订单已派生工单，不可" + action + "，如需调整请取消后重建");
        }
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
