-- ============================================================
-- V34: 重新插入 MES 菜单到正确的 parent_id
-- V33 清理了错误位置的菜单，现在用正确的 parent_id 重建
-- 使用 INSERT IGNORE 确保幂等
-- ============================================================

-- ==================== 采购管理 (parent_id=2006) ====================
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('采购订单头', 2006, 1, 'order', 'mes/pur/order/index', 1, 0, 'C', '0', '0', 'mes:pur:order:list', '#', 'admin', sysdate(), '', NULL, '采购订单头菜单');
SET @pur_order_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2006 AND perms = 'mes:pur:order:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('采购订单头查询', @pur_order_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('采购订单头新增', @pur_order_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('采购订单头修改', @pur_order_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('采购订单头删除', @pur_order_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('采购订单行', 2006, 2, 'line', 'mes/pur/order-line/index', 1, 0, 'C', '0', '0', 'mes:pur:order-line:list', '#', 'admin', sysdate(), '', NULL, '采购订单行菜单');
SET @pur_line_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2006 AND perms = 'mes:pur:order-line:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('采购订单行查询', @pur_line_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('采购订单行新增', @pur_line_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('采购订单行修改', @pur_line_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('采购订单行删除', @pur_line_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== 日历管理 (parent_id=2007) ====================
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('排班日历', 2007, 1, 'calendar', 'mes/cal/calendar/index', 1, 0, 'C', '0', '0', 'mes:cal:calendar:list', '#', 'admin', sysdate(), '', NULL, '排班日历菜单');
SET @cal_calendar_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:calendar:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('排班日历查询', @cal_calendar_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:calendar:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('排班日历新增', @cal_calendar_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:calendar:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('排班日历修改', @cal_calendar_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:calendar:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('排班日历删除', @cal_calendar_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:calendar:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('节假日设置', 2007, 2, 'holiday', 'mes/cal/holiday/index', 1, 0, 'C', '0', '0', 'mes:cal:holiday:list', '#', 'admin', sysdate(), '', NULL, '节假日设置菜单');
SET @cal_holiday_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:holiday:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('节假日查询', @cal_holiday_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('节假日新增', @cal_holiday_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('节假日修改', @cal_holiday_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('节假日删除', @cal_holiday_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('排班计划', 2007, 3, 'plan', 'mes/cal/plan/index', 1, 0, 'C', '0', '0', 'mes:cal:plan:list', '#', 'admin', sysdate(), '', NULL, '排班计划菜单');
SET @cal_plan_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:plan:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('排班计划查询', @cal_plan_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('排班计划新增', @cal_plan_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('排班计划修改', @cal_plan_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('排班计划删除', @cal_plan_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('计划班组关联', 2007, 4, 'team', 'mes/cal/plan-team/index', 1, 0, 'C', '0', '0', 'mes:cal:plan-team:list', '#', 'admin', sysdate(), '', NULL, '计划班组关联菜单');
SET @cal_pt_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:plan-team:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('计划班组查询', @cal_pt_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('计划班组新增', @cal_pt_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('计划班组修改', @cal_pt_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('计划班组删除', @cal_pt_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('计划班次', 2007, 5, 'shift', 'mes/cal/shift/index', 1, 0, 'C', '0', '0', 'mes:cal:shift:list', '#', 'admin', sysdate(), '', NULL, '计划班次菜单');
SET @cal_shift_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:shift:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('计划班次查询', @cal_shift_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('计划班次新增', @cal_shift_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('计划班次修改', @cal_shift_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('计划班次删除', @cal_shift_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('班组设置', 2007, 6, 'team', 'mes/cal/team/index', 1, 0, 'C', '0', '0', 'mes:cal:team:list', '#', 'admin', sysdate(), '', NULL, '班组菜单');
SET @cal_team_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:team:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('班组查询', @cal_team_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('班组新增', @cal_team_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('班组修改', @cal_team_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('班组删除', @cal_team_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('班组成员', 2007, 7, 'member', 'mes/cal/team-member/index', 1, 0, 'C', '0', '0', 'mes:cal:team-member:list', '#', 'admin', sysdate(), '', NULL, '班组成员菜单');
SET @cal_member_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2007 AND perms = 'mes:cal:team-member:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('班组成员查询', @cal_member_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('班组成员新增', @cal_member_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('班组成员修改', @cal_member_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('班组成员删除', @cal_member_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== 工装管理 (parent_id=2008) ====================
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('工装夹具类型', 2008, 1, 'tooltype', 'mes/tm/tooltype/index', 1, 0, 'C', '0', '0', 'mes:tm:tooltype:list', '#', 'admin', sysdate(), '', NULL, '工装夹具类型菜单');
SET @tm_type_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2008 AND perms = 'mes:tm:tooltype:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('工装夹具类型查询', @tm_type_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tooltype:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('工装夹具类型新增', @tm_type_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tooltype:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('工装夹具类型修改', @tm_type_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tooltype:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('工装夹具类型删除', @tm_type_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tooltype:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('工装夹具清单', 2008, 2, 'tool', 'mes/tm/tool/index', 1, 0, 'C', '0', '0', 'mes:tm:tool:list', '#', 'admin', sysdate(), '', NULL, '工装夹具清单菜单');
SET @tm_tool_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2008 AND perms = 'mes:tm:tool:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('工装夹具清单查询', @tm_tool_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('工装夹具清单新增', @tm_tool_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('工装夹具清单修改', @tm_tool_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('工装夹具清单删除', @tm_tool_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool:remove', '#', 'admin', sysdate(), '', NULL, '');
