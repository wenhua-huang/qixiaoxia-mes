package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.qc.QcDefectRecord;
import com.ruoyi.system.domain.mes.qc.QcJudgeConfig;
import com.ruoyi.system.domain.mes.qc.QcJudgeResult;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.service.mes.qc.IQcJudgeService;
import com.ruoyi.system.service.mes.qc.QcConstants;
import org.springframework.stereotype.Service;

/** 质检判定引擎 — 纯逻辑：行判定(数值区间) + 整单判定(Ac/致命/缺陷率) */
@Service
public class QcJudgeServiceImpl implements IQcJudgeService {

    @Override
    public String judgeLine(QcOrderLine line) {
        if (!QcConstants.RESULT_TYPE_NUMBER.equals(line.getQcResultType())) {
            return line.getLineResult();
        }
        if (StringUtils.isBlank(line.getCheckValText())) {
            return null;
        }
        double val;
        try {
            val = Double.parseDouble(line.getCheckValText().trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("检测项[" + line.getIndexName() + "]实测值不是数字：" + line.getCheckValText());
        }
        BigDecimal std = line.getStanderVal();
        BigDecimal lo = line.getThresholdMin();
        BigDecimal hi = line.getThresholdMax();
        Double lower = (std != null) ? (lo != null ? std.add(lo).doubleValue() : null) : (lo != null ? lo.doubleValue() : null);
        Double upper = (std != null) ? (hi != null ? std.add(hi).doubleValue() : null) : (hi != null ? hi.doubleValue() : null);
        boolean fail = (lower != null && val < lower) || (upper != null && val > upper);
        return fail ? QcConstants.LINE_FAIL : QcConstants.LINE_PASS;
    }

    @Override
    public QcJudgeResult judge(List<QcOrderLine> lines, List<QcDefectRecord> defects, QcJudgeConfig cfg) {
        int failLines = 0;
        for (QcOrderLine line : lines) {
            String r = judgeLine(line);
            if (r == null) {
                throw new ServiceException("检测项[" + line.getIndexName() + "]未录入实测值，无法判定");
            }
            line.setLineResult(r);
            if (QcConstants.LINE_FAIL.equals(r)) {
                failLines++;
            }
        }
        int cr = 0, maj = 0, min = 0;
        for (QcDefectRecord d : defects) {
            int q = d.getDefectQuantity() == null ? 1 : d.getDefectQuantity();
            if (QcConstants.DEFECT_CRITICAL.equals(d.getDefectLevel())) { cr += q; }
            else if (QcConstants.DEFECT_MAJOR.equals(d.getDefectLevel())) { maj += q; }
            else if (QcConstants.DEFECT_MINOR.equals(d.getDefectLevel())) { min += q; }
            else { throw new ServiceException("未知缺陷等级：" + d.getDefectLevel()); }
        }
        int defectQty = cr + maj + min;
        int unqualified = Math.max(defectQty, failLines);
        double checkQty = cfg.getQuantityCheck() == null || cfg.getQuantityCheck() <= 0 ? 0 : cfg.getQuantityCheck();
        double crRate = pct(cr, checkQty), majRate = pct(maj, checkQty), minRate = pct(min, checkQty);
        String result = QcConstants.RESULT_PASS;
        if (cfg.getAcQuantity() != null && unqualified > cfg.getAcQuantity()) { result = QcConstants.RESULT_FAIL; }
        if (cr > 0) { result = QcConstants.RESULT_FAIL; }
        if (checkQty > 0 && (crRate > cfg.getCrRateLimit() || majRate > cfg.getMajRateLimit() || minRate > cfg.getMinRateLimit())) {
            result = QcConstants.RESULT_FAIL;
        }
        QcJudgeResult r = new QcJudgeResult();
        r.setCrQuantity(cr); r.setMajQuantity(maj); r.setMinQuantity(min);
        r.setCrRate(crRate); r.setMajRate(majRate); r.setMinRate(minRate);
        r.setQuantityUnqualified(unqualified); r.setResult(result);
        return r;
    }

    private double pct(int qty, double base) {
        return base <= 0 ? 0 : Math.round(qty * 10000.0 / base) / 100.0;
    }
}
