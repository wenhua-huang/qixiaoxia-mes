package com.ruoyi.web.controller.mes.qc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.ruoyi.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RQC 退料检验全流程集成测试 — 退料单建单触发生成 + 执行退料拦截门 + 判定放行 + QC_CHECK 待办联动。
 *
 * 链路：模板绑定物料 → 建退料单(POST /mes/wm/rtissue 携带行) → 断言 RQC 待检单生成 + rt_issue 头挂点回填；
 * PENDING 单阻断执行退料 → 判定 PASS 后放行（库存增加 + 单据 POSTED + 列表 qcStatus=PASSED）；
 * 生成时建 QC_CHECK 待办，判定后待办关闭。
 *
 * @author qixiaoxia
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("RQC 退料检验全流程集成测试")
class QcRqcIT extends BaseIntegrationTest
{
    private static final long FACTORY_ID = 1L;
    private static final long RT_ITEM = 9401L;
    /** 两检测项快照：#1 标准 100±1、#2 标准 200±2（合格区间内取值判定 PASS） */
    private static final String VAL1_PASS = "100.0";
    private static final String VAL2_PASS = "200.0";

    private String rqcUrl() { return "http://localhost:" + port + "/mes/qc/rqc"; }

    private String rtUrl() { return "http://localhost:" + port + "/mes/wm/rtissue"; }

    @BeforeEach
    void clean()
    {
        truncateTables(
            "qxx_qc_order_line", "qxx_qc_defect_record", "qxx_qc_rqc",
            "qxx_qc_template_index", "qxx_qc_template_product", "qxx_qc_template",
            "qxx_wm_rt_issue_line", "qxx_wm_rt_issue",
            "qxx_wm_transaction", "qxx_wm_material_stock", "qxx_pro_material_trace",
            "sys_todo_list");
    }

