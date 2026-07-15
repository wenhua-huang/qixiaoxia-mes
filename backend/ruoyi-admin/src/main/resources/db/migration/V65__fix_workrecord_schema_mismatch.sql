-- ============================================================
-- V65: 修复 qxx_pro_workrecord 表结构与代码不匹配
--
-- 背景：V64 用 CREATE TABLE IF NOT EXISTS 建表，但生产环境早已有
--      一个旧结构的 qxx_pro_workrecord（含 operation_flag/operation_time，
--      缺 workorder_id/workorder_code/process_name/clock_in_time/
--      clock_out_time/work_duration/status 等会话字段），导致 V64 被跳过，
--      clockIn 接口报 "Unknown column 'workorder_id' in 'field list'"。
--
-- 修复策略：旧表 0 条业务数据，直接 DROP + 按代码期望重建。
--          DROP IF EXISTS 幂等；重建结构对齐 Mapper XML。
--
-- 幂等：本迁移可重复执行（DROP IF EXISTS + CREATE）。
-- 日期：2026-07-13
-- ============================================================

DROP TABLE IF EXISTS qxx_pro_workrecord;

CREATE TABLE qxx_pro_workrecord (
  record_id                   bigint          not null auto_increment    comment '记录ID',
  factory_id                  bigint          not null                   comment '工厂ID(关联qxx_md_factory)',
  -- 操作人员
  user_id                     bigint          not null                   comment '用户ID',
  user_name                   varchar(64)     default null               comment '用户名',
  nick_name                   varchar(128)    default null               comment '用户昵称',
  -- 工作站（上工必填，一人一岗约束）
  workstation_id              bigint          not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(128)    default null               comment '工作站名称',
  -- 可选关联（工位为主，任务/工单可选）
  workorder_id                bigint          default null               comment '生产工单ID(关联qxx_pro_workorder,可选)',
  workorder_code              varchar(64)     default null               comment '生产工单编码',
  task_id                     bigint          default null               comment '生产任务ID(关联qxx_pro_task,可选)',
  task_code                   varchar(64)     default null               comment '生产任务编码',
  process_name                varchar(255)    default null               comment '工序名称(冗余便于展示)',
  -- 会话时间（核心）
  clock_in_time               datetime        not null                   comment '上工时间',
  clock_out_time              datetime        default null               comment '下工时间(下工结算时填)',
  work_duration               int             default 0                  comment '工作时长(分钟,下工时计算=下工时间-上工时间)',
  status                      varchar(32)     not null default 'ACTIVE'  comment '会话状态:ACTIVE-在岗,CLOSED-已下工',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (record_id),
  key idx_factory_id (factory_id),
  key idx_user_status (factory_id, user_id, status),
  key idx_workstation (factory_id, workstation_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上下工会话记录表';
