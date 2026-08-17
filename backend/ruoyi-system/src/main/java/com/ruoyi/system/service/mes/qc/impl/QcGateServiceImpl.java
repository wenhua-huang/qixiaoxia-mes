package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOqcMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.IQcGateService;
import com.ruoyi.system.service.mes.qc.QcConstants;

/**
 * 质检拦截门实现（IQC/OQC 已实现，IPQC/RQC 见各桩方法注释的交付任务）
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
    private WmProductSalesLineMapper wmProductSalesLineMapper;

    @Override
    public void assertItemRecptConfirmable(WmItemRecpt header, List<WmItemRecptLine> lines)
    {
        if (header == null || lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = lines.stream().map(WmItemRecptLine::getItemId).collect(Collectors.toSet());
        for (Long itemId : items)
        {
            checkItemInspected(header, lines, itemId);
        }
    }

    /** 单物料校验：需检物料必须有 COMPLETED 且 PASS/CONCESSION 的检验单 */
    private void checkItemInspected(WmItemRecpt header, List<WmItemRecptLine> lines, Long itemId)
    {
        if (factoryService.resolveTemplate(QcConstants.TYPE_IQC, itemId, null) == null)
        {
            return;  // 未绑定模板 = 免检
        }
        List<QcIqc> orders = iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, header.getRecptId(), itemId);
        boolean passed = orders.stream().anyMatch(o -> QcConstants.STATUS_COMPLETED.equals(o.getStatus())
            && (QcConstants.RESULT_PASS.equals(o.getCheckResult())
                || QcConstants.RESULT_CONCESSION.equals(o.getCheckResult())));
        if (passed)
        {
            return;
        }
        throw new ServiceException("物料[" + itemCodeOf(lines, itemId) + "]需来料检验合格后方可确认入库（"
            + hintOf(orders) + "）");
    }

    private String itemCodeOf(List<WmItemRecptLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmItemRecptLine::getItemCode).orElse(String.valueOf(itemId));
    }

    /** 未生成检验单 / 检验单未完成的差异提示 */
    private String hintOf(List<QcIqc> orders)
    {
        if (orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcIqc first = orders.get(0);
        return "检验单[" + first.getIqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }

    @Override
    public void assertProductRecptConfirmable(WmProductRecpt header)
    {
        // 桩：成品入库 IQC 联动在 Task 14 交付后实现
        throw new UnsupportedOperationException("assertProductRecptConfirmable 待 Task 14 实现");
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
        Set<Long> items = lines.stream().map(WmProductSalesLine::getItemId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long itemId : items)
        {
            checkItemShipped(header, lines, itemId);
        }
    }

    /** 单物料校验：需检物料必须有 COMPLETED 且 PASS/CONCESSION 的出货检验单 */
    private void checkItemShipped(WmProductSales header, List<WmProductSalesLine> lines, Long itemId)
    {
        if (factoryService.resolveTemplate(QcConstants.TYPE_OQC, itemId, null) == null)
        {
            return;  // 未绑定模板 = 免检
        }
        List<QcOqc> orders = oqcMapper.selectBySource(QcConstants.SOURCE_PRODUCT_SALES, header.getSalesId(), itemId);
        boolean passed = orders.stream().anyMatch(o -> QcConstants.STATUS_COMPLETED.equals(o.getStatus())
            && (QcConstants.RESULT_PASS.equals(o.getCheckResult())
                || QcConstants.RESULT_CONCESSION.equals(o.getCheckResult())));
        if (passed)
        {
            return;
        }
        throw new ServiceException("物料[" + salesItemCodeOf(lines, itemId) + "]需出货检验合格后方可出库确认（"
            + oqcHintOf(orders) + "）");
    }

    private String salesItemCodeOf(List<WmProductSalesLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmProductSalesLine::getItemCode).orElse(String.valueOf(itemId));
    }

    /** 未生成检验单 / 检验单未完成的差异提示 */
    private String oqcHintOf(List<QcOqc> orders)
    {
        if (orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcOqc first = orders.get(0);
        return "检验单[" + first.getOqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }

    @Override
    public void assertRtIssueExecutable(WmRtIssue header, List<WmRtIssueLine> lines)
    {
        // 桩：退料 RQC 校验在 Task 16 交付后实现
        throw new UnsupportedOperationException("assertRtIssueExecutable 待 Task 16 实现");
    }
}
