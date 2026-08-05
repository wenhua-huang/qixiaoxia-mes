package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;

/**
 * 通用外协服务 —— 统一管理外协发货/录结果/收货三步流程。
 * 分切/印刷等业务通过 OutsourceResultStrategy 注入领域逻辑。
 *
 * @author qixiaoxia
 */
public interface IOutsourceService
{
    /**
     * Step1 创建外协发货单：建单 + 扣库存发料 + 流转卡→OUTSOURCING + 写发料追溯
     */
    WmOutsourceOrder createOutsource(OutsourceRequest req);

    /**
     * Step2 厂商录加工结果：校验 vendorId 隔离 + 回调 strategy + 订单→PROCESSING
     */
    WmOutsourceOrder recordResult(Long orderId, List<WmOutsourceRecptLine> resultLines);

    /**
     * Step3 我方收货：入库加库存 + 建报工 + 写追溯 + 流转卡恢复 + 订单→RECEIVED
     */
    WmOutsourceOrder receiveOutsource(Long orderId);

    /** 列表（vendor_id 自动隔离） */
    List<WmOutsourceOrder> selectList(WmOutsourceOrder query);

    /** 详情（含 issueLines + recptLines） */
    WmOutsourceOrder selectByOrderId(Long orderId);
}
