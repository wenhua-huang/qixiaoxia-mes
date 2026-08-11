-- ============================================================
-- V109：外协 Code Review 修复（幂等约束 + 孤儿清理 + 索引）
--
-- 内容：
--   1. qxx_wm_outsource_order 加 (workorder_id, process_id) 唯一约束，防同工序重复建外协单
--      —— 加约束前先清理历史重复（保留 order_id 最大的一条，其余删行+明细）
--   2. 清理 V107 可能遗留的孤儿 issue_detail / material_trace（header 已删但明细残留）
--   3. 性能索引：
--      qxx_pro_feedback(factory_id, status, task_id)        — selectPendingTaskIds
--      qxx_pro_route_process(route_id, process_id)          — 外协工序 join
--      qxx_wm_issue_header(task_id)                         — 按任务查领料单
--
-- 幂等：约束/索引均用 PROCEDURE 包装（存在则跳过）；DELETE 重跑无目标行。
-- 注意：Flyway 走裸 JDBC 不经 FactoryIdInterceptor，但本迁移为全局数据修复，
--       DELETE/UPDATE 均通过关联条件限定，无需按 factory_id 切分。
-- ============================================================

SET NAMES utf8mb4;

-- ────────────────────────────────────────────
-- 0. 通用幂等工具：索引存在则跳过
-- ────────────────────────────────────────────
DROP PROCEDURE IF EXISTS add_index_if_missing;
CREATE PROCEDURE add_index_if_missing(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx
    ) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

-- ════════════════════════════════════════════
-- 1. 外协单 (workorder_id, process_id) 唯一约束
--    手工外协单 workorder_id/process_id 可为 NULL，MySQL 唯一索引允许多个 NULL，不受影响。
--    加约束前先清理历史重复（保留 order_id 最大的一条）。
-- ════════════════════════════════════════════

-- 1a. 删除重复外协单的收货/发料明细
DELETE d FROM qxx_wm_outsource_recpt_line d
JOIN qxx_wm_outsource_order o ON o.order_id = d.order_id
JOIN (
    SELECT workorder_id, process_id, MAX(order_id) AS keep_id
    FROM qxx_wm_outsource_order
    WHERE workorder_id IS NOT NULL AND process_id IS NOT NULL
    GROUP BY workorder_id, process_id
    HAVING COUNT(*) > 1
) dup ON dup.workorder_id = o.workorder_id AND dup.process_id = o.process_id
     AND o.order_id <> dup.keep_id;

DELETE d FROM qxx_wm_outsource_issue_line d
JOIN qxx_wm_outsource_order o ON o.order_id = d.order_id
JOIN (
    SELECT workorder_id, process_id, MAX(order_id) AS keep_id
    FROM qxx_wm_outsource_order
    WHERE workorder_id IS NOT NULL AND process_id IS NOT NULL
    GROUP BY workorder_id, process_id
    HAVING COUNT(*) > 1
) dup ON dup.workorder_id = o.workorder_id AND dup.process_id = o.process_id
     AND o.order_id <> dup.keep_id;

-- 1b. 删除重复外协单头（保留最新）
DELETE o FROM qxx_wm_outsource_order o
JOIN (
    SELECT workorder_id, process_id, MAX(order_id) AS keep_id
    FROM qxx_wm_outsource_order
    WHERE workorder_id IS NOT NULL AND process_id IS NOT NULL
    GROUP BY workorder_id, process_id
    HAVING COUNT(*) > 1
) dup ON dup.workorder_id = o.workorder_id AND dup.process_id = o.process_id
     AND o.order_id <> dup.keep_id;

-- 1c. 加唯一约束
CALL add_index_if_missing(
    'qxx_wm_outsource_order', 'uk_wo_process',
    'ALTER TABLE qxx_wm_outsource_order ADD UNIQUE KEY uk_wo_process (workorder_id, process_id)'
);

-- ════════════════════════════════════════════
-- 2. 孤儿明细清理（V107 删 header 后若明细未级联，残留无主行）
-- ════════════════════════════════════════════

-- 2a. 孤儿领料明细（issue_detail 表若存在）
DELETE d FROM qxx_wm_issue_detail d
LEFT JOIN qxx_wm_issue_header h ON h.issue_id = d.issue_id
WHERE h.issue_id IS NULL;

-- 2b. 孤儿物料追溯（material_trace.issue_id 非空但 header 已不存在）
DELETE t FROM qxx_pro_material_trace t
LEFT JOIN qxx_wm_issue_header h ON h.issue_id = t.issue_id
WHERE t.issue_id IS NOT NULL AND h.issue_id IS NULL;

-- ════════════════════════════════════════════
-- 3. 性能索引
-- ════════════════════════════════════════════

CALL add_index_if_missing(
    'qxx_pro_feedback', 'idx_feedback_status_task',
    'ALTER TABLE qxx_pro_feedback ADD KEY idx_feedback_status_task (factory_id, status, task_id)'
);

CALL add_index_if_missing(
    'qxx_pro_route_process', 'idx_route_process',
    'ALTER TABLE qxx_pro_route_process ADD KEY idx_route_process (route_id, process_id)'
);

CALL add_index_if_missing(
    'qxx_wm_issue_header', 'idx_issue_header_task',
    'ALTER TABLE qxx_wm_issue_header ADD KEY idx_issue_header_task (task_id)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;
