-- ============================================================
-- 企小侠文化纸盒MES系统 — 设备管理模块(dv)表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_dv_ (Device 设备管理)
-- 说明: 设备类型/台账/点检/保养/维修等设备全生命周期管理
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、设备类型表
-- 用途：树形分类管理设备类型，如 印刷机/全自动制袋机/半自动制袋机/辅助设备
-- ----------------------------
drop table if exists qxx_dv_machinery_type;
create table qxx_dv_machinery_type (
  machinery_type_id   bigint(20)      not null auto_increment    comment '设备类型ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  machinery_type_code varchar(64)     not null                   comment '设备类型编码',
  machinery_type_name varchar(255)    not null                   comment '设备类型名称',
  parent_type_id      bigint(20)      default 0 not null         comment '父类型ID(0表示根节点)',
  ancestors           varchar(500)    not null                   comment '所有层级父节点ID(用逗号分隔,如0,100,200)',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (machinery_type_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '设备类型表';

-- ----------------------------
-- 2、设备台账表
-- 用途：管理所有设备的台账信息，记录设备基本信息、维护历史及运行状态
-- ----------------------------
drop table if exists qxx_dv_machinery;
create table qxx_dv_machinery (
  machinery_id        bigint(20)      not null auto_increment    comment '设备ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  machinery_code      varchar(64)     not null                   comment '设备编码(唯一)',
  machinery_name      varchar(255)    not null                   comment '设备名称',
  machinery_brand     varchar(255)    default null               comment '设备品牌',
  machinery_spec      varchar(500)    default null               comment '设备规格型号',
  machinery_type_id   bigint(20)      not null                   comment '设备类型ID(关联qxx_dv_machinery_type)',
  machinery_type_code varchar(64)     default ''                 comment '设备类型编码',
  machinery_type_name varchar(255)    default ''                 comment '设备类型名称',
  workshop_id         bigint(20)      default 0                  comment '所属车间ID(关联qxx_md_workshop)',
  workshop_code       varchar(64)     default ''                 comment '所属车间编码',
  workshop_name       varchar(255)    default ''                 comment '所属车间名称',
  last_mainten_time   datetime        default null               comment '最后保养时间',
  last_check_time     datetime        default null               comment '最后点检时间',
  status              varchar(20)     default 'IDLE' not null    comment '设备状态:RUNNING-运行中,IDLE-空闲,MAINTENANCE-保养中,BREAKDOWN-故障停机',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (machinery_id),
  unique key uk_machinery_code (machinery_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '设备台账表';

-- ----------------------------
-- 3、点检保养项目表
-- 用途：定义设备点检和保养的标准项目，支持按不同周期类型配置
-- ----------------------------
drop table if exists qxx_dv_subject;
create table qxx_dv_subject (
  subject_id          bigint(20)      not null auto_increment    comment '项目ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  subject_code        varchar(64)     not null                   comment '项目编码(唯一)',
  subject_name        varchar(255)    not null                   comment '项目名称',
  subject_type        varchar(20)     not null                   comment '项目类型:CHECK-点检,MAINTEN-保养',
  cycle_type          varchar(20)     default null               comment '周期类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,CUSTOM-自定义',
  cycle_days          int(11)         default 0                  comment '周期天数(CUSTOM类型时使用)',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (subject_id),
  unique key uk_subject_code (subject_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '点检保养项目表';

-- ----------------------------
-- 4、点检保养计划头表
-- 用途：定义点检/保养计划的计划周期和有效期
-- ----------------------------
drop table if exists qxx_dv_check_plan;
create table qxx_dv_check_plan (
  plan_id             bigint(20)      not null auto_increment    comment '计划ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_code           varchar(64)     not null                   comment '计划编码(唯一)',
  plan_name           varchar(255)    not null                   comment '计划名称',
  plan_type           varchar(20)     not null                   comment '计划类型:CHECK-点检计划,MAINTEN-保养计划',
  start_date          date            default null               comment '计划开始日期',
  end_date            date            default null               comment '计划结束日期',
  cycle_type          varchar(20)     default null               comment '执行周期类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,CUSTOM-自定义',
  cycle_days          int(11)         default 0                  comment '执行周期天数(CUSTOM类型时使用)',
  status              varchar(20)     default 'DRAFT' not null   comment '计划状态:DRAFT-草稿,ACTIVE-生效中,PAUSED-已暂停,CLOSED-已关闭',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (plan_id),
  unique key uk_plan_code (plan_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '点检保养计划头表';

-- ----------------------------
-- 5、计划-设备关联表
-- 用途：关联点检保养计划与具体设备，一个计划可关联多台设备
-- ----------------------------
drop table if exists qxx_dv_check_machinery;
create table qxx_dv_check_machinery (
  record_id           bigint(20)      not null auto_increment    comment '关联记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_id             bigint(20)      not null                   comment '计划ID(关联qxx_dv_check_plan)',
  machinery_id        bigint(20)      not null                   comment '设备ID(关联qxx_dv_machinery)',
  machinery_code      varchar(64)     default ''                 comment '设备编码',
  machinery_name      varchar(255)    default ''                 comment '设备名称',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '计划-设备关联表';

-- ----------------------------
-- 6、计划-项目关联表
-- 用途：关联点检保养计划与检查项目，一个计划可包含多个检查项目
-- ----------------------------
drop table if exists qxx_dv_check_subject;
create table qxx_dv_check_subject (
  record_id           bigint(20)      not null auto_increment    comment '关联记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_id             bigint(20)      not null                   comment '计划ID(关联qxx_dv_check_plan)',
  subject_id          bigint(20)      not null                   comment '项目ID(关联qxx_dv_subject)',
  subject_code        varchar(64)     default ''                 comment '项目编码',
  subject_name        varchar(255)    default ''                 comment '项目名称',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '计划-项目关联表';

-- ----------------------------
-- 7、点检记录表
-- 用途：记录每次设备点检的执行情况和整体结果
-- ----------------------------
drop table if exists qxx_dv_check_record;
create table qxx_dv_check_record (
  record_id           bigint(20)      not null auto_increment    comment '点检记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_id             bigint(20)      not null                   comment '计划ID(关联qxx_dv_check_plan)',
  plan_code           varchar(64)     default ''                 comment '计划编码',
  plan_name           varchar(255)    default ''                 comment '计划名称',
  machinery_id        bigint(20)      not null                   comment '设备ID(关联qxx_dv_machinery)',
  machinery_code      varchar(64)     default ''                 comment '设备编码',
  machinery_name      varchar(255)    default ''                 comment '设备名称',
  check_date          date            not null                   comment '点检日期',
  check_user          varchar(64)     default ''                 comment '点检人',
  check_result        varchar(20)     default null               comment '整体检查结果:OK-合格,NG-不合格,NA-不适用',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '点检记录表';

-- ----------------------------
-- 8、点检记录行表
-- 用途：点检记录的明细行，逐项记录每个检查项目的检查结果
-- ----------------------------
drop table if exists qxx_dv_check_record_line;
create table qxx_dv_check_record_line (
  line_id             bigint(20)      not null auto_increment    comment '点检记录行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  record_id           bigint(20)      not null                   comment '点检记录ID(关联qxx_dv_check_record)',
  subject_id          bigint(20)      not null                   comment '检查项目ID(关联qxx_dv_subject)',
  subject_code        varchar(64)     default ''                 comment '检查项目编码',
  subject_name        varchar(255)    default ''                 comment '检查项目名称',
  check_result        varchar(20)     not null                   comment '检查结果:OK-合格,NG-不合格,NA-不适用',
  check_remark        varchar(500)    default ''                 comment '检查备注',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '点检记录行表';

-- ----------------------------
-- 9、保养记录表
-- 用途：记录每次设备保养的执行情况和整体结果
-- ----------------------------
drop table if exists qxx_dv_mainten_record;
create table qxx_dv_mainten_record (
  record_id           bigint(20)      not null auto_increment    comment '保养记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_id             bigint(20)      not null                   comment '计划ID(关联qxx_dv_check_plan)',
  plan_code           varchar(64)     default ''                 comment '计划编码',
  plan_name           varchar(255)    default ''                 comment '计划名称',
  machinery_id        bigint(20)      not null                   comment '设备ID(关联qxx_dv_machinery)',
  machinery_code      varchar(64)     default ''                 comment '设备编码',
  machinery_name      varchar(255)    default ''                 comment '设备名称',
  mainten_date        date            not null                   comment '保养日期',
  mainten_user        varchar(64)     default ''                 comment '保养人',
  mainten_result      varchar(20)     default null               comment '整体保养结果:OK-合格,NG-不合格,NA-不适用',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '保养记录表';

-- ----------------------------
-- 10、保养记录行表
-- 用途：保养记录的明细行，逐项记录每个保养项目的执行结果
-- ----------------------------
drop table if exists qxx_dv_mainten_record_line;
create table qxx_dv_mainten_record_line (
  line_id             bigint(20)      not null auto_increment    comment '保养记录行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  record_id           bigint(20)      not null                   comment '保养记录ID(关联qxx_dv_mainten_record)',
  subject_id          bigint(20)      not null                   comment '保养项目ID(关联qxx_dv_subject)',
  subject_code        varchar(64)     default ''                 comment '保养项目编码',
  subject_name        varchar(255)    default ''                 comment '保养项目名称',
  check_result        varchar(20)     not null                   comment '保养结果:OK-合格,NG-不合格,NA-不适用',
  check_remark        varchar(500)    default ''                 comment '保养备注',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '保养记录行表';

-- ----------------------------
-- 11、设备维修单表
-- 用途：管理设备故障报修、维修过程和验收确认的完整流程
-- ----------------------------
drop table if exists qxx_dv_repair;
create table qxx_dv_repair (
  repair_id           bigint(20)      not null auto_increment    comment '维修单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  repair_code         varchar(64)     not null                   comment '维修单编码(唯一)',
  machinery_id        bigint(20)      not null                   comment '设备ID(关联qxx_dv_machinery)',
  machinery_code      varchar(64)     default ''                 comment '设备编码',
  machinery_name      varchar(255)    default ''                 comment '设备名称',
  fault_desc          varchar(1000)   not null                   comment '故障描述',
  repair_date         date            default null               comment '报修日期',
  required_date       date            default null               comment '要求完成日期',
  actual_date         date            default null               comment '实际完成日期',
  repair_result       varchar(500)    default null               comment '维修结果描述',
  confirm_by          varchar(64)     default ''                 comment '验收人',
  status              varchar(20)     default 'PREPARE' not null comment '维修状态:PREPARE-待维修,REPAIRING-维修中,COMPLETED-已完成,CONFIRMED-已验收',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (repair_id),
  unique key uk_repair_code (repair_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '设备维修单表';

-- ----------------------------
-- 12、维修单行表
-- 用途：维修单明细行，记录维修过程中的具体操作内容、更换配件和费用
-- ----------------------------
drop table if exists qxx_dv_repair_line;
create table qxx_dv_repair_line (
  line_id             bigint(20)      not null auto_increment    comment '维修单行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  repair_id           bigint(20)      not null                   comment '维修单ID(关联qxx_dv_repair)',
  repair_content      varchar(1000)   not null                   comment '维修内容描述',
  replace_parts       varchar(500)    default null               comment '更换配件',
  cost                decimal(14,2)   default 0.00               comment '维修费用',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '维修单行表';
