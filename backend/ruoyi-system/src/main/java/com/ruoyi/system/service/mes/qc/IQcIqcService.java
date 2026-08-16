package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcIqc;

/**
 * 来料检验单Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcIqcService
{
    public List<QcIqc> selectQcIqcList(QcIqc qciqc);

    /** 详情（组装 lines + defectRecords） */
    public QcIqc selectQcIqcByIqcId(Long iqcId);

    public int insertQcIqc(QcIqc qciqc);

    public int updateQcIqc(QcIqc qciqc);

    public int deleteQcIqcByIqcId(Long iqcId);

    public int deleteQcIqcByIqcIds(Long[] iqcIds);

    /**
     * 按来源单据查检验单（供下游单据页面/gate 查检验状态）
     *
     * @param sourceDocType 来源单据类型(如 wm_item_recpt)
     * @param sourceDocId   来源单据ID
     * @return 检验单列表
     */
    public List<QcIqc> listBySource(String sourceDocType, Long sourceDocId);

    /** 关闭（作废）检验单 */
    public void closeIqc(Long iqcId);

    /** 执行判定（FAIL+让步理由→CONCESSION；行结果/缺陷统计/合格数回写，状态置 COMPLETED） */
    public void judgeIqc(Long iqcId, String concessionReason);
}
