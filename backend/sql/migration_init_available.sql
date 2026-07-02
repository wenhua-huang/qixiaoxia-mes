-- 初始化 quantity_available = quantity_onhand（存量数据迁移）
-- 修复：旧数据 quantity_available 默认值 0，导致齐套分析误报缺料
UPDATE qxx_wm_material_stock
SET quantity_available = quantity_onhand
WHERE quantity_available = 0 AND quantity_onhand > 0;
