-- =====================================================================
-- V122: 外部系统 API Key 凭证管理 - 注册菜单与按钮权限
-- 表 qxx_sys_api_key 已在 V121 创建；本迁移补齐 sys_menu + admin 角色授权。
-- 注：sys_menu 为系统表，无 factory_id。
-- =====================================================================

-- 1. C 菜单：系统管理 → API Key管理（挂 parent_id=1，order_num=10）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 118, 'API Key管理', 1, 10, 'apikey', 'system/apikey/index', 1, 0, 'C', '0', '0', 'system:apikey:list', 'key', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 118);

-- 2. F 按钮：凭证生成
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 1061, '凭证生成', 118, 1, 'F', '0', '0', 'system:apikey:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1061);

-- 3. F 按钮：启用/停用
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 1062, '启用停用', 118, 2, 'F', '0', '0', 'system:apikey:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1062);

-- 4. F 按钮：删除/吊销
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 1063, '凭证删除', 118, 3, 'F', '0', '0', 'system:apikey:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1063);

-- 5. 授权给超级管理员角色（role_id=1, factory_id=0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE menu_id IN (118, 1061, 1062, 1063)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
  );
