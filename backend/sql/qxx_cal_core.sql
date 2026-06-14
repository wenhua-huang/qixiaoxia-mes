-- ============================================================
-- 企小侠文化纸盒MES系统 — 排班管理模块(cal) DDL
-- 版本: v1.0
-- 日期: 2026-06-14
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_cal_ (Calendar 排班管理)
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、班组表
-- 用途：管理生产班组信息，如白班A组/夜班B组
-- ----------------------------
drop table if exists qxx_cal_team;
create table qxx_cal_team (
  team_id             bigint(20)      not null auto_increment    comment '班组ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  team_code           varchar(64)     not null                   comment '班组编码(唯一)',
  team_name           varchar(255)    not null                   comment '班组名称',
  team_type           varchar(20)     default null               comment '班组类型:DAY-白班,NIGHT-夜班,MIDDLE-中班,ROTATION-轮班',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (team_id),
  unique key uk_team_code (team_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '班组表';

-- ----------------------------
-- 2、班组成员表
-- 用途：管理班组与员工的关联关系
-- ----------------------------
drop table if exists qxx_cal_team_member;
create table qxx_cal_team_member (
  member_id           bigint(20)      not null auto_increment    comment '成员关联ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  team_id             bigint(20)      not null                   comment '班组ID(关联qxx_cal_team)',
  team_code           varchar(64)     default ''                 comment '班组编码',
  team_name           varchar(255)    default ''                 comment '班组名称',
  user_id             bigint(20)      not null                   comment '用户ID(关联sys_user)',
  user_name           varchar(64)     not null                   comment '用户姓名',
  phone               varchar(64)     default null               comment '联系电话',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (member_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '班组成员表';

-- ----------------------------
-- 3、排班计划表
-- 用途：定义排班计划的时间范围、日历类型和班次类型
-- ----------------------------
drop table if exists qxx_cal_plan;
create table qxx_cal_plan (
  plan_id             bigint(20)      not null auto_increment    comment '排班计划ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_code           varchar(64)     not null                   comment '排班计划编码(唯一)',
  plan_name           varchar(255)    not null                   comment '排班计划名称',
  calendar_type       varchar(20)     default null               comment '日历类型:WEEKLY-周历,MONTHLY-月历,QUARTERLY-季历,YEARLY-年历,CUSTOM-自定义',
  start_date          date            not null                   comment '排班开始日期',
  end_date            date            not null                   comment '排班结束日期',
  shift_type          varchar(20)     default null               comment '班次类型:TWOSHIFT-两班倒,THREESHIFT-三班倒,DAYONLY-常白班,CUSTOM-自定义',
  status              varchar(20)     default 'DRAFT' not null   comment '计划状态:DRAFT-草稿,ACTIVE-生效中,CLOSED-已关闭',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (plan_id),
  unique key uk_plan_code (plan_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '排班计划表';

-- ----------------------------
-- 4、计划班次表
-- 用途：定义排班计划下的具体班次时间安排
-- ----------------------------
drop table if exists qxx_cal_shift;
create table qxx_cal_shift (
  shift_id            bigint(20)      not null auto_increment    comment '班次ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_id             bigint(20)      not null                   comment '排班计划ID(关联qxx_cal_plan)',
  shift_seq           int(11)         not null                   comment '班次序号(如1-白班,2-夜班)',
  shift_name          varchar(255)    not null                   comment '班次名称(如白班/夜班/中班)',
  start_time          time            not null                   comment '班次开始时间(如08:00:00)',
  end_time            time            not null                   comment '班次结束时间(如20:00:00)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (shift_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '计划班次表';

-- ----------------------------
-- 5、计划班组关联表
-- 用途：关联排班计划与参与班组
-- ----------------------------
drop table if exists qxx_cal_plan_team;
create table qxx_cal_plan_team (
  record_id           bigint(20)      not null auto_increment    comment '关联记录ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_id             bigint(20)      not null                   comment '排班计划ID(关联qxx_cal_plan)',
  plan_code           varchar(64)     default ''                 comment '排班计划编码',
  plan_name           varchar(255)    default ''                 comment '排班计划名称',
  team_id             bigint(20)      not null                   comment '班组ID(关联qxx_cal_team)',
  team_code           varchar(64)     default ''                 comment '班组编码',
  team_name           varchar(255)    default ''                 comment '班组名称',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '计划班组关联表';

-- ----------------------------
-- 6、节假日设置表
-- 用途：管理节假日和调休工作日设定，用于排班计算时排除非工作日
-- ----------------------------
drop table if exists qxx_cal_holiday;
create table qxx_cal_holiday (
  holiday_id          bigint(20)      not null auto_increment    comment '节假日设置ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  holiday_date        date            not null                   comment '日期',
  holiday_name        varchar(255)    not null                   comment '节假日名称',
  holiday_type        varchar(20)     not null                   comment '类型:HOLIDAY-节假日,WORKDAY-调休工作日',
  enable_flag         char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (holiday_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '节假日设置表';

-- ----------------------------
-- 7、班组排班明细表
-- 用途：按日期记录各班组的具体排班安排（某天某个班组排到哪个班次）
-- ----------------------------
drop table if exists qxx_cal_teamshift;
create table qxx_cal_teamshift (
  teamshift_id        bigint(20)      not null auto_increment    comment '排班明细ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  shift_date          date            not null                   comment '排班日期',
  team_id             bigint(20)      not null                   comment '班组ID(关联qxx_cal_team)',
  team_code           varchar(64)     default ''                 comment '班组编码',
  team_name           varchar(255)    default ''                 comment '班组名称',
  shift_id            bigint(20)      not null                   comment '班次ID(关联qxx_cal_shift)',
  shift_name          varchar(255)    default ''                 comment '班次名称',
  plan_id             bigint(20)      not null                   comment '排班计划ID(关联qxx_cal_plan)',
  plan_code           varchar(64)     default ''                 comment '排班计划编码',
  plan_name           varchar(255)    default ''                 comment '排班计划名称',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (teamshift_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '班组排班明细表';
