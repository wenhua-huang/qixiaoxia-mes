package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
import com.ruoyi.system.domain.mes.wm.WmOutsourceIssueLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.vo.OutsourceBatchResult;

/**
 * 通用外协服务 —— 统一管理外协发货/录结果/收货三步流程。
 * 分切/印刷等业务通过 OutsourceResultStrategy 注入领域逻辑。
 *
 * @author qixiaoxia
 */
public interface IOutsourceService
{
    /**
     * Step1 创建外协发货单：建单 + 扣库存发料 + 流转卡→OUTSOURCING + 写发料追溯。
     * draft=true 时建草稿单(DRAFT，不扣料)，需后续 executeOutsource 执行扣料。
     */
    WmOutsourceOrder createOutsource(OutsourceRequest req);

    /**
     * 执行发料（草稿 DRAFT → 已发料 ISSUED）：扣库存 + 写追溯 + 流转卡→OUTSOURCING。
     */
    WmOutsourceOrder executeOutsource(Long orderId);

    /**
     * 批量执行发料：逐张草稿单(DRAFT)扣库存发料。
     * 逐单独立事务+独立 Redis 锁，单条失败不影响其他单，失败原因汇总返回。
     */
    OutsourceBatchResult batchExecuteOutsource(List<Long> orderIds);

    /**
     * 修改草稿单发料行（仓库/批次/数量可改）。仅 DRAFT 状态可改。
     */
    void updateIssueLines(Long orderId, List<WmOutsourceIssueLine> lines);

    /**
     * 删除草稿外协单：仅 DRAFT 状态可删（未扣库存），级联删除发料行。
     */
    void deleteOutsource(Long orderId);

    /**
     * Step2 厂商录加工结果：校验 vendorId 隔离 + 回调 strategy。
     * ISSUED/VENDOR_RCVD 首次录入 → PROCESSING；PROCESSING 可分批补录；
     * 累计收货量达到发料量时自动 → FINISHED。
     */
    WmOutsourceOrder recordResult(Long orderId, List<WmOutsourceRecptLine> resultLines);

    /**
     * 厂商签收：ISSUED→VENDOR_RCVD，记录签收人和时间。物料保管责任转移到厂商。
     */
    WmOutsourceOrder vendorReceive(Long orderId);

    /**
     * 厂商手动完成：PROCESSING → FINISHED，允许短交（录不满计划量时用）。
     */
    WmOutsourceOrder complete(Long orderId);

    /**
     * 厂商发货：FINISHED/PROCESSING → SHIPPED，厂商将加工完成的物料发回工厂。
     */
    WmOutsourceOrder ship(Long orderId);

    /**
     * Step3 我方收货：入库加库存 + 建报工 + 写追溯 + 流转卡恢复 + 订单→RECEIVED
     */
    WmOutsourceOrder receiveOutsource(Long orderId);

    /**
     * 批量收货：逐张可收货订单(PROCESSING/SHIPPED)入库收货。
     * 逐单独立事务+独立 Redis 锁，单条失败不影响其他单，失败原因汇总返回。
     */
    OutsourceBatchResult batchReceiveOutsource(List<Long> orderIds);

    /** 列表（vendor_id 自动隔离） */
    List<WmOutsourceOrder> selectList(WmOutsourceOrder query);

    /** 详情（含 issueLines + recptLines） */
    WmOutsourceOrder selectByOrderId(Long orderId);
}
