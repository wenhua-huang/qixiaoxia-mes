package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.OutsourceReceiveRequest;
import com.ruoyi.system.domain.mes.pro.OutsourceResultRequest;
import com.ruoyi.system.domain.mes.pro.ProSlittingRecord;
import com.ruoyi.system.domain.mes.pro.SlittingRequest;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;

/**
 * 分切作业 Service（厂内 + 外协统一模型）
 *
 * <p>厂内 INTERNAL：一步完成领料出库 + 建母卷/子卷 + 入库 + 报工（原有逻辑）。
 * <p>外协 OUTSOURCE：三步
 * <ol>
 *   <li>executeSlitting(OUTSOURCE)：我方建单+发料，母卷 OUTSOURCED，状态 ISSUED</li>
 *   <li>recordOutsourceResult：厂商录子卷结果，子卷 OUTSOURCED，状态 SLITTING</li>
 *   <li>receiveOutsource：我方收货，子卷 IN_STOCK+入库，母卷 CONSUMED+报工，状态 RECEIVED</li>
 * </ol>
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
public interface IProSlittingService {

    /**
     * 执行分切（按 slitMode 分流）：
     * <ul>
     *   <li>INTERNAL（默认）：厂内一步完成</li>
     *   <li>OUTSOURCE：建外协单并发料（一卷一单，多卷循环）</li>
     * </ul>
     *
     * @param request 分切请求
     * @return 分切记录（外协多卷发料时返回首条，完整列表见 parentRolls 字段或重新查 list）
     */
    ProSlittingRecord executeSlitting(SlittingRequest request);

    /**
     * 外协厂商录分切结果（子卷规格 + 重量）。
     * 仅单据所属厂商可操作；提交后子卷 OUTSOURCED，单据 → SLITTING。
     *
     * @param slitId  分切单ID
     * @param request 子卷结果
     * @return 更新后的分切记录（含子卷列表）
     */
    ProSlittingRecord recordOutsourceResult(Long slitId, OutsourceResultRequest request);

    /**
     * 我方确认外协收货：子卷 IN_STOCK + 入库事务、母卷 CONSUMED + 报工 + 追溯，单据 → RECEIVED。
     *
     * @param slitId  分切单ID
     * @param request 收货参数（入库仓库、纸边，可空）
     * @return 更新后的分切记录（含子卷列表）
     */
    ProSlittingRecord receiveOutsource(Long slitId, OutsourceReceiveRequest request);

    /** 查询物料在库库存（厂内选领料物料时展示可用批次） */
    List<WmMaterialStock> listAvailableStock(Long itemId);

    /** 查询在库母卷（外协发料时选择卷号；status=IN_STOCK） */
    List<WmRollDetail> listAvailableParentRolls(Long itemId);

    /** 分切记录列表（厂商账号自动按 vendor_id 过滤） */
    List<ProSlittingRecord> selectList(ProSlittingRecord query);

    /** 分切记录详情（含母卷 + 子卷列表） */
    ProSlittingRecord selectBySlitId(Long slitId);
}
