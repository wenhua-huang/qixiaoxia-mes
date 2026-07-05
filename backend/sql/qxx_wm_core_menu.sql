-- ============================================================
-- 企小侠MES — wm-core 菜单 & 权限配置
-- 版本: v1.0  日期: 2026-06-12
-- 父菜单: menu_id=2002 (仓储管理, 已在开发顺序中预设)
-- ============================================================

-- 0. 生产领料 (order 4)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('生产领料', 2002, 4, 'issue', 'mes/wm/issue/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:issue:list', '#', 'admin', sysdate(), '', NULL, '');
SET @issue_menu = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, create_by, create_time, update_by, update_time, remark)
VALUES ('领料查询', @issue_menu, 1, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:issue:query', 'admin', sysdate(), '', NULL, ''),
       ('领料新增', @issue_menu, 2, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:issue:add', 'admin', sysdate(), '', NULL, ''),
       ('领料修改', @issue_menu, 3, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:issue:edit', 'admin', sysdate(), '', NULL, ''),
       ('领料删除', @issue_menu, 4, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:issue:remove', 'admin', sysdate(), '', NULL, '');

-- 1. 仓库基础数据 (order 8-10)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库区定义', 2002, 8, 'location', 'mes/wm/location/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:location:list', '#', 'admin', sysdate(), '', NULL, '');
SET @loc_menu = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库位定义', 2002, 9, 'area', 'mes/wm/area/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:area:list', '#', 'admin', sysdate(), '', NULL, '');

-- 2. 库存核心 (order 10-12)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库存事务', 2002, 10, 'transaction', 'mes/wm/transaction/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:transaction:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('批次管理', 2002, 11, 'batch', 'mes/wm/batch/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:batch:list', '#', 'admin', sysdate(), '', NULL, '');

-- 3. 物料入库单 (order 12-14) — Header-Line-Detail
-- itemrecpt header already exists at order 3, re-use it
-- Add line + detail as sub-menus of itemrecpt
SELECT @itemrecpt_id := menu_id FROM sys_menu WHERE path = 'itemrecpt' AND parent_id = 2002;
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('物料入库行', @itemrecpt_id, 1, 'itemrecptline', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:itemrecptline:list', '#', 'admin', sysdate(), '', NULL, '');

-- 4. 杂项入库 (order 13)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('杂项入库', 2002, 13, 'miscrecpt', 'mes/wm/miscrecpt/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:miscrecpt:list', '#', 'admin', sysdate(), '', NULL, '');

-- 5. 杂项出库 (order 14)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('杂项出库', 2002, 14, 'miscissue', 'mes/wm/miscissue/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:miscissue:list', '#', 'admin', sysdate(), '', NULL, '');

-- 6. 调拨转移 (order 15)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('调拨转移', 2002, 15, 'transfer', 'mes/wm/transfer/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:transfer:list', '#', 'admin', sysdate(), '', NULL, '');

-- 7. 盘点 (order 16-17)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('盘点方案', 2002, 16, 'takingplan', 'mes/wm/takingplan/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:takingplan:list', '#', 'admin', sysdate(), '', NULL, '');

-- 8. 供应商退货 (order 18)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('供应商退货', 2002, 18, 'rtvendor', 'mes/wm/rtvendor/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:rtvendor:list', '#', 'admin', sysdate(), '', NULL, '');

-- 9. 销售退货 (order 19)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('销售退货', 2002, 19, 'rtsales', 'mes/wm/rtsales/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:rtsales:list', '#', 'admin', sysdate(), '', NULL, '');

-- 10. 条码管理 (order 20)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('条码管理', 2002, 20, 'barcode', 'mes/wm/barcode/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:barcode:list', '#', 'admin', sysdate(), '', NULL, '');

-- 11. 装箱管理 (order 21)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('装箱管理', 2002, 21, 'package', 'mes/wm/package/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:package:list', '#', 'admin', sysdate(), '', NULL, '');

-- 12. 纸卷明细 (order 22)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('纸卷明细', 2002, 22, 'rolldetail', 'mes/wm/rolldetail/index', NULL, NULL, 1, 0, 'C', '0', '0', 'mes:wm:rolldetail:list', '#', 'admin', sysdate(), '', NULL, '');

-- ============================================================
-- 按钮权限 (每个菜单对应的 CRUD 按钮)
-- ============================================================
-- 函数：批量插入按钮权限
-- 用法：CALL insert_wm_buttons(菜单ID, 权限前缀);

-- 库区
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, create_by, create_time, update_by, update_time, remark)
VALUES ('库区查询', @loc_menu, 1, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:query', 'admin', sysdate(), '', NULL, ''),
       ('库区新增', @loc_menu, 2, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:add', 'admin', sysdate(), '', NULL, ''),
       ('库区修改', @loc_menu, 3, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:edit', 'admin', sysdate(), '', NULL, ''),
       ('库区删除', @loc_menu, 4, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:remove', 'admin', sysdate(), '', NULL, ''),
       ('库区导出', @loc_menu, 5, '', NULL, 1, 0, 'F', '0', '0', 'mes:wm:location:export', 'admin', sysdate(), '', NULL, '');
