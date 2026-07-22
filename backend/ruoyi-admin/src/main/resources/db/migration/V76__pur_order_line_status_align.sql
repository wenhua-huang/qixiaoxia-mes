-- =====================================================================
-- V76: 采购订单行状态与头状态对齐
-- =====================================================================
-- 背景：V06 建表时头表 status 默认 'DRAFT'、行表 status 默认 'ORDERED'，
-- 且行表枚举连 DRAFT 都没有。导致「在草稿单上新增行」时，行自动变成 ORDERED
-- 而头仍停在 DRAFT，产生头/行状态不一致（如 PO20260717090 头=DRAFT 行=ORDERED）。
-- 本迁移：
--   1. 行表 status 默认值改为 DRAFT，枚举注释补 DRAFT/APPROVED，与头表对齐
--   2. 存量脏数据修复：头=DRAFT/APPROVED 但行=ORDERED 的，行回退到与头一致
-- 幂等：ALTER MODIFY 可重复执行；UPDATE 重跑时 WHERE 不再命中已修复数据
-- =====================================================================

-- 1. 行表 status 默认值与注释对齐头表
ALTER TABLE qxx_pur_order_line
    MODIFY COLUMN status varchar(50) DEFAULT 'DRAFT'
    COMMENT '行状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消';

-- 2. 存量脏数据修复：行 status 超过头 status 的，回退到头当前状态
--    覆盖场景：头=DRAFT/APPROVED，行却 =ORDERED（收货中/已收货的头行本就一致，不动）
UPDATE qxx_pur_order_line l
INNER JOIN qxx_pur_order o ON l.order_id = o.order_id
SET l.status = o.status
WHERE o.status IN ('DRAFT', 'APPROVED')
  AND l.status = 'ORDERED';
