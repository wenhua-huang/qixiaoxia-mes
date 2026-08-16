package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcDefectRecord;
import com.ruoyi.system.domain.mes.qc.QcJudgeConfig;
import com.ruoyi.system.domain.mes.qc.QcJudgeResult;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;

/**
 * 质检判定引擎 Service（纯逻辑，无 Mapper 依赖）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcJudgeService
{
    /**
     * 单行判定：数值型按标准值+偏差（无标准值则上下限为绝对值）自动判定；
     * 非数值型原样返回人工判定结果；未录实测值返回 null
     */
    String judgeLine(QcOrderLine line);

    /**
     * 整单判定：逐行判定并回填 lineResult（未录完抛异常），
     * 汇总缺陷数/缺陷率，按 Ac 值/致命缺陷/缺陷率阈值给出整单结果
     */
    QcJudgeResult judge(List<QcOrderLine> lines, List<QcDefectRecord> defects, QcJudgeConfig cfg);
}
