-- V61__product_recpt_menu_and_cleanup.sql
-- 产品入库单迁移到 qxx_wm_product_recpt:菜单恢复 + 权限对齐 + 历史数据清理
-- 背景:工单完工入库此前写入 qxx_wm_item_recpt(recpt_type=PRODUCE),与采购入库混表。
-- 现已改为写入 qxx_wm_product_recpt(独立闭环),本迁移恢复「产品入库」菜单并清理历史 PRODUCE 数据。

-- ============================================================
-- Part 1: 恢复「产品入库」菜单显示 + 权限标识对齐后端
-- ============================================================
-- 后端 WmProductRecptController @PreAuthorize 使用 mes:wm:product_recpt:*（有下划线）
-- 原 DB 菜单 perms 为 mes:wm:productrecpt:list（无下划线），需对齐
UPDATE sys_menu
SET visible = '0',
    perms   = 'mes:wm:product_recpt:list',
    menu_name = '产品入库'
WHERE menu_id = 2204;

-- ============================================================
-- Part 2: 补充产品入库按钮权限（幂等）
-- 清理旧的无下划线按钮权限（与后端 mes:wm:product_recpt:* 对齐，改用有下划线）
DELETE FROM sys_menu WHERE parent_id = 2204 AND perms LIKE 'mes:wm:productrecpt:%';
-- ============================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '产品入库查询', 2204, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:product_recpt:query', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2204 AND perms = 'mes:wm:product_recpt:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '产品入库新增', 2204, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:product_recpt:add', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2204 AND perms = 'mes:wm:product_recpt:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '产品入库修改', 2204, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:product_recpt:edit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2204 AND perms = 'mes:wm:product_recpt:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '产品入库删除', 2204, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:product_recpt:remove', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2204 AND perms = 'mes:wm:product_recpt:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '产品入库导出', 2204, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:product_recpt:export', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2204 AND perms = 'mes:wm:product_recpt:export');

-- ============================================================
-- Part 3: 历史数据清理
-- 删除 item_recpt 中 recpt_type=PRODUCE 的生产入库草稿（已迁移到 product_recpt）
-- ⚠️ 安全约束:仅删 status='DRAFT'(未过账无库存事务影响),CONFIRMED/POSTED 保留
--    原因:已过账单据已推库存事务(qxx_wm_transaction 引用其 recpt_id),
--         删除会造成事务孤儿指向不存在的 recpt_id,破坏审计追溯
-- 迁移清理:清所有工厂的 DRAFT PRODUCE 数据(non-business,跨工厂清理是有意为之)
-- 顺序:先明细/行,后表头(避免外键残留)
-- ============================================================
DELETE FROM qxx_wm_item_recpt_detail
WHERE recpt_id IN (
    SELECT recpt_id FROM qxx_wm_item_recpt
    WHERE recpt_type = 'PRODUCE' AND status = 'DRAFT'
);

DELETE FROM qxx_wm_item_recpt_line
WHERE recpt_id IN (
    SELECT recpt_id FROM qxx_wm_item_recpt
    WHERE recpt_type = 'PRODUCE' AND status = 'DRAFT'
);

DELETE FROM qxx_wm_item_recpt
WHERE recpt_type = 'PRODUCE' AND status = 'DRAFT';
