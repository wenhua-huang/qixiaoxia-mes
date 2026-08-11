-- ============================================================
-- V106：外协收货行补批次属性列
--
-- 背景：外协收货入库需要生成独立成品批次（不再叠回原材料批次桶），
--       getOrGenerateBatchCode 按 (item+vendor+produce_date+expire_date+lot_number) 查重，
--       收货行需承载这些批次属性（厂商录结果时可选录入，不填则后端用收货日期兜底）。
--
-- 幂等：ALTER 用 PROCEDURE 包装（列存在则跳过），与 V100 风格一致。
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'produce_date', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN produce_date datetime DEFAULT NULL COMMENT ''生产日期''');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'expire_date', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN expire_date datetime DEFAULT NULL COMMENT ''有效期''');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'lot_number', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN lot_number varchar(64) DEFAULT NULL COMMENT ''生产批号''');

DROP PROCEDURE IF EXISTS add_col_if_missing;
