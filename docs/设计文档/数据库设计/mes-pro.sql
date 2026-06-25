-- ============================================================
-- 企小侠文化纸盒MES系统 — 生产管理模块(pro)表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_pro_ (Production Management 生产管理)
-- 说明: 生产工单/工艺路线/生产排产/报工/投料/流转卡/外协/安灯等完整生产生命周期
--       通用扩展: 客户订单号,产品尺寸,印刷要求,包装/发货要求,订单类型(纸袋/礼品盒共用)
--       纸袋专用: 绳料规格(rope_spec)，礼品盒订单留NULL
--       外发工序扩展: 工序类型(自制/外发/分切), 报工类型(自制/外发代填/外发直填)
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- 一、生产工单与BOM
-- ============================================================

-- ----------------------------
-- 1、生产工单表
-- 用途: 核心生产单据，承接销售订单生成生产任务
-- 通用扩展: 客户订单号,产品尺寸,印刷要求,包装要求,发货要求,订单类型(纸袋/礼品盒共用)
-- 纸袋专用: 绳料规格(rope_spec)
-- ----------------------------
drop table if exists qxx_pro_workorder;
create table qxx_pro_workorder (
  workorder_id                bigint(20)      not null auto_increment    comment '工单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workorder_code              varchar(64)     not null                   comment '工单编码(唯一)',
  workorder_name              varchar(255)    not null                   comment '工单名称',
  workorder_type              varchar(64)     default 'SELF'             comment '工单类型:SELF-自制,OUTSOURCE-外协',
  order_source                varchar(64)     not null                   comment '来源类型:SALES_ORDER-销售订单,MANUAL-手工创建,PLAN-计划生成',
  source_code                 varchar(64)     default null               comment '来源单据编码(销售订单号等)',
  product_id                  bigint(20)      not null                   comment '产品ID(关联qxx_md_item)',
  product_code                varchar(64)     not null                   comment '产品编码',
  product_name                varchar(255)    not null                   comment '产品名称',
  product_spc                 varchar(255)    default null               comment '产品规格型号',
  route_product_id            bigint(20)      default null               comment '路线产品ID(关联qxx_pro_route_product)',
  unit_of_measure             varchar(64)     not null                   comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  quantity                    decimal(14,2)   default 0.00  not null     comment '计划生产数量',
  quantity_produced           decimal(14,2)   default 0.00               comment '已生产数量',
  quantity_changed            decimal(14,2)   default 0.00               comment '调整数量(增补/扣减)',
  quantity_scheduled          decimal(14,2)   default 0.00               comment '已排产数量',
  -- 客户信息
  client_id                   bigint(20)      default null               comment '客户ID(关联qxx_md_client)',
  client_code                 varchar(64)     default null               comment '客户编码',
  client_name                 varchar(255)    default null               comment '客户名称',
  -- 通用扩展字段（纸袋/礼品盒共用，rope_spec为纸袋专用）
  client_order_code           varchar(64)     default null               comment '客户订单号(PO号)',
  product_size                varchar(100)    default null               comment '产品尺寸(长*宽*高mm),如254*127*330mm',
  printing_req                varchar(500)    default null               comment '印刷要求描述,如1色满版黑印刷/彩印',
  rope_spec                   varchar(200)    default null               comment '绳料规格要求(纸袋专用,礼品盒留NULL)',
  package_req                 varchar(500)    default null               comment '包装要求描述,如250个/箱,贴唛头',
  shipping_req                varchar(500)    default null               comment '发货要求描述',
  order_type                  varchar(50)     default 'NEW'              comment '订单类型:NEW-新单,REPEAT-返单',
  -- 外协信息
  vendor_id                   bigint(20)      default null               comment '外协供应商ID(关联qxx_md_vendor)',
  vendor_code                 varchar(64)     default null               comment '外协供应商编码',
  vendor_name                 varchar(255)    default null               comment '外协供应商名称',
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  -- 批次与计划
  batch_code                  varchar(64)     default null               comment '批次号',
  request_date                datetime        default null               comment '需求日期(客户要求的交期)',
  -- 批次拆分通过流转卡(qxx_pro_card)实现，工单不再拆分子工单
  cancel_date                 datetime        default null               comment '取消日期',
  finish_date                 datetime        default null               comment '实际完成日期',
  status                      varchar(64)     default 'PREPARE'          comment '工单状态:PREPARE-待生产,PRODUCING-生产中,COMPLETED-已完成,CANCEL-已取消,CLOSED-已关闭',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (workorder_id),
  unique key uk_workorder_code (workorder_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产工单表';


-- ----------------------------
-- 2、工单BOM组成表
-- 用途: 记录每个工单所需的物料清单，与标准BOM可不同(工单级BOM覆盖)
-- 支持双单位物料消耗
-- ----------------------------
drop table if exists qxx_pro_workorder_bom;
create table qxx_pro_workorder_bom (
  line_id                     bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workorder_id                bigint(20)      not null                   comment '生产工单ID(关联qxx_pro_workorder)',
  item_id                     bigint(20)      not null                   comment 'BOM物料ID(关联qxx_md_item)',
  item_code                   varchar(64)     not null                   comment 'BOM物料编码',
  item_name                   varchar(255)    not null                   comment 'BOM物料名称',
  item_spc                    varchar(255)    default null               comment '规格型号',
  unit_of_measure             varchar(64)     not null                   comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  -- 双单位扩展
  unit2                       varchar(64)     default null               comment '辅助单位编码(如ROLL-卷/TON-吨,纸袋/礼品盒通用)',
  unit2_name                  varchar(64)     default null               comment '辅助单位名称',
  conversion_rate             decimal(10,4)   default 1.0000             comment '主单位→辅助单位换算率',
  process_id                  bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  process_name                varchar(255)    default null               comment '工序名称',
  item_or_product             varchar(20)     not null                   comment '物料产品标识:RAW-原料,SEMI-半成品,FINISHED-成品,AUXILIARY-辅料,PACK-包材',
  quantity                    decimal(14,2)   default 0.00  not null     comment '单位用量(每个成品消耗的数量)',
  total_quantity              decimal(14,2)   default 0.00               comment '预计总用量(单位用量*计划数量)',
  total_quantity2             decimal(14,2)   default 0.00               comment '预计总用量(辅助单位)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工单BOM组成表';


-- ============================================================
-- 二、工序与工艺路线
-- ============================================================

-- ----------------------------
-- 3、生产工序表
-- 工序类型: INTERNAL-自制,OUTSOURCE-外发,SLITTING-分切(纸袋)/CUTTING-裁切(礼品盒)等
-- ----------------------------
drop table if exists qxx_pro_process;
create table qxx_pro_process (
  process_id                  bigint(20)      not null auto_increment    comment '工序ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  process_code                varchar(64)     not null                   comment '工序编码(唯一)',
  process_name                varchar(255)    not null                   comment '工序名称',
  process_type                varchar(50)     default 'INTERNAL'         comment '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  attention                   varchar(1000)   default null               comment '工艺要求/注意事项',
  enable_flag                 char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (process_id),
  unique key uk_process_code (process_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产工序表';


-- ----------------------------
-- 4、工序作业内容表
-- 用途: 定义每个工序的具体作业步骤/辅助设备/辅助材料
-- ----------------------------
drop table if exists qxx_pro_process_content;
create table qxx_pro_process_content (
  content_id                  bigint(20)      not null auto_increment    comment '内容ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  process_id                  bigint(20)      not null                   comment '工序ID(关联qxx_pro_process)',
  order_num                   int(4)          default 0                  comment '顺序编号(作业步骤序号)',
  content_text                varchar(500)    default null               comment '作业内容说明',
  device                      varchar(255)    default null               comment '辅助设备/工具',
  material                    varchar(255)    default null               comment '辅助材料/辅料',
  doc_url                     varchar(255)    default null               comment '作业指导书URL(SOP文件)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (content_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工序作业内容表';


-- ----------------------------
-- 5、工艺路线表
-- 用途: 定义产品的生产工序流程，如 印刷→裱纸→模切→贴绳→包装
-- ----------------------------
drop table if exists qxx_pro_route;
create table qxx_pro_route (
  route_id                    bigint(20)      not null auto_increment    comment '工艺路线ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  route_code                  varchar(64)     not null                   comment '工艺路线编码(唯一)',
  route_name                  varchar(255)    not null                   comment '工艺路线名称',
  route_desc                  varchar(500)    default null               comment '工艺路线说明',
  enable_flag                 char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (route_id),
  unique key uk_route_code (route_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工艺路线表';


-- ----------------------------
-- 6、工艺路线-工序组成表
-- 外发工序扩展: process_type, vendor_id/vendor_name, is_outsource
-- ----------------------------
drop table if exists qxx_pro_route_process;
create table qxx_pro_route_process (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  route_id                    bigint(20)      not null                   comment '工艺路线ID(关联qxx_pro_route)',
  process_id                  bigint(20)      not null                   comment '工序ID(关联qxx_pro_process)',
  process_code                varchar(64)     default null               comment '工序编码',
  process_name                varchar(255)    default null               comment '工序名称',
  process_type                varchar(50)     default 'INTERNAL'         comment '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  order_num                   int(4)          default 1                  comment '工序序号(1,2,3...)',
  next_process_id             bigint(20)      default null               comment '下一道工序ID',
  next_process_code           varchar(64)     default null               comment '下一道工序编码',
  next_process_name           varchar(255)    default null               comment '下一道工序名称',
  link_type                   varchar(64)     default 'SS'               comment '与下一道工序关系:SS-顺序,FS-并行',
  default_pre_time            int(11)         default 0                  comment '默认准备时长(分钟)',
  default_suf_time            int(11)         default 0                  comment '默认等待时长(分钟)',
  color_code                  char(7)         default '#00AEF3'          comment '甘特图显示颜色(16进制)',
  key_flag                    varchar(64)     default 'N'                comment '是否关键工序:Y-是,N-否',
  is_check                    char(1)         default 'N'                comment '是否检验工序:Y-是,N-否(检验工序需质检)',
  -- 外发工序扩展
  is_outsource                char(1)         default '0'                comment '是否外发工序(1-是,0-否)',
  vendor_id                   bigint(20)      default null               comment '外协厂ID(关联qxx_md_vendor,外发工序指定)',
  vendor_code                 varchar(64)     default null               comment '外协厂编码',
  vendor_name                 varchar(255)    default null               comment '外协厂名称',
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工艺路线-工序组成表';


-- ----------------------------
-- 7、产品与工艺路线关联表
-- 用途: 将产品物料与工艺路线关联，定义产品的标准生产流程
-- ----------------------------
drop table if exists qxx_pro_route_product;
create table qxx_pro_route_product (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  route_id                    bigint(20)      not null                   comment '工艺路线ID(关联qxx_pro_route)',
  item_id                     bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code                   varchar(64)     not null                   comment '产品物料编码',
  item_name                   varchar(255)    not null                   comment '产品物料名称',
  specification               varchar(500)    default null               comment '规格型号',
  unit_of_measure             varchar(64)     not null                   comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  quantity                    int(11)         default 1                  comment '基准生产数量(工艺路线的标准批量)',
  production_time             decimal(12,2)   default 1.00               comment '标准生产用时',
  time_unit_type              varchar(64)     default 'MINUTE'           comment '时间单位:SECOND-秒,MINUTE-分钟,HOUR-小时,DAY-天',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品与工艺路线关联表';


-- ----------------------------
-- 8、工艺路线产品BOM表
-- 用途: 定义各工序需要用到的物料及用量，与工序ID强关联
-- ----------------------------
drop table if exists qxx_pro_route_product_bom;
create table qxx_pro_route_product_bom (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  route_id                    bigint(20)      not null                   comment '工艺路线ID(关联qxx_pro_route)',
  process_id                  bigint(20)      not null                   comment '工序ID(关联qxx_pro_process,指定该物料在哪个工序投入)',
  product_id                  bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item,成品/半成品)',
  item_id                     bigint(20)      not null                   comment 'BOM物料ID(关联qxx_md_item,原料/辅料)',
  item_code                   varchar(64)     not null                   comment 'BOM物料编码',
  item_name                   varchar(255)    not null                   comment 'BOM物料名称',
  specification               varchar(500)    default null               comment '规格型号',
  unit_of_measure             varchar(64)     not null                   comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  quantity                    decimal(12,2)   default 1.00               comment '用料比例(每个成品/半成品消耗的物料数量)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工艺路线产品BOM表';


-- ============================================================
-- 2.5、工序参数模版（四层参数体系）
-- ============================================================

-- ----------------------------
-- 8.1、工序参数模版定义表 (L1)
-- 用途: 定义每种工序有哪些参数，是参数元数据的"字典"
-- 设计: EAV(行即参数) — 每个参数一行，不同工序参数完全不同
-- 印刷: 色数/印刷机型号/油墨品牌/印刷速度/色差标准值/文件版本号
-- 分切: 来源卷号/分切宽度规格/分切长度/刀设备编号数
-- 覆膜: 膜类型(光膜/哑膜)/膜厚/覆膜机型号/张力参数/速度
-- 烫金: 烫金版编号/烫金面积/金箔型号/温度/压力参数
-- 制袋: 袋型/袋口宽/袋高/底宽/手提绳类型/打孔规格/责任人
-- ----------------------------
drop table if exists qxx_pro_param_template;
create table qxx_pro_param_template (
  template_id       bigint(20)    not null auto_increment  comment '模版ID',
  factory_id        bigint(20)    not null                 comment '工厂ID(关联qxx_md_factory)',
  process_id        bigint(20)    not null                 comment '工序ID(关联qxx_pro_process)',
  param_code        varchar(64)   not null                 comment '参数编码(英文标识,同一工序下唯一)',
  param_name        varchar(128)  not null                 comment '参数名称(中文显示名)',
  param_group       varchar(64)   default 'PROCESS'        comment '参数分组:MACHINE-设备参数,PROCESS-工艺参数,MATERIAL-材料参数,QUALITY-质量参数,PRODUCT-产品规格参数',
  param_type        varchar(32)   not null default 'VARCHAR' comment '参数值类型:INT,DECIMAL,VARCHAR,ENUM,DATE,BOOLEAN',
  unit              varchar(32)   default null             comment '单位(如mm,μm,℃,m/min,mm²,kg)',
  enum_values       varchar(500)  default null             comment '枚举可选值(JSON数组,param_type=ENUM时必填,如["光膜","哑膜"])',
  default_value     varchar(255)  default null             comment '默认值(新建路线工序时自动填充)',
  min_value         decimal(14,4) default null             comment '最小值(超出时触发偏差预警)',
  max_value         decimal(14,4) default null             comment '最大值(超出时触发偏差预警)',
  sort_order        int(4)        default 1                comment '排序号(同一工序下参数显示顺序)',
  is_required       char(1)       default 'Y'              comment '是否必填(Y-是,N-否)',
  is_report_visible char(1)       default 'Y'              comment '报工时是否显示(Y-是=操作工可见可填,N-否=仅工艺设计时可见)',
  enable_flag       char(1)       default '1'              comment '是否启用(1-是,0-否)',
  remark            varchar(500)  default ''               comment '备注',
  create_by         varchar(64)   default ''               comment '创建者',
  create_time       datetime      default current_timestamp comment '创建时间',
  update_by         varchar(64)   default ''               comment '更新者',
  update_time       datetime      default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (template_id),
  unique key uk_process_param (factory_id, process_id, param_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工序参数模版定义表';


-- ----------------------------
-- 8.2、路线产品工序标准参数值表 (L2)
-- 用途: 产品级标准参数值，挂在 route_product 上，不同产品即使共享路线也可不同参数值
-- 创建路线产品关联时从模版(L1)复制默认值，工艺工程师修改为该产品的标准值
-- 与 route_product_bom 同层：都是产品-路线级别的"标准配方"
-- ----------------------------
drop table if exists qxx_pro_route_process_param;
create table qxx_pro_route_process_param (
  record_id         bigint(20)    not null auto_increment  comment '记录ID',
  factory_id        bigint(20)    not null                 comment '工厂ID(关联qxx_md_factory)',
  route_product_id  bigint(20)    not null                 comment '路线产品ID(关联qxx_pro_route_product.record_id)',
  process_id        bigint(20)    not null                 comment '工序ID(关联qxx_pro_process,冗余便于按工序查询)',
  template_id       bigint(20)    not null                 comment '参数模版ID(关联qxx_pro_param_template)',
  param_value       varchar(500)  default null             comment '标准参数值(统一存字符串,前端按param_type校验)',
  remark            varchar(500)  default ''               comment '备注',
  create_by         varchar(64)   default ''               comment '创建者',
  create_time       datetime      default current_timestamp comment '创建时间',
  update_by         varchar(64)   default ''               comment '更新者',
  update_time       datetime      default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id),
  unique key uk_route_product_param (route_product_id, template_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '路线产品工序标准参数值表';


-- ----------------------------
-- 8.3、工单工序参数值表 (L3)
-- 用途: 创建工单时从路线标准(L2)复制，计划员可按本单需求调整
-- 设计: standard_value(快照锁定) + adjusted_value(独立修改)，追溯清晰
-- 与 workorder_bom 同层：单向下行复制，可改不回流
-- ----------------------------
drop table if exists qxx_pro_workorder_param;
create table qxx_pro_workorder_param (
  record_id         bigint(20)    not null auto_increment  comment '记录ID',
  factory_id        bigint(20)    not null                 comment '工厂ID(关联qxx_md_factory)',
  workorder_id      bigint(20)    not null                 comment '工单ID(关联qxx_pro_workorder)',
  route_product_id  bigint(20)    not null                 comment '路线产品ID(关联qxx_pro_route_product,追溯来源)',
  template_id       bigint(20)    not null                 comment '参数模版ID(关联qxx_pro_param_template)',
  standard_value    varchar(500)  default null             comment '路线标准值(快照,从L2复制锁定)',
  adjusted_value    varchar(500)  default null             comment '工单调整值(计划员修改后的值,为null则沿用standard_value)',
  remark            varchar(500)  default ''               comment '调整原因说明',
  create_by         varchar(64)   default ''               comment '创建者',
  create_time       datetime      default current_timestamp comment '创建时间',
  update_by         varchar(64)   default ''               comment '更新者',
  update_time       datetime      default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id),
  unique key uk_workorder_param (workorder_id, route_product_id, template_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工单工序参数值表';


-- ----------------------------
-- 8.4、报工实际参数值表 (L4)
-- 用途: 操作工报工时填写实际参数值，与工单参数(L3)对比→偏差分析
-- 设计: 通过 template_id 获取取值约束(min/max/enum)，实际值超出范围→is_deviation='Y'
-- ----------------------------
drop table if exists qxx_pro_feedback_param;
create table qxx_pro_feedback_param (
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
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '报工实际参数值表';


-- ============================================================
-- 三、生产排产与报工
-- ============================================================

-- ----------------------------
-- 9、生产任务/排产表
-- 用途: 将工单指派到具体工作站/设备进行排产，甘特图可视化排程
-- 排产扩展: machine_code机台号,setup_duration调机时长,unit_duration单位耗时,offline_qty下机个数
-- ----------------------------
drop table if exists qxx_pro_task;
create table qxx_pro_task (
  task_id                     bigint(20)      not null auto_increment    comment '任务ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  task_code                   varchar(64)     not null                   comment '任务编码',
  task_name                   varchar(255)    not null                   comment '任务名称',
  workorder_id                bigint(20)      not null                   comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code              varchar(64)     not null                   comment '生产工单编码',
  workorder_name              varchar(255)    not null                   comment '工单名称',
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     not null                   comment '工作站编码',
  workstation_name            varchar(255)    not null                   comment '工作站名称',
  route_id                    bigint(20)      not null                   comment '工艺路线ID(关联qxx_pro_route)',
  route_code                  varchar(64)     default null               comment '工艺路线编码',
  process_id                  bigint(20)      not null                   comment '工序ID(关联qxx_pro_process)',
  process_code                varchar(64)     default null               comment '工序编码',
  process_name                varchar(255)    default null               comment '工序名称',
  item_id                     bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code                   varchar(64)     not null                   comment '产品物料编码',
  item_name                   varchar(255)    not null                   comment '产品物料名称',
  specification               varchar(500)    default null               comment '规格型号',
  unit_of_measure             varchar(64)     not null                   comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  quantity                    decimal(14,2)   default 1.00  not null     comment '排产数量',
  quantity_produced           decimal(14,2)   default 0.00               comment '已生产数量',
  quantity_qualified          decimal(14,2)   default 0.00               comment '合格品累计数量',
  quantity_unqualified        decimal(14,2)   default 0.00               comment '不合格品累计数量',
  quantity_changed            decimal(14,2)   default 0.00               comment '调整数量',
  -- 客户信息
  client_id                   bigint(20)      default null               comment '客户ID(关联qxx_md_client)',
  client_code                 varchar(64)     default null               comment '客户编码',
  client_name                 varchar(255)    default null               comment '客户名称',
  client_nick                 varchar(255)    default null               comment '客户简称',
  -- 排产扩展（纸袋/礼品盒通用）
  machine_code                varchar(64)     default null               comment '机台号/设备编号(排产到具体机器)',
  setup_duration              int(11)         default 0                  comment '调机时长(分钟)',
  unit_duration               decimal(10,2)   default 0.00               comment '单位耗时(分钟,纸袋=制袋耗时/礼品盒=成型耗时)',
  offline_qty                 int(11)         default 0                  comment '下机个数(每批次离线数量)',
  -- 排产时间
  start_time                  datetime        default current_timestamp  comment '计划开始时间',
  duration                    int(11)         default 1                  comment '计划生产时长(分钟)',
  end_time                    datetime        default null               comment '计划完成时间',
  color_code                  char(7)         default '#00AEF3'          comment '甘特图显示颜色(16进制)',
  request_date                datetime        default null               comment '需求日期(客户交期)',
  finish_date                 datetime        default null               comment '实际完成日期',
  cancel_date                 datetime        default null               comment '取消日期',
  -- 外协信息
  vendor_id                   bigint(20)      default null               comment '外协供应商ID(关联qxx_md_vendor,外发工序时填写)',
  vendor_code                 varchar(64)     default null               comment '外协供应商编码',
  outsource_factory_id        bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  status                      varchar(64)     default 'NORMAL'           comment '生产状态:PREPARE-待排产,NORMAL-正常,PRODUCING-生产中,COMPLETED-已完成,PAUSED-暂停,CANCEL-取消',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (task_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产任务/排产表';


-- ----------------------------
-- 10、报工记录表
-- ⭐核心扩展: feedback_type区分自制/外发代填/外发直填, 外协厂信息, 外协领料单关联
-- 工序产出数量 + 合格/不合格/工废/料废数量
-- ----------------------------
drop table if exists qxx_pro_feedback;
create table qxx_pro_feedback (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
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
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
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
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '报工记录表';

-- ----------------------------
-- 10.1、报工物料消耗表
-- 用途: 记录报工时的实际物料消耗，新增报工时从工单BOM(qxx_pro_workorder_bom)自动填入默认值
-- ----------------------------
drop table if exists qxx_pro_feedback_consume;
create table qxx_pro_feedback_consume (
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
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '报工物料消耗表';

-- ----------------------------
-- 11、上下工记录表
-- 用途: 记录操作工的上工/下工时间，用于工时统计和效率分析
-- ----------------------------
drop table if exists qxx_pro_workrecord;
create table qxx_pro_workrecord (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  user_id                     bigint(20)      not null                   comment '用户ID',
  user_name                   varchar(64)     default null               comment '用户名',
  nick_name                   varchar(128)    default null               comment '用户昵称',
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(128)    default null               comment '工作站名称',
  task_id                     bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code                   varchar(64)     default null               comment '生产任务编码',
  operation_flag              char(1)         not null                   comment '操作类型:ON-上工,OFF-下工',
  operation_time              datetime        default null               comment '操作时间',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '上下工记录表';


-- ----------------------------
-- 14、用户工作站绑定表
-- 用途: 记录用户与工作站的绑定关系，控制操作工只能在绑定的工作站上操作
-- ----------------------------
drop table if exists qxx_pro_user_workstation;
create table qxx_pro_user_workstation (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
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
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '用户工作站绑定表';


-- ============================================================
-- 四、安灯呼叫系统
-- ============================================================

-- ----------------------------
-- 15、安灯呼叫配置表
-- 用途: 定义安灯呼叫的原因、级别、处置人，用于产线异常快速响应
-- ----------------------------
drop table if exists qxx_pro_andon_config;
create table qxx_pro_andon_config (
  config_id                   bigint(20)      not null auto_increment    comment '配置ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  andon_reason                varchar(500)    not null                   comment '呼叫原因(如:设备故障/缺料/质量问题/堵料)',
  andon_level                 varchar(64)     not null                   comment '紧急级别:LEVEL1-紧急(停产),LEVEL2-重要(影响效率),LEVEL3-一般(需协助)',
  handler_role_id             bigint(20)      default null               comment '默认处置角色ID',
  handler_role_name           varchar(128)    default null               comment '默认处置角色名称',
  handler_user_id             bigint(20)      default null               comment '默认处置人ID',
  handler_user_name           varchar(64)     default null               comment '默认处置人用户名',
  handler_nick_name           varchar(64)     default null               comment '默认处置人昵称',
  enable_flag                 char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (config_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '安灯呼叫配置表';


-- ----------------------------
-- 16、安灯呼叫记录表
-- 用途: 产线异常时操作工通过安灯系统呼叫支援的实际记录
-- ----------------------------
drop table if exists qxx_pro_andon_record;
create table qxx_pro_andon_record (
  record_id                   bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(128)    default null               comment '工作站名称',
  user_id                     bigint(20)      not null                   comment '呼叫用户ID',
  user_name                   varchar(64)     default null               comment '呼叫用户名',
  nick_name                   varchar(128)    default null               comment '呼叫用户昵称',
  workorder_id                bigint(20)      default null               comment '关联生产工单ID(关联qxx_pro_workorder)',
  workorder_code              varchar(64)     default null               comment '关联生产工单编码',
  workorder_name              varchar(255)    default null               comment '关联生产工单名称',
  process_id                  bigint(20)      default null               comment '关联工序ID(关联qxx_pro_process)',
  process_code                varchar(64)     default null               comment '关联工序编码',
  process_name                varchar(255)    default null               comment '关联工序名称',
  machinery_id                bigint(20)      default null               comment '关联设备ID(关联qxx_dv_machinery)',
  machinery_code              varchar(64)     default null               comment '关联设备编码',
  machinery_name              varchar(255)    default null               comment '关联设备名称',
  andon_reason                varchar(500)    not null                   comment '呼叫原因',
  andon_level                 varchar(64)     default 'LEVEL3'           comment '紧急级别:LEVEL1-紧急,LEVEL2-重要,LEVEL3-一般',
  handle_time                 datetime        default null               comment '处置完成时间',
  handler_user_id             bigint(20)      default null               comment '处置人ID',
  handler_user_name           varchar(64)     default null               comment '处置人用户名',
  handler_nick_name           varchar(64)     default null               comment '处置人昵称',
  status                      varchar(64)     default 'ACTIVE'           comment '处置状态:ACTIVE-呼叫中,HANDLING-处置中,CLOSED-已关闭',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '安灯呼叫记录表';


-- ============================================================
-- 五、流转卡管理
-- ============================================================

-- ----------------------------
-- 17、流转卡表
-- 用途: ⭐流转单跟踪，记录工序间物料/半成品的流转信息，实现生产全过程追溯
-- 支持流转卡赋码(条码/二维码)
-- ----------------------------
drop table if exists qxx_pro_card;
create table qxx_pro_card (
  card_id                     bigint(20)      not null auto_increment    comment '流转卡ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  card_code                   varchar(64)     default null               comment '流转卡编码(唯一)',
  workorder_id                bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code              varchar(64)     default null               comment '生产工单编码',
  workorder_name              varchar(255)    default null               comment '生产工单名称',
  task_id                     bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code                   varchar(64)     default null               comment '生产任务编码',
  batch_code                  varchar(64)     default null               comment '生产批次号',
  item_id                     bigint(20)      default null               comment '产品物料ID(关联qxx_md_item)',
  item_code                   varchar(64)     not null                   comment '产品物料编码',
  item_name                   varchar(255)    not null                   comment '产品物料名称',
  specification               varchar(500)    default null               comment '规格型号',
  unit_of_measure             varchar(64)     not null                   comment '主单位编码',
  unit_name                   varchar(64)     default null               comment '主单位名称',
  barcode_url                 varchar(255)    default null               comment '流转卡赋码地址(条码/二维码图片URL)',
  quantity_transfered         decimal(12,2)   default null               comment '流转数量(本流转卡承载的数量)',
  current_process_id          bigint(20)      default null               comment '当前工序ID(关联qxx_pro_process,记录流转卡所在工序)',
  current_process_name        varchar(255)    default null               comment '当前工序名称',
  status                      varchar(64)     default null               comment '流转卡状态:ACTIVE-流转中,COMPLETED-已完工,SCRAPPED-已报废',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (card_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '流转卡表';


-- ----------------------------
-- 18、流转卡工序信息表
-- 用途: 记录流转卡在每道工序的投入/产出/操作人/工作站信息
-- 支持外发工序的流转跟踪
-- ----------------------------
drop table if exists qxx_pro_card_process;
create table qxx_pro_card_process (
  record_id                   bigint(20)      not null auto_increment    comment '流水ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  card_id                     bigint(20)      not null                   comment '流转卡ID(关联qxx_pro_card)',
  card_code                   varchar(64)     default null               comment '流转卡编码',
  seq_num                     int(11)         default 1                  comment '工序序号(第几道工序)',
  process_id                  bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  process_code                varchar(64)     default null               comment '工序编码',
  process_name                varchar(255)    default null               comment '工序名称',
  process_type                varchar(50)     default 'INTERNAL'         comment '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  input_time                  datetime        default null               comment '进入工序时间(上道工序完成时间)',
  output_time                 datetime        default null               comment '出工序时间(本道工序完成时间)',
  quantity_input              decimal(12,2)   default null               comment '投入数量',
  quantity_output             decimal(12,2)   default null               comment '产出数量',
  quantity_unqualified        decimal(12,2)   default null               comment '不合格品数量',
  -- 工作站信息
  workstation_id              bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code            varchar(64)     default null               comment '工作站编码',
  workstation_name            varchar(128)    default null               comment '工作站名称',
  -- 操作人信息
  user_id                     bigint(20)      not null                   comment '操作人用户ID',
  user_name                   varchar(64)     default null               comment '操作人用户名',
  nick_name                   varchar(128)    default null               comment '操作人昵称',
  -- 外发工序扩展
  vendor_id                   bigint(20)      default null               comment '外协厂ID(关联qxx_md_vendor,外发工序专用)',
  vendor_code                 varchar(64)     default null               comment '外协厂编码',
  vendor_name                 varchar(255)    default null               comment '外协厂名称',
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  -- 质检信息
  ipqc_id                     bigint(20)      default null               comment '过程检验单ID(关联qxx_qc_ipqc)',
  ipqc_code                   varchar(64)     default null               comment '过程检验单编码',
  feedback_id                 bigint(20)      default null               comment '报工记录ID(关联qxx_pro_feedback)',
  issue_detail_id             bigint(20)      default null               comment '领料明细ID(关联qxx_wm_issue_detail,建立card↔原料批次直接追溯)',
  remark                      varchar(500)    default ''                 comment '备注',
  create_by                   varchar(64)     default ''                 comment '创建者',
  create_time                 datetime        default current_timestamp  comment '创建时间',
  update_by                   varchar(64)     default ''                 comment '更新者',
  update_time                 datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '流转卡工序信息表';


-- ============================================================
-- 六、物料追溯
-- ============================================================

-- ----------------------------
-- 19、物料谱系追溯表
-- 用途: ⭐记录所有物料转移的父子关系（投料/产出/分切/合并），实现正向追溯(原料→成品)和反向追溯(成品→原料)
--
-- === 追溯闭环 ===
--
--   正向追溯（原料 → 成品）：
--   ROLL_DETAIL(卷号RX-001) → [ISSUE] → CARD → [PRODUCE] → BATCH(成品批次B-001) → 客户
--
--   反向追溯（成品 → 原料）：
--   BATCH(成品B-001) → [PRODUCE] → CARD → [ISSUE] → BATCH/ROLL_DETAIL(原料) → 供应商
--
--   分切追溯：
--   ROLL_DETAIL(母卷) → [SLIT] → ROLL_DETAIL(子卷1,子卷2,...)
--      ↑ parent_roll_id 自引用(qxx_wm_roll_detail)
--      ↑ 本表也记录一条 SLIT trace（双保险）
--
-- === 数据写入时机 ===
--
--   投料(ISSUE):        领料单过账 → INSERT trace(parent=BATCH→child=CARD)
--   产出(PRODUCE):      最后工序报工 → INSERT trace(parent=CARD→child=BATCH成品)
--   分切(SLIT):         分切工序报工 → INSERT trace(parent=母卷→child=子卷)
--   流转卡拆分(MERGE):   拆卡 → INSERT trace(parent=原卡→child=子卡)
--
-- === 与 card_process.issue_detail_id 的关系 ===
--   card_process.issue_detail_id → 快速追溯某工序的领料来源（单步查询）
--   material_trace → 完整谱系图（递归查询），双通道满足不同精度需求
-- ----------------------------
drop table if exists qxx_pro_material_trace;
create table qxx_pro_material_trace (
  trace_id          bigint(20)      not null auto_increment    comment '追溯记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  trace_type        varchar(32)     not null                   comment '追溯类型:ISSUE-投料消耗,PRODUCE-生产产出,SLIT-分切,MERGE-合并,ADJUST-调整',
  -- 父节点（来源）
  parent_type       varchar(32)     not null                   comment '父类型:BATCH/ROLL_DETAIL/MATERIAL_STOCK/CARD',
  parent_id         bigint(20)      not null                   comment '父记录ID',
  -- 子节点（去向）
  child_type        varchar(32)     not null                   comment '子类型:BATCH/ROLL_DETAIL/MATERIAL_STOCK/CARD',
  child_id          bigint(20)      not null                   comment '子记录ID',
  -- 数量与单位
  quantity          decimal(14,4)   not null                   comment '转移数量',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  -- 业务关联
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  card_id           bigint(20)      default null               comment '流转卡ID(关联qxx_pro_card)',
  card_process_id   bigint(20)      default null               comment '流转卡工序ID(关联qxx_pro_card_process)',
  issue_id          bigint(20)      default null               comment '领料单ID(关联qxx_wm_issue_header)',
  issue_detail_id   bigint(20)      default null               comment '领料明细ID(关联qxx_wm_issue_detail)',
  feedback_id       bigint(20)      default null               comment '报工记录ID(关联qxx_pro_feedback)',
  transaction_id    bigint(20)      default null               comment '库存事务ID(关联qxx_wm_transaction)',
  process_id        bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  trace_time        datetime        default current_timestamp  comment '追溯时间(业务发生时间)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (trace_id),
  key idx_parent (factory_id, parent_type, parent_id),
  key idx_child (factory_id, child_type, child_id),
  key idx_workorder (workorder_id),
  key idx_card (card_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料谱系追溯表';
