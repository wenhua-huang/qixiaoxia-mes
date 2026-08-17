package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcOqc;

/**
 * 出货检验单Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcOqcService
{
    public List<QcOqc> selectQcOqcList(QcOqc qcoqc);

    /** 详情（组装 lines + defectRecords） */
    public QcOqc selectQcOqcByOqcId(Long oqcId);

    public int insertQcOqc(QcOqc qcoqc);

    public int updateQcOqc(QcOqc qcoqc);

    public int deleteQcOqcByOqcId(Long oqcId);

    public int deleteQcOqcByOqcIds(Long[] oqcIds);

    /**
     * 按来源单据查检验单（供下游单据页面/gate 查检验状态）
     *
     * @param sourceDocType 来源单据类型(如 wm_product_sales)
     * @param sourceDocId   来源单据ID
     * @return 检验单列表
     */
    public List<QcOqc> listBySource(String sourceDocType, Long sourceDocId);

    /** 关闭（作废）检验单 */
    public void closeOqc(Long oqcId);

    /** 执行判定（FAIL+让步理由→CONCESSION；行结果/缺陷统计/合格数回写，状态置 COMPLETED） */
    public void judgeOqc(Long oqcId, String concessionReason);
}
