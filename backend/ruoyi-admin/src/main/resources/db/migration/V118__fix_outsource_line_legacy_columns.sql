-- ============================================================
-- V118：修复外协发料/收货行表遗留 NOT NULL 列
--
-- 背景：V56 创建的 qxx_wm_outsource_issue_line / qxx_wm_outsource_recpt_line
--       是旧"外协领料单/入库单"框架的子表，含 issue_id/recpt_id 等 NOT NULL 列。
--       V100 引入通用外协订单框架（order_id 取代 issue_id/recpt_id），
--       但用 CREATE TABLE IF NOT EXISTS，旧表存在所以未重建，
--       只 ADD 了 order_id 等新列。结果：
--         - insertIssueLine 不写 issue_id → 撞 "Field 'issue_id' doesn't have a default value"
--         - warehouse_id / unit_name 在 V56 是 NOT NULL，但 V100 新代码允许为空
--           （PC 端直接发货可能不指定仓库，FIFO 解析不到批次时仓库为空）
--       同隐患潜伏在 recpt_line.recpt_id 上。
--
-- 修复：两表生产数据均为空（旧框架从未使用），直接删除遗留列，
--       并把 warehouse_id / unit_name 调整为 V100 设计的可空口径。
--       使用 information_schema 判存，幂等兼容本地全新库（V100 直接建表无遗留列）。
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS drop_col_if_exists;
CREATE PROCEDURE drop_col_if_exists(IN tbl VARCHAR(64), IN col VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` DROP COLUMN `', col, '`');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

DROP PROCEDURE IF EXISTS modify_col_nullable;
CREATE PROCEDURE modify_col_nullable(IN tbl VARCHAR(64), IN col VARCHAR(64), IN coltype VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col
                 AND is_nullable='NO') THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` MODIFY COLUMN `', col, '` ', coltype, ' NULL');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

-- qxx_wm_outsource_issue_line：删除 V56 遗留外键列与未使用字段
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'issue_id');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'item_type');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'unit2');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'unit2_name');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'quantity_issue');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'quantity_issue2');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'bundle_count');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'location_id');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'area_id');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'update_by');
CALL drop_col_if_exists('qxx_wm_outsource_issue_line', 'update_time');

-- qxx_wm_outsource_recpt_line：删除 V56 遗留外键列与未使用字段
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'recpt_id');
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'quantity_recpt');
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'quantity_box');
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'location_id');
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'area_id');
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'update_by');
CALL drop_col_if_exists('qxx_wm_outsource_recpt_line', 'update_time');

-- 放宽 NOT NULL 约束为可空（与 V100 新建表 DDL 一致）
CALL modify_col_nullable('qxx_wm_outsource_issue_line', 'warehouse_id', 'bigint');
CALL modify_col_nullable('qxx_wm_outsource_issue_line', 'unit_name', 'varchar(64)');
CALL modify_col_nullable('qxx_wm_outsource_recpt_line', 'warehouse_id', 'bigint');
CALL modify_col_nullable('qxx_wm_outsource_recpt_line', 'unit_name', 'varchar(64)');

DROP PROCEDURE IF EXISTS drop_col_if_exists;
DROP PROCEDURE IF EXISTS modify_col_nullable;
