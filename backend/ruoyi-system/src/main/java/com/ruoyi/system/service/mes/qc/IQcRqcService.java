package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcRqc;

/**
 * 退料检验单Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
public interface IQcRqcService
{
    public List<QcRqc> selectQcRqcList(QcRqc qcrqc);

    /** 详情（组装 lines + defectRecords） */
    public QcRqc selectQcRqcByRqcId(Long rqcId);

    /**
     * 新增（编码缺省自动生成；body 可携带 lines 一并落库；
     * 未携带 lines 时按模板快照自动生成检测项）
     */
    public int insertQcRqc(QcRqc qcrqc);

    public int updateQcRqc(QcRqc qcrqc);

    public int deleteQcRqcByRqcId(Long rqcId);

    public int deleteQcRqcByRqcIds(Long[] rqcIds);

    /**
     * 按来源单据查检验单（供退料单页面/gate 查检验状态）
     *
     * @param sourceDocType 来源单据类型(wm_rt_issue)
     * @param sourceDocId   来源单据ID
     * @return 检验单列表
     */
    public List<QcRqc> listBySource(String sourceDocType, Long sourceDocId);

    /** 关闭（作废）检验单（仅待检验/检验中可关；已判定单据不可关） */
    public void closeRqc(Long rqcId);

    /**
     * 执行判定（FAIL+让步理由→CONCESSION；行结果/缺陷统计/合格数回写，状态置 COMPLETED；
     * 判定通过后由退料单核验放行，不自动执行退库）
     */
    public void judgeRqc(Long rqcId, String concessionReason);
}
