-- ============================================================
-- V124: 销售订单审核流程（状态机 PREPARE→PENDING→CONFIRMED，驳回回退）
--
-- 背景：销售订单为生产源头，原"确认即生效"缺审核把关。新增审核环节：
--   手工建单 PREPARE(待提交) --提交--> PENDING(待审核) --审核通过--> CONFIRMED(已确认)
--   驳回(必填意见) 回退 PREPARE；CRM 推单直接进 PENDING。
--
-- 含：① qxx_sal_order 加 approve_by/approve_time/approve_remark
--    ② 字典 mes_sal_order_status（5 态，供前端 dict-tag）
--    ③ 按钮权限 mes:sal:order:submit / mes:sal:order:approve
--
-- 幂等：DDL 靠 Flyway 单次执行；字典/菜单用 INSERT IGNORE / WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- ① 销售订单审核字段（紧跟 status，审核语义聚合）
ALTER TABLE qxx_sal_order
  ADD COLUMN approve_by varchar(64) DEFAULT NULL COMMENT '审核人' AFTER status,
  ADD COLUMN approve_time datetime DEFAULT NULL COMMENT '审核时间' AFTER approve_by,
  ADD COLUMN approve_remark varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因' AFTER approve_time;

-- ② 销售订单状态字典（dict_id/dict_code 自增，按 dict_type+dict_value 幂等）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '销售订单状态', 'mes_sal_order_status', '0', 'admin', sysdate(), '销售订单审核状态机'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_sal_order_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待提交', 'PREPARE',  'mes_sal_order_status', '', 'info',    'Y', '0', 'admin', sysdate(), '草稿，可编辑/删除/提交审核'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PREPARE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '待审核', 'PENDING',  'mes_sal_order_status', '', 'warning', 'N', '0', 'admin', sysdate(), '已提交，等待审核'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PENDING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已确认', 'CONFIRMED','mes_sal_order_status', '', 'success', 'N', '0', 'admin', sysdate(), '审核通过，可转工单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CONFIRMED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已关闭', 'CLOSED',   'mes_sal_order_status', '', 'primary', 'N', '0', 'admin', sysdate(), '终态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CLOSED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已取消', 'CANCEL',   'mes_sal_order_status', '', 'danger',  'N', '0', 'admin', sysdate(), '终态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CANCEL');

-- ③ 审核按钮权限（F 类型，挂在 2901 销售订单菜单下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2909, '销售订单提交审核', 2901, 8, 'F', '0', '0', 'mes:sal:order:submit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2909);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2916, '销售订单审核', 2901, 9, 'F', '0', '0', 'mes:sal:order:approve', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2916);

-- admin 角色授权（sys_role_menu 主键含 factory_id，全局为 0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu WHERE menu_id IN (2909, 2916)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0);
