-- ============================================================
-- V127: 补授 admin 角色「采购订单详情页」菜单（2921）
--
-- 背景：V126 ④ 用「复制拥有采购订单列表菜单(2600)的角色」给详情页 2921 授权，
--      但本项目 admin 在 sys_role_menu 中没有 2600 的显式行（一直靠 user_id=1
--      旁路拿到全部菜单），导致 2921 没有任何角色授权记录（对照销售 2917 有
--      admin 显式授权，因 2901 有 admin 行）。
--
--      admin 用户虽能旁路看到路由，但角色管理界面不会显示该授权，且非 admin
--      角色若后续被授予 2600 也应同步拿到 2921。本迁移补齐：
--        ① admin(role_id=1, factory_id=0) 显式授权 2921（与 2917/2920 对齐）
--        ② 兜底：给所有拥有 2600 列表菜单的角色授予 2921（防 V126 之后新增授权）
--
-- 幂等：INSERT ... SELECT ... WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- 动态取采购订单列表菜单ID（自增，不同环境不同）
SELECT @purOrderMenuId := menu_id FROM sys_menu
WHERE perms = 'mes:pur:order:list' AND menu_type = 'C' LIMIT 1;

-- ① admin 显式授权详情页 2921（factory_id=0 全局）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2921, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2921 AND factory_id = 0);

-- ② 兜底：给所有拥有列表菜单的角色授予详情页，沿用其 factory_id
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT rm.role_id, 2921, rm.factory_id
FROM sys_role_menu rm
WHERE rm.menu_id = @purOrderMenuId
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 2921 AND rm2.factory_id = rm.factory_id
  );
