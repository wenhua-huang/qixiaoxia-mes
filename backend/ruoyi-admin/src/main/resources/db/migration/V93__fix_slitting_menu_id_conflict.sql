-- ════════════════════════════════════════════
-- V93：修复 V92 分切菜单 ID 冲突
-- ════════════════════════════════════════════
-- 背景：V92 的 6.x 节尝试用 menu_id=2310/23101/23102/23103 插入"分切作业"菜单，
--       但这些 ID 已被"工序管理"（menu_id=2310）及其子按钮占用。
--       V92 使用 WHERE NOT EXISTS，冲突时静默跳过，导致：
--         1. 前端路由 /mes/pro/slitting 报 404（menu 未入库 -> getRouters 无对应节点）
--         2. mes:pro:slitting:list / add / query / edit 权限位缺失
--
-- 修复：改用不冲突的 menu_id=2313（2311/2312 已用，2313 空闲），子按钮 23131~23133。
--       同步给 role_id=11 / 12（V92 里出现过的产线角色）授权。
--
-- 表结构说明：sys_menu 无 factory_id；sys_role_menu 有 factory_id（复合主键），主工厂=1。
-- ════════════════════════════════════════════

-- 1. C-menu：生产管理 -> 分切作业
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2313, '分切作业', 2003, 13, 'slitting', 'mes/pro/slitting/index', 1, 0, 'C', '0', '0', 'mes:pro:slitting:list', 'scissors', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2313);

-- 2. F-menu 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23131, '分切查询', 2313, 1, 'F', '0', '0', 'mes:pro:slitting:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23131);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23132, '分切执行', 2313, 2, 'F', '0', '0', 'mes:pro:slitting:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23132);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23133, '分切详情', 2313, 3, 'F', '0', '0', 'mes:pro:slitting:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23133);

-- 3. 给已有产线角色（11=产线组长, 12=产线操作员）授权 —— factory_id=1（主工厂）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 2313, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 11 AND menu_id = 2313 AND factory_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 23131, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 11 AND menu_id = 23131 AND factory_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 23132, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 11 AND menu_id = 23132 AND factory_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 23133, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 11 AND menu_id = 23133 AND factory_id = 1);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 12, 2313, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 12 AND menu_id = 2313 AND factory_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 12, 23131, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 12 AND menu_id = 23131 AND factory_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 12, 23132, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 12 AND menu_id = 23132 AND factory_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 12, 23133, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 12 AND menu_id = 23133 AND factory_id = 1);
