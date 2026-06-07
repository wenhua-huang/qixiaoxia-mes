-- ============================================================
-- 企小侠文化纸盒MES系统 — 工装夹具管理模块(tm)表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_tm_ (Tool Management 工装夹具管理)
-- 说明: 工装夹具类型/清单/保养等工装夹具全生命周期管理
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、工装夹具类型表
-- 用途：管理工装夹具的分类信息，维护保养类型和周期配置
-- ----------------------------
drop table if exists qxx_tm_tool_type;
create table qxx_tm_tool_type (
  tool_type_id        bigint(20)      not null auto_increment    comment '工装夹具类型ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  tool_type_code      varchar(64)     not null                   comment '类型编码(唯一)',
  tool_type_name      varchar(255)    not null                   comment '类型名称',
  need_code_flag      char(1)         default '1' not null       comment '是否需要编码(1-需要,0-不需要)',
  mainten_type        varchar(20)     default null               comment '保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数',
  mainten_cycle       int(11)         default 0                  comment '保养周期(与保养类型配合,如:月+3=每3个月保养一次)',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (tool_type_id),
  unique key uk_tool_type_code (tool_type_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工装夹具类型表';

-- ----------------------------
-- 2、工装夹具清单表
-- 用途：管理工装夹具台账信息，记录库存、使用状态和保养安排
-- ----------------------------
drop table if exists qxx_tm_tool;
create table qxx_tm_tool (
  tool_id             bigint(20)      not null auto_increment    comment '工装夹具ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  tool_code           varchar(64)     not null                   comment '工装夹具编码(唯一)',
  tool_name           varchar(255)    not null                   comment '工装夹具名称',
  brand               varchar(255)    default null               comment '品牌',
  spec                varchar(500)    default null               comment '规格型号',
  tool_type_id        bigint(20)      not null                   comment '工装夹具类型ID(关联qxx_tm_tool_type)',
  tool_type_code      varchar(64)     default ''                 comment '工装夹具类型编码',
  tool_type_name      varchar(255)    default ''                 comment '工装夹具类型名称',
  quantity            int(11)         default 1                  comment '总数量',
  available_quantity  int(11)         default 1                  comment '可用数量(总数量减去使用中和保养中的数量)',
  mainten_type        varchar(20)     default null               comment '保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数',
  next_mainten_date   date            default null               comment '下次保养日期',
  status              varchar(20)     default 'STORE' not null   comment '状态:STORE-在库,USING-使用中,MAINTENANCE-保养中,SCRAPPED-报废',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (tool_id),
  unique key uk_tool_code (tool_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工装夹具清单表';
