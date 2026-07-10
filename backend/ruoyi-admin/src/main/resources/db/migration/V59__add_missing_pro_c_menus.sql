-- ============================================================
-- V59: 补齐缺失的生产管理 C-menu（排产任务/生产报工/流转卡）
--
-- 根因：V18 注释假设 init_shengxiang.sql 已创建 menu_id 2302/2303/2304
--       三个 C-menu 及子按钮权限，但该文件从未存在。导致生产环境这三个
--       菜单完全缺失，仅剩三个孤儿导出按钮（23025/23035/23045）。
--
-- 本迁移补齐：
--   2302 排产任务（task）   + 子权限 23021~23024（23025 导出已存在）
--   2303 生产报工（feedback）+ 子权限 23031~23034（23035 导出已存在）
--   2304 流转卡（card）     + 子权限 23041~23044（23045 导出已存在）
--
-- 幂等：全部 WHERE NOT EXISTS；sys_menu 为系统表无 factory_id。
-- ============================================================

-- ════════════════════════════════════════════
-- 1. C-menu（parent_id=2003 生产管理）
-- ════════════════════════════════════════════

-- 2302 排产任务
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2302, '排产任务', 2003, 2, 'task', 'mes/pro/task/index', 1, 0, 'C', '0', '0', 'mes:pro:task:list', 'date', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2302);

-- 2303 生产报工
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2303, '生产报工', 2003, 3, 'feedback', 'mes/pro/feedback/index', 1, 0, 'C', '0', '0', 'mes:pro:feedback:list', 'edit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2303);

-- 2304 流转卡
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2304, '流转卡', 2003, 4, 'card', 'mes/pro/card/index', 1, 0, 'C', '0', '0', 'mes:pro:card:list', 'form', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2304);

-- ════════════════════════════════════════════
-- 2. F-menu 子按钮权限
-- ════════════════════════════════════════════

-- 2302 排产任务：查询/新增/修改/删除（导出 23025 已由 V18 创建）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23021, '任务查询', 2302, 1, 'F', '0', '0', 'mes:pro:task:query', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23021);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23022, '任务新增', 2302, 2, 'F', '0', '0', 'mes:pro:task:add', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23022);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23023, '任务修改', 2302, 3, 'F', '0', '0', 'mes:pro:task:edit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23023);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23024, '任务删除', 2302, 4, 'F', '0', '0', 'mes:pro:task:remove', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23024);

-- 2303 生产报工：查询/新增/修改/删除（导出 23035 已由 V18 创建）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23031, '报工查询', 2303, 1, 'F', '0', '0', 'mes:pro:feedback:query', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23031);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23032, '报工新增', 2303, 2, 'F', '0', '0', 'mes:pro:feedback:add', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23032);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23033, '报工修改', 2303, 3, 'F', '0', '0', 'mes:pro:feedback:edit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23033);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23034, '报工删除', 2303, 4, 'F', '0', '0', 'mes:pro:feedback:remove', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23034);

-- 2304 流转卡：查询/新增/修改/删除（导出 23045 已由 V18 创建）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23041, '流转卡查询', 2304, 1, 'F', '0', '0', 'mes:pro:card:query', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23041);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23042, '流转卡新增', 2304, 2, 'F', '0', '0', 'mes:pro:card:add', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23042);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23043, '流转卡修改', 2304, 3, 'F', '0', '0', 'mes:pro:card:edit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23043);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23044, '流转卡删除', 2304, 4, 'F', '0', '0', 'mes:pro:card:remove', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23044);

-- ════════════════════════════════════════════
-- 3. 角色授权
-- ════════════════════════════════════════════

-- ⚠️ sys_role_menu 主键含 factory_id（NOT NULL，Flyway 裸 JDBC 不走拦截器）→ 必须显式写 factory_id=0（全局）
--    sys_menu 为系统表无 factory_id，上面的 INSERT 不涉及。

-- admin (role_id=1)：全部新菜单
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    2302, 23021, 23022, 23023, 23024, 23025,
    2303, 23031, 23032, 23033, 23034, 23035,
    2304, 23041, 23042, 23043, 23044, 23045
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);

-- 生产角色 (role_id=11)：全部新菜单（与 V18 授予生产角色的模式一致）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    2302, 23021, 23022, 23023, 23024, 23025,
    2303, 23031, 23032, 23033, 23034, 23035,
    2304, 23041, 23042, 23043, 23044, 23045
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 11 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);
