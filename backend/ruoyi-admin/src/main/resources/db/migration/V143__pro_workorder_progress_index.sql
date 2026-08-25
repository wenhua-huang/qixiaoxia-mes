-- V143: 工单列表在制进度 —— qxx_pro_task 补 (workorder_id, status) 复合索引
--
-- 背景：工单列表 includeProgress=true 时批量 SELECT ... WHERE workorder_id IN (...) AND status NOT IN (...)
--      原表无 workorder_id 索引，会全表扫描。加复合索引同时覆盖状态过滤。
-- 用 PROCEDURE 包装实现幂等（information_schema 检查 + PREPARE/EXECUTE），重跑安全。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_index_if_missing;
CREATE PROCEDURE add_index_if_missing(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx) THEN
        SET @s = ddl;
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_index_if_missing('qxx_pro_task', 'idx_task_workorder_status',
    'ALTER TABLE qxx_pro_task ADD INDEX idx_task_workorder_status (workorder_id, status)');

DROP PROCEDURE IF EXISTS add_index_if_missing;
