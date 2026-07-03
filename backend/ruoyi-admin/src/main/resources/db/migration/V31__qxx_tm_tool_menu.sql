-- 菜单 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具清单', 2008, '2', 'tool', 'mes/tm/tool/index', 1, 0, 'C', '0', '0', 'mes:tm:tool:list', '#', 'admin', sysdate(), '', null, '工装夹具清单菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具清单查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:query',        '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具清单新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:add',          '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具清单修改', @parentId, 2008,  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:edit',         '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具清单删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:remove',       '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具清单导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:export',       '#', 'admin', sysdate(), '', null, '');
