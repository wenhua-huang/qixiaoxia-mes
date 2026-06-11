-- ============================================================
-- 企小侠文化纸盒MES系统 — 基础数据模块(md)表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_md_ (Master Data 基础数据)
-- 说明: 物料/产品/BOM/供应商/客户/车间/工作站/工厂等基础主数据
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、工厂定义表
-- 用途：多工厂/租户隔离的核心表。每个工厂是一个独立的业务单元
--       工厂间可通过外协/采购建立协作关系（跨工厂数据可见由业务层控制）
--
-- === 工厂初始化（平台开发人员直接SQL）===
--
-- ① 建工厂（必须）
--   INSERT INTO qxx_md_factory (factory_code, factory_name, short_name)
--   VALUES ('SX', '圣享纸品有限公司', '圣享');
--
-- ② 建工厂管理员（必须，手机号登录微信小程序）
--   INSERT INTO sys_user (user_name, nick_name, phonenumber, password, factory_id)
--   VALUES ('sx_admin', '圣享管理员', '138xxxx', '$2a$...', @factory_id);
--   INSERT INTO sys_user_role (factory_id, user_id, role_id) VALUES (@factory_id, @user_id, 1);
--   -- 部门、角色、普通员工由管理员登录后自己在UI创建
--
-- === 外协场景：本工厂需要把活外发到另一个工厂 ===
--
-- ③ 把目标工厂加为本工厂的外协供应商（本工厂管理员在UI操作，非平台开发人员）
--    在"新增外协供应商"页面，下拉选择系统工厂（如万隆），系统自动写入 outsource_factory_id：
--   INSERT INTO qxx_md_vendor (vendor_code, vendor_name, vendor_type, factory_id, outsource_factory_id)
--   VALUES ('VEN-WL', '万隆纸品', 'OUTSOURCE', @my_factory_id, @target_factory_id);
--   -- factory_id：本工厂（谁建的这条供应商记录）
--   -- outsource_factory_id：目标工厂（这个供应商对应的系统工厂，对方员工登录后据此看到外协任务）
-- ----------------------------
drop table if exists qxx_md_factory;
create table qxx_md_factory (
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

-- ----------------------------
-- 初始化-工厂定义表数据
-- ----------------------------
insert into qxx_md_factory (factory_id, factory_code, factory_name, short_name, address, contact, phone, enable_flag, remark, create_by, create_time, update_by, update_time)
values (1, 'SX', '圣享工厂', '圣享', null, null, null, '1', '', 'admin', sysdate(), '', sysdate());

-- ----------------------------
-- 2、物料产品表
-- 用途：核心主数据表，管理所有物料/半成品/成品/辅料。支持多行业（纸张/纸袋/礼品盒）。
--       行业专用属性已抽取子表：qxx_md_item_attr_paper / _paper_bag / _gift_box
--       通用属性（产品尺寸/装箱规格/印刷要求）保留在主表。
-- SPU/变体自引用：parent_id=0→顶层SPU/原料；parent_id>0→变体（如 奔趣纸袋←奔趣-彩印红绳）
-- 变体(item_type/code/name)继承父产品，差异字段有值=覆盖/null=继承
--
-- === 变体创建时机（parent_id>0 的行什么时候写入）===
-- 变体由系统/业务员创建，不是物料管理员手动建档：
-- ┌──────────────┬─────────────────────┬──────────────────────────────┐
-- │ 场景          │ 写入时机             │ 操作方式                      │
-- ├──────────────┼─────────────────────┼──────────────────────────────┤
-- │ 物料建档      │ 可自动创建标准变体     │ 系统：INSERT 一条 parent_id=SPU │
-- │              │ 差异字段全=NULL      │ 的变体行，作为"默认规格"        │
-- ├──────────────┼─────────────────────┼──────────────────────────────┤
-- │ 首次客户定制   │ 业务员接单时手动创建   │ 业务员在物料界面"新增变体"       │
-- │              │ 填差异字段（彩印/红绳） │ ① INSERT INTO qxx_md_item      │
-- │              │                     │    parent_id=SPU_ID             │
-- │              │                     │ ② INSERT INTO 行业子表           │
-- │              │                     │    SELECT ... FROM 子表          │
-- │              │                     │    WHERE item_id=SPU_ID          │
-- │              │                     │    → 方案A：复制父产品子表行       │
-- │              │                     │ ③ UPDATE 差异字段               │
-- ├──────────────┼─────────────────────┼──────────────────────────────┤
-- │ 返单（同规格） │ 不创建，复用已有变体    │ 业务员下拉选已有变体 item_id     │
-- └──────────────┴─────────────────────┴──────────────────────────────┘
-- 变体生命周期：首次接单→创建；返单→复用；随产品生命周期长期存在。
-- 与库存纸卷(roll_detail)的区别：变体是产品定义(永久可复用)，纸卷是物理实例(消耗性)。
--
-- === 多行业子表设计 ===
-- 行业专用属性不在主表内联，按行业拆分到独立子表（item_id 1:1）：
--   qxx_md_item_attr_paper      — 纸张原材料属性（门幅/克重/来源/种类）
--   qxx_md_item_attr_paper_bag  — 纸袋成品属性（绳料/口部/底板）
--   qxx_md_item_attr_gift_box   — 礼品盒成品属性（预留）
-- 查询时：主表 + LEFT JOIN 对应行业子表，无需判断 parent_id 回退（方案A：变体创建时已复制父行）。
-- ----------------------------
drop table if exists qxx_md_item;
create table qxx_md_item (
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

-- ----------------------------
-- 2a、物料纸张属性表（物料产品表的行业子表）
-- 用途：存储纸张原材料的行业专用属性。item_id 与 qxx_md_item 1:1。
--       只有物料类型为 RAW-原料 且属于纸张分类时才插入行。
--       变体创建时（方案A）：复制父产品 SPU 的对应子表行 → 再 UPDATE 差异字段。
-- ----------------------------
drop table if exists qxx_md_item_attr_paper;
create table qxx_md_item_attr_paper (
  attr_id           bigint(20)      not null auto_increment    comment '属性ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  paper_width       varchar(20)     default null               comment '纸张门幅(mm),如925mm',
  paper_weight      varchar(20)     default null               comment '纸张克重(g),如120g',
  paper_source      varchar(100)    default null               comment '纸张来源/品牌,如联盛A2/蓝叶-牛卡',
  paper_type        varchar(50)     default null               comment '纸张种类:乌卡/俄卡/箱板纸/白牛皮/TC箱板纸/瑞典赤牛',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (attr_id),
  unique key uk_item_id (item_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料纸张属性表';

-- ----------------------------
-- 2b、物料纸袋成品属性表（物料产品表的行业子表）
-- 用途：存储纸袋成品的行业专用属性。item_id 与 qxx_md_item 1:1。
--       只有物料类型为 FINISHED-成品 且属于纸袋产品时才插入行。
--       变体创建时（方案A）：复制父产品 SPU 的对应子表行 → 再 UPDATE 差异字段。
-- ----------------------------
drop table if exists qxx_md_item_attr_paper_bag;
create table qxx_md_item_attr_paper_bag (
  attr_id           bigint(20)      not null auto_increment    comment '属性ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  rope_spec         varchar(200)    default null               comment '绳料规格要求,如7.5cm间距黄牛皮色圆纸绳',
  mouth_type        varchar(50)     default null               comment '口部提拔:锯齿口/平口/翻口',
  bottom_type       varchar(50)     default null               comment '底板类型:无/灰底白板/加强底板',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (attr_id),
  unique key uk_item_id (item_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料纸袋成品属性表';

-- ----------------------------
-- 2c、物料礼品盒属性表（物料产品表的行业子表，预留）
-- 用途：存储礼品盒成品的行业专用属性。item_id 与 qxx_md_item 1:1。
--       字段待业务方确认后补充。
-- ----------------------------
drop table if exists qxx_md_item_attr_gift_box;
create table qxx_md_item_attr_gift_box (
  attr_id           bigint(20)      not null auto_increment    comment '属性ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  -- TODO: 礼品盒具体属性字段待业务方确认，参考方向：
  -- box_material      varchar(100)  default null             comment '盒身材质:灰板纸/白卡纸/瓦楞纸',
  -- lining_material   varchar(100)  default null             comment '内衬材质:绒布/珍珠棉/丝绸',
  -- cover_type        varchar(50)   default null             comment '盒盖类型:天地盖/翻盖/抽拉式',
  -- surface_treatment varchar(100)  default null             comment '表面处理:覆膜/烫金/UV/压纹',
  -- magnet_flag       char(1)       default '0'              comment '是否带磁吸(1-是,0-否)',
  -- ribbon_spec       varchar(200)  default null             comment '丝带规格',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (attr_id),
  unique key uk_item_id (item_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料礼品盒属性表';

-- ----------------------------
-- 3、物料产品分类表
-- 用途：父子层级分类，如 原料→纸张→箱板纸（通常1~2层，无需物化路径）
-- 查询子树用 MySQL 8.0 递归CTE：
--   WITH RECURSIVE cte AS (
--     SELECT * FROM qxx_md_item_type WHERE item_type_id = ?
--     UNION ALL
--     SELECT t.* FROM qxx_md_item_type t JOIN cte ON t.parent_type_id = cte.item_type_id
--   ) SELECT * FROM cte;
-- ----------------------------
drop table if exists qxx_md_item_type;
create table qxx_md_item_type (
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

-- ----------------------------
-- 4、产品BOM关系表
-- 用途：定义成品→原料的物料清单构成，如一个纸袋=纸张+绳子+胶水+纸箱
-- ----------------------------
drop table if exists qxx_md_product_bom;
create table qxx_md_product_bom (
  bom_id            bigint(20)      not null auto_increment    comment 'BOM行ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '成品物料ID(关联qxx_md_item)',
  bom_item_id       bigint(20)      not null                   comment 'BOM子物料ID(关联qxx_md_item,名称/规格/编码通过JOIN获取)',
  unit_of_measure   varchar(64)     not null                   comment '子物料BOM用量单位(可能≠库存单位,如库存按卷/BOM按kg)',
  quantity          decimal(14,4)   default 1.0000 not null    comment '单个成品消耗子物料数量',
  scrap_rate        decimal(5,4)    default 0.0000             comment '损耗率(如0.05=5%)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注(如绳长/间距等说明)',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (bom_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品BOM关系表';

-- ----------------------------
-- 5、物料批次属性配置表
-- 用途：定义物料入库/生产时**必须填写哪些追踪属性**，不直接生成批次号
-- 批次规则挂在 item_id 上（与 ktg-mes 一致），逐物料配置。同分类不同物料可配不同规则。
--
-- === 批次生成机制（本表 = 规则清单，不是批次号生成器）===
-- 一个 item_id 要么是采购的原料(RAW)，要么是生产的成品(FINISHED)，不会身兼两职。
-- 因此批次生成分为两种场景，各自有不同的生成时机和依据：
--
-- ┌──────────────┬─────────────────────┬────────────────────────────────────┐
-- │ 场景           │ 采购物料(RAW)        │ 成品(FINISHED)                     │
-- ├──────────────┼─────────────────────┼────────────────────────────────────┤
-- │ 批次生成时机    │ 原料到货入库时        │ ⭐ 排产拆分流转卡时                   │
-- │ 触发操作       │ 仓库收货员填入库单     │ 计划员创建流转卡 → 一并生成批次        │
-- │ 典型追踪属性    │ vendor_flag='Y'     │ workorder_flag='Y'               │
-- │              │ po_code_flag='Y'    │ client_flag='Y'                  │
-- │              │ recpt_date_flag='Y' │ co_code_flag='Y'                 │
-- │              │ lot_number_flag='Y' │ produce_date_flag='Y'            │
-- │              │ paper_roll_flag='Y' │ lot_number_flag='Y'              │
-- │              │ paper_width_flag='Y'│ quality_status_flag='Y'          │
-- │ 查重条件       │ item+供应商+采购单    │ item+工单+生产日期                  │
-- │              │ +入库日期+卷号        │ +客户+销售订单                     │
-- │ 批次号格式     │ 系统流水号            │ 系统流水号                         │
-- │              │ BATCH-20260607-001  │ BATCH-20260607-002               │
-- └──────────────┴─────────────────────┴────────────────────────────────────┘
--
-- === 批次生成三步流程 ===
-- ① 验证：读本表配置 → 检查 Y 属性是否都传了值 → N 属性强制置 null
-- ② 查重：按"Y 属性组合"查 qxx_wm_batch 表 → 找到则返回已有批次号（不重复建）
-- ③ 生成：没找到 → 调用编码器生成新 batch_code → INSERT qxx_wm_batch → 生成条码
--    原料：入库时触发（仓库收货员）；成品/半成品：排产拆卡时触发（计划员）
--    详见 mes-wm.sql 的 qxx_wm_batch 表注释
--
-- 关键：批次号是流水号，批次身份是属性组合。同物料+同属性组合=同一批次。
--       入库员/计划员不需要手动编批次号，只需填好追踪属性，系统自动匹配或生成。
-- ----------------------------
drop table if exists qxx_md_item_batch_config;
create table qxx_md_item_batch_config (
  config_id             bigint(20)      not null auto_increment     comment '配置ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id               bigint(20)      not null                    comment '物料ID(关联qxx_md_item)',
  produce_date_flag     char(1)         default '0'                 comment '是否追踪生产日期(1-是,0-否)',
  expire_date_flag      char(1)         default '0'                 comment '是否追踪有效期(1-是,0-否)',
  recpt_date_flag       char(1)         default '0'                 comment '是否追踪入库日期(1-是,0-否)',
  vendor_flag           char(1)         default '0'                 comment '是否追踪供应商(1-是,0-否)',
  client_flag           char(1)         default '0'                 comment '是否追踪客户(1-是,0-否)',
  co_code_flag          char(1)         default '0'                 comment '是否追踪销售订单号(1-是,0-否)',
  po_code_flag          char(1)         default '0'                 comment '是否追踪采购订单号(1-是,0-否)',
  workorder_flag        char(1)         default '0'                 comment '是否追踪生产工单(1-是,0-否)',
  task_flag             char(1)         default '0'                 comment '是否追踪生产任务(1-是,0-否)',
  workstation_flag      char(1)         default '0'                 comment '是否追踪工作站(1-是,0-否)',
  tool_flag             char(1)         default '0'                 comment '是否追踪工具(1-是,0-否)',
  mold_flag             char(1)         default '0'                 comment '是否追踪模具(1-是,0-否)',
  lot_number_flag       char(1)         default '0'                 comment '是否追踪生产批号(1-是,0-否)',
  quality_status_flag   char(1)         default '0'                 comment '是否追踪质量状态(1-是,0-否)',
  paper_roll_flag       char(1)         default '0'                 comment '是否追踪纸卷号(纸张行业可选,非纸张物料=0,1-是,0-否)',
  paper_width_flag      char(1)         default '0'                 comment '是否追踪门幅(纸张行业可选,非纸张物料=0,1-是,0-否)',
  enable_flag           char(1)         default '1'                 comment '是否启用(1-是,0-否)',
  remark                varchar(500)    default ''                  comment '备注',
  create_by             varchar(64)     default ''                  comment '创建者',
  create_time           datetime        default current_timestamp   comment '创建时间',
  update_by             varchar(64)     default ''                  comment '更新者',
  update_time           datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (config_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料批次属性配置表';

-- ----------------------------
-- 6、供应商表
-- 用途：管理原材料供应商和外协加工商，圣享工厂需区分这两种类型
-- ----------------------------
drop table if exists qxx_md_vendor;
create table qxx_md_vendor (
  vendor_id         bigint(20)      not null auto_increment    comment '供应商ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  vendor_code       varchar(64)     not null                   comment '供应商编码(唯一)',
  vendor_name       varchar(255)    not null                   comment '供应商全称',
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  vendor_nick       varchar(255)    default null               comment '供应商简称',
  vendor_en         varchar(255)    default null               comment '供应商英文名称',
  vendor_type       varchar(20)     default 'MATERIAL'         comment '供应商类型:MATERIAL-原材料供应商,OUTSOURCE-外协加工商,BOTH-两者皆是',
  vendor_des        varchar(500)    default null               comment '供应商简介/经营范围',
  vendor_logo       varchar(255)    default null               comment '供应商LOGO图片地址',
  vendor_level      varchar(64)     default null               comment '供应商等级:A-优秀,B-良好,C-一般,D-待评估',
  vendor_score      int(11)         default 0                  comment '供应商评分(0-100)',
  address           varchar(500)    default null               comment '供应商详细地址',
  website           varchar(255)    default null               comment '供应商官网地址',
  email             varchar(255)    default null               comment '供应商企业邮箱',
  tel               varchar(64)     default null               comment '供应商企业电话',
  contact1          varchar(64)     default null               comment '主要联系人姓名',
  contact1_tel      varchar(64)     default null               comment '主要联系人电话',
  contact1_email    varchar(255)    default null               comment '主要联系人邮箱',
  contact2          varchar(64)     default null               comment '备用联系人姓名',
  contact2_tel      varchar(64)     default null               comment '备用联系人电话',
  contact2_email    varchar(255)    default null               comment '备用联系人邮箱',
  credit_code       varchar(64)     default null               comment '统一社会信用代码',
  settlement_type   varchar(50)     default null               comment '结算方式:MONTHLY-月结,CASH-现结,OTHER-其他',
  payment_terms     varchar(100)    default null               comment '付款条件/账期',
  coop_status       varchar(20)     default 'ACTIVE'           comment '合作状态:ACTIVE-合作中,INACTIVE-暂停,PENDING-待审核,STOPPED-终止',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',

  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (vendor_id),
  unique key uk_vendor_code (vendor_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '供应商表';

-- ----------------------------
-- 7、客户表
-- 用途：管理客户信息，圣享按外贸/内贸区分客户群体
-- ----------------------------
drop table if exists qxx_md_client;
create table qxx_md_client (
  client_id         bigint(20)      not null auto_increment    comment '客户ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  client_code       varchar(64)     not null                   comment '客户编码(唯一)',
  client_name       varchar(255)    not null                   comment '客户全称',
  client_nick       varchar(255)    default null               comment '客户简称',
  client_en         varchar(255)    default null               comment '客户英文名称(外贸客户必填)',
  client_type       varchar(20)     default null               comment '客户类型:DOMESTIC-内贸,FOREIGN-外贸,SPOT-现货',
  salesperson       varchar(64)     default null               comment '业务员姓名(如陈仁林/陈丽丽)',
  credit_code       varchar(64)     default null               comment '统一社会信用代码',
  address           varchar(500)    default null               comment '客户地址',
  website           varchar(255)    default null               comment '客户官网',
  email             varchar(255)    default null               comment '客户邮箱',
  tel               varchar(64)     default null               comment '客户电话',
  contact1          varchar(64)     default null               comment '联系人1姓名',
  contact1_tel      varchar(64)     default null               comment '联系人1电话',
  contact1_email    varchar(255)    default null               comment '联系人1邮箱',
  contact2          varchar(64)     default null               comment '联系人2姓名',
  contact2_tel      varchar(64)     default null               comment '联系人2电话',
  contact2_email    varchar(255)    default null               comment '联系人2邮箱',
  shipping_address  varchar(500)    default null               comment '收货地址(可多个,分号分隔)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',

  key idx_factory_id (factory_id),
  primary key (client_id),
  unique key uk_client_code (client_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '客户表';

-- ----------------------------
-- 8、单位管理表
-- 用途：统一管理所有计量单位及主单位换算关系
-- ----------------------------
drop table if exists qxx_md_unit_measure;
create table qxx_md_unit_measure (
  unit_id           bigint(20)      not null auto_increment    comment '单位ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  unit_code         varchar(64)     not null                   comment '单位编码(如PCS/ROLL/KG/TON/M/G)',
  unit_name         varchar(64)     not null                   comment '单位名称(如个/卷/千克/吨/米/克)',
  primary_unit      varchar(64)     default null               comment '所属主单位编码(当本单位是辅助单位时)',
  conversion_rate   decimal(14,6)   default 1.000000           comment '与主单位的换算率(如1吨=1000千克)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',

  key idx_factory_id (factory_id),
  primary key (unit_id),
  unique key uk_unit_code (unit_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '单位管理表';

-- ----------------------------
-- 9、车间管理表
-- 用途：定义工厂车间，圣享有印刷车间、制袋车间
-- ----------------------------
drop table if exists qxx_md_workshop;
create table qxx_md_workshop (
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

-- ----------------------------
-- 10、工作站管理表
-- 用途：定义生产设备/工作单元，圣享的印刷机和制袋机
-- ----------------------------
drop table if exists qxx_md_workstation;
create table qxx_md_workstation (
  workstation_id    bigint(20)      not null auto_increment    comment '工作站ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workstation_code  varchar(64)     not null                   comment '工作站编码(如PRINT-01/BAG-01)',
  workstation_name  varchar(255)    not null                   comment '工作站名称(如1号印刷机/2号全自动制袋机)',
  workshop_id       bigint(20)      not null                   comment '所属车间ID(关联qxx_md_workshop)',
  workstation_type  varchar(50)     default null               comment '工作站类型:PRINT-印刷机,BAG_AUTO-全自动制袋机,BAG_SEMI-半自动制袋机,OTHER-其他',
  process_type      varchar(50)     default null               comment '可执行工序类型:PRINT-印刷,BAG_MAKE-制袋,SLITTING-分切,INSPECT-检验',
  capacity          int(11)         default 0                  comment '单位时间产能(个/小时)',
  status            varchar(20)     default 'IDLE'             comment '当前状态:IDLE-空闲,RUNNING-运行中,MAINTENANCE-保养中,BREAKDOWN-故障',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (workstation_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工作站管理表';

-- ----------------------------
-- 11、工作站-设备关联表
-- 用途：关联工作站与设备台账（一台工作站可挂多个设备）
-- ----------------------------
drop table if exists qxx_md_workstation_machine;
create table qxx_md_workstation_machine (
  record_id         bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workstation_id    bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  machinery_id      bigint(20)      not null                   comment '设备ID(关联qxx_dv_machinery)',
  machinery_code    varchar(64)     default null               comment '设备编码',
  machinery_name    varchar(255)    default null               comment '设备名称',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工作站-设备关联表';

-- ----------------------------
-- 12、工作站-人员关联表
-- 用途：关联工作站与操作人员
-- ----------------------------
drop table if exists qxx_md_workstation_worker;
create table qxx_md_workstation_worker (
  record_id         bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  workstation_id    bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  user_id           bigint(20)      not null                   comment '用户ID(关联sys_user)',
  user_name         varchar(64)     default null               comment '用户姓名',
  role_type         varchar(20)     default 'OPERATOR'         comment '角色:OPERATOR-操作工,SETUP-调机员,INSPECTOR-检验员',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '工作站-人员关联表';

-- ----------------------------
-- 13、产品SOP表(作业指导书)
-- 用途：关联产品的操作指导书附件 暂时不用
-- ----------------------------
drop table if exists qxx_md_product_sop;
create table qxx_md_product_sop (
  sop_id            bigint(20)      not null auto_increment    comment 'SOP ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '产品ID(关联qxx_md_item)',
  sop_name          varchar(255)    not null                   comment 'SOP名称',
  sop_url           varchar(255)    default null               comment 'SOP文件URL',
  order_num         int(11)         default 1                  comment '排序号',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (sop_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品SOP表';

-- ----------------------------
-- 14、产品SIP表(检验指导书)
-- 用途：关联产品的检验标准文件 暂时不用
-- ----------------------------
drop table if exists qxx_md_product_sip;
create table qxx_md_product_sip (
  sip_id            bigint(20)      not null auto_increment    comment 'SIP ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '产品ID(关联qxx_md_item)',
  sip_name          varchar(255)    not null                   comment 'SIP名称',
  sip_url           varchar(255)    default null               comment 'SIP文件URL',
  order_num         int(11)         default 1                  comment '排序号',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (sip_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品SIP表';
