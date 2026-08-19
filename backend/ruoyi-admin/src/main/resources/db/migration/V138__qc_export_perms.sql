-- ============================================================
-- V138：质检模块导出按钮权限（7 条 F 类按钮 + 角色 1/11 授权）
--
-- 背景：V137 建了 7 页面菜单但漏了导出按钮；前端三页
--      （检测项/模板/缺陷）已用 v-hasPermi 引用 export 权限。
--      iqc/ipqc/oqc/rqc 四个 export 为后续波次页面前置种下，无害。
--
-- menu_id 取 V137 各页面按钮段空闲位（V137 实际占用 28911-28976，
-- 本文件用 28916/28926/28936/28947/28957/28967/28977，无冲突）。
--
-- 幂等：INSERT ... SELECT ... FROM DUAL WHERE NOT EXISTS
-- 授权：sys_role_menu 主键含 factory_id，Flyway 裸 JDBC 必须显式写
--      factory_id=0（照抄 V137 授权段写法）
-- 字符集：utf8mb4
-- 日期：2026-08-16
-- ============================================================
SET NAMES utf8mb4;

-- ════════ 1. 导出按钮（F） ════════
-- 检测项目（V137 按钮 28911-28915，export 排 6）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28916, '检测项目导出', 28901, 6, 'F', '0', '0', 'mes:qc:index:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28916);

-- 检验模板（V137 按钮 28921-28925，export 排 6）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28926, '检验模板导出', 28902, 6, 'F', '0', '0', 'mes:qc:template:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28926);

-- 缺陷字典（V137 按钮 28931-28935，export 排 6）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28936, '缺陷字典导出', 28903, 6, 'F', '0', '0', 'mes:qc:defect:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28936);

-- 来料检验单（V137 按钮 28941-28946 含判定，export 排 7）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28947, '来料检验单导出', 28904, 7, 'F', '0', '0', 'mes:qc:iqc:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28947);

-- 过程检验单（V137 按钮 28951-28956 含判定，export 排 7）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28957, '过程检验单导出', 28905, 7, 'F', '0', '0', 'mes:qc:ipqc:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28957);

-- 出货检验单（V137 按钮 28961-28966 含判定，export 排 7）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28967, '出货检验单导出', 28906, 7, 'F', '0', '0', 'mes:qc:oqc:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28967);

-- 退料检验单（V137 按钮 28971-28976 含判定，export 排 7）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28977, '退料检验单导出', 28907, 7, 'F', '0', '0', 'mes:qc:rqc:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28977);

-- ════════ 2. 角色授权（角色 1 / 11，factory_id=0，幂等） ════════
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    28916, 28926, 28936, 28947, 28957, 28967, 28977
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    28916, 28926, 28936, 28947, 28957, 28967, 28977
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 11 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);
