-- ============================================================
-- V65: qxx_pro_doc_generation_log 增加 source_feedback_id + 唯一索引 (仅 A)
--
-- 背景：产品入库单幂等原本按 workorder_id 级判重，导致：
--   1) 中间工序报工先审核 → 汇总所有 AUDITED 合格数被污染 (bug)
--   2) 分批完工场景下第 2 张入库单被静默丢弃
--
-- 修复：把入库单幂等键从"工单级"改为"feedback 级"，每次末工序报工审核都生成一张
--       独立入库单，数量取该 feedback.quantity_qualified。
--       退料单 / 领料单 / 缺料采购单等 doc_type 保持工单级幂等（source_feedback_id 为 NULL）。
--
-- 索引 A：(workorder_id, doc_type, source_feedback_id) 覆盖 feedbackId 非空的 RECPT 分批
--   ── MySQL 唯一索引对多个 NULL 视为不重复；这正是我们想要的：
--     * ISSUE/RETURN/PUR_ORDER 每工序/每领料单可生成一张 → 各行 source_feedback_id=NULL 允许共存
--     * RECPT 手动补录 (source_feedback_id=NULL) 由应用层 hasReceiptLogForFeedback 判断，
--       不依赖 DB 唯一约束兜底（三条入口锁 key 不同的并发下退化为覆盖式插入，可接受）
--
-- ⚠️ 生产运维注意事项：
--   本次迁移只加字段/索引，不回填历史 log 的 source_feedback_id (保持为 NULL)。
--   ---
--   过渡期风险：V65 之前已生成过入库单的进行中 (status != COMPLETED) 工单，
--   V65 后新审核的末工序 feedback 会被视为"新的分批"再生成一张入库单 → 可能造成超量入库。
--   ---
--   缓解：
--   1. 部署前先让 PRODUCING 工单尽量完成 (末工序全部审核 → 工单自动 COMPLETED)
--   2. 或部署后运维手动排查 (source_feedback_id IS NULL 的 RECPT log 对应工单
--      是否仍在 PRODUCING 状态)，必要时手动 UPDATE source_feedback_id 关联到具体 feedback
--
-- 幂等：字段 / 索引存在时跳过 (information_schema 判定)
-- ============================================================

-- 1) ADD COLUMN source_feedback_id
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qxx_pro_doc_generation_log'
      AND COLUMN_NAME = 'source_feedback_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_doc_generation_log ADD COLUMN source_feedback_id BIGINT DEFAULT NULL COMMENT ''触发单据的报工 record_id (RECPT 类型必填, 其它类型可空)'' AFTER doc_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 索引 A：(workorder_id, doc_type, source_feedback_id) 唯一
--    仅约束 feedbackId 非空的 RECPT 行；NULL 行 (ISSUE/RETURN/PUR_ORDER/manual-RECPT) 不受约束
SET @idx_a_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qxx_pro_doc_generation_log'
      AND INDEX_NAME = 'uk_doc_log_wo_type_feedback'
);
SET @sql := IF(@idx_a_exists = 0,
    'CREATE UNIQUE INDEX uk_doc_log_wo_type_feedback ON qxx_pro_doc_generation_log (workorder_id, doc_type, source_feedback_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
