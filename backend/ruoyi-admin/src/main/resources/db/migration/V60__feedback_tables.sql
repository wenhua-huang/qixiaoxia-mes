-- ============================================================
-- V60: 报工模块建表 DDL
-- 说明：报工三张表（qxx_pro_feedback / qxx_pro_feedback_param / qxx_pro_feedback_consume）
--      此前仅存在于 docs 设计文档，无 Flyway 版本化迁移，全新部署时缺失。
--      报工菜单权限(2303)已由 V59__add_missing_pro_c_menus.sql 补全，本迁移仅建表。
-- 幂等：CREATE TABLE IF NOT EXISTS，生产库已有表不报错。
-- 日期：2026-07-10
-- ============================================================

-- 1.1 报工记录主表
CREATE TABLE IF NOT EXISTS qxx_pro_feedback (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id                  bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  feedback_type               varchar(64)     not null                   comment '报工类型:INTERNAL-自制报工,OUTSOURCE_AGENT-外发代填(我方代录),OUTSOURCE_VENDOR-外发直填(外协厂自主报工)',
  feedback_code               varchar(64)     default null               comment '报工单编码',
  -- 工作站信息
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(255)    default null               comment '工作站名称',
  -- 工单信息
  workorder_id                bigint(20)      not null                   comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code              varchar(64)     default null               comment '生产工单编码',
  workorder_name              varchar(255)    default null               comment '生产工单名称',
  -- 工艺信息
  route_id                    bigint(20)      not null                   comment '工艺路线ID(关联qxx_pro_route)',
  route_code                  varchar(64)     default null               comment '工艺路线编码',
  process_id                  bigint(20)      not null                   comment '工序ID(关联qxx_pro_process)',
  process_code                varchar(64)     default null               comment '工序编码',
  process_name                varchar(255)    default null               comment '工序名称',
  -- 任务信息
  task_id                     bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code                   varchar(64)     default null               comment '生产任务编码',
  -- 产品物料信息
  item_id                     bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code                   varchar(64)     not null                   comment '产品物料编码',
  item_name                   varchar(255)    not null                   comment '产品物料名称',
  unit_of_measure             varchar(64)     default null               comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  specification               varchar(500)    default null               comment '规格型号',
  -- 外协扩展字段
  vendor_id                   bigint(20)      default null               comment '外协厂ID(关联qxx_md_vendor,feedback_type为外发时必填)',
  vendor_code                 varchar(64)     default null               comment '外协厂编码',
  vendor_name                 varchar(255)    default null               comment '外协厂名称',
  outsource_factory_id        bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  outsource_issue_id          bigint(20)      default null               comment '外协领料单ID(关联qxx_wm_outsource_issue,外发工序报工时关联)',
  outsource_issue_code        varchar(64)     default null               comment '外协领料单编码',
  outsource_recpt_id          bigint(20)      default null               comment '外协入库单ID(关联qxx_wm_outsource_recpt,外协完工返回时关联)',
  -- 批次信息
  expire_date                 datetime        default null               comment '有效期至',
  lot_number                  varchar(128)    default null               comment '生产批号',
  -- 产出数量
  quantity                    decimal(14,2)   default null               comment '排产数量(任务计划数)',
  quantity_feedback           decimal(14,2)   default null               comment '本次报工数量(工序产出总数)',
  quantity_qualified          decimal(14,2)   default null               comment '合格品数量',
  quantity_unqualified        decimal(14,2)   default null               comment '不合格品数量',
  quantity_uncheck            decimal(14,2)   default null               comment '待检测数量(待质检判定)',
  quantity_labor_scrap        decimal(14,2)   default null               comment '工废数量(操作原因报废)',
  quantity_material_scrap     decimal(14,2)   default null               comment '料废数量(材料原因报废)',
  quantity_other_scrap        decimal(14,2)   default null               comment '其他报废数量',
  -- 报工人员信息
  user_name                   varchar(64)     default null               comment '报工用户名(操作工账号)',
  nick_name                   varchar(64)     default null               comment '报工用户昵称',
  feedback_channel            varchar(64)     default null               comment '报工途径:PC-电脑端,PAD-平板端,SCAN-扫码枪,WECHAT-微信小程序',
  feedback_time               datetime        default null               comment '报工时间',
  record_user                 varchar(64)     default null               comment '记录人(录单人)',
  record_nick                 varchar(64)     default null               comment '记录人昵称',
  status                      varchar(64)     default 'PREPARE'          comment '报工状态:PREPARE-待确认,CONFIRMED-已确认,AUDITED-已审核',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (record_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工记录表';

-- 1.2 报工实际参数值表
CREATE TABLE IF NOT EXISTS qxx_pro_feedback_param (
  record_id          bigint(20)    not null auto_increment  comment '记录ID',
  factory_id         bigint(20)    not null                 comment '工厂ID(关联qxx_md_factory)',
  feedback_id        bigint(20)    not null                 comment '报工记录ID(关联qxx_pro_feedback.record_id)',
  workorder_param_id bigint(20)    default null             comment '工单参数ID(关联qxx_pro_workorder_param,对比基准)',
  template_id        bigint(20)    not null                 comment '参数模版ID(关联qxx_pro_param_template,取值约束)',
  actual_value       varchar(500)  default null             comment '实际参数值(操作工填报)',
  is_deviation       char(1)       default null             comment '是否偏差(Y-超出min/max,N-正常,NULL-无约束未判定)',
  remark             varchar(500)  default ''               comment '偏差说明',
  create_by          varchar(64)   default ''               comment '创建者',
  create_time        datetime      default current_timestamp comment '创建时间',
  update_by          varchar(64)   default ''               comment '更新者',
  update_time        datetime      default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id),
  unique key uk_feedback_param (feedback_id, template_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工实际参数值表';

-- 1.3 报工物料消耗表
CREATE TABLE IF NOT EXISTS qxx_pro_feedback_consume (
  consume_id    bigint(20)      not null auto_increment    comment '消耗ID',
  factory_id    bigint(20)      not null default 1         comment '工厂ID',
  feedback_id   bigint(20)      not null                   comment '报工ID(关联qxx_pro_feedback.record_id)',
  workorder_id  bigint(20)      default null               comment '工单ID(关联qxx_pro_workorder)',
  item_id       bigint(20)      default null               comment '物料ID(关联qxx_md_item)',
  item_code     varchar(100)    default ''                 comment '物料编码',
  item_name     varchar(200)    default ''                 comment '物料名称',
  quantity      decimal(18,4)   default null               comment '消耗数量',
  batch_code    varchar(100)    default ''                 comment '批次号',
  primary key (consume_id),
  key idx_feedback_id (feedback_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工物料消耗表';
