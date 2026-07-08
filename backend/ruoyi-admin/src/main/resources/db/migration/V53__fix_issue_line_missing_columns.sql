-- ============================================================
-- 补全 qxx_wm_issue_line 缺失列（幂等）
-- 背景：早期手工建表时缺少 issue_code/process_*/unit2*/quantity_*2/conversion_rate 等列，
--       V43 用 CREATE TABLE IF NOT EXISTS 看到表已存在就跳过，未补列。
--       导致 WmIssueLineMapper.selectVo 查询报 Unknown column。
-- 幂等：列存在则跳过
-- ============================================================

SET NAMES utf8mb4;

-- issue_code（冗余领料单编码，便于查询）
DROP PROCEDURE IF EXISTS proc_add_line_issue_code;
DELIMITER $$
CREATE PROCEDURE proc_add_line_issue_code()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='issue_code') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN issue_code varchar(64) DEFAULT NULL COMMENT '领料单编码(冗余)' AFTER issue_id;
    END IF;
END$$
DELIMITER ;
CALL proc_add_line_issue_code();
DROP PROCEDURE IF EXISTS proc_add_line_issue_code;

-- unit2 / unit2_name / conversion_rate（辅单位双计量）
DROP PROCEDURE IF EXISTS proc_add_line_unit2_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_line_unit2_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='unit2') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN unit2 varchar(64) DEFAULT NULL COMMENT '辅单位编码' AFTER quantity_issued;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='unit2_name') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN unit2_name varchar(64) DEFAULT NULL COMMENT '辅单位名称' AFTER unit2;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='conversion_rate') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN conversion_rate decimal(18,6) DEFAULT NULL COMMENT '主辅单位转换率' AFTER unit2_name;
    END IF;
END$$
DELIMITER ;
CALL proc_add_line_unit2_cols();
DROP PROCEDURE IF EXISTS proc_add_line_unit2_cols;

-- quantity_issue2 / quantity_issued2（辅单位数量）
DROP PROCEDURE IF EXISTS proc_add_line_qty2_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_line_qty2_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='quantity_issue2') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN quantity_issue2 decimal(14,4) DEFAULT 0.0000 COMMENT '辅单位申请数量' AFTER conversion_rate;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='quantity_issued2') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN quantity_issued2 decimal(14,4) DEFAULT 0.0000 COMMENT '辅单位已发料数量' AFTER quantity_issue2;
    END IF;
END$$
DELIMITER ;
CALL proc_add_line_qty2_cols();
DROP PROCEDURE IF EXISTS proc_add_line_qty2_cols;

-- process_id / process_code / process_name（工序关联）
DROP PROCEDURE IF EXISTS proc_add_line_process_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_line_process_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='process_id') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN process_id bigint(20) DEFAULT NULL COMMENT '工序ID' AFTER quantity_issued2;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='process_code') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN process_code varchar(64) DEFAULT NULL COMMENT '工序编码' AFTER process_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='process_name') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN process_name varchar(255) DEFAULT NULL COMMENT '工序名称' AFTER process_code;
    END IF;
END$$
DELIMITER ;
CALL proc_add_line_process_cols();
DROP PROCEDURE IF EXISTS proc_add_line_process_cols;
