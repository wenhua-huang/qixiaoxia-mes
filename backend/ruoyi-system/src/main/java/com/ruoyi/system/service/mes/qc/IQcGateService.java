package com.ruoyi.system.service.mes.qc;

import java.util.List;

import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;

/**
 * 质检拦截门 — 业务单据关键节点前校验检验完成情况，未达门槛抛 ServiceException 阻断
 * （波次 3-5 直接扩展本接口，既有签名不得改动）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcGateService
{
    /**
     * 采购入库确认前校验：需检物料（绑定了 IQC 模板）必须存在 COMPLETED 且
     * PASS/CONCESSION 判定的检验单，否则抛 ServiceException
     */
    void assertItemRecptConfirmable(WmItemRecpt header, List<WmItemRecptLine> lines);

    /**
     * 外协收货入库前校验：需检物料（绑定了 IQC 模板）必须存在 COMPLETED 且
     * PASS/CONCESSION 判定的检验单，否则抛 ServiceException 阻断收货
     */
    void assertOutsourceReceivable(WmOutsourceOrder order, List<WmOutsourceRecptLine> lines);

    /** 成品入库确认前校验（Task 14 实现） */
    void assertProductRecptConfirmable(WmProductRecpt header);

    /** 销售出库过账前校验（Task 12 实现） */
    void assertProductSalesPostable(WmProductSales header);

    /** 退料发出前校验（Task 16 实现） */
    void assertRtIssueExecutable(WmRtIssue header, List<WmRtIssueLine> lines);
}
