package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
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
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
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
        Set<Long> items = lines.stream().map(WmProductRecptLine::getItemId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long itemId : items)
        {
            checkProductInspected(header, lines, itemId);
        }
    }

    /** 单产品校验：需检产品（绑定了 IPQC 模板）必须有 COMPLETED 且 PASS/CONCESSION 的完工检验单 */
    private void checkProductInspected(WmProductRecpt header, List<WmProductRecptLine> lines, Long itemId)
    {
        if (factoryService.resolveTemplate(QcConstants.TYPE_IPQC, itemId, null) == null)
        {
            return;  // 未绑定模板 = 免检
        }
        List<QcIpqc> orders = ipqcMapper.selectBySource(QcConstants.SOURCE_PRODUCT_RECPT, header.getRecptId(), itemId);
        boolean passed = orders.stream().anyMatch(o -> QcConstants.STATUS_COMPLETED.equals(o.getStatus())
            && (QcConstants.RESULT_PASS.equals(o.getCheckResult())
                || QcConstants.RESULT_CONCESSION.equals(o.getCheckResult())));
        if (passed)
        {
            return;
        }
        throw new ServiceException("物料[" + recptItemCodeOf(lines, itemId) + "]需完工检验合格后方可确认入库（"
            + ipqcHintOf(orders) + "）");
    }

    private String recptItemCodeOf(List<WmProductRecptLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmProductRecptLine::getItemCode).orElse(String.valueOf(itemId));
    }

    /** 未生成检验单 / 检验单未完成的差异提示 */
    private String ipqcHintOf(List<QcIpqc> orders)
    {
        if (orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcIpqc first = orders.get(0);
        return "检验单[" + first.getIpqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
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
        if (header == null || lines == null || lines.isEmpty())
        {
            return;
        }
        Set<Long> items = lines.stream().map(WmRtIssueLine::getItemId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long itemId : items)
        {
            checkRtItemInspected(header, lines, itemId);
        }
    }

    /** 单退料物料校验：绑定 RQC 模板的必须有 COMPLETED 且 PASS/CONCESSION 的退料检验单 */
    private void checkRtItemInspected(WmRtIssue header, List<WmRtIssueLine> lines, Long itemId)
    {
        if (factoryService.resolveTemplate(QcConstants.TYPE_RQC, itemId, null) == null)
        {
            return;  // 未绑定模板 = 免检
        }
        List<QcRqc> orders = rqcMapper.selectBySource(QcConstants.SOURCE_RT_ISSUE, header.getRtId(), itemId);
        boolean passed = orders.stream().anyMatch(o -> QcConstants.STATUS_COMPLETED.equals(o.getStatus())
            && (QcConstants.RESULT_PASS.equals(o.getCheckResult())
                || QcConstants.RESULT_CONCESSION.equals(o.getCheckResult())));
        if (passed)
        {
            return;
        }
        throw new ServiceException("物料[" + rtItemCodeOf(lines, itemId) + "]需退料检验合格后方可执行退料（"
            + rqcHintOf(orders) + "）");
    }

    private String rtItemCodeOf(List<WmRtIssueLine> lines, Long itemId)
    {
        return lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst()
            .map(WmRtIssueLine::getItemCode).orElse(String.valueOf(itemId));
    }

    /** 未生成检验单 / 检验单未完成的差异提示 */
    private String rqcHintOf(List<QcRqc> orders)
    {
        if (orders.isEmpty())
        {
            return "未生成检验单";
        }
        QcRqc first = orders.get(0);
        return "检验单[" + first.getRqcCode() + "]状态:" + first.getStatus() + "/" + first.getCheckResult();
    }
}
