-- ============================================================
-- 企小侠文化纸盒MES系统 — mes-sys 菜单及权限数据
-- 用途：注册 mes-sys 模块的菜单和按钮权限，授权给超级管理员
-- 注意：会先创建父菜单 2009 '系统管理'(如果不存在)
-- 重要：导入前确保客户端字符集为 utf8mb4（mysql --default-character-set=utf8mb4）
-- ============================================================

SET NAMES utf8mb4;

-- 0. 创建 '系统管理' 目录菜单 (M 类型，放在 MES系统(2000) 下)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2009, '系统管理', 2000, 9, 'sys', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'system', 'admin', NOW());

-- 1. 编码生成规则
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2910, '编码规则',  2009, 1, 'autocoderule',  'mes/sys/autocoderule/index',  NULL, '', 1, 0, 'C', '0', '0', 'mes:sys:autocoderule:list',  'code', 'admin', NOW());
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2911, '编码规则查询', 2910, 1, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderule:query',  '#', 'admin', NOW()),
(2912, '编码规则新增', 2910, 2, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderule:add',    '#', 'admin', NOW()),
(2913, '编码规则修改', 2910, 3, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderule:edit',   '#', 'admin', NOW()),
(2914, '编码规则删除', 2910, 4, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderule:remove', '#', 'admin', NOW()),
(2915, '编码规则导出', 2910, 5, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderule:export', '#', 'admin', NOW());

-- 2. 编码生成记录(只读) — 分段管理内嵌在规则页，不设独立菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2930, '编码记录',  2009, 3, 'autocoderesult', 'mes/sys/autocoderesult/index', NULL, '', 1, 0, 'C', '0', '0', 'mes:sys:autocoderesult:list',  'log', 'admin', NOW());
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2931, '编码记录查询', 2930, 1, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderesult:query',  '#', 'admin', NOW()),
(2932, '编码记录删除', 2930, 2, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderesult:remove', '#', 'admin', NOW()),
(2933, '编码记录导出', 2930, 3, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:autocoderesult:export', '#', 'admin', NOW());

-- 3. 附件管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2940, '附件管理',  2009, 4, 'attachment',     'mes/sys/attachment/index',     NULL, '', 1, 0, 'C', '0', '0', 'mes:sys:attachment:list',  'upload', 'admin', NOW());
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2941, '附件查询',   2940, 1, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:attachment:query',  '#', 'admin', NOW()),
(2942, '附件新增',   2940, 2, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:attachment:add',    '#', 'admin', NOW()),
(2943, '附件修改',   2940, 3, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:attachment:edit',   '#', 'admin', NOW()),
(2944, '附件删除',   2940, 4, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:attachment:remove', '#', 'admin', NOW()),
(2945, '附件导出',   2940, 5, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:attachment:export', '#', 'admin', NOW());

-- 4. 系统消息
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2950, '系统消息',  2009, 5, 'message',        'mes/sys/message/index',        NULL, '', 1, 0, 'C', '0', '0', 'mes:sys:message:list',  'message', 'admin', NOW());
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2951, '消息查询',   2950, 1, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:message:query',  '#', 'admin', NOW()),
(2952, '消息新增',   2950, 2, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:message:add',    '#', 'admin', NOW()),
(2953, '消息删除',   2950, 3, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:message:remove', '#', 'admin', NOW()),
(2954, '消息导出',   2950, 4, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:message:export', '#', 'admin', NOW());

-- 5. 待办事项
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2960, '待办事项',  2009, 6, 'todolist',       'mes/sys/todolist/index',       NULL, '', 1, 0, 'C', '0', '0', 'mes:sys:todolist:list',  'list', 'admin', NOW());
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2961, '待办查询',   2960, 1, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:todolist:query',  '#', 'admin', NOW()),
(2962, '待办新增',   2960, 2, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:todolist:add',    '#', 'admin', NOW()),
(2963, '待办修改',   2960, 3, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:todolist:edit',   '#', 'admin', NOW()),
(2964, '待办删除',   2960, 4, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:todolist:remove', '#', 'admin', NOW()),
(2965, '待办导出',   2960, 5, '',  '',  NULL, '', 1, 0, 'F', '0', '0', 'mes:sys:todolist:export', '#', 'admin', NOW());

-- 6. 授权给超级管理员(role_id=1, factory_id=1)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id) VALUES (1, 2009, 1);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id) SELECT 1, menu_id, 1 FROM sys_menu WHERE menu_id IN (2910,2911,2912,2913,2914,2915, 2930,2931,2932,2933, 2940,2941,2942,2943,2944,2945, 2950,2951,2952,2953,2954, 2960,2961,2962,2963,2964,2965);
