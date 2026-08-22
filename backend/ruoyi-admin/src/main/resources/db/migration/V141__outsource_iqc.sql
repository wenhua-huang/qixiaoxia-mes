-- ═══════════════════════════════════════════════════════════════════════
-- V141 外协收料来料质检挂点
--
-- 背景：外协收货原本"收料即入库"，无任何质检门控，加工回来的成品/半成品
--       未经检验直接入可用库存。本次复用 IQC 来料检验链路：外协厂商发货时
--       自动生成 PENDING 检验单（source_doc_type=wm_outsource_order），
--       我方收货入库前由 QcGate 校验必须 COMPLETED + PASS/CONCESSION。
--
-- 改动：
--   1. qxx_wm_outsource_order 加 iqc_id/iqc_code（首条来料检验单挂点，
--      与 qxx_wm_item_recpt.iqc_id 同口径；多物料多张单时仅首张回填）
--
-- 注：无字典/菜单/权限变更——外协来料检在现有"来料检验 IQC"菜单内统一处理。
--     ALTER 用 PROCEDURE 包装实现幂等（列已存在则跳过）。
-- @author qixiaoxia
-- ═══════════════════════════════════════════════════════════════════════

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_wm_outsource_order', 'iqc_id',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN iqc_id bigint DEFAULT NULL COMMENT ''首条来料检验单ID'' AFTER feedback_id');
CALL add_col_if_missing('qxx_wm_outsource_order', 'iqc_code',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN iqc_code varchar(64) DEFAULT NULL COMMENT ''首条来料检验单号'' AFTER iqc_id');

DROP PROCEDURE IF EXISTS add_col_if_missing;
