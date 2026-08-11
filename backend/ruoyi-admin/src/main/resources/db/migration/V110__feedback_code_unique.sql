-- ============================================================
-- V110：报工编码 feedback_code 唯一约束（防御并发重复）
--
-- 背景：checkFeedbackCodeUnique 是 SELECT-then-INSERT，不同 card/workorder 的
--       并发报工不共享 Redis 锁，可绕过应用检查产生重复编码。
--       Controller 的 "FB"+currentTimeMillis() 兜底在同毫秒下也必重复。
-- 策略：先清理历史重复（保留 record_id 最小的一条，其余置空 feedback_code，
--       不删除业务数据），再加唯一约束。空字符串与 NULL 均不受唯一索引影响。
-- ============================================================

SET NAMES utf8mb4;

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

-- 1. 将重复的 feedback_code 置空（保留最早一条），避免加约束失败
UPDATE qxx_pro_feedback f
JOIN (
    SELECT feedback_code, MIN(record_id) AS keep_id
    FROM qxx_pro_feedback
    WHERE feedback_code IS NOT NULL AND feedback_code <> ''
    GROUP BY feedback_code
    HAVING COUNT(*) > 1
) dup ON dup.feedback_code = f.feedback_code AND f.record_id <> dup.keep_id
SET f.feedback_code = NULL;

-- 2. 加唯一约束（允许多个 NULL/空串，MySQL 唯一索引视 NULL 为互异）
CALL add_index_if_missing(
    'qxx_pro_feedback', 'uk_feedback_code',
    'ALTER TABLE qxx_pro_feedback ADD UNIQUE KEY uk_feedback_code (feedback_code)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;
