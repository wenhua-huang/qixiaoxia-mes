-- ============================================================
-- V115：补全外协发料 trace 的 card_id + 补插 MATERIAL_STOCK → CARD 边
--
-- 背景：
--   V114 只对 trace.card_id 已非空的 OUTSOURCE_ISSUE 边补了 MATERIAL_STOCK→CARD 边。
--   但早期外协单（如 card 263 对应的 order 78/79）写入 trace 时 order.card_id 未传播，
--   导致 trace.card_id=NULL，V114 无法覆盖。本迁移：
--     1. 用 OUTSOURCE_ORDER.card_id 回填 trace.card_id（仅当 trace.card_id 为空时）
--     2. 对回填后的边补插 MATERIAL_STOCK → CARD 边
--
-- 幂等：UPDATE 带 card_id IS NULL 条件；INSERT 带 NOT EXISTS。
-- 注意：Flyway 裸 JDBC，业务表 INSERT/UPDATE 显式带 factory_id 条件。
-- ============================================================

SET NAMES utf8mb4;

-- ============ 1. 回填 trace.card_id（从外协单 header 取） ============
UPDATE qxx_pro_material_trace t
JOIN qxx_wm_outsource_order o ON o.order_id = t.child_id
SET t.card_id = o.card_id,
    t.process_id = COALESCE(t.process_id, o.process_id)
WHERE t.trace_type = 'OUTSOURCE_ISSUE'
  AND t.child_type = 'OUTSOURCE_ORDER'
  AND t.card_id IS NULL
  AND o.card_id IS NOT NULL;

-- ============ 2. 补插 MATERIAL_STOCK → CARD 边（含 V114 遗漏的部分） ============
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
    t.trace_time, 'flyway-v115', NOW()
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
