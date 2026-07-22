-- =====================================================================
-- V77: 明确采购订单头 total_amount/total_quantity 的语义为「原订购冻结值」
-- =====================================================================
-- 背景：此前 total_amount/total_quantity 注释为「采购总金额/数量」，语义模糊。
-- 实际 recalcOrderTotals 始终 SUM 全部行（含已取消行），行为=「下单时原订购值」。
-- 取消/部分收货等异常流转，通过查询时计算的 received_amount/cancelled_amount
-- （见 PurOrderMapper.xml selectPurOrderVo 子查询）表达差异，不回写这两个字段。
-- 本迁移只改注释、明确语义，不改数据结构。
-- 幂等：ALTER MODIFY 可重复执行，不改变已有数据。
-- =====================================================================

ALTER TABLE qxx_pur_order
    MODIFY COLUMN total_quantity decimal(14,4) DEFAULT 0.0000
    COMMENT '原订购总数量(冻结值,取消/关闭不改;已取消数见查询计算列)';

ALTER TABLE qxx_pur_order
    MODIFY COLUMN total_amount decimal(14,2) DEFAULT 0.00
    COMMENT '原采购总金额(冻结值,取消/关闭不改;已收/已取消金额见查询计算列)';
