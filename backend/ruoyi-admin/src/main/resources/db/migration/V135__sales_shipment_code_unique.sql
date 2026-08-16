-- ============================================================
-- V135: 销售出库/发运单编码唯一约束 — 防止产生脏数据
--
-- 背景：
--   qxx_wm_product_sales 出现同 factory 同 sales_code 两张单
--   （215/216 均 SALE20260723023，create_by 为空，系 2026-07-23
--   E2E 脚本直插写死编码所致，业务编码器本身未发现问题）。
--   数据库层此前无约束，任何来源（测试脚本/直插 SQL/并发）都可能再撞码，
--   导致按编码查单结果不确定（byCode 只能靠排序兜底）。
--
-- 方案：
--   1. 幂等修正存量重复：同 (factory_id, sales_code) 组内保留
--      create_time 最早的一张（业务先入账），其余重命名为
--      原编码-D{sales_id}（带主键后缀保证唯一且重复执行稳定）。
--   2. 加唯一索引 uk_factory_sales_code(factory_id, sales_code)、
--      uk_factory_shipment_code(factory_id, shipment_code)，
--      编码生成走编码器序号，同工厂内天然不冲突；跨工厂允许同码
--      （多工厂隔离设计）。
--
-- 幂等：UPDATE 带 JOIN 重复组判断（改名后不再命中）；索引用
--       INFORMATION_SCHEMA 判断后创建。可重复执行。
-- 字符集：utf8mb4
-- 日期：2026-08-16
-- ============================================================

SET NAMES utf8mb4;

-- ────────────────────────────────────────────────────────────
-- Part 1: 幂等修正存量重复编码（保留每组最早一张，其余加 -D{sales_id} 后缀）
-- ────────────────────────────────────────────────────────────
UPDATE qxx_wm_product_sales t
JOIN (
    SELECT p.sales_id
    FROM qxx_wm_product_sales p
    JOIN (
        SELECT factory_id, sales_code, MIN(create_time) AS min_ct, MIN(sales_id) AS min_id
        FROM qxx_wm_product_sales
        GROUP BY factory_id, sales_code
        HAVING COUNT(*) > 1
    ) dup ON p.factory_id = dup.factory_id AND p.sales_code = dup.sales_code
    WHERE NOT (p.create_time = dup.min_ct AND p.sales_id = dup.min_id)
) d ON t.sales_id = d.sales_id
SET t.sales_code = CONCAT(t.sales_code, '-D', t.sales_id),
    t.update_by = 'admin', t.update_time = NOW();

UPDATE qxx_wm_product_sales_shipment t
JOIN (
    SELECT s.shipment_id
    FROM qxx_wm_product_sales_shipment s
    JOIN (
        SELECT factory_id, shipment_code, MIN(create_time) AS min_ct, MIN(shipment_id) AS min_id
        FROM qxx_wm_product_sales_shipment
        GROUP BY factory_id, shipment_code
        HAVING COUNT(*) > 1
    ) dup ON s.factory_id = dup.factory_id AND s.shipment_code = dup.shipment_code
    WHERE NOT (s.create_time = dup.min_ct AND s.shipment_id = dup.min_id)
) d ON t.shipment_id = d.shipment_id
SET t.shipment_code = CONCAT(t.shipment_code, '-D', t.shipment_id),
    t.update_by = 'admin', t.update_time = NOW();

-- ────────────────────────────────────────────────────────────
-- Part 2: 唯一索引（存储过程幂等创建，范式同 V130）
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS proc_add_sales_code_uk;
DELIMITER $$
CREATE PROCEDURE proc_add_sales_code_uk()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_sales'
                   AND INDEX_NAME = 'uk_factory_sales_code') THEN
        ALTER TABLE qxx_wm_product_sales
          ADD UNIQUE KEY uk_factory_sales_code (factory_id, sales_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_sales_shipment'
                   AND INDEX_NAME = 'uk_factory_shipment_code') THEN
        ALTER TABLE qxx_wm_product_sales_shipment
          ADD UNIQUE KEY uk_factory_shipment_code (factory_id, shipment_code);
    END IF;
END$$
DELIMITER ;
CALL proc_add_sales_code_uk();
DROP PROCEDURE IF EXISTS proc_add_sales_code_uk;
