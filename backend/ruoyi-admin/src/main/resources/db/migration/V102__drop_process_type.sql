-- ============================================================
-- V102：删除 process_type，外发判定统一到 is_outsource
--
-- 背景：process_type（INTERNAL-自制/OUTSOURCE-外发）原本挂在工序定义上，
--       但"自制/外发"不是工序的本质属性——同一道工序（如分切）在不同路线里
--       可能厂内做也可能外发。外发判定应只看路线工序的 is_outsource（1/0）。
--       process_type 退化为冗余且语义错误，本迁移删除该字段。
--
-- 改动：
--   1. DROP qxx_pro_process / qxx_pro_route_process / qxx_pro_card_process 的 process_type 列
--   2. 确保 qxx_pro_route_process 有 is_outsource 列（历史表可能漏建）
--   3. 把引用"外发加工"工序(process_id=207)的路线节点标 is_outsource='1'
--   4. 删除死字典 mes_process_type（零代码引用，且其数据是 workstation 语义的污染）
--
-- 保留：qxx_md_workstation.process_type（同名不同义，存"设备可执行工序编码"PRINT/BAG_MAKE/SLITTING）
--       V16 原文不改（已执行迁移）；pro_process=207 的"外发加工"工序实体保留（被路线引用）。
--
-- 幂等：ALTER/DELETE 均用 information_schema / 行存在判断，可重复执行。
-- 注：sys_dict_*/sys_dict_data 系统表无 factory_id，全局表。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 1. 幂等 DROP COLUMN（列不存在则跳过）
-- ════════════════════════════════════════════
DROP PROCEDURE IF EXISTS drop_col_if_exists;
CREATE PROCEDURE drop_col_if_exists(IN tbl VARCHAR(64), IN col VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` DROP COLUMN `', col, '`');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL drop_col_if_exists('qxx_pro_process', 'process_type');
CALL drop_col_if_exists('qxx_pro_route_process', 'process_type');
CALL drop_col_if_exists('qxx_pro_card_process', 'process_type');

DROP PROCEDURE IF EXISTS drop_col_if_exists;

-- ════════════════════════════════════════════
-- 2. 确保 route_process 有 is_outsource 列（防历史表漏建，与 manual_tables 对齐）
-- ════════════════════════════════════════════
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_pro_route_process', 'is_outsource',
    'ALTER TABLE qxx_pro_route_process ADD COLUMN is_outsource char(1) DEFAULT ''0'' COMMENT ''是否外发工序(1-是,0-否)''');

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- ════════════════════════════════════════════
-- 3. 把引用"外发加工"工序(process_id=207)的路线节点标为外发
--    背景：V16 的 route_process record 309 引用了 process_id=207（外发加工），
--          但当初未插 is_outsource 列（走表默认 '0'）。外发判定改读 is_outsource 后，
--          这些节点必须正式打上外发标记，否则外协流程会判不到。
--    幂等：is_outsource 已是 '1' 时 UPDATE 无副作用。
-- ════════════════════════════════════════════
UPDATE qxx_pro_route_process SET is_outsource = '1' WHERE process_id = 207 AND (is_outsource IS NULL OR is_outsource = '0');

-- ════════════════════════════════════════════
-- 4. 删除死字典 mes_process_type
--    背景：V20 把 PRINT/BAG_MAKE/SLITTING/INSPECT（工序编码）误种成"工序类型"字典值，
--          概念污染；且全工程零代码引用（前端 processTypeOptions 硬编码，不读字典）。
--    sys_dict_type.dict_id=21 对应 dict_type='mes_process_type'。
-- ════════════════════════════════════════════
DELETE FROM sys_dict_data WHERE dict_type = 'mes_process_type';
DELETE FROM sys_dict_type WHERE dict_type = 'mes_process_type';
