-- ============================================================
-- V146: 销售订单新增「备货订单」类型
--
-- 背景：无订单的备货生产原先走「生产工单新增」，该口子关闭后需要替代路径。
--   方案：销售订单订单类型字典新增 STOCK(备货订单)，备货订单走与普通销售
--   订单完全相同的审核流（PREPARE→PENDING→CONFIRMED），审核通过后照常
--   「生成工单」。审批口子不另开。
--
--   注：V145 为并行分支 A1 的 revoke_workorder_add_perm（撤销工单新增权限），
--   本库已由其先行应用；本迁移顺延为 V146，Flyway ignoreMigrationPatterns
--   会将本地缺失的 V145 视作 missing 放过。
--
--   order_type 列为 varchar(50)，无需改表；客户列均可空，无需改表。
--
-- 含：① 订单类型字典 mes_sal_order_type（NEW 新单 / REPEAT 返单 / STOCK 备货订单）
--    ② 内部客户主数据「工厂备货」(CLI-STOCK-001)，备货订单选它作为客户，
--       不强制真实客户（沿用「现货客户」兜底先例，复用客户选择器与转工单客户关联）
--
-- 幂等：字典按 dict_type+dict_value、客户按 client_code 用 WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-09-06
-- ============================================================

SET NAMES utf8mb4;

-- ① 销售订单类型字典（dict_id 自增，按 dict_type+dict_value 幂等）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '销售订单类型', 'mes_sal_order_type', '0', 'admin', sysdate(), '销售订单类型:新单/返单/备货订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_sal_order_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '新单',     'NEW',   'mes_sal_order_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '常规客户新订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_type' AND dict_value = 'NEW');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '返单',     'REPEAT','mes_sal_order_type', '', 'success', 'N', '0', 'admin', sysdate(), '老客户返单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_type' AND dict_value = 'REPEAT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '备货订单', 'STOCK', 'mes_sal_order_type', '', 'warning', 'N', '0', 'admin', sysdate(), '工厂备货生产,无真实客户,走销售订单审核流后转工单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_type' AND dict_value = 'STOCK');

-- ② 内部客户主体「工厂备货」(client_id 自增；client_code 全局唯一，按其幂等；factory_id 显式写)
INSERT INTO qxx_md_client (factory_id, client_code, client_name, client_nick, client_type, enable_flag, remark, create_by, create_time)
SELECT 1, 'CLI-STOCK-001', '工厂备货', '工厂备货', NULL, '1', '备货生产内部主体(非真实客户),用于备货订单', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_client WHERE client_code = 'CLI-STOCK-001');
