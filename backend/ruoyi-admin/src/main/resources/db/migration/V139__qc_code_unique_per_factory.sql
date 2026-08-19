-- ============================================================
-- V139: QC 模块编码唯一索引改为 (factory_id, code) 复合唯一
--
-- 背景：
--   V137 建表时 uk_index_code / uk_template_code / uk_defect_code /
--   uk_iqc_code / uk_ipqc_code / uk_oqc_code / uk_rqc_code 均为
--   单列全局唯一，与多工厂隔离设计冲突——不同工厂不允许出现同编码的
--   检测项/模板/缺陷/检验单。应用层 checkXxxCodeUnique 已由
--   FactoryIdInterceptor 自动注入 factory_id（即按工厂判重），DB
--   约束范围必须与之对齐。
--
-- 方案：
--   删除 7 个单列唯一索引，改建 (factory_id, xxx_code) 复合唯一索引。
--   存量数据在旧单列唯一约束下全局不重复，故同工厂内必然无重复，
--   无需数据清洗。
--
-- 幂等：存储过程内用 INFORMATION_SCHEMA 判断后 DROP/ADD，可重复执行。
-- 字符集：utf8mb4
-- 日期：2026-08-19
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS proc_qc_code_uk_per_factory;
DELIMITER $$
CREATE PROCEDURE proc_qc_code_uk_per_factory()
BEGIN
    -- 删除旧的单列全局唯一索引（若存在）
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_index'
               AND INDEX_NAME = 'uk_index_code') THEN
        ALTER TABLE qxx_qc_index DROP INDEX uk_index_code;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_template'
               AND INDEX_NAME = 'uk_template_code') THEN
        ALTER TABLE qxx_qc_template DROP INDEX uk_template_code;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_defect'
               AND INDEX_NAME = 'uk_defect_code') THEN
        ALTER TABLE qxx_qc_defect DROP INDEX uk_defect_code;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_iqc'
               AND INDEX_NAME = 'uk_iqc_code') THEN
        ALTER TABLE qxx_qc_iqc DROP INDEX uk_iqc_code;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_ipqc'
               AND INDEX_NAME = 'uk_ipqc_code') THEN
        ALTER TABLE qxx_qc_ipqc DROP INDEX uk_ipqc_code;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_oqc'
               AND INDEX_NAME = 'uk_oqc_code') THEN
        ALTER TABLE qxx_qc_oqc DROP INDEX uk_oqc_code;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_rqc'
               AND INDEX_NAME = 'uk_rqc_code') THEN
        ALTER TABLE qxx_qc_rqc DROP INDEX uk_rqc_code;
    END IF;

    -- 新建 (factory_id, code) 复合唯一索引（若不存在）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_index'
                   AND INDEX_NAME = 'uk_factory_index_code') THEN
        ALTER TABLE qxx_qc_index
          ADD UNIQUE KEY uk_factory_index_code (factory_id, index_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_template'
                   AND INDEX_NAME = 'uk_factory_template_code') THEN
        ALTER TABLE qxx_qc_template
          ADD UNIQUE KEY uk_factory_template_code (factory_id, template_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_defect'
                   AND INDEX_NAME = 'uk_factory_defect_code') THEN
        ALTER TABLE qxx_qc_defect
          ADD UNIQUE KEY uk_factory_defect_code (factory_id, defect_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_iqc'
                   AND INDEX_NAME = 'uk_factory_iqc_code') THEN
        ALTER TABLE qxx_qc_iqc
          ADD UNIQUE KEY uk_factory_iqc_code (factory_id, iqc_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_ipqc'
                   AND INDEX_NAME = 'uk_factory_ipqc_code') THEN
        ALTER TABLE qxx_qc_ipqc
          ADD UNIQUE KEY uk_factory_ipqc_code (factory_id, ipqc_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_oqc'
                   AND INDEX_NAME = 'uk_factory_oqc_code') THEN
        ALTER TABLE qxx_qc_oqc
          ADD UNIQUE KEY uk_factory_oqc_code (factory_id, oqc_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_qc_rqc'
                   AND INDEX_NAME = 'uk_factory_rqc_code') THEN
        ALTER TABLE qxx_qc_rqc
          ADD UNIQUE KEY uk_factory_rqc_code (factory_id, rqc_code);
    END IF;
END$$
DELIMITER ;
CALL proc_qc_code_uk_per_factory();
DROP PROCEDURE IF EXISTS proc_qc_code_uk_per_factory;