    @Test
    @DisplayName("退料单携行创建后生成 RQC 待检单：rt_issue.rqc_id 回填 + 检验行快照 + 列表 qcStatus=PENDING")
    void should_generate_rqc_on_rt_issue_create()
    {
        setupRqcBinding("TPL-RQC-01");
        Long rtId = createRtIssue("RT-RQC-01", "8");

        List<Long> rqcIds = rqcIdsOfRt(rtId);
        assertThat(rqcIds).hasSize(1);
        Map<String, Object> rqc = jdbcTemplate.queryForMap(
            "select rqc_type, status, source_doc_type, item_id, quantity_check, quantity_min_check, "
            + "quantity_max_unqualified, item_name from qxx_qc_rqc where rqc_id = ?", rqcIds.get(0));
        assertThat(rqc.get("rqc_type")).isEqualTo("PROD_RETURN");
        assertThat(rqc.get("status")).isEqualTo("PENDING");
        assertThat(rqc.get("source_doc_type")).isEqualTo("wm_rt_issue");
        assertThat(((Number) rqc.get("item_id")).longValue()).isEqualTo(RT_ITEM);
        assertThat(((Number) rqc.get("quantity_check")).intValue()).isEqualTo(8);
        assertThat(((Number) rqc.get("quantity_min_check")).intValue()).isEqualTo(5);
        assertThat(((Number) rqc.get("quantity_max_unqualified")).intValue()).isEqualTo(0);
        assertThat((String) rqc.get("item_name")).contains("物料");

        // 退料单头挂点回填
        Map<String, Object> rt = jdbcTemplate.queryForMap(
            "select rqc_id, rqc_code from qxx_wm_rt_issue where rt_id = ?", rtId);
        assertThat(((Number) rt.get("rqc_id")).longValue()).isEqualTo(rqcIds.get(0));
        assertThat(String.valueOf(rt.get("rqc_code"))).isNotBlank();

        Integer lineCount = jdbcTemplate.queryForObject(
            "select count(1) from qxx_qc_order_line where qc_type = 'RQC' and qc_id = ?",
            Integer.class, rqcIds.get(0));
        assertThat(lineCount).isEqualTo(2);

        assertThat(qcStatusOf("RT-RQC-01")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("PENDING 退料检单阻断执行退料：execute 拒绝且单据保持草稿、库存未动")
    void should_reject_execute_when_pending()
    {
        setupRqcBinding("TPL-RQC-02");
        Long rtId = createRtIssue("RT-RQC-02", "6");
        assertThat(rqcIdsOfRt(rtId)).hasSize(1);

        Map<?, ?> resp = put(rtUrl() + "/execute/" + rtId, null);
        assertThat(resp.get("code")).isEqualTo(500);
        assertThat(resp.get("msg").toString()).contains("需退料检验合格后方可执行退料");

        assertThat(rtStatus(rtId)).isEqualTo("DRAFT");
        assertThat(stockQty(RT_ITEM)).isEqualByComparingTo("0");
        assertThat(qcStatusOf("RT-RQC-02")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("判定 PASS 后放行：RQC COMPLETED+PASS → execute 成功，库存增加，单据 POSTED，列表 qcStatus=PASSED")
    void should_pass_execute_after_judged()
    {
        setupRqcBinding("TPL-RQC-03");
        Long rtId = createRtIssue("RT-RQC-03", "6");
        Long rqcId = rqcIdsOfRt(rtId).get(0);

        recordLineValuesAndJudgePass(rqcId);

        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result from qxx_qc_rqc where rqc_id = ?", rqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("PASS");

        Map<?, ?> execResp = put(rtUrl() + "/execute/" + rtId, null);
        assertThat(execResp.get("code")).isEqualTo(200);
        assertThat(rtStatus(rtId)).isEqualTo("POSTED");
        assertThat(stockQty(RT_ITEM)).isEqualByComparingTo("6");
        assertThat(qcStatusOf("RT-RQC-03")).isEqualTo("PASSED");
    }

    @Test
    @DisplayName("生成 RQC 时建 QC_CHECK 待办；判定 PASS 后待办关闭为 COMPLETED/检验合格")
    void should_create_todo_on_generate_and_close_on_judge()
    {
        setupRqcBinding("TPL-RQC-04");
        Long rtId = createRtIssue("RT-RQC-04", "5");
        Long rqcId = rqcIdsOfRt(rtId).get(0);

        Map<String, Object> pending = jdbcTemplate.queryForMap(
            "select todo_title, status, source_doc_type, source_doc_id, priority "
            + "from sys_todo_list where source_doc_type = 'RQC' and source_doc_id = ?", rqcId);
        assertThat(pending.get("status")).isEqualTo("PENDING");
        assertThat(pending.get("source_doc_type")).isEqualTo("RQC");
        assertThat(((Number) pending.get("source_doc_id")).longValue()).isEqualTo(rqcId);
        assertThat(pending.get("priority")).isEqualTo("NORMAL");
        assertThat((String) pending.get("todo_title")).contains("RQC");

        recordLineValuesAndJudgePass(rqcId);

        Map<String, Object> done = jdbcTemplate.queryForMap(
            "select status, handle_result, handle_time from sys_todo_list "
            + "where source_doc_type = 'RQC' and source_doc_id = ?", rqcId);
        assertThat(done.get("status")).isEqualTo("COMPLETED");
        assertThat(done.get("handle_result")).isEqualTo("检验合格");
        assertThat(done.get("handle_time")).isNotNull();
    }

    // ============ 前置数据与辅助 ============

    /** 建 RQC 模板(2 个数值检测项,标准 100±1 / 200±2) + 物料绑定(抽检 5, Ac=0, 三率阈值 0) */
    private void setupRqcBinding(String templateCode)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template (factory_id, template_code, template_name, qc_types, enable_flag, create_by) "
            + "values (?, ?, ?, 'RQC', '1', 'admin')", FACTORY_ID, templateCode, templateCode + "-名称");
        Long templateId = jdbcTemplate.queryForObject(
            "select template_id from qxx_qc_template where template_code = ?", Long.class, templateCode);
        insertTemplateIndex(templateId, 4001L, "RQC-LEN", "长度", 100, -1, 1, 1);
        insertTemplateIndex(templateId, 4002L, "RQC-WID", "宽度", 200, -2, 2, 2);
        jdbcTemplate.update(
            "insert into qxx_qc_template_product (factory_id, template_id, item_id, item_code, item_name, "
            + "quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, create_by) "
            + "values (?, ?, ?, ?, ?, 5, 0, 0, 0, 0, 'admin')",
            FACTORY_ID, templateId, RT_ITEM, itemCodeOf(RT_ITEM), itemCodeOf(RT_ITEM) + "-物料");
    }

    private void insertTemplateIndex(Long templateId, long indexId, String code, String name,
        double stander, double min, double max, int orderNum)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, "
            + "index_type, qc_result_type, stander_val, threshold_min, threshold_max, order_num, create_by) "
            + "values (?, ?, ?, ?, ?, 'RQC', 'NUMBER', ?, ?, ?, ?, 'admin')",
            FACTORY_ID, templateId, indexId, code, name, stander, min, max, orderNum);
    }

