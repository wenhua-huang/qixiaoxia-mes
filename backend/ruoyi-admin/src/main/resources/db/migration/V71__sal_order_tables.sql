-- ============================================================
-- V71: 销售订单模块(qxx_sal_order / qxx_sal_order_line) + 工单关联列 + 菜单
--
-- 背景:数据库设计决策.md 标注 qxx_sal_order「⚠️待补:客户需求先落销售订单,再转工单」。
--      工单表已有 order_source/source_code 预留销售订单关联(头级),本迁移只补
--      行级追溯列 sales_order_line_id(转工单来源),不加冗余 sales_order_id。
--      已转量/可转量查询时按 sales_order_line_id 聚合工单计算,不建计数列。
--
-- 含:① 销售订单头表 ② 销售订单明细行表 ③ 工单加 sales_order_line_id 列
--    ④ 销售管理目录 + 销售订单菜单 + 6 个按钮权限 ⑤ admin 角色授权
--
-- 幂等:CREATE IF NOT EXISTS / 列存在跳过(存储过程) / 菜单 WHERE NOT EXISTS / role_menu WHERE NOT EXISTS
-- 字符集:utf8mb4,每列带 comment(沿用 mes-pro.sql 风格)
-- 日期:2026-07-15
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 销售订单头表 qxx_sal_order
--    用途:承接客户订单(客户->圣享),记产品/数量/交期/商务条款,转工单的上游
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_sal_order (
  order_id          bigint(20)    not null auto_increment    comment '销售订单ID',
  factory_id        bigint(20)    not null                   comment '工厂ID(关联qxx_md_factory)',
  order_code        varchar(64)   not null                   comment '销售订单号(SO+yyyyMMdd+流水,genSerialCode ORDER_NO)',
  order_name        varchar(255)  default null               comment '订单名称',
  order_type        varchar(50)   default 'NEW'              comment '订单类型:NEW-新单,REPEAT-返单',
  client_id         bigint(20)    default null               comment '客户ID(关联qxx_md_client)',
  client_code       varchar(64)   default null               comment '客户编码',
  client_name       varchar(255)  default null               comment '客户名称',
  client_nick       varchar(255)  default null               comment '客户简称',
  client_order_code varchar(64)   default null               comment '客户订单号(PO号)',
  salesperson       varchar(64)   default null               comment '业务员',
  business_line     varchar(20)   default null               comment '业务线:DOMESTIC-内贸,FOREIGN-外贸,SPOT-现货',
  sample_flag       char(1)       default 'N'                comment '是否有样品(Y-是,N-否)',
  order_date        datetime      default null               comment '订单日期',
  request_date      datetime      default null               comment '需求交期(客户要求交货日)',
  total_amount      decimal(14,2) default null               comment '订单总金额',
  payment_method    varchar(64)   default null               comment '付款方式',
  status            varchar(64)   default 'PREPARE'          comment '状态:PREPARE-待确认,CONFIRMED-已确认,CLOSED-已关闭,CANCEL-已取消',
  remark            varchar(500)  default ''                 comment '备注',
  create_by         varchar(64)   default ''                 comment '创建者',
  create_time       datetime      default current_timestamp  comment '创建时间',
  update_by         varchar(64)   default ''                 comment '更新者',
  update_time       datetime      default current_timestamp on update current_timestamp comment '更新时间',
  primary key (order_id),
  key idx_factory_id (factory_id),
  unique key uk_order_code (order_code)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售订单表';

-- ============================================================
-- 2. 销售订单明细行表 qxx_sal_order_line
--    用途:一单多产品,每行=一个产品(转工单到行级)。规格字段对齐工单便于1:1回填。
--    不建 quantity_produced 计数列:已转量查询时按 sales_order_line_id 聚合 qxx_pro_workorder 计算。
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_sal_order_line (
  line_id         bigint(20)    not null auto_increment    comment '明细行ID',
  factory_id      bigint(20)    not null                   comment '工厂ID(关联qxx_md_factory)',
  order_id        bigint(20)    not null                   comment '销售订单ID(关联qxx_sal_order)',
  line_no         int(11)       default null               comment '行号',
  product_id      bigint(20)    default null               comment '产品ID(关联qxx_md_item,引用既有SPU/SKU)',
  product_code    varchar(64)   default null               comment '产品编码',
  product_name    varchar(255)  default null               comment '产品名称',
  product_spc     varchar(255)  default null               comment '产品规格型号',
  unit_of_measure varchar(64)   default null               comment '主单位编码',
  unit_name       varchar(64)   default null               comment '主单位名称',
  quantity        decimal(14,2) not null default 0.00      comment '订单数量',
  unit_price      decimal(14,4) default null               comment '单价',
  line_amount     decimal(14,2) default null               comment '行金额(单价*数量)',
  spacing         varchar(50)   default null               comment '间距(纸袋专用,如7.5cm)',
  product_size    varchar(100)  default null               comment '产品尺寸(长*宽*高mm)',
  printing_req    varchar(500)  default null               comment '印刷要求',
  rope_spec       varchar(200)  default null               comment '绳料规格(纸袋专用,如红色圆纸绳)',
  package_req     varchar(500)  default null               comment '包装要求',
  shipping_req    varchar(500)  default null               comment '发货要求',
  request_date    datetime      default null               comment '行级需求交期',
  remark          varchar(500)  default ''                 comment '备注',
  create_by       varchar(64)   default ''                 comment '创建者',
  create_time     datetime      default current_timestamp  comment '创建时间',
  update_by       varchar(64)   default ''                 comment '更新者',
  update_time     datetime      default current_timestamp on update current_timestamp comment '更新时间',
  primary key (line_id),
  key idx_factory_id (factory_id),
  key idx_order_id (order_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售订单明细行表';

-- ============================================================
-- 3. 工单加行级追溯列 sales_order_line_id
--    头级关联复用工单已有 source_code(=销售订单号)+order_source='SALES_ORDER',不加冗余 sales_order_id。
--    幂等:列存在则跳过(V55 模式)
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_workorder_sales_order_line_id;
DELIMITER $$
CREATE PROCEDURE proc_add_workorder_sales_order_line_id()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pro_workorder'
                   AND COLUMN_NAME='sales_order_line_id') THEN
        ALTER TABLE qxx_pro_workorder ADD COLUMN sales_order_line_id bigint(20) DEFAULT NULL COMMENT '销售订单行ID(关联qxx_sal_order_line,转工单来源)' AFTER source_code;
        ALTER TABLE qxx_pro_workorder ADD INDEX idx_sales_order_line_id (sales_order_line_id);
    END IF;
END$$
DELIMITER ;
CALL proc_add_workorder_sales_order_line_id();
DROP PROCEDURE IF EXISTS proc_add_workorder_sales_order_line_id;

-- ============================================================
-- 4. 菜单:销售管理目录(M,挂 2000 MES管理下) + 销售订单菜单(C) + 6 个按钮权限(F)
--    sys_menu 为全局表(无 factory_id)。幂等:WHERE NOT EXISTS by menu_id。
--    menu_id 取 2900-2907(确认空闲,2910 为编码规则)。
-- ============================================================
-- 4.1 销售管理目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2900, '销售管理', 2000, 9, 'sal', NULL, 1, 0, 'M', '0', '0', NULL, 'money', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2900);

-- 4.2 销售订单菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2901, '销售订单', 2900, 1, 'order', 'mes/sal/order/index', 1, 0, 'C', '0', '0', 'mes:sal:order:list', '#', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2901);

