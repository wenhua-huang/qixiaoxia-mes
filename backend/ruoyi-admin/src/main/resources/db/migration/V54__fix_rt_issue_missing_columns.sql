-- ============================================================
-- 补全 wm 模块旧表缺失列（幂等）
-- 背景：V50 用 CREATE TABLE IF NOT EXISTS 建表，生产环境部分表在 V50 之前已手工
--       建表，导致 V50 跳过未补列。V53 已修 issue_line 部分缺失列（漏 batch_*），
--       本迁移补齐 kitDashboard 只读路径涉及的其余缺失列：
--         1. qxx_wm_rt_issue.rqc_code              （V50 建表内联，旧表缺失）
--         2. qxx_wm_issue_line.batch_id/batch_code （V50 建表内联，V53 漏补）
--         3. qxx_wm_item_recpt.workorder_id/code   （实体+mapper 新增，从未写迁移）
-- 幂等：列存在则跳过
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. qxx_wm_rt_issue.rqc_code
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_rt_issue_rqc_code;
DELIMITER $$
CREATE PROCEDURE proc_add_rt_issue_rqc_code()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_issue' AND COLUMN_NAME='rqc_code') THEN
        ALTER TABLE qxx_wm_rt_issue ADD COLUMN rqc_code varchar(64) DEFAULT NULL COMMENT '退料质检单编码' AFTER rqc_id;
    END IF;
END$$
DELIMITER ;
CALL proc_add_rt_issue_rqc_code();
DROP PROCEDURE IF EXISTS proc_add_rt_issue_rqc_code;

-- ============================================================
-- 2. qxx_wm_issue_line.batch_id / batch_code
--    （V50 建表内联定义，V53 补 issue_code/unit2/process_* 时漏补）
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_issue_line_batch_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_issue_line_batch_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='batch_id') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN batch_id bigint(20) DEFAULT NULL COMMENT '批次ID' AFTER process_name;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line' AND COLUMN_NAME='batch_code') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN batch_code varchar(64) DEFAULT NULL COMMENT '批次编码' AFTER batch_id;
    END IF;
END$$
DELIMITER ;
CALL proc_add_issue_line_batch_cols();
DROP PROCEDURE IF EXISTS proc_add_issue_line_batch_cols;

-- ============================================================
-- 3. qxx_wm_item_recpt.workorder_id / workorder_code
--    （实体 WmItemRecpt + mapper 新增字段，从未写过迁移；生产入库单需关联工单）
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_item_recpt_workorder_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_item_recpt_workorder_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_item_recpt' AND COLUMN_NAME='workorder_id') THEN
        ALTER TABLE qxx_wm_item_recpt ADD COLUMN workorder_id bigint(20) DEFAULT NULL COMMENT '生产工单ID(生产入库时填写)' AFTER total_quantity;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_item_recpt' AND COLUMN_NAME='workorder_code') THEN
        ALTER TABLE qxx_wm_item_recpt ADD COLUMN workorder_code varchar(64) DEFAULT NULL COMMENT '生产工单编码' AFTER workorder_id;
    END IF;
END$$
DELIMITER ;
CALL proc_add_item_recpt_workorder_cols();
DROP PROCEDURE IF EXISTS proc_add_item_recpt_workorder_cols;
