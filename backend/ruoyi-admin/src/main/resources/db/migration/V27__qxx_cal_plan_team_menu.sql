-- 菜单 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班组关联', 2007, '1', 'team', 'mes/cal/plan-team/index', 1, 0, 'C', '0', '0', 'mes:cal:plan-team:list', '#', 'admin', sysdate(), '', null, '计划班组关联菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班组关联查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:query',        '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班组关联新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:add',          '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班组关联修改', @parentId, 2007,  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:edit',         '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班组关联删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:remove',       '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班组关联导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:export',       '#', 'admin', sysdate(), '', null, '');