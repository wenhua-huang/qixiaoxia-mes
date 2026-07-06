-- ============================================================
-- V41: 清理日历管理(cal)模块菜单 — 删除3个错误C型菜单，合并F型权限
--
-- 问题背景：
--   V24~V30 创建了 7 个 C 型菜单，其中 V27(计划班组关联)、V28(计划班次)、
--   V30(班组成员) 指向不存在的独立页面组件（mes/cal/plan-team/index 等）。
--   实际上 shift.vue/team.vue 是 plan/index.vue 的内嵌 tab 子组件，
--   member.vue 是 team/index.vue 的内嵌子组件，不应在侧边栏显示独立菜单。
--   同时 INSERT IGNORE + LAST_INSERT_ID() 导致大量重复孤儿 F 型按钮。
--
-- 修复策略：
--   清空所有 cal 相关菜单数据，按 ktg-mes-ui 原始结构重建：
--   - 仅 4 个 C 型菜单（侧边栏可见）
--   - shift/plan-team/team-member 的 F 型权限挂到对应父菜单下
--     （API 鉴权需要，但不在侧边栏显示独立菜单）
-- ============================================================

-- 1. 删除所有 cal 相关菜单（无 FK 约束，安全删除）
DELETE FROM sys_menu WHERE perms LIKE 'mes:cal:%';

-- ============================================================
-- 2. 重建 4 个 C 型菜单及完整 F 型按钮权限
-- ============================================================

-- 2.1 班组设置 (team) — 含班组成员权限（member.vue 内嵌调用）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('班组设置', 2007, 1, 'team', 'mes/cal/team/index', 1, 0, 'C', '0', '0', 'mes:cal:team:list', '#', 'admin', NOW(), '', NULL, '');
SET @menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('班组查询',       @menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:query',        '#', 'admin', NOW(), '', NULL, ''),
('班组新增',       @menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:add',          '#', 'admin', NOW(), '', NULL, ''),
('班组修改',       @menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:edit',         '#', 'admin', NOW(), '', NULL, ''),
('班组删除',       @menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:remove',       '#', 'admin', NOW(), '', NULL, ''),
('班组导出',       @menu_id, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:export',       '#', 'admin', NOW(), '', NULL, ''),
-- 班组成员权限（member.vue 调用 team-member API）
('班组成员查询',   @menu_id, 6, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:query',  '#', 'admin', NOW(), '', NULL, ''),
('班组成员新增',   @menu_id, 7, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:add',    '#', 'admin', NOW(), '', NULL, ''),
('班组成员修改',   @menu_id, 8, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:edit',   '#', 'admin', NOW(), '', NULL, ''),
('班组成员删除',   @menu_id, 9, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:remove', '#', 'admin', NOW(), '', NULL, ''),
('班组成员导出',   @menu_id,10, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team-member:export', '#', 'admin', NOW(), '', NULL, '');

-- 2.2 排班计划 (plan) — 含班次/计划班组权限（shift.vue/team.vue 内嵌调用）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('排班计划', 2007, 2, 'plan', 'mes/cal/plan/index', 1, 0, 'C', '0', '0', 'mes:cal:plan:list', '#', 'admin', NOW(), '', NULL, '');
SET @menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('排班计划查询',   @menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:query',        '#', 'admin', NOW(), '', NULL, ''),
('排班计划新增',   @menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:add',          '#', 'admin', NOW(), '', NULL, ''),
('排班计划修改',   @menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:edit',         '#', 'admin', NOW(), '', NULL, ''),
('排班计划删除',   @menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:remove',       '#', 'admin', NOW(), '', NULL, ''),
('排班计划导出',   @menu_id, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:export',       '#', 'admin', NOW(), '', NULL, ''),
-- 班次权限（shift.vue 调用 shift API）
('班次查询',       @menu_id, 6, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:query',       '#', 'admin', NOW(), '', NULL, ''),
('班次新增',       @menu_id, 7, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:add',         '#', 'admin', NOW(), '', NULL, ''),
('班次修改',       @menu_id, 8, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:edit',        '#', 'admin', NOW(), '', NULL, ''),
('班次删除',       @menu_id, 9, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:remove',      '#', 'admin', NOW(), '', NULL, ''),
('班次导出',       @menu_id,10, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:export',      '#', 'admin', NOW(), '', NULL, ''),
-- 计划班组权限（plan/team.vue 调用 plan-team API）
('计划班组查询',   @menu_id,11, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:query',   '#', 'admin', NOW(), '', NULL, ''),
('计划班组新增',   @menu_id,12, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:add',     '#', 'admin', NOW(), '', NULL, ''),
('计划班组修改',   @menu_id,13, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:edit',    '#', 'admin', NOW(), '', NULL, ''),
('计划班组删除',   @menu_id,14, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:remove',  '#', 'admin', NOW(), '', NULL, ''),
('计划班组导出',   @menu_id,15, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan-team:export',  '#', 'admin', NOW(), '', NULL, '');

-- 2.3 节假日设置 (holiday)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('节假日设置', 2007, 3, 'holiday', 'mes/cal/holiday/index', 1, 0, 'C', '0', '0', 'mes:cal:holiday:list', '#', 'admin', NOW(), '', NULL, '');
SET @menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('节假日查询',     @menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:query',  '#', 'admin', NOW(), '', NULL, ''),
('节假日新增',     @menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:add',    '#', 'admin', NOW(), '', NULL, ''),
('节假日修改',     @menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:edit',   '#', 'admin', NOW(), '', NULL, ''),
('节假日删除',     @menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:remove', '#', 'admin', NOW(), '', NULL, ''),
('节假日导出',     @menu_id, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:export', '#', 'admin', NOW(), '', NULL, '');

-- 2.4 排班日历 (calendar) — 只读视图，仅查询权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('排班日历', 2007, 4, 'calendar', 'mes/cal/calendar/index', 1, 0, 'C', '0', '0', 'mes:cal:calendar:list', '#', 'admin', NOW(), '', NULL, '');
SET @menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
('排班日历查询',   @menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:cal:calendar:query', '#', 'admin', NOW(), '', NULL, '');
