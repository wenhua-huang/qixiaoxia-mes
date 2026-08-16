package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;

/**
 * 检验单行Service接口（IQC/IPQC/OQC/RQC 四类单据共用）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcOrderLineService
{
    /**
     * 按单据查检验行
     *
     * @param qcType 检验单类型(IQC/IPQC/OQC/RQC)
     * @param qcId   检验单ID
     * @return 检验行列表（按 order_num 排序）
     */
    public List<QcOrderLine> selectByOrder(String qcType, Long qcId);

    /**
     * 全删全插替换检验行（判定流程回填行结果后落库；调用方须已填好 checkValText/缺陷数等）
     *
     * @param qcType 检验单类型
     * @param qcId   检验单ID
     * @param lines  新行集（null/空=清空该单全部行）
     * @return 插入行数
     */
    public int replaceLines(String qcType, Long qcId, List<QcOrderLine> lines);

    /**
     * 按单据删除全部检验行
     *
     * @param qcType 检验单类型
     * @param qcId   检验单ID
     * @return 删除行数
     */
    public int deleteByOrder(String qcType, Long qcId);
}
