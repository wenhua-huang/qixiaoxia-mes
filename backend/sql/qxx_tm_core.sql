-- ============================================================
-- 企小侠文化纸盒MES系统 — 模具管理模块(tm)表设计
-- 版本: v1.0
-- 日期: 2026-06-14
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_tm_ (Tool Management 模具管理)
-- 说明: 模具类型/台账/领用归还/保养维修等模具全生命周期管理
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、模具类型表
-- 用途：管理模具的分类信息，维护保养类型和周期配置
-- ----------------------------
drop table if exists qxx_tm_tool_type;
create table qxx_tm_tool_type (
  tool_type_id        bigint(20)      not null auto_increment    comment '模具类型ID',
  factory_id          bigint(20)      not null                   comment '工厂ID',
  tool_type_code      varchar(64)     not null                   comment '类型编码(唯一)',
  tool_type_name      varchar(255)    not null                   comment '类型名称',
  need_code_flag      char(1)         default '1'                 comment '是否需要编码(1-是,0-否)',
  mainten_type        varchar(20)     default null               comment '保养类型: DAY-每天, WEEK-每周, MONTH-每月, QUARTER-每季, HALFYEAR-每半年, YEAR-每年',
  mainten_cycle       int(11)         default 0                  comment '保养周期(与保养类型配合, 如: MONTH+3=每3个月保养一次)',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是, 0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (tool_type_id),
  unique key uk_tool_type_code (tool_type_code),
  key idx_factory_id (factory_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '模具类型表';

-- ----------------------------
-- 2、模具台账表
-- 用途：管理模具台账信息，记录库存、使用状态、存放位置和保养安排
-- ----------------------------
drop table if exists qxx_tm_tool;
create table qxx_tm_tool (
  tool_id             bigint(20)      not null auto_increment    comment '模具ID',
  factory_id          bigint(20)      not null                   comment '工厂ID',
  tool_code           varchar(64)     not null                   comment '模具编码(唯一)',
  tool_name           varchar(255)    not null                   comment '模具名称',
  brand               varchar(255)    default null               comment '品牌',
  spec                varchar(500)    default null               comment '规格型号',
  tool_type_id        bigint(20)      not null                   comment '模具类型ID(关联qxx_tm_tool_type)',
  tool_type_code      varchar(64)     default ''                 comment '模具类型编码',
  tool_type_name      varchar(255)    default ''                 comment '模具类型名称',
  location            varchar(255)    default null               comment '存放位置',
  quantity            int(11)         default 1                  comment '总数量',
  available_quantity  int(11)         default 1                  comment '可用数量(总数量-使用中-保养中)',
  mainten_type        varchar(20)     default null               comment '保养类型: DAY-每天, WEEK-每周, MONTH-每月, QUARTER-每季, HALFYEAR-每半年, YEAR-每年',
  mainten_cycle       int(11)         default 0                  comment '保养周期',
  next_mainten_date   date            default null               comment '下次保养日期',
  status              varchar(20)     default 'STORE' not null   comment '状态: STORE-在库, USING-使用中, MAINTAINING-保养中, SCRAPPED-报废',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是, 0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (tool_id),
  unique key uk_tool_code (tool_code),
  key idx_factory_id (factory_id),
  key idx_tool_type_id (tool_type_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '模具台账表';

-- ----------------------------
-- 3、模具领用/归还记录表
-- 用途：记录模具的领用和归还流水
-- ----------------------------
drop table if exists qxx_tm_tool_borrow;
create table qxx_tm_tool_borrow (
  borrow_id           bigint(20)      not null auto_increment    comment '领用记录ID',
  factory_id          bigint(20)      not null                   comment '工厂ID',
  borrow_code         varchar(64)     not null                   comment '领用单号',
  tool_id             bigint(20)      not null                   comment '模具ID(关联qxx_tm_tool)',
  tool_code           varchar(64)     default ''                 comment '模具编码',
  tool_name           varchar(255)    default ''                 comment '模具名称',
  borrow_type         varchar(20)     not null                   comment '类型: BORROW-领用, RETURN-归还',
  borrower            varchar(64)     not null                   comment '领用人/归还人',
  borrow_dept         varchar(64)     default ''                 comment '领用部门',
  borrow_time         datetime        default null               comment '领用时间',
  plan_return_time    datetime        default null               comment '计划归还时间',
  actual_return_time  datetime        default null               comment '实际归还时间',
  quantity            int(11)         default 1                  comment '数量',
  status              varchar(20)     default 'BORROWED'         comment '状态: BORROWED-已领用, RETURNED-已归还, OVERDUE-已逾期',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (borrow_id),
  key idx_factory_id (factory_id),
  key idx_tool_id (tool_id),
  key idx_borrow_code (borrow_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '模具领用/归还记录表';

-- ----------------------------
-- 4、模具保养/维修记录表
-- 用途：记录模具的保养和维修历史
-- ----------------------------
drop table if exists qxx_tm_tool_maintenance;
create table qxx_tm_tool_maintenance (
  maintenance_id      bigint(20)      not null auto_increment    comment '保养/维修记录ID',
  factory_id          bigint(20)      not null                   comment '工厂ID',
  maintenance_code    varchar(64)     not null                   comment '保养/维修单号',
  tool_id             bigint(20)      not null                   comment '模具ID(关联qxx_tm_tool)',
  tool_code           varchar(64)     default ''                 comment '模具编码',
  tool_name           varchar(255)    default ''                 comment '模具名称',
  maintenance_type    varchar(20)     not null                   comment '类型: MAINTENANCE-保养, REPAIR-维修',
  maintenance_date    date            default null               comment '保养/维修日期',
  maintenance_person  varchar(64)     default ''                 comment '保养/维修人',
  maintenance_content varchar(1000)   default ''                 comment '保养/维修内容',
  maintenance_result  varchar(500)    default ''                 comment '保养/维修结果',
  cost                decimal(10,2)   default 0.00               comment '费用',
  next_maintenance_date date          default null               comment '下次保养日期',
  status              varchar(20)     default 'COMPLETED'        comment '状态: COMPLETED-已完成, IN_PROGRESS-进行中',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (maintenance_id),
  key idx_factory_id (factory_id),
  key idx_tool_id (tool_id),
  key idx_maintenance_code (maintenance_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '模具保养/维修记录表';
