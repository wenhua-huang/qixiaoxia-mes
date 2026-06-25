-- ============================================================
-- mes-pro Phase 4+5 菜单种子数据 + WM生产领料退料菜单
-- 用途：为新创建的生产管理页面注册路由和按钮权限
-- 执行方式：mysql -h localhost -u root -p mes < seed_pro_phase4_menu.sql
-- 日期：2026-06-20
-- ============================================================

-- ============================================================
-- 一、修复已有菜单的组件路径
-- ============================================================

-- 1.1 工艺路线(2301)：组件路径 route → proroute（匹配实际文件路径）
UPDATE sys_menu SET component = 'mes/pro/proroute/index' WHERE menu_id = 2301 AND component = 'mes/pro/route/index';

-- 1.2 流转卡(2304)：组件路径 card → card（已重命名目录，保持一致）
-- 无需修改，init_shengxiang.sql 已使用 card

-- ============================================================
-- 二、新增 PRO 模块菜单（2003 生产管理下）
-- ============================================================

-- 2.1 生产看板 (menu_id=2306, parent=2003 生产管理)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2306, '生产看板', 2003, 7, 'dashboard', 'mes/pro/dashboard/index', 1, 0, 'C', '0', '0', 'mes:pro:dashboard:query', 'dashboard', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23061, '看板查询', 2306, 1, 'F', '0', '0', 'mes:pro:dashboard:query', 'admin', sysdate());

-- 2.2 物料追溯 (menu_id=2307, parent=2003)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2307, '物料追溯', 2003, 8, 'materialtrace', 'mes/pro/materialtrace/index', 1, 0, 'C', '0', '0', 'mes:pro:materialtrace:query', 'chart', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23071, '追溯查询', 2307, 1, 'F', '0', '0', 'mes:pro:materialtrace:query', 'admin', sysdate());

-- 2.3 上下工记录 (menu_id=2308, parent=2003)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2308, '上下工记录', 2003, 9, 'workrecord', 'mes/pro/workrecord/index', 1, 0, 'C', '0', '0', 'mes:pro:workrecord:list', 'clock', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23081, '记录查询', 2308, 1, 'F', '0', '0', 'mes:pro:workrecord:query', 'admin', sysdate()),
(23082, '记录新增', 2308, 2, 'F', '0', '0', 'mes:pro:workrecord:add',   'admin', sysdate()),
(23083, '记录修改', 2308, 3, 'F', '0', '0', 'mes:pro:workrecord:edit',  'admin', sysdate()),
(23084, '记录删除', 2308, 4, 'F', '0', '0', 'mes:pro:workrecord:remove','admin', sysdate());

-- 2.4 用户工作站绑定 (menu_id=2309, parent=2003)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2309, '用户工作站', 2003, 10, 'userworkstation', 'mes/pro/userworkstation/index', 1, 0, 'C', '0', '0', 'mes:pro:userworkstation:list', 'people', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23091, '绑定查询', 2309, 1, 'F', '0', '0', 'mes:pro:userworkstation:query', 'admin', sysdate()),
(23092, '绑定新增', 2309, 2, 'F', '0', '0', 'mes:pro:userworkstation:add',   'admin', sysdate()),
(23093, '绑定修改', 2309, 3, 'F', '0', '0', 'mes:pro:userworkstation:edit',  'admin', sysdate()),
(23094, '绑定删除', 2309, 4, 'F', '0', '0', 'mes:pro:userworkstation:remove','admin', sysdate());

-- ============================================================
-- 三、新增工序管理菜单（Phase 1 已完成但菜单缺失）
-- ============================================================

-- 3.1 工序管理 (menu_id=2310, parent=2003)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2310, '工序管理', 2003, 0, 'process', 'mes/pro/process/index', 1, 0, 'C', '0', '0', 'mes:pro:process:list', 'list', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23101, '工序查询', 2310, 1, 'F', '0', '0', 'mes:pro:process:query',  'admin', sysdate()),
(23102, '工序新增', 2310, 2, 'F', '0', '0', 'mes:pro:process:add',    'admin', sysdate()),
(23103, '工序修改', 2310, 3, 'F', '0', '0', 'mes:pro:process:edit',   'admin', sysdate()),
(23104, '工序删除', 2310, 4, 'F', '0', '0', 'mes:pro:process:remove', 'admin', sysdate()),
(23105, '工序导出', 2310, 5, 'F', '0', '0', 'mes:pro:process:export', 'admin', sysdate());

-- ============================================================
-- 四、新增 WM 生产退料菜单（2002 仓储管理下）
-- ============================================================

-- 4.1 生产退料 (menu_id=2207, parent=2002)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2207, '生产退料', 2002, 8, 'rtissue', 'mes/wm/rtissue/index', 1, 0, 'C', '0', '0', 'mes:wm:rtissue:list', '#', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(22071, '退料查询', 2207, 1, 'F', '0', '0', 'mes:wm:rtissue:query',  'admin', sysdate()),
(22072, '退料新增', 2207, 2, 'F', '0', '0', 'mes:wm:rtissue:add',    'admin', sysdate()),
(22073, '退料修改', 2207, 3, 'F', '0', '0', 'mes:wm:rtissue:edit',   'admin', sysdate()),
(22074, '退料删除', 2207, 4, 'F', '0', '0', 'mes:wm:rtissue:remove', 'admin', sysdate());

-- ============================================================
-- 五、补充缺失的 F-menu 按钮权限
--    - 排产任务(2302) F-menu 已在 init_shengxiang.sql 中存在 → 无需添加
--    - 生产报工(2303) F-menu 已存在 → 无需添加
--    - 流转卡(2304) F-menu 已存在 → 无需添加
--    - 生产领料(2203) F-menu 已存在（权限已统一为 mes:wm:issue:*）→ 无需添加
-- ============================================================

-- 5.1 排产任务：补充导出按钮（init_shengxiang.sql 缺少 export）
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23025, '排产导出', 2302, 5, 'F', '0', '0', 'mes:pro:task:export', 'admin', sysdate());

-- 5.2 生产报工：补充导出按钮
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23035, '报工导出', 2303, 5, 'F', '0', '0', 'mes:pro:feedback:export', 'admin', sysdate());

-- 5.3 流转卡：补充导出按钮
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23045, '流转卡导出', 2304, 5, 'F', '0', '0', 'mes:pro:card:export', 'admin', sysdate());

-- ============================================================
-- 六、授予 admin 角色（role_id=1）所有新菜单的权限
-- ============================================================

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (
    2306, 23061, 2307, 23071,
    2308, 23081, 23082, 23083, 23084,
    2309, 23091, 23092, 23093, 23094,
    2310, 23101, 23102, 23103, 23104, 23105,
    2207, 22071, 22072, 22073, 22074,
    23025, 23035, 23045
);

-- 六、授予生产角色（role_id=11, sx_production）新菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 11, menu_id FROM sys_menu WHERE menu_id IN (2306, 23061, 2307, 23071, 2308, 23081, 2310, 23101);

-- 授予仓库角色（role_id=10, sx_warehouse）退料菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 10, menu_id FROM sys_menu WHERE menu_id IN (2207, 22071, 22072, 22073, 22074);
