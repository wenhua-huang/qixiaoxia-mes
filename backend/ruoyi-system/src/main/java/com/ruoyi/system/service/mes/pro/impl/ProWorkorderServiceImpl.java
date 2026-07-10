package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.WmIssueConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProWorkorderDeviationVO;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.service.mes.pro.IProWorkorderBomService;
import com.ruoyi.system.service.mes.pro.IProWorkorderParamService;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.wm.IWmMaterialStockService;
import com.ruoyi.system.service.mes.wm.IWmIssueHeaderService;
import com.ruoyi.system.service.mes.wm.IWmIssueLineService;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;
import com.ruoyi.system.domain.mes.pro.ProWorkorderChange;
import com.ruoyi.system.service.mes.pro.IProWorkorderChangeService;
import com.ruoyi.system.domain.mes.pro.ProWorkorderDetailVO;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProRoute;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;
import com.ruoyi.system.domain.mes.pro.ProProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.service.mes.pro.IProRouteProductService;
import com.ruoyi.system.service.mes.pro.IProRouteService;
import com.ruoyi.system.service.mes.pro.IProRouteProcessService;
import com.ruoyi.system.service.mes.pro.IProParamTemplateService;
import com.ruoyi.system.service.mes.pro.IProProcessService;
import com.ruoyi.system.service.mes.pro.IProTaskService;
import com.ruoyi.system.service.mes.pro.IProRouteProductBomService;
import com.ruoyi.system.service.mes.pro.IProRouteProcessParamService;
import com.ruoyi.system.domain.mes.pro.ProRouteProductBom;
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;

