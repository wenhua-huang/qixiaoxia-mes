-- ============================================================
-- V66: qxx_pro_doc_generation_log 删除旧唯一索引 uk_wo_doc
--
-- 背景：入库单幂等键从"工单级"改为"feedback 级"，V67 新增
--       uk_doc_log_wo_type_feedback (workorder_id, doc_type, source_feedback_id)。
--       旧的 uk_wo_doc (workorder_id, doc_type, doc_id) 在新分批入库单模型下语义已失效
--       （不同 feedback 生成的入库单 doc_id 天然不同，旧键永不触发），仅增加维护心智负担
--       且掩盖真正的幂等约束，故先于 V67 删除。
--       注：V66 与 V67 同次启动顺序执行（V66 先 DROP 旧索引，V67 再 ADD 新列+索引），
--       期间应用未对外服务，无约束空窗风险。
--
-- 幂等：索引不存在时跳过 (information_schema 判定)
-- ============================================================

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'qxx_pro_doc_generation_log'
      AND INDEX_NAME = 'uk_wo_doc'
);
SET @sql := IF(@idx_exists > 0,
    'ALTER TABLE qxx_pro_doc_generation_log DROP INDEX uk_wo_doc',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
