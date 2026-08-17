package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcIpqc;

/**
 * 过程检验单Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
public interface IQcIpqcService
{
    public List<QcIpqc> selectQcIpqcList(QcIpqc qcipqc);

    /** 详情（组装 lines + defectRecords） */
    public QcIpqc selectQcIpqcByIpqcId(Long ipqcId);

    /**
     * 新增（手工创建首检/巡检/抽检/完工检）：校验 ipqcType 枚举 + 工单/物料/模板必填，
     * 编码缺省自动生成；body 可携带 lines 一并落库
     */
    public int insertQcIpqc(QcIpqc qcipqc);

    public int updateQcIpqc(QcIpqc qcipqc);

    public int deleteQcIpqcByIpqcId(Long ipqcId);

    public int deleteQcIpqcByIpqcIds(Long[] ipqcIds);

    /**
     * 按来源单据查检验单（供流转卡工序/入库单页面/gate 查检验状态）
     *
     * @param sourceDocType 来源单据类型(pro_card_process/wm_product_recpt)
     * @param sourceDocId   来源单据ID
     * @return 检验单列表
     */
    public List<QcIpqc> listBySource(String sourceDocType, Long sourceDocId);

    /** 关闭（作废）检验单（仅待检验/检验中可关；已判定单据不可关） */
    public void closeIpqc(Long ipqcId);

    /**
     * 执行判定（FAIL+让步理由→CONCESSION；行结果/缺陷统计/合格数回写，状态置 COMPLETED；
     * 判定通过后不自动流转流转卡，只完成检验单本身）
     */
    public void judgeIpqc(Long ipqcId, String concessionReason);
}
