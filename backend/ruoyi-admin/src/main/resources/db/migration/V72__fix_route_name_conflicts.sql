-- ============================================================
-- V72: 修复路由名冲突（route_name 为空 → path fallback 导致同名覆盖）
--
-- 背景：SysMenuServiceImpl.getRouteName() 在 route_name 为空时回退到 path + capitalize。
--       sal/order(path=order) 与 pur/order(path=order) 都生成路由名 "Order"，
--       Vue Router 注册同名路由时后注册的覆盖先注册的，导致 pur/order 404。
--       同理 cal/plan-team(path=team) 与 cal/team(path=team) 都生成 "Team"。
--
-- 修复：为冲突菜单显式设置 route_name，确保全局唯一。
-- 幂等：WHERE route_name IS NULL OR route_name = ''
-- ============================================================

-- 1. 销售订单 route_name → SalOrder（避免与采购订单的 Order 冲突）
UPDATE sys_menu
SET route_name = 'SalOrder'
WHERE menu_id = 2901 AND (route_name IS NULL OR route_name = '');

-- 2. 计划班组关联 route_name → PlanTeam（避免与班组设置的 Team 冲突）
UPDATE sys_menu
SET route_name = 'PlanTeam'
WHERE perms = 'mes:cal:plan-team:list' AND menu_type = 'C' AND (route_name IS NULL OR route_name = '');