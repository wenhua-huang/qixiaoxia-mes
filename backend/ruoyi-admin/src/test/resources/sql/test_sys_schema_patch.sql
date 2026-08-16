-- 集成测试 schema 补丁：种子 SQL 与 dev 库现状的少量列差异补齐。
-- 1) ry_20260417.sql 是旧基线种子，缺 V70/V98 给 sys_user 加的列
--    （Flyway baseline=136 跳过 V61-V136，qxx 表由 manual_tables.sql 提供现状，避免 V73 等 ALTER 重放冲突）
-- 2) manual_tables.sql 缺 V79 的 qxx_wm_item_recpt_line.pur_order_line_id、
--    V98 的 qxx_pro_material_trace.vendor_id（与迁移后 dev 库逐列 diff 核实）
-- 3) V71 的销售订单表 qxx_sal_order/qxx_sal_order_line 不在 manual_tables.sql
--    （存量 SalOrderIT 依赖），工单行级追溯列 sales_order_line_id 同缺
-- 幂等：ALTER 前查 information_schema（与 V98 同一写法）。

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'openid');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN openid varchar(100) DEFAULT NULL COMMENT ''微信openid(小程序登录绑定)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'wage_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN wage_type varchar(20) DEFAULT NULL COMMENT ''工资类型:MONTHLY-月工资,PIECE-计件,HOURLY-计时''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'employee_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN employee_type varchar(20) DEFAULT NULL COMMENT ''员工类型:REGULAR-正式工,TEMPORARY-临时工''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'hire_date');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN hire_date date DEFAULT NULL COMMENT ''入职日期''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'vendor_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''关联厂商ID(外协厂商员工账号绑定qxx_md_vendor;我方员工为NULL)'' AFTER factory_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_vendor_id');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE sys_user ADD INDEX idx_vendor_id (vendor_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_item_recpt_line' AND column_name = 'pur_order_line_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_item_recpt_line ADD COLUMN pur_order_line_id bigint(20) DEFAULT NULL COMMENT ''采购订单行ID(关联qxx_pur_order_line,退货精确回写)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_material_trace' AND column_name = 'vendor_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_material_trace ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''供应商ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- V71 销售订单表（manual_tables.sql 未含，SalOrderIT 依赖）
-- DDL 与 V71__sal_order_tables.sql 逐列一致，CREATE IF NOT EXISTS 幂等
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

-- V71 给工单加的行级追溯列（manual_tables.sql 的 qxx_pro_workorder 未含）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_workorder' AND column_name = 'sales_order_line_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_workorder ADD COLUMN sales_order_line_id bigint(20) DEFAULT NULL COMMENT ''销售订单行ID(关联qxx_sal_order_line,转工单来源)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_workorder' AND index_name = 'idx_sales_order_line_id');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE qxx_pro_workorder ADD INDEX idx_sales_order_line_id (sales_order_line_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V95 给 qxx_sal_order_line 加的动态扩展属性列（SalOrderLineMapper select 引用）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_sal_order_line' AND column_name = 'line_attrs');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_sal_order_line ADD COLUMN line_attrs JSON DEFAULT NULL COMMENT ''扩展属性(JSON扁平结构),分类驱动的动态属性快照''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V121 给 qxx_sal_order 加的订单来源列（SalOrderMapper select 引用）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_sal_order' AND column_name = 'source');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_sal_order ADD COLUMN source tinyint(1) NOT NULL DEFAULT 1 COMMENT ''订单来源:1-直接新增,2-CRM系统'' AFTER sample_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V124 给 qxx_sal_order 加的审核字段（SalOrderMapper select 引用）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_sal_order' AND column_name = 'approve_by');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_sal_order ADD COLUMN approve_by varchar(64) DEFAULT NULL COMMENT ''审核人'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_sal_order' AND column_name = 'approve_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_sal_order ADD COLUMN approve_time datetime DEFAULT NULL COMMENT ''审核时间'' AFTER approve_by',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_sal_order' AND column_name = 'approve_remark');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_sal_order ADD COLUMN approve_remark varchar(500) DEFAULT NULL COMMENT ''审核意见/驳回原因'' AFTER approve_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
