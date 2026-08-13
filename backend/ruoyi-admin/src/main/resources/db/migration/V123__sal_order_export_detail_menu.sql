-- ============================================================
-- V123: 销售订单「明细导出」按钮权限（PDF/Excel 详情导出）
--
-- 背景：销售订单新增"导出详情为 PDF/Excel"功能（对外确认书 / 对内对账），
--      需独立权限点 mes:sal:order:exportDetail，便于按角色控制对外单据导出。
--
-- 含：① sys_menu 插按钮（menu_id=2908，parent=2901 销售订单）
--    ② sys_role_menu 给 admin(role_id=1) 授权（factory_id=0 全局）
--
-- 幂等：INSERT ... SELECT ... WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- ① 销售订单明细导出按钮（F 类型，挂在 2901 销售订单菜单下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2908, '销售订单明细导出', 2901, 7, 'F', '0', '0', 'mes:sal:order:exportDetail', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2908);

-- ② admin 角色授权（sys_role_menu 主键含 factory_id，全局为 0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2908, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2908 AND factory_id = 0);