    /** 建退料单（PC 创建路径，携带行）— 触发 RQC 生成 hook */
    private Long createRtIssue(String rtCode, String qty)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("itemId", RT_ITEM);
        line.put("itemCode", itemCodeOf(RT_ITEM));
        line.put("itemName", itemCodeOf(RT_ITEM) + "-物料");
        line.put("itemSpc", "SPEC-RQC");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantityRt", new BigDecimal(qty));
        line.put("warehouseId", 1);

        Map<String, Object> header = new HashMap<>();
        header.put("rtCode", rtCode);
        header.put("rtName", "RQC集成测试-" + rtCode);
        header.put("workorderId", 3);
        header.put("workorderCode", "WO-IT");
        header.put("warehouseId", 1);
        header.put("lines", List.of(line));

        Map<?, ?> resp = post(rtUrl(), header);
        assertThat(resp.get("code")).isEqualTo(200);
        return jdbcTemplate.queryForObject(
            "select rt_id from qxx_wm_rt_issue where rt_code = ?", Long.class, rtCode);
    }

    /** 取检验单详情 → 回填两行合格实测值 → edit 落库 → judge → 断言 PASS */
    private void recordLineValuesAndJudgePass(Long rqcId)
    {
        Map<?, ?> detail = (Map<?, ?>) get(rqcUrl() + "/" + rqcId).get("data");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) detail.get("lines");
        assertThat(lines).hasSize(2);
        lines.get(0).put("checkValText", VAL1_PASS);
        lines.get(1).put("checkValText", VAL2_PASS);

        Map<String, Object> editBody = new HashMap<>();
        editBody.put("rqcId", rqcId);
        editBody.put("lines", lines);
        Map<?, ?> editResp = put(rqcUrl(), editBody);
        assertThat(editResp.get("code")).isEqualTo(200);

        Map<?, ?> judgeResp = put(rqcUrl() + "/judge/" + rqcId, Map.of());
        assertThat(judgeResp.get("code")).isEqualTo(200);
    }

    private List<Long> rqcIdsOfRt(Long rtId)
    {
        return jdbcTemplate.queryForList(
            "select rqc_id from qxx_qc_rqc where source_doc_type = 'wm_rt_issue' and source_doc_id = ? order by rqc_id",
            Long.class, rtId);
    }

    private String rtStatus(Long rtId)
    {
        return jdbcTemplate.queryForObject("select status from qxx_wm_rt_issue where rt_id = ?", String.class, rtId);
    }

    private BigDecimal stockQty(long itemId)
    {
        return jdbcTemplate.queryForObject(
            "select ifnull(sum(quantity_onhand), 0) from qxx_wm_material_stock where item_id = ?",
            BigDecimal.class, itemId);
    }

    /** 列表接口 qcStatus 汇总计算列 */
    private String qcStatusOf(String rtCode)
    {
        Map<?, ?> resp = get(rtUrl() + "/list?rtCode=" + rtCode);
        List<?> rows = (List<?>) resp.get("rows");
        assertThat(rows).hasSize(1);
        return (String) ((Map<?, ?>) rows.get(0)).get("qcStatus");
    }

    private String itemCodeOf(long itemId)
    {
        return "RQC-ITEM-" + itemId % 100;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> post(String url, Object body)
    {
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(body), Map.class);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> put(String url, Object body)
    {
        ResponseEntity<Map> resp = restTemplate.exchange(
            url, HttpMethod.PUT, body == null ? authRequest() : authRequest(body), Map.class);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> get(String url)
    {
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, authRequest(), Map.class);
        return resp.getBody();
    }
}
