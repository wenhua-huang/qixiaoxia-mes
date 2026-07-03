-- 菜单 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头', 2006, '1', 'order', 'mes/pur/order/index', 1, 0, 'C', '0', '0', 'mes:pur:order:list', '#', 'admin', sysdate(), '', null, '采购订单头菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:query',        '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:add',          '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头修改', @parentId, 2006,  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:edit',         '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:remove',       '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:export',       '#', 'admin', sysdate(), '', null, '');