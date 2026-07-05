-- ============================================================
-- V36: 修复仓库管理菜单 — 路径纠正 + 补全缺失菜单
-- 问题：qxx_wm_core_menu.sql 中 path 与前端路由不一致（如 location→storage_location）
--      生产环境缺少 7 个仓库核心菜单
-- ============================================================

-- ==================== 1. 修复已有菜单的 path ====================
UPDATE sys_menu SET path = 'storage_location'  WHERE parent_id = 2002 AND path = 'location';
UPDATE sys_menu SET path = 'storage_area'      WHERE parent_id = 2002 AND path = 'area';
UPDATE sys_menu SET path = 'stock_taking_plan' WHERE parent_id = 2002 AND path = 'takingplan';
UPDATE sys_menu SET path = 'rt_vendor'         WHERE parent_id = 2002 AND path = 'rtvendor';
UPDATE sys_menu SET path = 'rt_sales'           WHERE parent_id = 2002 AND path = 'rtsales';
UPDATE sys_menu SET path = 'roll_detail'       WHERE parent_id = 2002 AND path = 'rolldetail';
UPDATE sys_menu SET path = 'misc_recpt'        WHERE parent_id = 2002 AND path = 'miscrecpt';
UPDATE sys_menu SET path = 'misc_issue'        WHERE parent_id = 2002 AND path = 'miscissue';
UPDATE sys_menu SET path = 'item_recpt'        WHERE parent_id = 2002 AND path = 'itemrecpt';

-- ==================== 2. 补全缺失菜单 ====================

-- 仓库
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('仓库', 2002, 1, 'warehouse', 'mes/wm/warehouse/index', 1, 0, 'C', '0', '0', 'mes:wm:warehouse:list', '#', 'admin', sysdate(), '', NULL, '仓库菜单');
SET @wh_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2002 AND perms = 'mes:wm:warehouse:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('仓库查询', @wh_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:warehouse:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('仓库新增', @wh_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:warehouse:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('仓库修改', @wh_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:warehouse:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('仓库删除', @wh_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:warehouse:remove', '#', 'admin', sysdate(), '', NULL, ''),
       ('仓库导出', @wh_id, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:warehouse:export', '#', 'admin', sysdate(), '', NULL, '');

-- 物料库存
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('物料库存', 2002, 2, 'material_stock', 'mes/wm/material_stock/index', 1, 0, 'C', '0', '0', 'mes:wm:stock:list', '#', 'admin', sysdate(), '', NULL, '物料库存菜单');
SET @stock_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2002 AND perms = 'mes:wm:stock:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('物料库存查询', @stock_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:stock:query', '#', 'admin', sysdate(), '', NULL, '');

-- 物料入库
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('物料入库', 2002, 3, 'item_recpt', 'mes/wm/item_recpt/index', 1, 0, 'C', '0', '0', 'mes:wm:itemrecpt:list', '#', 'admin', sysdate(), '', NULL, '物料入库菜单');
SET @ir_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2002 AND perms = 'mes:wm:itemrecpt:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('物料入库查询', @ir_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('物料入库新增', @ir_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('物料入库修改', @ir_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('物料入库删除', @ir_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:itemrecpt:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 库存发料
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库存发料', 2002, 4, 'issue', 'mes/wm/issue/index', 1, 0, 'C', '0', '0', 'mes:wm:issue:list', '#', 'admin', sysdate(), '', NULL, '库存发料菜单');
SET @issue_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2002 AND perms = 'mes:wm:issue:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库存发料查询', @issue_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('库存发料新增', @issue_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('库存发料修改', @issue_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('库存发料删除', @issue_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 库存盘点
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库存盘点', 2002, 10, 'stock_taking', 'mes/wm/stock_taking/index', 1, 0, 'C', '0', '0', 'mes:wm:taking:list', '#', 'admin', sysdate(), '', NULL, '库存盘点菜单');
SET @taking_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2002 AND perms = 'mes:wm:taking:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库存盘点查询', @taking_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:taking:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('库存盘点新增', @taking_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:taking:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('库存盘点修改', @taking_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:taking:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('库存盘点删除', @taking_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:taking:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 销售发货
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('销售发货', 2002, 12, 'product_sales', 'mes/wm/product_sales/index', 1, 0, 'C', '0', '0', 'mes:wm:sales:list', '#', 'admin', sysdate(), '', NULL, '销售发货菜单');
SET @sales_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2002 AND perms = 'mes:wm:sales:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('销售发货查询', @sales_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('销售发货新增', @sales_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('销售发货修改', @sales_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('销售发货删除', @sales_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== 3. 清理本地测试环境可能存在的采购重复 ====================
-- 删除 perms 相同、但不在生产环境 ID 范围内的重复采购菜单
-- 保留较早创建的记录（较小的 menu_id）
DELETE FROM sys_menu WHERE parent_id = 2006 AND perms = 'mes:pur:order:list' AND menu_id != (SELECT MIN(m2.menu_id) FROM (SELECT menu_id FROM sys_menu WHERE parent_id = 2006 AND perms = 'mes:pur:order:list') AS m2);
DELETE FROM sys_menu WHERE parent_id = 2006 AND perms = 'mes:pur:order-line:list' AND menu_id != (SELECT MIN(m2.menu_id) FROM (SELECT menu_id FROM sys_menu WHERE parent_id = 2006 AND perms = 'mes:pur:order-line:list') AS m2);