-- 4.3 销售订单按钮权限(查询/新增/修改/删除/导出/转工单)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2902, '销售订单查询', 2901, 1, 'F', '0', '0', 'mes:sal:order:query', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2902);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2903, '销售订单新增', 2901, 2, 'F', '0', '0', 'mes:sal:order:add', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2903);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2904, '销售订单修改', 2901, 3, 'F', '0', '0', 'mes:sal:order:edit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2904);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2905, '销售订单删除', 2901, 4, 'F', '0', '0', 'mes:sal:order:remove', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2905);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2906, '销售订单导出', 2901, 5, 'F', '0', '0', 'mes:sal:order:export', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2906);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2907, '销售转工单', 2901, 6, 'F', '0', '0', 'mes:sal:order:workorder', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2907);

-- ============================================================
-- 5. admin(role_id=1) 授权销售订单全部菜单
--    ⚠️ sys_role_menu 含 factory_id(NOT NULL),Flyway 裸 JDBC 必须显式写 factory_id=0(全局)
--    幂等:WHERE NOT EXISTS by (role_id, menu_id, factory_id)
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2900, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2900 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2901, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2901 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2902, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2902 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2903, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2903 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2904, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2904 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2905, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2905 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2906, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2906 AND factory_id = 0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2907, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2907 AND factory_id = 0);
