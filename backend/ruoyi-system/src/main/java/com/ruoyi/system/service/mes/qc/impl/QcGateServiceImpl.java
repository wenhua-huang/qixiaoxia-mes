package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import com.ruoyi.system.domain.mes.qc.QcRqc;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.mapper.mes.qc.QcIpqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcRqcMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.IQcGateService;
import com.ruoyi.system.service.mes.qc.QcConstants;

/**
 * 质检拦截门实现（IQC/IPQC/OQC/RQC 四类业务单据的执行前校验）
 *
 * <p>批量优化：模板解析与检验单反查均按单据维度批量进行（每类 2 条 SQL），
 * 消除逐物料 N+1，缩短持锁时间。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcGateServiceImpl implements IQcGateService
{
    @Autowired
    private IQcFactoryService factoryService;

    @Autowired
    private QcIqcMapper iqcMapper;

    @Autowired
    private QcOqcMapper oqcMapper;

    @Autowired
    private QcRqcMapper rqcMapper;

    @Autowired
    private WmProductSalesLineMapper wmProductSalesLineMapper;

    @Autowired
    private QcIpqcMapper ipqcMapper;

    @Autowired
    private WmProductRecptLineMapper wmProductRecptLineMapper;

    @Override
    public void assertItemRecptConfirmable(WmItemRecpt header, List<WmItemRecptLine> lines)
    {
        if (header == null || lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = distinctItems(lines, WmItemRecptLine::getItemId);
        Map<Long, QcTemplateProduct> binds = factoryService.resolveTemplates(
            QcConstants.TYPE_IQC, items, null);
        if (binds.isEmpty())
        {
            return;  // 全部未绑定模板 = 免检
        }
        List<QcIqc> orders = iqcMapper.selectBySourceItems(
            QcConstants.SOURCE_ITEM_RECPT, header.getRecptId(), binds.keySet());
        Map<Long, List<QcIqc>> byItem = orders.stream()
            .filter(o -> o.getItemId() != null)
            .collect(Collectors.groupingBy(QcIqc::getItemId));
        for (Map.Entry<Long, QcTemplateProduct> e : binds.entrySet())
        {
            Long itemId = e.getKey();
            if (anyPassed(byItem.get(itemId), QcIqc::getStatus, QcIqc::getCheckResult))
            {
                continue;
            }
            throw new ServiceException("物料[" + itemCodeOf(lines, itemId) + "]需来料检验合格后方可确认入库（"
                + iqcHintOf(byItem.get(itemId)) + "）");
        }
    }

    @Override
    public void assertOutsourceReceivable(WmOutsourceOrder order, List<WmOutsourceRecptLine> lines)
    {
        if (order == null || lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = distinctItems(lines, WmOutsourceRecptLine::getItemId);
        Map<Long, QcTemplateProduct> binds = factoryService.resolveTemplates(
            QcConstants.TYPE_IQC, items, null);
        if (binds.isEmpty())
        {
            return;  // 全部未绑定模板 = 免检
        }
        List<QcIqc> orders = iqcMapper.selectBySourceItems(
            QcConstants.SOURCE_OUTSOURCE_ORDER, order.getOrderId(), binds.keySet());
        Map<Long, List<QcIqc>> byItem = orders.stream()
            .filter(o -> o.getItemId() != null)
            .collect(Collectors.groupingBy(QcIqc::getItemId));
        for (Map.Entry<Long, QcTemplateProduct> e : binds.entrySet())
        {
            Long itemId = e.getKey();
            if (anyPassed(byItem.get(itemId), QcIqc::getStatus, QcIqc::getCheckResult))
            {
                continue;
            }
            throw new ServiceException("物料[" + outsourceItemCodeOf(lines, itemId) + "]需来料检验合格后方可收货入库（"
                + iqcHintOf(byItem.get(itemId)) + "）");
        }
    }

    @Override
    public void assertProductRecptConfirmable(WmProductRecpt header)
    {
        if (header == null || header.getRecptId() == null)
        {
            return;
        }
        WmProductRecptLine query = new WmProductRecptLine();
        query.setRecptId(header.getRecptId());
        List<WmProductRecptLine> lines = wmProductRecptLineMapper.selectWmProductRecptLineList(query);
        if (lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = distinctItems(lines, WmProductRecptLine::getItemId);
        Map<Long, QcTemplateProduct> binds = factoryService.resolveTemplates(
            QcConstants.TYPE_IPQC, items, null);
        if (binds.isEmpty())
        {
            return;
        }
        List<QcIpqc> orders = ipqcMapper.selectBySourceItems(
            QcConstants.SOURCE_PRODUCT_RECPT, header.getRecptId(), binds.keySet());
        Map<Long, List<QcIpqc>> byItem = orders.stream()
            .filter(o -> o.getItemId() != null)
            .collect(Collectors.groupingBy(QcIpqc::getItemId));
        for (Map.Entry<Long, QcTemplateProduct> e : binds.entrySet())
        {
            Long itemId = e.getKey();
            if (anyPassed(byItem.get(itemId), QcIpqc::getStatus, QcIpqc::getCheckResult))
            {
                continue;
            }
            throw new ServiceException("物料[" + recptItemCodeOf(lines, itemId) + "]需完工检验合格后方可确认入库（"
                + ipqcHintOf(byItem.get(itemId)) + "）");
        }
    }

    @Override
    public void assertProductSalesPostable(WmProductSales header)
    {
        if (header == null)
        {
            return;
        }
        List<WmProductSalesLine> lines = wmProductSalesLineMapper.selectLinesBySalesId(header.getSalesId());
        if (lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = distinctItems(lines, WmProductSalesLine::getItemId);
        Map<Long, QcTemplateProduct> binds = factoryService.resolveTemplates(
            QcConstants.TYPE_OQC, items, null);
        if (binds.isEmpty())
        {
            return;
        }
        List<QcOqc> orders = oqcMapper.selectBySourceItems(
            QcConstants.SOURCE_PRODUCT_SALES, header.getSalesId(), binds.keySet());
        Map<Long, List<QcOqc>> byItem = orders.stream()
            .filter(o -> o.getItemId() != null)
            .collect(Collectors.groupingBy(QcOqc::getItemId));
        for (Map.Entry<Long, QcTemplateProduct> e : binds.entrySet())
        {
            Long itemId = e.getKey();
            if (anyPassed(byItem.get(itemId), QcOqc::getStatus, QcOqc::getCheckResult))
            {
                continue;
            }
            throw new ServiceException("物料[" + salesItemCodeOf(lines, itemId) + "]需出货检验合格后方可出库确认（"
                + oqcHintOf(byItem.get(itemId)) + "）");
        }
    }

    @Override
    public void assertRtIssueExecutable(WmRtIssue header, List<WmRtIssueLine> lines)
    {
        if (header == null || lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = distinctItems(lines, WmRtIssueLine::getItemId);
        Map<Long, QcTemplateProduct> binds = factoryService.resolveTemplates(
            QcConstants.TYPE_RQC, items, null);
        if (binds.isEmpty())
        {
            return;
        }
        List<QcRqc> orders = rqcMapper.selectBySourceItems(
            QcConstants.SOURCE_RT_ISSUE, header.getRtId(), binds.keySet());
        Map<Long, List<QcRqc>> byItem = orders.stream()
            .filter(o -> o.getItemId() != null)
            .collect(Collectors.groupingBy(QcRqc::getItemId));
        for (Map.Entry<Long, QcTemplateProduct> e : binds.entrySet())
        {
            Long itemId = e.getKey();
            if (anyPassed(byItem.get(itemId), QcRqc::getStatus, QcRqc::getCheckResult))
            {
                continue;
            }
            throw new ServiceException("物料[" + rtItemCodeOf(lines, itemId) + "]需退料检验合格后方可执行退料（"
                + rqcHintOf(byItem.get(itemId)) + "）");
        }
    }

    // ── 共用判定：存在 COMPLETED 且 PASS/CONCESSION 的检验单即放行 ──

    private <T> boolean anyPassed(List<T> orders,
                                  java.util.function.Function<T, String> statusFn,
                                  java.util.function.Function<T, String> resultFn)
    {
        return orders != null && orders.stream().anyMatch(o ->
            QcConstants.STATUS_COMPLETED.equals(statusFn.apply(o))
                && (QcConstants.RESULT_PASS.equals(resultFn.apply(o))
                    || QcConstants.RESULT_CONCESSION.equals(resultFn.apply(o))));
    }

    // ── 物料编码查找（按 itemId 匹配行） ──

    private String itemCodeOf(List<WmItemRecptLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmItemRecptLine::getItemCode).orElse(String.valueOf(itemId));
    }

    private String outsourceItemCodeOf(List<WmOutsourceRecptLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmOutsourceRecptLine::getItemCode).orElse(String.valueOf(itemId));
    }

    private String recptItemCodeOf(List<WmProductRecptLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmProductRecptLine::getItemCode).orElse(String.valueOf(itemId));
    }

    private String salesItemCodeOf(List<WmProductSalesLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmProductSalesLine::getItemCode).orElse(String.valueOf(itemId));
    }

    private String rtItemCodeOf(List<WmRtIssueLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmRtIssueLine::getItemCode).orElse(String.valueOf(itemId));
    }

    // ── 差异提示 ──

    private String iqcHintOf(List<QcIqc> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcIqc first = orders.get(0);
        return "检验单[" + first.getIqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }

    private String ipqcHintOf(List<QcIpqc> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcIpqc first = orders.get(0);
        return "检验单[" + first.getIpqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }

    private String oqcHintOf(List<QcOqc> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcOqc first = orders.get(0);
        return "检验单[" + first.getOqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }

    private String rqcHintOf(List<QcRqc> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcRqc first = orders.get(0);
        return "检验单[" + first.getRqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }

    // ── 工具：去重非空 itemId ──

    private <T> Set<Long> distinctItems(List<T> lines, java.util.function.Function<T, Long> getter)
    {
        return lines.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
