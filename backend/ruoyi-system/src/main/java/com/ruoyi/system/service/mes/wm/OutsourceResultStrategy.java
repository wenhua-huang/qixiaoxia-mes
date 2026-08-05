package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;

/**
 * 外协加工结果策略 —— 各业务（分切/印刷/...）实现自己的"录产出"逻辑。
 *
 * 通用框架在 recordResult/receive 阶段会回调 strategy，
 * 让业务方有机会创建自己的领域对象（如分切建子卷 WmRollDetail）。
 *
 * @author qixiaoxia
 */
public interface OutsourceResultStrategy
{
    /**
     * 获取策略标识（对应 WmOutsourceOrder.sourceType，如 "SLITTING"）
     */
    String getSourceType();

    /**
     * 厂商录加工结果时回调（订单 ISSUED→PROCESSING）。
     * 业务方可在此创建领域对象（如子卷），返回收货行供后续入库用。
     *
     * @param order    外协订单（含 issueLines）
     * @param resultLines 厂商录入的产出明细（前端传入）
     * @return 处理后的收货行（已关联领域对象 sourceRefId 等）
     */
    List<WmOutsourceRecptLine> onRecordResult(WmOutsourceOrder order, List<WmOutsourceRecptLine> resultLines);

    /**
     * 收货时回调（订单 PROCESSING→RECEIVED）。
     * 业务方可在此做领域特定的入库前处理（如子卷状态→IN_STOCK）。
     *
     * @param order      外协订单
     * @param recptLines 收货行
     * @param feedbackId 刚建的报工ID
     */
    void onReceive(WmOutsourceOrder order, List<WmOutsourceRecptLine> recptLines, Long feedbackId);
}