/**
 * 生产工单Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProWorkorderServiceImpl implements IProWorkorderService
{
    @Autowired
    private ProWorkorderMapper qxxProWorkorderMapper;

    @Autowired
    private IProWorkorderBomService proWorkorderBomService;

    @Autowired
    private IProWorkorderParamService proWorkorderParamService;

    @Autowired
    private IWmMaterialStockService wmMaterialStockService;

    @Autowired
    private IProWorkorderChangeService proWorkorderChangeService;

    @Autowired
    private IProRouteProductService proRouteProductService;

    @Autowired
    private IProRouteService proRouteService;

    @Autowired
    private IProRouteProcessService proRouteProcessService;

    @Autowired
    private IProParamTemplateService proParamTemplateService;

    @Autowired
    private IProProcessService proProcessService;

    @Autowired
    private IProRouteProductBomService proRouteProductBomService;

    @Autowired
    private IProRouteProcessParamService proRouteProcessParamService;

    @Autowired
    private IProTaskService proTaskService;

    @Autowired
    private IWmIssueHeaderService wmIssueHeaderService;

    @Autowired
    private IWmIssueLineService wmIssueLineService;

    @Autowired
    private IWmWarehouseService wmWarehouseService;

    @Autowired
    private com.ruoyi.system.service.mes.md.IMdItemService mdItemService;

    @Autowired
    private com.ruoyi.system.service.mes.md.IMdItemBatchConfigService mdItemBatchConfigService;

    @Autowired
    private com.ruoyi.system.service.mes.md.IMdProductBomService mdProductBomService;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager txManager;

    /**
     * 查询生产工单
     *
     * @param workorderId 生产工单主键
     * @return 生产工单
     */
    @Override
    public ProWorkorder selectProWorkorderByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
    }

    /**
     * 查询生产工单详情（含BOM、工序参数、路线工序、路线选项，一次返回前端所需全部数据）
     *
     * @param workorderId 生产工单主键
     * @return 生产工单详情VO
     */
    @Override
    public ProWorkorderDetailVO getWorkorderDetail(Long workorderId)
    {
        ProWorkorderDetailVO vo = new ProWorkorderDetailVO();
        ProWorkorder wo = selectProWorkorderByWorkorderId(workorderId);
        vo.setWorkorder(wo);

        // 预加载路线产品及路线工序（BOM和参数可能需要从路线模板回填，工序名用于富化）
        ProRouteProduct routeProduct = null;
        java.util.Set<Long> routePids = new java.util.HashSet<>();
        List<ProRouteProcess> cachedRouteProcesses = null;
        java.util.Map<Long, String> procNameMap = new java.util.HashMap<>();
        if (wo.getRouteProductId() != null)
        {
            routeProduct = proRouteProductService.selectProRouteProductByRecordId(wo.getRouteProductId());
            if (routeProduct != null)
            {
                cachedRouteProcesses = proRouteProcessService.selectProRouteProcessByRouteId(routeProduct.getRouteId());
                if (cachedRouteProcesses != null)
                    for (ProRouteProcess p : cachedRouteProcesses)
                    {
                        routePids.add(p.getProcessId());
                        procNameMap.put(p.getProcessId(), p.getProcessName());
                    }
            }
        }
        // 补全工序名：从工序主数据加载（路线工序可能不包含所有工序）
        List<ProProcess> allProcesses = proProcessService.selectProProcessList(new ProProcess());
        if (allProcesses != null)
            for (ProProcess p : allProcesses)
                procNameMap.putIfAbsent(p.getProcessId(), p.getProcessName());

        // 1. BOM 物料行 — 已保存数据优先，空则从工艺路线模板回填
        List<ProWorkorderBom> bomList = proWorkorderBomService.selectProWorkorderBomByWorkorderId(workorderId);
        if ((bomList == null || bomList.isEmpty()) && routeProduct != null)
        {
            List<ProRouteProductBom> routeBoms = proRouteProductBomService.selectProRouteProductBomByRouteId(routeProduct.getRouteId());
            if (routeBoms != null && !routeBoms.isEmpty())
            {
                bomList = new ArrayList<>();
                for (ProRouteProductBom rb : routeBoms)
                {
                    // 只加载匹配当前产品且属于路线工序的 BOM 行
                    if (rb.getProductId() != null && routeProduct.getItemId() != null
                            && !rb.getProductId().equals(routeProduct.getItemId()))
                        continue;
                    if (rb.getProcessId() != null && !routePids.isEmpty()
                            && !routePids.contains(rb.getProcessId()))
                        continue;
                    ProWorkorderBom bom = new ProWorkorderBom();
                    bom.setProcessId(rb.getProcessId());
                    bom.setProcessName(rb.getProcessId() != null ? procNameMap.getOrDefault(rb.getProcessId(), "") : "");
                    bom.setItemId(rb.getItemId());
                    bom.setItemCode(rb.getItemCode());
                    bom.setItemName(rb.getItemName());
                    bom.setItemSpc(rb.getSpecification());
                    bom.setUnitOfMeasure(rb.getUnitOfMeasure());
                    bom.setUnitName(rb.getUnitName());
                    bom.setItemOrProduct("RAW");
                    // 数量转换：Double → BigDecimal
                    if (rb.getQuantity() != null)
                        bom.setQuantity(new java.math.BigDecimal(rb.getQuantity()));
                    bomList.add(bom);
                }
            }
        }
        vo.setBomList(bomList != null ? bomList : new ArrayList<>());

        // 2. 工序参数 — 已保存数据优先，空则从工艺路线模板回填（L2→L1），并富化模板名称和工序名称
        List<ProWorkorderParam> rawParams = proWorkorderParamService.selectProWorkorderParamByWorkorderId(workorderId);
        if ((rawParams == null || rawParams.isEmpty()) && routeProduct != null)
        {
            // L2：路线工序参数
            List<ProRouteProcessParam> routeParams = proRouteProcessParamService.selectProRouteProcessParamByRouteProductId(wo.getRouteProductId());
            if (routeParams != null && !routeParams.isEmpty())
            {
                rawParams = new ArrayList<>();
                for (ProRouteProcessParam rpp : routeParams)
                {
                    ProWorkorderParam wp = new ProWorkorderParam();
                    wp.setRouteProductId(wo.getRouteProductId());
                    wp.setTemplateId(rpp.getTemplateId());
                    wp.setStandardValue(rpp.getParamValue());
                    rawParams.add(wp);
                }
            }
            else
            {
                // L1：参数模板（无 L2 数据时回退）
                List<ProParamTemplate> templates = proParamTemplateService.selectProParamTemplateList(new ProParamTemplate());
                if (templates != null && !templates.isEmpty())
                {
                    rawParams = new ArrayList<>();
                    for (ProParamTemplate t : templates)
                    {
                        if (!routePids.isEmpty() && t.getProcessId() != null
                                && !routePids.contains(t.getProcessId()))
                            continue;
                        ProWorkorderParam wp = new ProWorkorderParam();
                        wp.setRouteProductId(wo.getRouteProductId());
                        wp.setTemplateId(t.getTemplateId());
                        wp.setStandardValue(t.getDefaultValue());
                        rawParams.add(wp);
                    }
                }
            }
        }

        // 富化参数（模板名称 + 工序名称）
        if (rawParams != null && !rawParams.isEmpty())
        {
            List<ProParamTemplate> allTemplates = proParamTemplateService.selectProParamTemplateList(new ProParamTemplate());
            java.util.Map<Long, ProParamTemplate> tMap = new java.util.HashMap<>();
            if (allTemplates != null)
                for (ProParamTemplate t : allTemplates)
                    tMap.put(t.getTemplateId(), t);

            // 工序名已在预加载阶段准备好（procNameMap），直接复用

            List<java.util.Map<String, Object>> enrichedParams = new java.util.ArrayList<>();
            for (ProWorkorderParam p : rawParams)
            {
                java.util.Map<String, Object> ep = new java.util.HashMap<>();
                ep.put("recordId", p.getRecordId());
                ep.put("workorderId", p.getWorkorderId());
                ep.put("routeProductId", p.getRouteProductId());
                ep.put("templateId", p.getTemplateId());
                ep.put("factoryId", p.getFactoryId());

                ProParamTemplate tmpl = tMap.get(p.getTemplateId());
                String paramName = (tmpl != null) ? tmpl.getParamName() : "";
                String defVal = (tmpl != null) ? tmpl.getDefaultValue() : "";
                Long tmplProcessId = (tmpl != null) ? tmpl.getProcessId() : null;

                String stdVal = (p.getStandardValue() != null && !p.getStandardValue().isEmpty())
                        ? p.getStandardValue() : defVal;
                if ((stdVal == null || stdVal.isEmpty()) && p.getAdjustedValue() != null)
                    stdVal = p.getAdjustedValue();

                ep.put("standardValue", stdVal);
                ep.put("adjustedValue", p.getAdjustedValue());
                ep.put("remark", p.getRemark());

                String processName = procNameMap.get(tmplProcessId);
                ep.put("processId", tmplProcessId);
                ep.put("_processName", processName != null ? processName : "");
                ep.put("_paramName", paramName);
                ep.put("paramName", paramName);

                enrichedParams.add(ep);
            }
            vo.setParamList(enrichedParams);
        }
        else
        {
            vo.setParamList(new java.util.ArrayList<>());
        }

        // 3. 路线工序（用于 Step 2 accordion 骨架，复用预加载数据）
        vo.setRouteProcesses(cachedRouteProcesses != null ? cachedRouteProcesses : new java.util.ArrayList<>());

        // 4. 可选路线产品列表（编辑时用于路线下拉框切换）
        if (wo.getProductId() != null)
        {
            ProRouteProduct query = new ProRouteProduct();
            query.setItemId(wo.getProductId());
            List<ProRouteProduct> routeProducts = proRouteProductService.selectProRouteProductList(query);
            if (routeProducts != null && !routeProducts.isEmpty())
            {
                // 预加载所有路线名称
                List<ProRoute> allRoutes = proRouteService.selectProRouteList(new ProRoute());
                java.util.Map<Long, String> routeNameMap = new java.util.HashMap<>();
                if (allRoutes != null)
                {
                    for (ProRoute r : allRoutes)
                        routeNameMap.put(r.getRouteId(), 
    (r.getRouteName() != null && !r.getRouteName().isEmpty()) ? r.getRouteName() 
    : (r.getRouteCode() != null ? r.getRouteCode() : "路线#" + r.getRouteId()));
                }

                List<java.util.Map<String, Object>> routeOptions = new java.util.ArrayList<>();
                for (ProRouteProduct rp : routeProducts)
                {
                    java.util.Map<String, Object> opt = new java.util.HashMap<>();
                    opt.put("recordId", rp.getRecordId());
                    opt.put("routeId", rp.getRouteId());
                    opt.put("itemId", rp.getItemId());
                    opt.put("itemName", rp.getItemName());
                    opt.put("_routeName", routeNameMap.getOrDefault(rp.getRouteId(), "路线#" + rp.getRouteId()));
                    routeOptions.add(opt);
                }
                vo.setRouteOptions(routeOptions);
            }
            else
            {
                vo.setRouteOptions(new java.util.ArrayList<>());
            }
        }
        else
        {
            vo.setRouteOptions(new java.util.ArrayList<>());
        }

        return vo;
    }

    /**
     * 查询生产工单列表
     *
     * @param proWorkorder 生产工单
     * @return 生产工单
     */
    @Override
    public List<ProWorkorder> selectProWorkorderList(ProWorkorder proWorkorder)
    {
        return qxxProWorkorderMapper.selectProWorkorderList(proWorkorder);
    }

    /**
     * 查询所有生产工单
     *
     * @return 生产工单集合
     */
    @Override
    public List<ProWorkorder> selectAll()
    {
        return qxxProWorkorderMapper.selectProWorkorderList(new ProWorkorder());
    }

    /**
     * 检查工单编码唯一性
     *
     * @param proWorkorder 生产工单
     * @return 结果
     */
    @Override
    public boolean checkWorkorderCodeUnique(ProWorkorder proWorkorder)
    {
        ProWorkorder existing = qxxProWorkorderMapper.selectProWorkorderByWorkorderCode(proWorkorder.getWorkorderCode());
        if (existing == null) return true;
        if (existing.getWorkorderId().equals(proWorkorder.getWorkorderId())) return true;
        throw new ServiceException("工单编码[" + proWorkorder.getWorkorderCode() + "]已存在");
    }

    /**
     * 新增生产工单
     *
     * @param proWorkorder 生产工单
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProWorkorder(ProWorkorder proWorkorder)
    {
        proWorkorder.setCreateTime(DateUtils.getNowDate());
        proWorkorder.setCreateBy(SecurityUtils.getUsername());
        if (proWorkorder.getStatus() == null) proWorkorder.setStatus("PREPARE");
        if (proWorkorder.getWorkorderType() == null) proWorkorder.setWorkorderType("SELF");
        if (proWorkorder.getOrderType() == null) proWorkorder.setOrderType("NEW");
        if (proWorkorder.getOrderSource() == null) proWorkorder.setOrderSource("MANUAL");
        return qxxProWorkorderMapper.insertProWorkorder(proWorkorder);
    }

    /**
     * 修改生产工单
     *
     * @param proWorkorder 生产工单
     * @return 结果
     */
    @Override
    @Transactional
    public int updateProWorkorder(ProWorkorder proWorkorder)
    {
        // 获取修改前的工单，用于变更对比
        ProWorkorder old = selectProWorkorderByWorkorderId(proWorkorder.getWorkorderId());
        proWorkorder.setUpdateTime(DateUtils.getNowDate());
        proWorkorder.setUpdateBy(SecurityUtils.getUsername());
        int rows = qxxProWorkorderMapper.updateProWorkorder(proWorkorder);

        // 非 PREPARE 状态的工单修改需记录变更（PREPARE 状态可自由修改）
        if (old != null && !"PREPARE".equals(old.getStatus()))
        {
            recordChange(old, proWorkorder, "BOM/QTY/PARAM/STATUS", "工单修改");
        }

        return rows;
    }

    /** 记录工单变更 */
    private void recordChange(ProWorkorder old, ProWorkorder newWo, String changeType, String reason)
    {
        try {
            // 对比关键字段
            if (!nullSafeEquals(old.getQuantity(), newWo.getQuantity()))
                createChange(newWo.getWorkorderId(), "QTY", "quantity", String.valueOf(old.getQuantity()), String.valueOf(newWo.getQuantity()), reason);
            if (!nullSafeEquals(old.getStatus(), newWo.getStatus()))
                createChange(newWo.getWorkorderId(), "STATUS", "status", old.getStatus(), newWo.getStatus(), reason);
            if (!nullSafeEquals(old.getProductId(), newWo.getProductId()))
                createChange(newWo.getWorkorderId(), "PRODUCT", "productId", String.valueOf(old.getProductId()), String.valueOf(newWo.getProductId()), reason);
            if (!nullSafeEquals(old.getRequestDate(), newWo.getRequestDate()))
                createChange(newWo.getWorkorderId(), "DATE", "requestDate", old.getRequestDate() != null ? old.getRequestDate().toString() : null, newWo.getRequestDate() != null ? newWo.getRequestDate().toString() : null, reason);
        } catch (Exception ignored) { /* 变更记录不影响主流程 */ }
    }

    private boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private void createChange(Long workorderId, String changeType, String fieldName, String oldValue, String newValue, String reason)
    {
        ProWorkorderChange change = new ProWorkorderChange();
        change.setWorkorderId(workorderId);
        change.setChangeType(changeType);
        change.setFieldName(fieldName);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setChangeReason(reason);
        change.setStatus("APPROVED"); // 直接生效（后续可改为 PENDING 审批流）
        change.setCreateBy(SecurityUtils.getUsername());
        change.setCreateTime(DateUtils.getNowDate());
        proWorkorderChangeService.insertProWorkorderChange(change);
    }

    /**
     * 批量删除生产工单
     *
     * @param workorderIds 需要删除的生产工单主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderByWorkorderIds(Long[] workorderIds)
    {
        return qxxProWorkorderMapper.deleteProWorkorderByWorkorderIds(workorderIds);
    }

    /**
     * 删除生产工单信息
     *
     * @param workorderId 生产工单主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderMapper.deleteProWorkorderByWorkorderId(workorderId);
    }

    /**
     * 开工：PREPARE → PRODUCING
     * 状态校验 + 更新状态 + 记录开工时间
     *
     * @param workorderId 生产工单主键
     * @return 结果
     */
    @Override
    @Transactional
    public int startProduction(Long workorderId)
    {
        ProWorkorder wo = selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) {
            throw new ServiceException("工单不存在");
        }
        if (!"PREPARE".equals(wo.getStatus())) {
            throw new ServiceException("只有待生产状态的工单才能开工，当前状态：" + wo.getStatus());
        }
        wo.setStatus("PRODUCING");
        wo.setUpdateTime(DateUtils.getNowDate());
        wo.setUpdateBy(SecurityUtils.getUsername());
        return qxxProWorkorderMapper.updateProWorkorder(wo);
    }

    /**
     * 取消工单：PREPARE / PRODUCING → CANCEL
     *
     * @param workorderId 生产工单主键
     * @return 结果
     */
    @Override
    public int cancelWorkorder(Long workorderId)
    {
        ProWorkorder wo = selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) throw new ServiceException("工单不存在");
        if (!"PREPARE".equals(wo.getStatus()) && !"PRODUCING".equals(wo.getStatus())) {
            throw new ServiceException("只有待生产或生产中的工单才能取消，当前状态：" + wo.getStatus());
        }

        // Step 1: 作废工单下所有非终态领料单（ALLOCATED 态会自动恢复预占库存）
        // 已发料(ISSUED/PARTIAL_ISSUED)的不作废（已扣库存，需走退料流程）
        WmIssueHeader issueQuery = new WmIssueHeader();
        issueQuery.setWorkorderId(workorderId);
        List<WmIssueHeader> issues = wmIssueHeaderService.selectWmIssueHeaderList(issueQuery);
        if (issues != null) {
            for (WmIssueHeader issue : issues) {
                String st = issue.getStatus();
                // 草稿/待审核/已下达/已预占 → 作废（cancel 内部对 ALLOCATED 会先恢复库存）
                if (WmIssueConstants.STATUS_DRAFT.equals(st) || WmIssueConstants.STATUS_PENDING.equals(st)
                        || WmIssueConstants.STATUS_APPROVED.equals(st) || WmIssueConstants.STATUS_ALLOCATED.equals(st)
                        || "CONFIRMED".equals(st)) {
                    try {
                        // CONFIRMED 领料单已确认并预占库存，作废前先释放预占
                        if ("CONFIRMED".equals(st)) {
                            wmIssueHeaderService.releaseAllocation(issue.getIssueId());
                        }
                        wmIssueHeaderService.cancel(issue.getIssueId(), "工单取消，关联领料单自动作废");
                    } catch (Exception ex) {
                        // 单张作废失败不阻断工单取消，忽略继续
                    }
                }
            }
        }

        // Step 2: 全部释放成功后更新工单状态
        wo.setStatus("CANCEL");
        wo.setUpdateTime(DateUtils.getNowDate());
        wo.setUpdateBy(SecurityUtils.getUsername());
        return qxxProWorkorderMapper.updateProWorkorder(wo);
    }

    /**
     * 物料齐套检查（实时计算，不持久化）
     * 读取工单 BOM → 实时查询 wm 库存 → 返回每行物料的供需情况
     *
     * @param workorderId 生产工单主键
     * @return 齐套检查结果列表
     */
    @Override
    public List<Map<String, Object>> checkMaterialReadiness(Long workorderId)
    {
        // 1. 获取工单 BOM 物料清单
        List<ProWorkorderBom> bomList = proWorkorderBomService.selectProWorkorderBomByWorkorderId(workorderId);
        if (bomList == null || bomList.isEmpty()) {
            throw new ServiceException("工单无 BOM 数据，请先维护物料清单");
        }

        // 2. 获取工单信息（用于 factory_id 过滤）
        ProWorkorder wo = selectProWorkorderByWorkorderId(workorderId);

        // 3. 本工单已备料量（领料单 ALLOCATED/PARTIAL_ISSUED/ISSUED）—— 预占/出库时已扣
        //    quantity_available，齐套计算需加回，否则发料后永远显示缺料
        Map<Long, BigDecimal> reservedByItem = sumReservedByItem(workorderId);

        List<Map<String, Object>> result = new ArrayList<>();
        boolean allSufficient = true;

        for (ProWorkorderBom bom : bomList) {
            // 查询该物料在工厂下的所有库存
            WmMaterialStock query = new WmMaterialStock();
            query.setItemId(bom.getItemId());
            if (wo.getFactoryId() != null) {
                query.setFactoryId(wo.getFactoryId());
            }
            List<WmMaterialStock> stocks = wmMaterialStockService.selectWmMaterialStockList(query);

            // 汇总可用库存（确认领料时已预占扣减quantity_available）
            // 存量数据已通过 migration_init_available.sql 初始化为 quantity_onhand
            BigDecimal availableTotal = BigDecimal.ZERO;
            for (WmMaterialStock stock : stocks) {
                BigDecimal avail = stock.getQuantityAvailable();
                if (avail != null) {
                    availableTotal = availableTotal.add(avail);
                }
            }

            // 本工单已备料量（已预占或已发料，库存 available 已被扣除，需加回）
            BigDecimal reserved = reservedByItem.getOrDefault(bom.getItemId(), BigDecimal.ZERO);
            // 有效可用量 = 纯库存可用 + 本工单已备料
            BigDecimal effectiveQty = availableTotal.add(reserved);

            // 所需用量
            BigDecimal required = bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO;
            boolean sufficient = effectiveQty.compareTo(required) >= 0;

            Map<String, Object> row = new HashMap<>();
            row.put("lineId", bom.getLineId());
            row.put("itemId", bom.getItemId());
            row.put("itemCode", bom.getItemCode());
            row.put("itemName", bom.getItemName());
            row.put("unitName", bom.getUnitName());
            row.put("requiredQty", required);
            row.put("availableQty", availableTotal);
            row.put("reservedForOrder", reserved);
            row.put("effectiveQty", effectiveQty);
            row.put("sufficient", sufficient);
            row.put("shortageQty", sufficient ? BigDecimal.ZERO : required.subtract(effectiveQty));
            result.add(row);

            if (!sufficient) {
                allSufficient = false;
            }
        }

        return result;
    }

    /**
     * 汇总本工单领料单中已为各物料备好的数量（按 itemId 聚合）
     *
     * <p>只统计状态 ∈ {ALLOCATED, PARTIAL_ISSUED, ISSUED} 的领料单：这三种状态下，
     * 对应 quantity_available 均已被（预占或出库冻结）扣除，料已为工单备好，
     * 齐套分析需将其加回，否则发料后会持续误报缺料。</p>
     *
     * @param workorderId 生产工单主键
     * @return itemId → 已备料量
     */
    private Map<Long, BigDecimal> sumReservedByItem(Long workorderId)
    {
        Map<Long, BigDecimal> reservedByItem = new HashMap<>();
        WmIssueHeader hQuery = new WmIssueHeader();
        hQuery.setWorkorderId(workorderId);
        List<WmIssueHeader> headers = wmIssueHeaderService.selectWmIssueHeaderList(hQuery);

        // 仅统计已预占/已发料的领料单（草稿、待审、已下达未预占的不算备好）
        List<Long> issueIds = new ArrayList<>();
        for (WmIssueHeader h : headers) {
            String st = h.getStatus();
            if (WmIssueConstants.STATUS_ALLOCATED.equals(st)
                    || WmIssueConstants.STATUS_PARTIAL_ISSUED.equals(st)
                    || WmIssueConstants.STATUS_ISSUED.equals(st)) {
                issueIds.add(h.getIssueId());
            }
        }
        if (issueIds.isEmpty()) {
            return reservedByItem;
        }

        // 按物料聚合领料数量（quantity_issue：应发量；预占/出库态下等于已备好量）
        for (Long issueId : issueIds) {
            WmIssueLine lQuery = new WmIssueLine();
            lQuery.setIssueId(issueId);
            for (WmIssueLine line : wmIssueLineService.selectWmIssueLineList(lQuery)) {
                BigDecimal qty = line.getQuantityIssue() != null ? line.getQuantityIssue() : BigDecimal.ZERO;
                reservedByItem.merge(line.getItemId(), qty, BigDecimal::add);
            }
        }
        return reservedByItem;
    }

    /**
     * 开工前检查 + 自动生成领料单 + 开工（事务性）
     * 四步：物料齐套检查 → 排产检查 → 按工序生成领料单 → 开工
     * 物料是硬约束（FAIL必短路）；排产是软约束，forceSchedule=true 可豁免（记变更记录）
     *
     * @param workorderId 生产工单主键
     * @param forceSchedule 排产FAIL时是否豁免开工
     * @param overrideReason 豁免理由（forceSchedule=true且排产FAIL时必填）
     * @return 每步检查结果列表 [{step, stepName, status:PASS/FAIL/SKIP/OVERRIDE, message, details}]
     */
    @Override
    public List<Map<String, Object>> preStartCheck(Long workorderId, boolean forceSchedule, String overrideReason)
    {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setTimeout(30);
        return lockTemplate.execute("pro:workorder:start:" + workorderId,
                () -> tt.execute(status -> doPreStartCheck(workorderId, forceSchedule, overrideReason)));
    }

    private List<Map<String, Object>> doPreStartCheck(Long workorderId, boolean forceSchedule, String overrideReason)
    {
        ProWorkorder wo = selectProWorkorderByWorkorderId(workorderId);
        if (wo == null)
        {
            throw new ServiceException("工单不存在");
        }
        List<Map<String, Object>> steps = new ArrayList<>();

        // Step 1: 物料齐套检查（只读，物料是硬约束，不豁免）
        Map<String, Object> step1 = doMaterialCheck(workorderId);
        steps.add(step1);
        if ("FAIL".equals(step1.get("status")))
        {
            addSkipSteps(steps, 2, 4);
            return steps;
        }

        // Step 2: 排产检查（软约束，forceSchedule=true 且填了理由可豁免）
        Map<String, Object> step2 = doSchedulingCheck(wo);
        if ("FAIL".equals(step2.get("status")) && forceSchedule)
        {
            step2 = overrideSchedulingStep(wo, step2, overrideReason);
        }
        steps.add(step2);
        if ("FAIL".equals(step2.get("status")))
        {
            addSkipSteps(steps, 3, 4);
            return steps;
        }

        // Step 3: 按工序生成领料单
        Map<String, Object> step3 = doGenerateIssueOrders(wo);
        steps.add(step3);
        if ("FAIL".equals(step3.get("status")))
        {
            addSkipStep(steps, 4);
            return steps;
        }

        // Step 4: 开工
        steps.add(doStartProduction(workorderId));

        return steps;
    }

    /** 排产检查FAIL时的豁免处理：校验理由 → 标记OVERRIDE → 写工单变更记录（与主流程同事务） */
    private Map<String, Object> overrideSchedulingStep(ProWorkorder wo, Map<String, Object> step, String reason)
    {
        if (StringUtils.isEmpty(reason))
        {
            step.put("message", step.get("message") + "；豁免开工需填写豁免理由");
            return step; // 理由为空则保持 FAIL，不豁免
        }
        step.put("status", "OVERRIDE");
        step.put("message", step.get("message") + "（已豁免开工，理由：" + reason + "）");
        createChange(wo.getWorkorderId(), "OVERRIDE_START", "scheduling", "FAIL", "OVERRIDE", reason);
        return step;
    }

    /** Step 1: 物料齐套检查 */
    private Map<String, Object> doMaterialCheck(Long workorderId)
    {
        Map<String, Object> step = new HashMap<>();
        step.put("step", 1);
        step.put("stepName", "物料齐套检查");
        try
        {
            List<Map<String, Object>> rows = checkMaterialReadiness(workorderId);
            boolean allSufficient = rows.stream().allMatch(r -> Boolean.TRUE.equals(r.get("sufficient")));
            step.put("status", allSufficient ? "PASS" : "FAIL");
            step.put("message", allSufficient ? "所有物料齐套，可以开工" : "存在缺料物料，暂不可开工");
            step.put("details", rows);
        }
        catch (ServiceException e)
        {
            step.put("status", "FAIL");
            step.put("message", e.getMessage());
            step.put("details", new ArrayList<>());
        }
        return step;
    }

    /** Step 2: 排产检查 — 确认每个工序都有排产任务 */
    private Map<String, Object> doSchedulingCheck(ProWorkorder wo)
    {
        Map<String, Object> step = new HashMap<>();
        step.put("step", 2);
        step.put("stepName", "排产检查");

        // 获取工单关联的工艺路线
        ProRouteProduct rp = null;
        if (wo.getRouteProductId() != null)
            rp = proRouteProductService.selectProRouteProductByRecordId(wo.getRouteProductId());
        if (rp == null)
        {
            step.put("status", "FAIL");
            step.put("message", "工单未关联工艺路线，无法进行排产检查");
            step.put("details", new ArrayList<>());
            return step;
        }

        // 获取工艺路线的所有工序
        List<ProRouteProcess> routeProcesses = proRouteProcessService.selectProRouteProcessByRouteId(rp.getRouteId());
        if (routeProcesses == null || routeProcesses.isEmpty())
        {
            step.put("status", "FAIL");
            step.put("message", "工艺路线下无工序定义");
            step.put("details", new ArrayList<>());
            return step;
        }

        // 检查每个工序是否都有排产任务
        List<Map<String, Object>> missingProcesses = new ArrayList<>();
        for (ProRouteProcess rproc : routeProcesses)
        {
            ProTask taskQuery = new ProTask();
            taskQuery.setWorkorderId(wo.getWorkorderId());
            taskQuery.setProcessId(rproc.getProcessId());
            List<ProTask> tasks = proTaskService.selectProTaskList(taskQuery);
            if (tasks == null || tasks.isEmpty())
            {
                Map<String, Object> missing = new HashMap<>();
                missing.put("processId", rproc.getProcessId());
                missing.put("processCode", rproc.getProcessCode());
                missing.put("processName", rproc.getProcessName());
                missing.put("orderNum", rproc.getOrderNum());
                missingProcesses.add(missing);
            }
        }

        if (!missingProcesses.isEmpty())
        {
            step.put("status", "FAIL");
            step.put("message", "以下工序尚未排产，请先进行排产操作");
            step.put("details", missingProcesses);
        }
        else
        {
            step.put("status", "PASS");
            step.put("message", "所有工序已排产（共" + routeProcesses.size() + "道工序）");
            step.put("details", new ArrayList<>());
        }
        return step;
    }

    /** Step 3: 按工序分组生成领料单 — 一个工序一张领料单，无物料消耗的工序跳过 */
    private Map<String, Object> doGenerateIssueOrders(ProWorkorder wo)
    {
        Map<String, Object> step = new HashMap<>();
        step.put("step", 3);
        step.put("stepName", "生成领料单");

        // 获取工单 BOM
        List<ProWorkorderBom> bomList = proWorkorderBomService.selectProWorkorderBomByWorkorderId(wo.getWorkorderId());
        if (bomList == null || bomList.isEmpty())
        {
            step.put("status", "FAIL");
            step.put("message", "工单无BOM数据，无法生成领料单");
            step.put("details", new ArrayList<>());
            return step;
        }

        // 按 processId 分组
        Map<Long, List<ProWorkorderBom>> grouped = new LinkedHashMap<>();
        for (ProWorkorderBom bom : bomList)
        {
            Long pid = bom.getProcessId() != null ? bom.getProcessId() : 0L;
            grouped.computeIfAbsent(pid, k -> new ArrayList<>()).add(bom);
        }

        // 找到默认仓库
        Long defaultWarehouseId = findDefaultWarehouse(wo.getFactoryId());

        List<Map<String, Object>> generatedIssues = new ArrayList<>();
        List<Map<String, Object>> skippedProcesses = new ArrayList<>();
        int totalCreated = 0;

        for (Map.Entry<Long, List<ProWorkorderBom>> entry : grouped.entrySet())
        {
            Long processId = entry.getKey();
            List<ProWorkorderBom> processBoms = entry.getValue();

            // processId=0 表示 BOM 行未关联工序，跳过
            if (processId == 0L)
            {
                Map<String, Object> skip = new HashMap<>();
                skip.put("processId", 0L);
                skip.put("processName", "未指定工序");
                skip.put("itemCount", processBoms.size());
                skippedProcesses.add(skip);
                continue;
            }

            String processName = processBoms.get(0).getProcessName();

            // 查找该工序对应的排产任务
            ProTask taskQuery = new ProTask();
            taskQuery.setWorkorderId(wo.getWorkorderId());
            taskQuery.setProcessId(processId);
            List<ProTask> tasks = proTaskService.selectProTaskList(taskQuery);
            ProTask task = (tasks != null && !tasks.isEmpty()) ? tasks.get(0) : null;
            String processCode = (task != null) ? task.getProcessCode() : null;

            // 幂等检查：如果该工单+任务已有领料单，跳过
            if (task != null)
            {
                WmIssueHeader existQuery = new WmIssueHeader();
                existQuery.setWorkorderId(wo.getWorkorderId());
                existQuery.setTaskId(task.getTaskId());
                List<WmIssueHeader> existingIssues = wmIssueHeaderService.selectWmIssueHeaderList(existQuery);
                if (existingIssues != null && !existingIssues.isEmpty())
                {
                    Map<String, Object> skip = new HashMap<>();
                    skip.put("processId", processId);
                    skip.put("processName", processName);
                    skip.put("message", "已存在领料单，跳过");
                    skippedProcesses.add(skip);
                    continue;
                }
            }

            // 生成领料单编码：LL + 日期 + 序号
            String issueCode = generateIssueCode();

            // 创建领料单头
            WmIssueHeader header = new WmIssueHeader();
            header.setFactoryId(wo.getFactoryId());
            header.setIssueCode(issueCode);
            header.setIssueName(wo.getWorkorderName() + "-" + (processName != null ? processName : "工序" + processId));
            header.setIssueType("PRODUCE");
            header.setWorkorderId(wo.getWorkorderId());
            header.setWorkorderCode(wo.getWorkorderCode());
            header.setWorkorderName(wo.getWorkorderName());
            if (task != null)
            {
                header.setTaskId(task.getTaskId());
                header.setTaskCode(task.getTaskCode());
                header.setWorkstationId(task.getWorkstationId());
                header.setWorkstationCode(task.getWorkstationCode());
                header.setWorkstationName(task.getWorkstationName());
            }
            header.setWarehouseId(defaultWarehouseId);
            header.setStatus("DRAFT");
            header.setIssueDate(new Date());
            wmIssueHeaderService.insertWmIssueHeader(header);

            // 创建领料单行
            BigDecimal totalQty = BigDecimal.ZERO;
            for (ProWorkorderBom bom : processBoms)
            {
                WmIssueLine line = new WmIssueLine();
                line.setIssueId(header.getIssueId());
                line.setFactoryId(wo.getFactoryId());
                line.setItemId(bom.getItemId());
                line.setItemCode(bom.getItemCode());
                line.setItemName(bom.getItemName());
                line.setItemSpc(bom.getItemSpc());
                line.setUnitOfMeasure(bom.getUnitOfMeasure());
                line.setUnitName(bom.getUnitName());
                line.setQuantityIssue(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
                line.setProcessId(processId);
                line.setProcessCode(processCode);
                line.setProcessName(processName);
                line.setWarehouseId(defaultWarehouseId);
                wmIssueLineService.insertWmIssueLine(line);

                BigDecimal lineQty = bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO;
                totalQty = totalQty.add(lineQty);
            }

            // 更新领料单总数量
            header.setQuantityTotal(totalQty);
            wmIssueHeaderService.updateWmIssueHeader(header);

            Map<String, Object> issueInfo = new HashMap<>();
            issueInfo.put("issueId", header.getIssueId());
            issueInfo.put("issueCode", issueCode);
            issueInfo.put("processId", processId);
            issueInfo.put("processCode", processCode);
            issueInfo.put("processName", processName);
            issueInfo.put("lineCount", processBoms.size());
            issueInfo.put("taskId", task != null ? task.getTaskId() : null);
            generatedIssues.add(issueInfo);
            totalCreated++;
        }

        // 构建 message
        StringBuilder msg = new StringBuilder();
        msg.append("已生成 ").append(totalCreated).append(" 张领料单");
        if (!skippedProcesses.isEmpty())
            msg.append("，").append(skippedProcesses.size()).append(" 个分组无工序关联已跳过");

        step.put("status", "PASS");
        step.put("message", msg.toString());
        step.put("details", generatedIssues);
        return step;
    }

    /** Step 4: 开工 */
    private Map<String, Object> doStartProduction(Long workorderId)
    {
        Map<String, Object> step = new HashMap<>();
        step.put("step", 4);
        step.put("stepName", "确认开工");
        try
        {
            startProduction(workorderId);
            step.put("status", "PASS");
            step.put("message", "工单已开工，状态已变更为「生产中」");
        }
        catch (ServiceException e)
        {
            step.put("status", "FAIL");
            step.put("message", e.getMessage());
        }
        step.put("details", new ArrayList<>());
        return step;
    }

    /** 查找工厂的默认仓库，找不到则返回 1 */
    private Long findDefaultWarehouse(Long factoryId)
    {
        WmWarehouse query = new WmWarehouse();
        if (factoryId != null) query.setFactoryId(factoryId);
        List<WmWarehouse> warehouses = wmWarehouseService.selectWmWarehouseList(query);
        if (warehouses != null && !warehouses.isEmpty())
            return warehouses.get(0).getWarehouseId();
        // 兜底：查询所有仓库
        List<WmWarehouse> all = wmWarehouseService.selectWmWarehouseAll();
        if (all != null && !all.isEmpty())
            return all.get(0).getWarehouseId();
        return 1L; // 最终兜底
    }

    /** 生成领料单编码 LL-yyyyMMddHHmmssSSS */
    private String generateIssueCode()
    {
        String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        return "LL" + ts.substring(2); // 取后15位
    }

    /** 跳过从 fromStep 到 toStep 的步骤 */
    private void addSkipSteps(List<Map<String, Object>> steps, int fromStep, int toStep)
    {
        String[] names = {"", "物料齐套检查", "排产检查", "生成领料单", "确认开工"};
        for (int i = fromStep; i <= toStep; i++)
        {
            Map<String, Object> s = new HashMap<>();
            s.put("step", i);
            s.put("stepName", names[i]);
            s.put("status", "SKIP");
            s.put("message", "前置步骤未通过，已跳过");
            s.put("details", new ArrayList<>());
            steps.add(s);
        }
    }

    /** 跳过单步 */
    private void addSkipStep(List<Map<String, Object>> steps, int stepNum)
    {
        addSkipSteps(steps, stepNum, stepNum);
    }

    /**
     * 创建生产工单（含BOM和工序参数）+ 可选SKU变体创建
     * 在一个事务中完成：可选变体创建 → 工单头插入 → BOM批量插入 → 工序参数批量插入 → 路线复制 → 变更记录
     *
     * @param workorder 生产工单（含 createSkuVariant/skuCode/skuName 变体控制字段）
     * @param bomList 工单BOM组成列表
     * @param paramList 工单工序参数值列表
     * @return 创建后的生产工单（含workorderId）
     */
    @Override
    @Transactional
    public ProWorkorder createWorkorderWithBom(ProWorkorder workorder, List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList)
    {
        // 1. 校验工单编码唯一性
        checkWorkorderCodeUnique(workorder);

        // 2. 设置默认值
        if (workorder.getStatus() == null)
            workorder.setStatus("PREPARE");
        if (workorder.getWorkorderType() == null)
            workorder.setWorkorderType("SELF");
        if (workorder.getOrderType() == null)
            workorder.setOrderType("NEW");
        if (workorder.getOrderSource() == null)
            workorder.setOrderSource("MANUAL");

        // ★ SKU变体创建：用户确认BOM偏离标准后，在提交工单时一并创建变体
        Long originalProductId = workorder.getProductId();
        String originalProductCode = workorder.getProductCode();
        String originalProductName = workorder.getProductName();
        Long skuItemId = null;

        if (Boolean.TRUE.equals(workorder.getCreateSkuVariant())
                && originalProductId != null && originalProductId > 0
                && workorder.getSkuCode() != null && !workorder.getSkuCode().isEmpty())
        {
            skuItemId = createSkuVariantForWorkorder(workorder, originalProductId);
        }

        // 3. 插入工单头
        workorder.setCreateTime(DateUtils.getNowDate());
        workorder.setCreateBy(SecurityUtils.getUsername());
        qxxProWorkorderMapper.insertProWorkorder(workorder);

        Long workorderId = workorder.getWorkorderId();
        if (workorderId == null)
        {
            throw new ServiceException("工单创建失败：未能获取工单ID");
        }

        // 4. 批量插入BOM行
        if (bomList != null && !bomList.isEmpty())
        {
            for (ProWorkorderBom bom : bomList)
            {
                bom.setWorkorderId(workorderId);
                if (bom.getFactoryId() == null)
                    bom.setFactoryId(workorder.getFactoryId());
                if (bom.getQuantity() != null && workorder.getQuantity() != null)
                {
                    bom.setTotalQuantity(bom.getQuantity().multiply(workorder.getQuantity()));
                }
                proWorkorderBomService.insertProWorkorderBom(bom);
            }
        }

        // 5. 批量插入工序参数行
        if (paramList != null && !paramList.isEmpty())
        {
            for (ProWorkorderParam param : paramList)
            {
                param.setWorkorderId(workorderId);
                if (param.getFactoryId() == null)
                    param.setFactoryId(workorder.getFactoryId());
                proWorkorderParamService.insertProWorkorderParam(param);
            }
        }

        // 6. SKU变体后置处理：复制工艺路线关联 + 回填工单调整值 + 记录变更
        if (skuItemId != null)
        {
            try
            {
                // 6a. 为变体复制父产品的工艺路线关联（含BOM + 参数，L2 标准值）
                proRouteProductService.copyRouteProductForSku(
                        originalProductId, skuItemId,
                        workorder.getSkuCode(), workorder.getSkuName());

                // 6b. ★ 回填工单调整值到变体路线（L3 adjusted → L2 standard）
                applyWorkorderAdjustmentsToSkuRoute(skuItemId, bomList, paramList);

                // 6c. 记录变体创建事件
                createChange(workorderId, "SKU", "productId",
                        String.valueOf(originalProductId),
                        String.valueOf(skuItemId),
                        "BOM偏离标准，自动创建SKU变体: " + workorder.getSkuCode());
            }
            catch (Exception e)
            {
                throw new ServiceException("SKU变体创建失败: " + e.getMessage());
            }
        }

        return workorder;
    }

    /**
     * 为工单创建SKU变体物料
     * ① 新建 MdItem（parent_id=原SPU, item_code=skuCode, 差异字段从工单回填）
     * ② 行业子表 + 批次配置由 MdItemServiceImpl.insertMdItem 自动复制
     * ③ 更新 workorder.productId/productCode/productName 为新的 SKU
     *
     * @param workorder 工单（会被修改 productId/code/name）
     * @param parentItemId 父产品（SPU）itemId
     * @return 新创建的SKU变体 itemId
     */
    private Long createSkuVariantForWorkorder(ProWorkorder workorder, Long parentItemId)
    {
        com.ruoyi.system.domain.mes.md.MdItem parentItem = mdItemService.selectMdItemById(parentItemId);
        if (parentItem == null)
        {
            throw new ServiceException("父产品不存在: itemId=" + parentItemId);
        }

        // 构建变体 MdItem
        com.ruoyi.system.domain.mes.md.MdItem skuItem = new com.ruoyi.system.domain.mes.md.MdItem();
        skuItem.setParentId(parentItemId);
        skuItem.setItemCode(workorder.getSkuCode());
        skuItem.setItemName(workorder.getSkuName() != null ? workorder.getSkuName() : workorder.getSkuCode());
        skuItem.setUnitOfMeasure(parentItem.getUnitOfMeasure());
        skuItem.setUnitName(parentItem.getUnitName());

        // 差异字段：工单调整值覆盖父产品默认值（有值=覆盖，null=继承）
        skuItem.setSpecification(parentItem.getSpecification());
        skuItem.setProductSize(
                workorder.getProductSize() != null ? workorder.getProductSize() : parentItem.getProductSize());
        skuItem.setPrintingReq(
                workorder.getPrintingReq() != null ? workorder.getPrintingReq() : parentItem.getPrintingReq());
        skuItem.setPackageSpec(parentItem.getPackageSpec());

        // 默认启用
        skuItem.setEnableFlag("1");
        skuItem.setBatchFlag(parentItem.getBatchFlag());

        // insertMdItem 会自动处理：类型继承 + 行业子表复制
        mdItemService.insertMdItem(skuItem);
        Long skuItemId = skuItem.getItemId();
        if (skuItemId == null)
        {
            throw new ServiceException("SKU变体创建失败：未能获取物料ID");
        }

        // 复制批次配置（insertMdItem 不处理 batch_config）
        com.ruoyi.system.domain.mes.md.MdItemBatchConfig parentConfig =
                mdItemBatchConfigService.selectMdItemBatchConfigByItemId(parentItemId);
        if (parentConfig != null)
        {
            com.ruoyi.system.domain.mes.md.MdItemBatchConfig skuConfig = new com.ruoyi.system.domain.mes.md.MdItemBatchConfig();
            skuConfig.setItemId(skuItemId);
            skuConfig.setProduceDateFlag(parentConfig.getProduceDateFlag());
            skuConfig.setExpireDateFlag(parentConfig.getExpireDateFlag());
            skuConfig.setRecptDateFlag(parentConfig.getRecptDateFlag());
            skuConfig.setVendorFlag(parentConfig.getVendorFlag());
            skuConfig.setClientFlag(parentConfig.getClientFlag());
            skuConfig.setCoCodeFlag(parentConfig.getCoCodeFlag());
            skuConfig.setPoCodeFlag(parentConfig.getPoCodeFlag());
            skuConfig.setWorkorderFlag(parentConfig.getWorkorderFlag());
            skuConfig.setTaskFlag(parentConfig.getTaskFlag());
            skuConfig.setWorkstationFlag(parentConfig.getWorkstationFlag());
            skuConfig.setToolFlag(parentConfig.getToolFlag());
            skuConfig.setMoldFlag(parentConfig.getMoldFlag());
            skuConfig.setLotNumberFlag(parentConfig.getLotNumberFlag());
            skuConfig.setQualityStatusFlag(parentConfig.getQualityStatusFlag());
            skuConfig.setPaperRollFlag(parentConfig.getPaperRollFlag());
            skuConfig.setPaperWidthFlag(parentConfig.getPaperWidthFlag());
            skuConfig.setEnableFlag(parentConfig.getEnableFlag());
            skuConfig.setRemark("从父产品 " + parentItem.getItemCode() + " 复制");
            mdItemBatchConfigService.insertMdItemBatchConfig(skuConfig);
        }

        // 复制标准产品 BOM（qxx_md_product_bom，物料页面"BOM组成"tab 展示用）
        com.ruoyi.system.domain.mes.md.MdProductBom bomQuery = new com.ruoyi.system.domain.mes.md.MdProductBom();
        bomQuery.setItemId(parentItemId);
        List<com.ruoyi.system.domain.mes.md.MdProductBom> parentBoms = mdProductBomService.selectMdProductBomList(bomQuery);
        if (parentBoms != null && !parentBoms.isEmpty()) {
            for (com.ruoyi.system.domain.mes.md.MdProductBom parentBom : parentBoms) {
                com.ruoyi.system.domain.mes.md.MdProductBom skuBom = new com.ruoyi.system.domain.mes.md.MdProductBom();
                skuBom.setItemId(skuItemId);
                skuBom.setBomItemId(parentBom.getBomItemId());
                skuBom.setUnitOfMeasure(parentBom.getUnitOfMeasure());
                skuBom.setQuantity(parentBom.getQuantity());
                skuBom.setScrapRate(parentBom.getScrapRate());
                skuBom.setEnableFlag(parentBom.getEnableFlag());
                skuBom.setRemark("从父产品 " + parentItem.getItemCode() + " 复制");
                skuBom.setCreateTime(DateUtils.getNowDate());
                skuBom.setCreateBy(SecurityUtils.getUsername());
                mdProductBomService.insertMdProductBom(skuBom);
            }
        }

        // 更新 workorder 的产品信息为变体
        workorder.setProductId(skuItemId);
        workorder.setProductCode(skuItem.getItemCode());
        workorder.setProductName(skuItem.getItemName());

        return skuItemId;
    }

    /**
     * 将工单的 BOM 调整和参数调整值回填到变体的路线数据中（L3 → L2）
     * 确保变体路线反映工单的实际差异化配置，而不是简单复制父产品。
     *
     * @param skuItemId 变体物料ID
     * @param bomList 工单 BOM 列表（含调整后的物料和用量）
     * @param paramList 工单工序参数列表（含 adjustedValue）
     */
    private void applyWorkorderAdjustmentsToSkuRoute(Long skuItemId,
            List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList) {
        if (skuItemId == null) return;

        // 1. 查找变体的所有 route_product 记录（由 copyRouteProductForSku 刚创建）
        ProRouteProduct rpQuery = new ProRouteProduct();
        rpQuery.setItemId(skuItemId);
        List<ProRouteProduct> skuRouteProducts = proRouteProductService.selectProRouteProductList(rpQuery);
        if (skuRouteProducts == null || skuRouteProducts.isEmpty()) return;

        // 2. BOM 回填：删除复制来的父产品BOM行，用工单BOM替换
        if (bomList != null && !bomList.isEmpty()) {
            for (ProRouteProduct rp : skuRouteProducts) {
                // 删除刚复制的 BOM 行（product_id = skuItemId, route_id = rp.routeId）
                proRouteProductBomService.deleteByRouteIdAndProductId(rp.getRouteId(), skuItemId);

                // 重新插入工单 BOM 行（L3 → L2）
                for (ProWorkorderBom bom : bomList) {
                    ProRouteProductBom newBom = new ProRouteProductBom();
                    newBom.setRouteId(rp.getRouteId());
                    newBom.setProcessId(bom.getProcessId());
                    newBom.setProductId(skuItemId);
                    newBom.setItemId(bom.getItemId());
                    newBom.setItemCode(bom.getItemCode());
                    newBom.setItemName(bom.getItemName());
                    newBom.setSpecification(bom.getItemSpc());
                    newBom.setUnitOfMeasure(bom.getUnitOfMeasure());
                    newBom.setUnitName(bom.getUnitName());
                    // ★ 用量 = 工单的单位用量（不是总用量）
                    newBom.setQuantity(bom.getQuantity() != null ? bom.getQuantity().doubleValue() : 1.0);
                    newBom.setCreateTime(DateUtils.getNowDate());
                    newBom.setCreateBy(SecurityUtils.getUsername());
                    proRouteProductBomService.insertProRouteProductBom(newBom);
                }
            }
        }

        // 3. 参数回填：将工单 adjustedValue ≠ standardValue 的参数更新到变体路线
        if (paramList != null && !paramList.isEmpty()) {
            for (ProRouteProduct rp : skuRouteProducts) {
                Long skuRouteProductId = rp.getRecordId();

                for (ProWorkorderParam wp : paramList) {
                    String adjusted = (wp.getAdjustedValue() != null) ? wp.getAdjustedValue().trim() : "";
                    // 只回填有实际调整的参数
                    if (adjusted.isEmpty()) continue;

                    // 查找变体路线中对应 templateId 的参数
                    ProRouteProcessParam query = new ProRouteProcessParam();
                    query.setRouteProductId(skuRouteProductId);
                    List<ProRouteProcessParam> existingParams =
                            proRouteProcessParamService.selectProRouteProcessParamByRouteProductId(skuRouteProductId);
                    if (existingParams != null) {
                        for (ProRouteProcessParam pp : existingParams) {
                            if (wp.getTemplateId() != null && wp.getTemplateId().equals(pp.getTemplateId())) {
                                // ★ 用工单调整值覆盖变体路线的标准参数值
                                pp.setParamValue(adjusted);
                                pp.setUpdateTime(DateUtils.getNowDate());
                                pp.setUpdateBy(SecurityUtils.getUsername());
                                proRouteProcessParamService.updateProRouteProcessParam(pp);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 修改生产工单（含BOM和工序参数）
     * 在一个事务中完成：工单头更新 → 旧BOM全量替换 → 工序参数upsert
     *
     * @param workorder 生产工单
     * @param bomList 工单BOM组成列表（全量替换，旧数据会被删除）
     * @param paramList 工单工序参数值列表（按recordId/templateId upsert）
     * @return 修改后的生产工单
     */
    @Override
    @Transactional
    public ProWorkorder updateWorkorderWithBom(ProWorkorder workorder, List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList)
    {
        Long workorderId = workorder.getWorkorderId();
        if (workorderId == null)
        {
            throw new ServiceException("工单ID不能为空");
        }

        // 1. 更新工单头（含非PREPARE变更追踪）
        updateProWorkorder(workorder);

        // 2. 全量替换BOM：先删后插
        proWorkorderBomService.deleteProWorkorderBomByWorkorderId(workorderId);
        if (bomList != null && !bomList.isEmpty())
        {
            for (ProWorkorderBom bom : bomList)
            {
                bom.setWorkorderId(workorderId);
                if (bom.getFactoryId() == null)
                    bom.setFactoryId(workorder.getFactoryId());
                // 计算预计总用量 = 单位用量 × 工单计划数量
                if (bom.getQuantity() != null && workorder.getQuantity() != null)
                {
                    bom.setTotalQuantity(bom.getQuantity().multiply(workorder.getQuantity()));
                }
                proWorkorderBomService.insertProWorkorderBom(bom);
            }
        }

        // 3. 工序参数 upsert
        if (paramList != null && !paramList.isEmpty())
        {
            // 查出现有参数，构建 templateId → recordId 映射
            List<ProWorkorderParam> existingParams = proWorkorderParamService.selectProWorkorderParamByWorkorderId(workorderId);
            Map<Long, Long> templateIdToRecordId = new HashMap<>();
            if (existingParams != null)
            {
                for (ProWorkorderParam ep : existingParams)
                {
                    if (ep.getTemplateId() != null)
                    {
                        templateIdToRecordId.put(ep.getTemplateId(), ep.getRecordId());
                    }
                }
            }

            // 收集新参数中的 templateId 集合
            java.util.Set<Long> newTemplateIds = new java.util.HashSet<>();
            for (ProWorkorderParam param : paramList)
            {
                if (param.getTemplateId() != null)
                {
                    newTemplateIds.add(param.getTemplateId());
                }
            }

            // 删除不在新列表中的旧参数
            if (existingParams != null)
            {
                java.util.List<Long> toDelete = new ArrayList<>();
                for (ProWorkorderParam ep : existingParams)
                {
                    if (ep.getTemplateId() != null && !newTemplateIds.contains(ep.getTemplateId()))
                    {
                        toDelete.add(ep.getRecordId());
                    }
                }
                if (!toDelete.isEmpty())
                {
                    proWorkorderParamService.deleteProWorkorderParamByRecordIds(toDelete.toArray(new Long[0]));
                }
            }

            // upsert 每个参数
            for (ProWorkorderParam param : paramList)
            {
                param.setWorkorderId(workorderId);
                if (param.getFactoryId() == null)
                    param.setFactoryId(workorder.getFactoryId());

                // 确定是否已有 recordId（前端传入 或 按 templateId 匹配已有记录）
                Long existingRecordId = param.getRecordId();
                if (existingRecordId == null && param.getTemplateId() != null)
                {
                    existingRecordId = templateIdToRecordId.get(param.getTemplateId());
                }

                if (existingRecordId != null)
                {
                    param.setRecordId(existingRecordId);
                    proWorkorderParamService.updateProWorkorderParam(param);
                }
                else
                {
                    proWorkorderParamService.insertProWorkorderParam(param);
                }
            }
        }
        else
        {
            // paramList 为空时，删除所有旧参数
            proWorkorderParamService.deleteProWorkorderParamByWorkorderId(workorderId);
        }

        // 4. 重新查询返回最新数据
        return selectProWorkorderByWorkorderId(workorderId);
    }

    /**
     * 偏离检测：比较提交的 BOM/参数 与 路线标准（后端唯一权威判定）
     * 纯计算，不持久化。前端据此弹窗询问用户是否创建变体。
     */
    @Override
    public ProWorkorderDeviationVO checkDeviation(
            ProWorkorder workorder, List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList) {
        ProWorkorderDeviationVO result = new ProWorkorderDeviationVO();
        List<ProWorkorderDeviationVO.DeviationItem> items = new ArrayList<>();

        Long routeProductId = workorder.getRouteProductId();
        if (routeProductId == null) {
            result.setHasDeviation(false);
            result.setDeviations(items);
            return result;
        }

        ProRouteProduct routeProduct = proRouteProductService.selectProRouteProductByRecordId(routeProductId);
        if (routeProduct == null) {
            result.setHasDeviation(false);
            result.setDeviations(items);
            return result;
        }

        // —— BOM 偏离检测：vs route_product_bom ——
        List<ProRouteProductBom> routeBoms = proRouteProductBomService
                .selectProRouteProductBomByRouteId(routeProduct.getRouteId());

        // 过滤出当前产品的 BOM 行（同一路线可能关联多个产品）
        List<ProRouteProductBom> filteredBoms = new ArrayList<>();
        Map<Long, ProRouteProductBom> routeBomMap = new HashMap<>();
        if (routeBoms != null) {
            routeBoms.stream()
                .filter(b -> b.getProductId() == null
                    || b.getProductId().equals(routeProduct.getItemId()))
                .forEach(b -> {
                    filteredBoms.add(b);
                    // putIfAbsent：同一物料多工序时保留首条，避免覆盖
                    routeBomMap.putIfAbsent(b.getItemId(), b);
                });
        }

        Map<Long, ProWorkorderBom> currBomMap = new HashMap<>();
        if (bomList != null) {
            bomList.stream()
                .filter(b -> b.getItemId() != null)
                .forEach(b -> currBomMap.putIfAbsent(b.getItemId(), b));
        }

        // 检查变更/新增
        if (bomList != null) {
            for (ProWorkorderBom b : bomList) {
                if (b.getItemId() == null) continue;
                ProRouteProductBom o = routeBomMap.get(b.getItemId());
                if (o == null) {
                    items.add(buildDeviation("BOM", b.getItemCode(), b.getItemName(),
                            b.getUnitName(), "-", str(b.getQuantity()), "物料新增"));
                } else {
                    double std = o.getQuantity() != null ? o.getQuantity() : 0;
                    double cur = b.getQuantity() != null ? b.getQuantity().doubleValue() : 0;
                    if (Math.abs(cur - std) > 0.0001) {
                        items.add(buildDeviation("BOM", b.getItemCode(), b.getItemName(),
                                b.getUnitName(), String.valueOf(std), String.valueOf(cur), "用量变更"));
                    }
                }
            }
        }
        // 检查删除（仅检查已过滤出的当前产品BOM行）
        for (ProRouteProductBom o : filteredBoms) {
            if (o.getItemId() != null && !currBomMap.containsKey(o.getItemId())) {
                items.add(buildDeviation("BOM", o.getItemCode(), o.getItemName(),
                        o.getUnitName(), String.valueOf(o.getQuantity()), "-", "物料已删"));
            }
        }

        // —— 参数偏离检测：vs route_process_param ——
        List<ProRouteProcessParam> routeParams = proRouteProcessParamService
                .selectProRouteProcessParamByRouteProductId(routeProductId);
        Map<Long, String> routeParamMap = new HashMap<>();
        if (routeParams != null) {
            routeParams.forEach(p -> {
                if (p.getTemplateId() != null) {
                    routeParamMap.put(p.getTemplateId(), p.getParamValue() != null ? p.getParamValue() : "");
                }
            });
        }

        if (paramList != null) {
            for (ProWorkorderParam p : paramList) {
                if (p.getTemplateId() == null) continue;
                String adj = (p.getAdjustedValue() != null) ? p.getAdjustedValue().trim() : "";
                if (adj.isEmpty()) continue;
                String std = routeParamMap.getOrDefault(p.getTemplateId(),
                        p.getStandardValue() != null ? p.getStandardValue() : "");
                if (!adj.equals(std)) {
                    // 富化参数名：从 paramTemplate 查询
                    String paramName = "";
                    String processName = "";
                    ProParamTemplate tmpl = proParamTemplateService.selectProParamTemplateByTemplateId(p.getTemplateId());
                    if (tmpl != null) {
                        paramName = tmpl.getParamName() != null ? tmpl.getParamName() : "";
                        if (tmpl.getProcessId() != null) {
                            ProProcess proc = proProcessService.selectProProcessByProcessId(tmpl.getProcessId());
                            if (proc != null) processName = proc.getProcessName() != null ? proc.getProcessName() : "";
                        }
                    }
                    items.add(buildDeviation("参数", paramName, processName,
                            "", std, adj, "参数调整"));
                }
            }
        }

        result.setHasDeviation(!items.isEmpty());
        result.setDeviations(items);
        return result;
    }

    private ProWorkorderDeviationVO.DeviationItem buildDeviation(
            String source, String itemCode, String itemName,
            String unitName, String standardVal, String actualVal, String diffLabel) {
        ProWorkorderDeviationVO.DeviationItem d = new ProWorkorderDeviationVO.DeviationItem();
        d.setSource(source);
        d.setItemCode(itemCode != null ? itemCode : "");
        d.setItemName(itemName != null ? itemName : "");
        d.setUnitName(unitName != null ? unitName : "");
        d.setStandardVal(standardVal != null ? standardVal : "");
        d.setActualVal(actualVal != null ? actualVal : "");
        d.setDiffLabel(diffLabel);
        return d;
    }

    private String str(Object o) { return o != null ? String.valueOf(o) : ""; }
}
