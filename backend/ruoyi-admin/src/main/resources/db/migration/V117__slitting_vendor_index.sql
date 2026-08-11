-- ============================================================
-- V117：分切记录 vendor_id 性能索引
--
-- 背景：
--   外协厂商账号在 App 端查看分切单列表时，查询条件为
--   WHERE factory_id = ? AND vendor_id = ? ORDER BY create_time DESC，
--   但 qxx_pro_slitting_record 仅有 idx_factory_id，vendor_id 无索引，
--   厂商数据量增长后会导致全表扫描。
--
-- 说明：
--   uk_wo_process (workorder_id, process_id) 唯一约束保持不变 ——
--   sourceType 由工序决定（分切工序→SLITTING，其余→GENERIC），同一工序
--   不可能同时存在两种来源类型的外协单，无需将 source_type 纳入唯一键。
--
-- 幂等：用 PROCEDURE 包装，索引已存在则跳过。
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_v117_index_if_missing;
CREATE PROCEDURE add_v117_index_if_missing(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx
    ) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

-- 厂商维度查询分切记录（App 厂商端列表）
CALL add_v117_index_if_missing(
    'qxx_pro_slitting_record', 'idx_slitting_vendor',
    'ALTER TABLE qxx_pro_slitting_record ADD KEY idx_slitting_vendor (vendor_id)'
);

DROP PROCEDURE IF EXISTS add_v117_index_if_missing;
