-- ============================================================
-- V116：补全外协发料 trace 的 card_id（V115 之后新产生的空值）
--
-- 背景：
--   工单开工时自动生成草稿外协单（card_id=NULL，卡尚未建），
--   后续 execute 时 writeIssueTrace 写入的 trace 也带 card_id=NULL。
--   V115 修复了当时已存在的数据，但重启后新建订单（如 86/87）仍有此问题。
--   代码侧已在 doExecute 中加 ensureCardId + ensureCardId 中加 trace 回填，
--   本迁移修复残留历史数据。
--
-- 幂等：UPDATE 带 card_id IS NULL 条件；INSERT 带 NOT EXISTS。
-- ============================================================

SET NAMES utf8mb4;

-- ============ 1. 回填 trace.card_id（从外协单 header 取） ============
UPDATE qxx_pro_material_trace t
JOIN qxx_wm_outsource_order o ON o.order_id = t.child_id
SET t.card_id = o.card_id
WHERE t.trace_type = 'OUTSOURCE_ISSUE'
  AND t.child_type = 'OUTSOURCE_ORDER'
  AND t.card_id IS NULL
  AND o.card_id IS NOT NULL;

-- ============ 2. 补插 MATERIAL_STOCK → CARD 边 ============
INSERT INTO qxx_pro_material_trace
    (factory_id, trace_type, parent_type, parent_id, child_type, child_id,
     quantity, unit_of_measure, workorder_id, card_id, vendor_id, process_id,
     trace_time, create_by, create_time)
SELECT
    t.factory_id,
    'OUTSOURCE_ISSUE',
    t.parent_type, t.parent_id,
    'CARD', t.card_id,
    t.quantity, t.unit_of_measure,
    t.workorder_id, t.card_id, t.vendor_id, t.process_id,
    t.trace_time, 'flyway-v116', NOW()
FROM qxx_pro_material_trace t
WHERE t.trace_type = 'OUTSOURCE_ISSUE'
  AND t.child_type = 'OUTSOURCE_ORDER'
  AND t.card_id IS NOT NULL
  AND t.parent_type = 'MATERIAL_STOCK'
  AND t.parent_id <> 0
  AND NOT EXISTS (
      SELECT 1 FROM qxx_pro_material_trace x
      WHERE x.parent_type = t.parent_type
        AND x.parent_id = t.parent_id
        AND x.child_type = 'CARD'
        AND x.child_id = t.card_id
        AND x.trace_type = 'OUTSOURCE_ISSUE'
  );
