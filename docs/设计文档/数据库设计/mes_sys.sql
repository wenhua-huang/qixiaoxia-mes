-- ============================================================
-- 企小侠文化纸盒MES系统 — 系统辅助表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: sys_ (系统级公共表)
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、编码生成规则表
-- 用途：定义所有业务单号的自动生成规则，如订单号 DD20260522013
-- ----------------------------
drop table if exists sys_auto_code_rule;
create table sys_auto_code_rule (
  rule_id           bigint(20)      not null auto_increment    comment '规则ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rule_code         varchar(64)     not null                   comment '规则编码',
  rule_name         varchar(255)    not null                   comment '规则名称',
  rule_desc         varchar(500)    default null               comment '规则描述',
  max_length        int(11)                                    comment '最大长度',
  is_padded         char(1)         not null                   comment '是否补齐(1-是,0-否)',
  padded_char       varchar(20)                               comment '补齐字符(如0)',
  padded_method     char(1)         default 'L'                comment '补齐方式(L-左补齐,R-右补齐)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-启用,0-停用)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
,
  key idx_factory_id (factory_id)  primary key (rule_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '编码生成规则表';

-- ----------------------------
-- 2、编码生成规则组成表
-- 用途：定义编码规则中每一段的生成逻辑（固定字符/日期/流水号等）
-- ----------------------------
drop table if exists sys_auto_code_part;
create table sys_auto_code_part (
  part_id           bigint(20)      not null auto_increment    comment '分段ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rule_id           bigint(20)      not null                   comment '规则ID(关联sys_auto_code_rule)',
  part_index        int(11)         not null                   comment '分段序号(从1开始)',
  part_type         varchar(20)     not null                   comment '分段类型:INPUTCHAR-输入字符,NOWDATE-当前日期,FIXCHAR-固定字符,SERIALNO-流水号',
  part_code         varchar(64)                                comment '分段编码',
  part_name         varchar(255)                               comment '分段名称',
  part_length       int(11)         not null                   comment '分段长度',
  date_format       varchar(20)                                comment '日期时间格式(如yyyyMMdd)',
  input_character   varchar(64)                                comment '输入字符(INPUTCHAR类型时使用)',
  fix_character     varchar(64)                                comment '固定字符(FIXCHAR类型时使用)',
  seria_start_no    int(11)                                    comment '流水号起始值',
  seria_step        int(11)                                    comment '流水号步长',
  seria_now_no      int(11)                                    comment '流水号当前值',
  cycle_flag        char(1)                                    comment '流水号是否循环(1-是,0-否)',
  cycle_method      varchar(20)                                comment '循环方式:YEAR-按年,MONTH-按月,DAY-按天,HOUR-按小时,MINUTE-按分钟,OTHER-按传入字符变',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
,
  key idx_factory_id (factory_id)  primary key (part_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '编码生成规则组成表';

-- ----------------------------
-- 3、编码生成记录表
-- 用途：记录每个编码规则最后一次生成的流水号，确保不重复
-- ----------------------------
drop table if exists sys_auto_code_result;
create table sys_auto_code_result (
  code_id           bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rule_id           bigint(20)      not null                   comment '规则ID(关联sys_auto_code_rule)',
  gen_date          varchar(20)     not null                   comment '生成日期时间(用于循环判断)',
  gen_index         int(11)                                    comment '最后产生的序号',
  last_result       varchar(64)                                comment '最后产生的完整编码值',
  last_serial_no    int(11)                                    comment '最后产生的流水号',
  last_input_char   varchar(64)                                comment '最后传入的参数(循环方式为OTHER时使用)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
,
  key idx_factory_id (factory_id)  primary key (code_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '编码生成记录表';

-- ----------------------------
-- 4、通用附件表
-- 用途：关联任意业务单据的附件（图片/PDF/文档等），通过source_doc_type+source_doc_id定位
-- ----------------------------
drop table if exists sys_attachment;
create table sys_attachment (
  attachment_id     bigint(20)      not null auto_increment    comment '附件ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  source_doc_id     bigint(20)                                 comment '关联的业务单据ID',
  source_doc_type   varchar(64)                                comment '业务单据类型(如wm_item_recpt/pro_workorder等)',
  file_url          varchar(255)    not null                   comment '文件访问URL(相对路径)',
  base_path         varchar(64)                                comment '存储域名/Base路径',
  file_name         varchar(255)                               comment '存储文件名(UUID)',
  orignal_name      varchar(255)                               comment '原始文件名(含扩展名)',
  file_type         varchar(64)                                comment '文件类型(png/jpg/pdf/docx/xlsx等)',
  file_size         double(12,2)                               comment '文件大小(KB)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
,
  key idx_factory_id (factory_id)  primary key (attachment_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '通用附件表';

-- ----------------------------
-- 5、系统消息表
-- 用途：站内消息通知（如审批提醒、预警通知等）
-- ----------------------------
drop table if exists sys_message;
create table sys_message (
  message_id        bigint(20)      not null auto_increment    comment '消息ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  user_id           bigint(20)      not null                   comment '接收用户ID(关联sys_user)',
  title             varchar(255)    not null                   comment '消息标题',
  content           text                                       comment '消息内容',
  message_type      varchar(64)     not null                   comment '消息类型:SYSTEM-系统消息,APPROVAL-审批通知,WARNING-预警通知,NOTICE-公告',
  source_doc_id     bigint(20)                                 comment '关联业务单据ID',
  source_doc_type   varchar(64)                                comment '关联业务单据类型',
  read_flag         char(1)         default '0' not null       comment '是否已读(1-已读,0-未读)',
  read_time         datetime                                   comment '阅读时间',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
,
  key idx_factory_id (factory_id)  primary key (message_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '系统消息表';

-- ----------------------------
-- 6、通用待办事项表
-- 用途：记录各模块产生的待办任务（审批/检验/点检等），支持统一待办入口
-- ----------------------------
drop table if exists sys_todo_list;
create table sys_todo_list (
  todo_id           bigint(20)      not null auto_increment    comment '待办ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  user_id           bigint(20)      not null                   comment '待办人用户ID(关联sys_user)',
  todo_title        varchar(255)    not null                   comment '待办标题',
  todo_type         varchar(64)     not null                   comment '待办类型:APPROVAL-审批,QC_CHECK-质检,DV_CHECK-点检,MAINTEN-保养,REPAIR-维修,OTHER-其他',
  source_doc_id     bigint(20)      not null                   comment '关联业务单据ID',
  source_doc_type   varchar(64)     not null                   comment '关联业务单据类型',
  source_doc_code   varchar(64)                                comment '关联业务单据编码',
  priority          varchar(20)     default 'NORMAL'           comment '优先级:URGENT-紧急,HIGH-高,NORMAL-普通,LOW-低',
  status            varchar(20)     default 'PENDING'          comment '状态:PENDING-待处理,PROCESSING-处理中,COMPLETED-已完成,REJECTED-已驳回',
  deadline          datetime                                   comment '截止时间',
  handle_time       datetime                                   comment '处理时间',
  handle_result     varchar(500)                               comment '处理结果/意见',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
,
  key idx_factory_id (factory_id)  primary key (todo_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '通用待办事项表';
