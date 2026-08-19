-- ============================================================
-- V140: 采购入库单编码唯一约束 — 防止重复单号落库
--
-- 背景：
--   qxx_wm_item_recpt 出现同 factory 同 recpt_code 两张单
--   （217 DRAFT / 218 POSTED 均 00RCVT20260819001，系 PC 端保存按钮
--   无防连点，双击把开弹窗时预生成的同一个号提交两次；后端 insert
--   无锁无查重，DB 此前亦无唯一索引，同号两行静默落库）。
--
-- 方案：
--   1. 幂等修正存量重复：同 (factory_id, recpt_code) 组内用 ROW_NUMBER
--      按业务状态（POSTED > CONFIRMED > DRAFT）、创建时间、主键排序，
--      保留 rn=1 一张，其余重命名为 原编码-D{recpt_id}。
--   2. 加唯一索引 uk_factory_recpt_code(factory_id, recpt_code)，
--      编码生成走编码器序号，同工厂内天然不冲突；跨工厂允许同码。
--
-- 幂等：UPDATE 带 JOIN 重复组判断（改名后 code 不再重复，不再命中）；
--       索引用 INFORMATION_SCHEMA 判断后创建。可重复执行。
-- 字符集：utf8mb4
-- 日期：2026-08-19
-- ============================================================

SET NAMES utf8mb4;

-- ────────────────────────────────────────────────────────────
-- Part 1: 幂等修正存量重复编码
--   保留规则：状态优先级 POSTED > CONFIRMED > DRAFT（FIELD 值越小越优），
--   同状态取 create_time 最早、recpt_id 最小；其余加 -D{recpt_id} 后缀。
-- ────────────────────────────────────────────────────────────
UPDATE qxx_wm_item_recpt t
JOIN (
    SELECT r.recpt_id
    FROM qxx_wm_item_recpt r
    JOIN (
        SELECT recpt_id,
               ROW_NUMBER() OVER (
                   PARTITION BY factory_id, recpt_code
                   ORDER BY FIELD(status, 'POSTED', 'CONFIRMED', 'DRAFT'),
                            create_time, recpt_id
               ) AS rn
        FROM qxx_wm_item_recpt
    ) ranked ON r.recpt_id = ranked.recpt_id
    JOIN (
        SELECT factory_id, recpt_code
        FROM qxx_wm_item_recpt
        GROUP BY factory_id, recpt_code
        HAVING COUNT(*) > 1
    ) dup ON r.factory_id = dup.factory_id AND r.recpt_code = dup.recpt_code
    WHERE ranked.rn > 1
) d ON t.recpt_id = d.recpt_id
SET t.recpt_code = CONCAT(t.recpt_code, '-D', t.recpt_id),
    t.update_by = 'admin', t.update_time = NOW();

-- ────────────────────────────────────────────────────────────
-- Part 2: 唯一索引（存储过程幂等创建，范式同 V130/V135）
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS proc_add_item_recpt_code_uk;
DELIMITER $$
CREATE PROCEDURE proc_add_item_recpt_code_uk()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_item_recpt'
                   AND INDEX_NAME = 'uk_factory_recpt_code') THEN
        ALTER TABLE qxx_wm_item_recpt
          ADD UNIQUE KEY uk_factory_recpt_code (factory_id, recpt_code);
    END IF;
END$$
DELIMITER ;
CALL proc_add_item_recpt_code_uk();
DROP PROCEDURE IF EXISTS proc_add_item_recpt_code_uk;
