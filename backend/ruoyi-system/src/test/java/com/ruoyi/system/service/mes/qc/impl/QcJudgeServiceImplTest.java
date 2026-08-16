package com.ruoyi.system.service.mes.qc.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.qc.*;
import com.ruoyi.system.service.mes.qc.QcConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("质检判定引擎")
class QcJudgeServiceImplTest {

    private final QcJudgeServiceImpl service = new QcJudgeServiceImpl();

    private QcOrderLine numberLine(Double std, Double min, Double max, String val) {
        QcOrderLine l = new QcOrderLine();
        l.setIndexName("测试数值项");
        l.setQcResultType(QcConstants.RESULT_TYPE_NUMBER);
        l.setStanderVal(std == null ? null : BigDecimal.valueOf(std));
        l.setThresholdMin(min == null ? null : BigDecimal.valueOf(min));
        l.setThresholdMax(max == null ? null : BigDecimal.valueOf(max));
        l.setCheckValText(val);
        return l;
    }
    private QcDefectRecord defect(String level, int qty) {
        QcDefectRecord d = new QcDefectRecord();
        d.setDefectLevel(level); d.setDefectQuantity(qty);
        return d;
    }
    private QcJudgeConfig cfg(int checkQty, int ac) {
        QcJudgeConfig c = new QcJudgeConfig();
        c.setQuantityCheck(checkQty); c.setAcQuantity(ac);
        c.setCrRateLimit(0); c.setMajRateLimit(0); c.setMinRateLimit(0);
        return c;
    }

    @Test
    @DisplayName("数值行判定_标准值加偏差区间内合格")
    void should_pass_when_val_within_stander_plus_threshold() {
        QcOrderLine l = numberLine(100.0, -2.0, 2.0, "101.5");
        assertEquals(QcConstants.LINE_PASS, service.judgeLine(l));
        l.setCheckValText("102.01");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
        l.setCheckValText("97.99");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_无标准值时上下限为绝对值")
    void should_use_absolute_bounds_when_no_stander() {
        QcOrderLine l = numberLine(null, 90.0, 110.0, "95");
        assertEquals(QcConstants.LINE_PASS, service.judgeLine(l));
        l.setCheckValText("110.5");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_区间端点为空只校验另一端")
    void should_check_only_bound_when_other_null() {
        QcOrderLine l = numberLine(null, null, 50.0, "60");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
        l.setCheckValText("50");
        assertEquals(QcConstants.LINE_PASS, service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_非数字实测值抛业务异常")
    void should_throw_when_number_val_not_parseable() {
        QcOrderLine l = numberLine(100.0, -1.0, 1.0, "abc");
        assertThrows(ServiceException.class, () -> service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_未录实测值返回null")
    void should_return_null_when_val_blank() {
        QcOrderLine l = numberLine(100.0, -1.0, 1.0, null);
        assertNull(service.judgeLine(l));
    }

    @Test
    @DisplayName("非数值行_保留人工判定结果")
    void should_keep_manual_result_for_non_number() {
        QcOrderLine l = new QcOrderLine();
        l.setQcResultType(QcConstants.RESULT_TYPE_TEXT);
        l.setLineResult(QcConstants.LINE_FAIL);
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
    }

    @Test
    @DisplayName("整单判定_未录完抛异常")
    void should_throw_when_any_line_unjudged() {
        QcOrderLine l = numberLine(100.0, -1.0, 1.0, null);
        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.judge(Collections.singletonList(l), Collections.emptyList(), cfg(10, 0)));
        assertTrue(ex.getMessage().contains("未录入"));
    }

    @Test
    @DisplayName("整单判定_不合格数超Ac值判FAIL")
    void should_fail_when_unqualified_over_ac() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeResult r = service.judge(Collections.singletonList(ok),
            Arrays.asList(defect(QcConstants.DEFECT_MINOR, 3), defect(QcConstants.DEFECT_MAJOR, 2)), cfg(20, 4));
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(5, r.getQuantityUnqualified());
    }

    @Test
    @DisplayName("整单判定_任一致命缺陷判FAIL")
    void should_fail_when_any_critical() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeResult r = service.judge(Collections.singletonList(ok),
            Collections.singletonList(defect(QcConstants.DEFECT_CRITICAL, 1)), cfg(100, 10));
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(1, r.getCrQuantity());
    }

    @Test
    @DisplayName("整单判定_缺陷率超阈值判FAIL")
    void should_fail_when_rate_over_limit() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeConfig c = cfg(100, 50);
        c.setMinRateLimit(2.0);
        QcJudgeResult r = service.judge(Collections.singletonList(ok),
            Collections.singletonList(defect(QcConstants.DEFECT_MINOR, 3)), c);
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(3.0, r.getMinRate());
    }

    @Test
    @DisplayName("整单判定_行FAIL但无缺陷记录时不合格数至少为FAIL行数")
    void should_count_fail_lines_when_no_defect_records() {
        QcOrderLine bad = numberLine(100.0, -1.0, 1.0, "200");
        QcJudgeResult r = service.judge(Collections.singletonList(bad), Collections.emptyList(), cfg(10, 0));
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(1, r.getQuantityUnqualified());
    }

    @Test
    @DisplayName("整单判定_全部合格判PASS")
    void should_pass_when_all_good() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeResult r = service.judge(Collections.singletonList(ok), Collections.emptyList(), cfg(10, 0));
        assertEquals(QcConstants.RESULT_PASS, r.getResult());
        assertEquals(QcConstants.LINE_PASS, ok.getLineResult());
    }
}
