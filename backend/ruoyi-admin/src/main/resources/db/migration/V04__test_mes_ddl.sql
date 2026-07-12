-- test MES DDL for integration tests

-- from mes-md.sql
CREATE TABLE IF NOT EXISTS qxx_md_factory (
  factory_id        bigint(20)      not null auto_increment    comment '工厂ID',
  factory_code      varchar(64)     not null                   comment '工厂编码(如SX-圣享,WL-万隆)',
  factory_name      varchar(255)    not null                   comment '工厂全称',
  short_name        varchar(64)     default null               comment '工厂简称',
  address           varchar(500)    default null               comment '工厂地址',
  contact           varchar(64)     default null               comment '工厂负责人',
  phone             varchar(20)     default null               comment '联系电话',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',

  key idx_factory_id (factory_id),
  primary key (factory_id),
  unique key uk_factory_code (factory_code)
) engine=innodb auto_increment=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工厂定义表';

-- from mes-md.sql
CREATE TABLE IF NOT EXISTS qxx_md_item (
  item_id           bigint(20)      not null auto_increment    comment '产品物料ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_code         varchar(64)     not null                   comment '产品物料编码(唯一)',
  item_name         varchar(255)    not null                   comment '产品物料名称',
  specification     varchar(500)    default null               comment '规格型号描述',
  unit_of_measure   varchar(64)     not null                   comment '主单位编码(如PCS-个,ROLL-卷,KG-千克,TON-吨,M-米)',
  unit_name         varchar(64)     not null                   comment '主单位名称',
  unit2             varchar(64)     default null               comment '辅助单位编码(如ROLL-卷/TON-吨,纸袋/礼品盒通用)',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  conversion_rate   decimal(10,4)   default 1.0000             comment '主单位→辅助单位换算率(如1卷=0.697吨)',
  item_type_id      bigint(20)      default 0                  comment '物料类型ID(关联qxx_md_item_type,变体继承父产品)',
  item_type_code    varchar(64)     default ''                 comment '物料类型编码(变体继承父产品,后端带出)',
  item_type_name    varchar(255)    default ''                 comment '物料类型名称(变体继承父产品,后端带出)',
  parent_id         bigint(20)      default 0                  comment '父产品ID(0=顶层SPU/原料,非0=变体,parent_id>0时item_type_id/code/name继承父产品)',
  -- 通用成品属性（纸袋和礼品盒共用）
  product_size      varchar(100)    default null               comment '产品尺寸(长*宽*高mm),如254*127*330mm',
  package_spec      varchar(100)    default null               comment '装箱规格(XX个/箱),如250个/箱',
  printing_req      varchar(500)    default null               comment '印刷要求描述,如1色满版黑印刷',
  -- 行业专用属性见子表: qxx_md_item_attr_paper / _paper_bag / _gift_box
  -- 库存控制字段
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  safe_stock_flag   char(1)         default '0' not null       comment '是否设置安全库存(1-是,0-否)',
  min_stock         decimal(14,4)   default 0.0000             comment '安全库存最低量(主单位)',
  max_stock         decimal(14,4)   default 0.0000             comment '安全库存最高量(主单位)',
  alert_stock       decimal(14,4)   default 0.0000             comment '预警库存量(低于此值触发预警)',
  high_value        char(1)         default '0' not null       comment '是否高价值物资(1-是,0-否)',
  batch_flag        char(1)         default '1' not null       comment '是否启用批次管理(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (item_id),
  unique key uk_item_code (item_code),
  key idx_factory_id (factory_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料产品表';

-- from mes-md.sql
CREATE TABLE IF NOT EXISTS qxx_md_item_type (
  item_type_id      bigint(20)      not null auto_increment    comment '产品物料类型ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_type_code    varchar(64)     not null                   comment '产品物料类型编码',
  item_type_name    varchar(255)    not null                   comment '产品物料类型名称',
  parent_type_id    bigint(20)      default 0 not null         comment '父类型ID(0表示根节点,MySQL 8.0递归CTE查子树)',
  item_or_product   varchar(20)     not null                   comment '产品物料标识:RAW-原料,SEMI-半成品,FINISHED-成品,AUXILIARY-辅料,PACK-包材',
  order_num         int(11)         default 1                  comment '同级排序号',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',

  key idx_factory_id (factory_id),
  primary key (item_type_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料产品分类表';

-- from mes-md.sql
CREATE TABLE IF NOT EXISTS qxx_md_workshop (
  workshop_id       bigint(20)      not null auto_increment    comment '车间ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workshop_code     varchar(64)     not null                   comment '车间编码(如PRINT-印刷车间,BAG-制袋车间)',
  workshop_name     varchar(255)    not null                   comment '车间名称',
  address           varchar(500)    default null               comment '车间位置/地址',
  manager           varchar(64)     default null               comment '车间负责人',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (workshop_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '车间管理表';

-- from mes-md.sql
CREATE TABLE IF NOT EXISTS qxx_md_workstation (
  workstation_id    bigint(20)      not null auto_increment    comment '工作站ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workstation_code  varchar(64)     not null                   comment '工作站编码(如PRINT-01/BAG-01)',
  workstation_name  varchar(255)    not null                   comment '工作站名称(如1号印刷机/2号全自动制袋机)',
  workshop_id       bigint(20)      not null                   comment '所属车间ID(关联qxx_md_workshop)',
  workstation_type  varchar(50)     default null               comment '工作站类型:PRINT-印刷机,BAG_AUTO-全自动制袋机,BAG_SEMI-半自动制袋机,OTHER-其他',
  process_id        bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  process_type      varchar(50)     default null               comment '工序类型(冗余字段,INTERNAL-自制/OUTSOURCE-外发/SLITTING-分切)',
  capacity          int(11)         default 0                  comment '单位时间产能(个/小时)',
  status            varchar(20)     default 'IDLE'             comment '当前状态:IDLE-空闲,RUNNING-运行中,MAINTENANCE-保养中,BREAKDOWN-故障',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_process_id (process_id),
  primary key (workstation_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工作站管理表';

-- from mes-wm.sql
CREATE TABLE IF NOT EXISTS qxx_wm_warehouse (
  warehouse_id      bigint(20)      not null auto_increment    comment '仓库ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  warehouse_code    varchar(64)     not null                   comment '仓库编码(唯一)',
  warehouse_name    varchar(255)    not null                   comment '仓库名称',
  warehouse_type    varchar(50)     default null               comment '仓库类型:RAW-原料仓,FINISHED-成品仓,AUX-辅料仓,LINE-线边库,TEMP-临时仓',
  address           varchar(500)    default null               comment '仓库位置/地址',
  area              decimal(14,2)   default 0.00               comment '仓库面积(平方米)',
  charge            varchar(64)     default null               comment '仓库负责人',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (warehouse_id),
  unique key uk_warehouse_code (warehouse_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '仓库表';

-- from mes-wm.sql
CREATE TABLE IF NOT EXISTS qxx_wm_storage_location (
  location_id       bigint(20)      not null auto_increment    comment '库区ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  location_code     varchar(64)     not null                   comment '库区编码(唯一)',
  location_name     varchar(255)    not null                   comment '库区名称',
  warehouse_id      bigint(20)      not null                   comment '仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  area              decimal(14,2)   default 0.00               comment '库区面积(平方米)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (location_id),
  unique key uk_location_code (location_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '库区表';

-- from mes-pro.sql
CREATE TABLE IF NOT EXISTS qxx_pro_process (
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

-- from mes-pro.sql
CREATE TABLE IF NOT EXISTS qxx_pro_route (
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

-- from mes-tm.sql
CREATE TABLE IF NOT EXISTS qxx_tm_tool (
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

-- from mes-tm.sql
CREATE TABLE IF NOT EXISTS qxx_tm_tool_type (
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

