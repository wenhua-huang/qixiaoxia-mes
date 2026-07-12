-- ============================================================
-- V63: 上下工打卡建表 DDL + clock 按钮权限补全
--
-- 说明：qxx_pro_workrecord / qxx_pro_user_workstation 两表此前仅存在于
--      docs 设计文档，无 Flyway 版本化迁移。部分环境存在手动建的旧表
--      （operation_flag/operation_time 扁平流水结构，无数据），本迁移
--      DROP 后重建为"工位会话"模式。两表此前从未经 Flyway 版本化，
--      且无业务数据，可安全重建。
--      菜单(2308/2309)及 CRUD 按钮权限已由 V18 创建。
--
-- 会话模式：上工 INSERT 一条 ACTIVE，下工 UPDATE 同一条 →
--          CLOSED + clock_out_time + work_duration，工时直接读字段。
--
-- 幂等：DROP IF EXISTS + CREATE；DML WHERE NOT EXISTS。
-- 日期：2026-07-12
-- ============================================================

-- ════════════════════════════════════════════
-- 1. 上下工会话记录表（工位打卡）
--    会话模式：上工建一条(status=ACTIVE)，下工结算同一条(status=CLOSED)
-- ════════════════════════════════════════════
DROP TABLE IF EXISTS qxx_pro_workrecord;
CREATE TABLE qxx_pro_workrecord (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id                  bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  -- 操作人员
  user_id                     bigint(20)      not null                   comment '用户ID',
  user_name                   varchar(64)     default null               comment '用户名',
  nick_name                   varchar(128)    default null               comment '用户昵称',
  -- 工作站（上工必填，一人一岗约束）
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(128)    default null               comment '工作站名称',
  -- 可选关联（工位为主，任务/工单可选）
  workorder_id                bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder,可选)',
  workorder_code              varchar(64)     default null               comment '生产工单编码',
  task_id                     bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task,可选)',
  task_code                   varchar(64)     default null               comment '生产任务编码',
  process_name                varchar(255)    default null               comment '工序名称(冗余便于展示)',
  -- 会话时间（核心）
  clock_in_time               datetime        not null                   comment '上工时间',
  clock_out_time              datetime        default null               comment '下工时间(下工结算时填)',
  work_duration               int(11)         default 0                  comment '工作时长(分钟,下工时计算=下工时间-上工时间)',
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

-- ════════════════════════════════════════════
-- 2. 用户工作站绑定表
--    用于"绑定优先不强制"的工位推荐（操作工快捷选择自己绑定的工位）
-- ════════════════════════════════════════════
DROP TABLE IF EXISTS qxx_pro_user_workstation;
CREATE TABLE qxx_pro_user_workstation (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id                  bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  user_id                     bigint(20)      not null                   comment '用户ID',
  user_name                   varchar(64)     default null               comment '用户名',
  nick_name                   varchar(128)    default null               comment '用户昵称',
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(128)    default null               comment '工作站名称',
  enable_flag                 char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  operation_time              datetime        default null               comment '绑定操作时间',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (record_id),
  key idx_factory_id (factory_id),
  key idx_user (factory_id, user_id),
  key idx_workstation (factory_id, workstation_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户工作站绑定表';

-- ════════════════════════════════════════════
-- 3. 补充 clock 打卡按钮权限（挂在 2308 上下工记录菜单下）
--    操作工角色需勾选此权限才能使用移动端打卡
-- ════════════════════════════════════════════
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23085, '打卡', 2308, 5, 'F', '0', '0', 'mes:pro:workrecord:clock', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23085);

-- admin / 生产角色(role_id=11) 授权打卡权限
-- ⚠️ sys_role_menu 含 factory_id(NOT NULL)，Flyway 裸 JDBC 必须显式写 factory_id=0（全局）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 23085, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 23085 AND factory_id = 0);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 23085, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 11 AND menu_id = 23085 AND factory_id = 0);
