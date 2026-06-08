-- ============================================================
-- 企小侠文化纸盒MES系统 — 仓储管理模块(wm)表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_wm_ (Warehouse Management 仓储管理)
-- 说明: 仓库/库区/库位、入库/出库/调拨/盘点/外协/条码/装箱等完整仓库生命周期
--       纸张行业扩展: 双单位(卷数+重量)、物流追踪字段
--       单据模式: Header-Line-Detail 三层结构
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- 一、仓库基础数据
-- ============================================================

-- ----------------------------
-- 1、仓库表
-- ----------------------------
drop table if exists qxx_wm_warehouse;
create table qxx_wm_warehouse (
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

-- ----------------------------
-- 2、库区表
-- ----------------------------
drop table if exists qxx_wm_storage_location;
create table qxx_wm_storage_location (
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

-- ----------------------------
-- 3、库位表
-- ----------------------------
drop table if exists qxx_wm_storage_area;
create table qxx_wm_storage_area (
  area_id           bigint(20)      not null auto_increment    comment '库位ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  area_code         varchar(64)     not null                   comment '库位编码(唯一)',
  area_name         varchar(255)    not null                   comment '库位名称',
  location_id       bigint(20)      not null                   comment '库区ID(关联qxx_wm_storage_location)',
  location_code     varchar(64)     default null               comment '库区编码',
  location_name     varchar(255)    default null               comment '库区名称',
  warehouse_id      bigint(20)      not null                   comment '仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  max_volume        decimal(14,2)   default 0.00               comment '最大容积',
  max_weight        decimal(14,2)   default 0.00               comment '最大承重(吨)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (area_id),
  unique key uk_area_code (area_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '库位表';

-- ----------------------------
-- 4、库存记录表
-- 纸张行业扩展: 双单位库存（主单位+辅助单位数量）
--
-- === 库存唯一性（双重保障）===
-- A. 应用层：INSERT 前先 SELECT 查重，查到则 UPDATE 数量（UPSERT 语义）
-- B. 数据库层：uk_stock 唯一索引兜底，防止并发/绕过应用层的重复插入
--
-- 唯一键 = factory_id + item_id + batch_id + warehouse_id + vendor_id + workorder_id + quality_status
-- batch_id/vendor_id/workorder_id 用 0 而非 NULL（MySQL 唯一索引不认 NULL）
--
-- 示例：
--   原料（纸张）= factory_id=1 + item_id=100 + batch_id=5 + warehouse_id=10 + vendor_id=3 + workorder_id=0 + quality_status=NORMAL
--   成品（纸袋）= factory_id=1 + item_id=200 + batch_id=8 + warehouse_id=20 + vendor_id=0 + workorder_id=15 + quality_status=NORMAL
--   辅料（胶水）= factory_id=1 + item_id=300 + batch_id=0 + warehouse_id=30 + vendor_id=0 + workorder_id=0 + quality_status=NORMAL
-- ----------------------------
drop table if exists qxx_wm_material_stock;
create table qxx_wm_material_stock (
  material_stock_id bigint(20)      not null auto_increment    comment '库存记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '主单位编码',
  unit_name         varchar(64)     not null                   comment '主单位名称',
  -- 纸张行业双单位扩展
  unit2             varchar(64)     default null               comment '辅助单位编码(纸张行业:ROLL-卷/TON-吨)',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  conversion_rate   decimal(10,4)   default 1.0000             comment '主单位→辅助单位换算率',
  quantity_onhand   decimal(14,4)   default 0.0000             comment '现有库存量(主单位)',
  quantity_onhand2  decimal(14,4)   default 0.0000             comment '现有库存量(辅助单位)',
  quantity_available decimal(14,4)  default 0.0000             comment '可用库存量(主单位,扣减已分配)',
  -- 批次与库存信息
  batch_id          bigint(20)      default 0 not null           comment '批次ID(关联qxx_wm_batch,0=无批次管理)',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID(关联qxx_wm_storage_location)',
  location_code     varchar(64)     default null               comment '库区编码',
  location_name     varchar(255)    default null               comment '库区名称',
  area_id           bigint(20)      default null               comment '库位ID(关联qxx_wm_storage_area)',
  area_code         varchar(64)     default null               comment '库位编码',
  area_name         varchar(255)    default null               comment '库位名称',
  vendor_id         bigint(20)      default 0 not null          comment '供应商ID(关联qxx_md_vendor,0=不适用)',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  workorder_id      bigint(20)      default 0 not null          comment '生产工单ID(关联qxx_pro_workorder,0=不适用)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  -- 纸卷明细见 qxx_wm_roll_detail 表
  expire_date       datetime        default null               comment '有效期至',
  lot_number        varchar(64)     default null               comment '生产批号',
  quality_status    varchar(50)     default 'NORMAL'           comment '质量状态:NORMAL-正常,HOLD-冻结,REJECT-不合格,SCRAP-报废',
  status            varchar(50)     default 'NORMAL'           comment '库存状态:NORMAL-正常,ALERT-预警,DEPLETED-耗尽(辅料使用)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  unique key uk_stock (factory_id, item_id, batch_id, warehouse_id, vendor_id, workorder_id, quality_status),
  primary key (material_stock_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '库存记录表';

-- ----------------------------
-- 5、库存事务表
-- 用途: 记录所有库存变动的审计日志（谁在何时因什么原因将何种物料从哪移到哪）
-- ----------------------------
drop table if exists qxx_wm_transaction;
create table qxx_wm_transaction (
  transaction_id    bigint(20)      not null auto_increment    comment '事务ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  transaction_type  varchar(50)     not null                   comment '事务类型:RECEIPT-入库,ISSUE-出库,TRANSFER-调拨,RETURN-退货,ADJUST-调整,SPLIT-分切',
  source_doc_type   varchar(64)     not null                   comment '来源单据类型(如wm_item_recpt)',
  source_doc_id     bigint(20)      not null                   comment '来源单据ID',
  source_doc_code   varchar(64)     default null               comment '来源单据编码',
  source_line_id    bigint(20)      default null               comment '来源单据行ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '变动数量(正=入库,负=出库)',
  -- 双单位扩展
  unit2             varchar(64)     default null               comment '辅助单位编码',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  quantity2         decimal(14,4)   default 0.0000             comment '变动数量(辅助单位)',
  batch_id          bigint(20)      default null               comment '批次ID(关联qxx_wm_batch)',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  vendor_id         bigint(20)      default null               comment '供应商ID(关联qxx_md_vendor)',
  client_id         bigint(20)      default null               comment '客户ID(关联qxx_md_client)',
  related_transaction_id bigint(20) default null               comment '关联事务ID(调拨时使用)',
  transaction_time  datetime        default current_timestamp  comment '事务时间',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (transaction_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '库存事务表';

-- ----------------------------
-- 6、批次记录表
-- 用途：记录物料批次及其追踪属性。批次身份 = 属性组合（非批次号本身），同物料+同属性=同一批次。
--
-- === 批次号生成时机 ===
--
-- 原料（RAW）：
--   ⏰ 入库时生成。仓库收货员填追踪属性（供应商/采购单/到货日期）→ 系统查重 → 匹配则复用，无则生成。
--   触发：qxx_wm_item_recpt 提交确认
--
-- 成品/半成品（FINISHED / SEMI）：
--   ⏰ 排产时生成。计划员拆分流转卡时一并创建批次 → 流转卡带着 batch_code 走完所有工序 → 完工入库时 batch 关闭。
--   触发：qxx_pro_card 创建时（排产页面）
--
-- === 完整生命周期 ===
--
--   原料批次：                                    成品批次：
--   ────────                                    ────────
--   入库单提交                                   排产（拆流转卡）
--     │                                            │
--     ▼                                            ▼
--   ① 读 qxx_md_item_batch_config                ① 生成 batch_code
--   ② 查重（同item+同追踪属性？）                   ② INSERT qxx_wm_batch (status=ACTIVE)
--      → 有 → 复用已有 batch_code                  ③ qxx_pro_card.batch_code = batch_code
--      → 无 → ③                                            │
--   ③ INSERT qxx_wm_batch (status=ACTIVE)                 │  流转卡走完各工序
--   ④ batch_code 关联到入库行                               │  印刷→制袋→贴绳→包装
--     │                                                    │
--     ▼                                                    ▼
--   领料出库引用 batch_code                            最后工序报工完成
--   消耗/退料都追溯到这个批次                                │
--                                                         ▼
--                                                     成品入库 qxx_wm_product_recpt
--                                                        │
--                                                        ▼
--                                                     UPDATE batch: status=CLOSED
--                                                        recpt_date = 入库日期
--
-- === 批次号 = 流水号，批次身份 = 属性组合 ===
-- 入库员/计划员不需要手动编批次号，只需填好追踪属性，系统自动匹配或生成。
-- 同物料 + 同属性组合 → 同一批次。返单生产时自动复用历史批次号。
-- ----------------------------
drop table if exists qxx_wm_batch;
create table qxx_wm_batch (
  batch_id          bigint(20)      not null auto_increment    comment '批次ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  batch_code        varchar(64)     not null                   comment '批次编码(唯一)',
  batch_name        varchar(255)    default null               comment '批次名称',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  produce_date      datetime        default null               comment '生产日期',
  expire_date       datetime        default null               comment '有效期至',
  recpt_date        datetime        default null               comment '入库日期',
  vendor_id         bigint(20)      default null               comment '供应商ID',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  workorder_id      bigint(20)      default null               comment '生产工单ID',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  quality_status    varchar(50)     default 'NORMAL'           comment '质量状态:NORMAL-正常,HOLD-冻结,REJECT-不合格',
  status            varchar(50)     default 'ACTIVE'           comment '批次状态:ACTIVE-活跃,CLOSED-关闭,EXPIRED-过期',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (batch_id),
  unique key uk_batch_code (batch_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '批次记录表';

-- ============================================================
-- 二、采购入库流程
-- ============================================================

-- ----------------------------
-- 7、到货通知单表
-- ----------------------------
drop table if exists qxx_wm_arrival_notice;
create table qxx_wm_arrival_notice (
  notice_id         bigint(20)      not null auto_increment    comment '通知单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  notice_code       varchar(64)     not null                   comment '通知单编码',
  notice_name       varchar(255)    default null               comment '通知单名称',
  source_doc_type   varchar(64)     default null               comment '来源单据类型(pur_order-采购订单)',
  source_doc_id     bigint(20)      default null               comment '来源单据ID(关联qxx_pur_order)',
  source_doc_code   varchar(64)     default null               comment '来源单据编码',
  vendor_id         bigint(20)      not null                   comment '供应商ID(关联qxx_md_vendor)',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  arrival_date      datetime        default null               comment '预计到货日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '到货总数量',
  iqc_id            bigint(20)      default null               comment '关联质检单ID(关联qxx_qc_iqc)',
  iqc_code          varchar(64)     default null               comment '关联质检单编码',
  status            varchar(50)     default 'PENDING'          comment '状态:PENDING-待收货,RECEIVING-收货中,RECEIVED-已收货,CANCEL-取消',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (notice_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '到货通知单表';

-- ----------------------------
-- 8、到货通知单行表
-- ----------------------------
drop table if exists qxx_wm_arrival_notice_line;
create table qxx_wm_arrival_notice_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  notice_id         bigint(20)      not null                   comment '通知单ID(关联qxx_wm_arrival_notice)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_arrival  decimal(14,4)   default 0.0000             comment '到货数量',
  quantity_recpt    decimal(14,4)   default 0.0000             comment '已入库数量',
  quantity_return   decimal(14,4)   default 0.0000             comment '退货数量',
  batch_code        varchar(64)     default null               comment '供应商批次号',
  iqc_id            bigint(20)      default null               comment '关联质检行ID',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '到货通知单行表';

-- ----------------------------
-- 9、物料入库单表(Header)
-- ----------------------------
drop table if exists qxx_wm_item_recpt;
create table qxx_wm_item_recpt (
  recpt_id          bigint(20)      not null auto_increment    comment '入库单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_code        varchar(64)     not null                   comment '入库单编码(自动生成)',
  recpt_name        varchar(255)    default null               comment '入库单名称',
  notice_id         bigint(20)      default null               comment '到货通知单ID(关联qxx_wm_arrival_notice)',
  notice_code       varchar(64)     default null               comment '到货通知单编码',
  vendor_id         bigint(20)      default null               comment '供应商ID(关联qxx_md_vendor)',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  warehouse_id      bigint(20)      not null                   comment '入库仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  recpt_date        datetime        default current_timestamp  comment '入库日期',
  recpt_type        varchar(50)     default 'PURCHASE'         comment '入库类型:PURCHASE-采购入库,PRODUCE-生产入库,OUTSOURCE-外协入库,MISC-杂项入库,RETURN-退货入库',
  total_quantity    decimal(14,4)   default 0.0000             comment '入库总数量(主单位)',
  iqc_id            bigint(20)      default null               comment '质检单ID(关联qxx_qc_iqc)',
  iqc_code          varchar(64)     default null               comment '质检单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账,CANCEL-取消',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (recpt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料入库单表';

-- ----------------------------
-- 10、物料入库单行表(Line)
-- ----------------------------
drop table if exists qxx_wm_item_recpt_line;
create table qxx_wm_item_recpt_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_id          bigint(20)      not null                   comment '入库单ID(关联qxx_wm_item_recpt)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '主单位编码',
  unit_name         varchar(64)     not null                   comment '主单位名称',
  unit2             varchar(64)     default null               comment '辅助单位编码',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  conversion_rate   decimal(10,4)   default 1.0000             comment '换算率',
  quantity_recpt    decimal(14,4)   default 0.0000             comment '入库数量(主单位)',
  quantity_recpt2   decimal(14,4)   default 0.0000             comment '入库数量(辅助单位,如重量吨)',
  batch_id          bigint(20)      default null               comment '批次ID(关联qxx_wm_batch)',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  expire_date       datetime        default null               comment '有效期至',
  notice_line_id    bigint(20)      default null               comment '到货通知行ID(关联qxx_wm_arrival_notice_line)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料入库单行表';

-- ----------------------------
-- 11、物料入库单明细表(Detail)
-- 用途: 记录入库到具体库存记录的对应关系
-- ----------------------------
drop table if exists qxx_wm_item_recpt_detail;
create table qxx_wm_item_recpt_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_id          bigint(20)      not null                   comment '入库单ID(关联qxx_wm_item_recpt)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_item_recpt_line)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '主单位编码',
  unit_name         varchar(64)     not null                   comment '主单位名称',
  unit2             varchar(64)     default null               comment '辅助单位编码',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '入库数量(主单位)',
  quantity2         decimal(14,4)   default 0.0000             comment '入库数量(辅助单位)',
  batch_id          bigint(20)      default null               comment '批次ID(关联qxx_wm_batch)',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  -- 纸卷明细见 qxx_wm_roll_detail 表
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '物料入库单明细表';

-- ----------------------------
-- 12、采购退货单表(Header)
-- ----------------------------
drop table if exists qxx_wm_rt_vendor;
create table qxx_wm_rt_vendor (
  rt_id             bigint(20)      not null auto_increment    comment '退货单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_code           varchar(64)     not null                   comment '退货单编码',
  rt_name           varchar(255)    default null               comment '退货单名称',
  recpt_id          bigint(20)      default null               comment '原入库单ID(关联qxx_wm_item_recpt)',
  recpt_code        varchar(64)     default null               comment '原入库单编码',
  vendor_id         bigint(20)      not null                   comment '供应商ID(关联qxx_md_vendor)',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  warehouse_id      bigint(20)      not null                   comment '退货仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  rt_date           datetime        default current_timestamp  comment '退货日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '退货总数量',
  rqc_id            bigint(20)      default null               comment '退料质检单ID(关联qxx_qc_rqc)',
  rqc_code          varchar(64)     default null               comment '退料质检单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (rt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '采购退货单表';

-- ----------------------------
-- 13、采购退货单行表
-- ----------------------------
drop table if exists qxx_wm_rt_vendor_line;
create table qxx_wm_rt_vendor_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_id             bigint(20)      not null                   comment '退货单ID(关联qxx_wm_rt_vendor)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_rt       decimal(14,4)   default 0.0000             comment '退货数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '采购退货单行表';

-- ----------------------------
-- 14、采购退货单明细表
-- ----------------------------
drop table if exists qxx_wm_rt_vendor_detail;
create table qxx_wm_rt_vendor_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_id             bigint(20)      not null                   comment '退货单ID(关联qxx_wm_rt_vendor)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_rt_vendor_line)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '退货数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '采购退货单明细表';

-- ============================================================
-- 三、生产领料/退料流程
-- ============================================================

-- ----------------------------
-- 15、生产领料单头表
-- ----------------------------
drop table if exists qxx_wm_issue_header;
create table qxx_wm_issue_header (
  issue_id          bigint(20)      not null auto_increment    comment '领料单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_code        varchar(64)     not null                   comment '领料单编码',
  issue_name        varchar(255)    default null               comment '领料单名称',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  task_id           bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code         varchar(64)     default null               comment '生产任务编码',
  workstation_id    bigint(20)      default null               comment '工作站ID(关联qxx_md_workstation)',
  workstation_code  varchar(64)     default null               comment '工作站编码',
  workstation_name  varchar(255)    default null               comment '工作站名称',
  warehouse_id      bigint(20)      not null                   comment '发料仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  issue_date        datetime        default current_timestamp  comment '领料日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '领料总数量',
  issue_type        varchar(50)     default 'PRODUCE'          comment '领料类型:PRODUCE-生产领料,MISC-杂项领料',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (issue_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产领料单头表';

-- ----------------------------
-- 16、生产领料单行表
-- ----------------------------
drop table if exists qxx_wm_issue_line;
create table qxx_wm_issue_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_id          bigint(20)      not null                   comment '领料单ID(关联qxx_wm_issue_header)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_issue    decimal(14,4)   default 0.0000             comment '申请领料数量',
  quantity_recpt    decimal(14,4)   default 0.0000             comment '实际发料数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产领料单行表';

-- ----------------------------
-- 17、生产领料单明细表
-- ----------------------------
drop table if exists qxx_wm_issue_detail;
create table qxx_wm_issue_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_id          bigint(20)      not null                   comment '领料单ID(关联qxx_wm_issue_header)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_issue_line)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '领料数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  -- 纸卷明细见 qxx_wm_roll_detail 表
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产领料单明细表';

-- ----------------------------
-- 18、生产退料单头表
-- ----------------------------
drop table if exists qxx_wm_rt_issue;
create table qxx_wm_rt_issue (
  rt_id             bigint(20)      not null auto_increment    comment '退料单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_code           varchar(64)     not null                   comment '退料单编码',
  rt_name           varchar(255)    default null               comment '退料单名称',
  issue_id          bigint(20)      default null               comment '原领料单ID(关联qxx_wm_issue_header)',
  issue_code        varchar(64)     default null               comment '原领料单编码',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  workstation_id    bigint(20)      default null               comment '工作站ID(关联qxx_md_workstation)',
  warehouse_id      bigint(20)      not null                   comment '退入仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  rt_date           datetime        default current_timestamp  comment '退料日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '退料总数量',
  rqc_id            bigint(20)      default null               comment '退料质检单ID(关联qxx_qc_rqc)',
  rqc_code          varchar(64)     default null               comment '退料质检单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (rt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产退料单头表';

-- ----------------------------
-- 19、生产退料单行表
-- ----------------------------
drop table if exists qxx_wm_rt_issue_line;
create table qxx_wm_rt_issue_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_id             bigint(20)      not null                   comment '退料单ID(关联qxx_wm_rt_issue)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_rt       decimal(14,4)   default 0.0000             comment '退料数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产退料单行表';

-- ----------------------------
-- 20、生产退料单明细表
-- ----------------------------
drop table if exists qxx_wm_rt_issue_detail;
create table qxx_wm_rt_issue_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_id             bigint(20)      not null                   comment '退料单ID(关联qxx_wm_rt_issue)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_rt_issue_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '退料数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产退料单明细表';

-- ============================================================
-- 四、工序产出/产品入库流程
-- ============================================================

-- ----------------------------
-- 21、产品产出记录表(Header)
-- 用途: 记录生产报工后的产品产出，入线边库
-- ----------------------------
drop table if exists qxx_wm_product_produce;
create table qxx_wm_product_produce (
  produce_id        bigint(20)      not null auto_increment    comment '产出记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  produce_code      varchar(64)     not null                   comment '产出记录编码',
  produce_name      varchar(255)    default null               comment '产出记录名称',
  workorder_id      bigint(20)      not null                   comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  task_id           bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code         varchar(64)     default null               comment '生产任务编码',
  workstation_id    bigint(20)      default null               comment '工作站ID(关联qxx_md_workstation)',
  workstation_code  varchar(64)     default null               comment '工作站编码',
  workstation_name  varchar(255)    default null               comment '工作站名称',
  process_id        bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  process_code      varchar(64)     default null               comment '工序编码',
  process_name      varchar(255)    default null               comment '工序名称',
  feedback_id       bigint(20)      default null               comment '报工记录ID(关联qxx_pro_feedback)',
  warehouse_id      bigint(20)      not null                   comment '线边库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '线边库编码',
  warehouse_name    varchar(255)    default null               comment '线边库名称',
  produce_date      datetime        default current_timestamp  comment '产出日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '产出总数量',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (produce_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品产出记录表';

-- ----------------------------
-- 22、产品产出记录行表
-- ----------------------------
drop table if exists qxx_wm_product_produce_line;
create table qxx_wm_product_produce_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  produce_id        bigint(20)      not null                   comment '产出记录ID(关联qxx_wm_product_produce)',
  item_id           bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '产品物料编码',
  item_name         varchar(255)    not null                   comment '产品物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_produce  decimal(14,4)   default 0.0000             comment '产出数量',
  quantity_qualified decimal(14,4)  default 0.0000             comment '合格数量',
  quantity_unqualified decimal(14,4) default 0.0000            comment '不合格数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '线边库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品产出记录行表';

-- ----------------------------
-- 23、产品产出记录明细表
-- ----------------------------
drop table if exists qxx_wm_product_produce_detail;
create table qxx_wm_product_produce_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  produce_id        bigint(20)      not null                   comment '产出记录ID(关联qxx_wm_product_produce)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_product_produce_line)',
  item_id           bigint(20)      not null                   comment '产品物料ID',
  item_code         varchar(64)     not null                   comment '产品物料编码',
  item_name         varchar(255)    not null                   comment '产品物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '产出数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '线边库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品产出记录明细表';

-- ----------------------------
-- 24、产品入库单表(Header)
-- 用途: 线边库产品转正式成品库
-- ----------------------------
drop table if exists qxx_wm_product_recpt;
create table qxx_wm_product_recpt (
  recpt_id          bigint(20)      not null auto_increment    comment '入库单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_code        varchar(64)     not null                   comment '入库单编码',
  recpt_name        varchar(255)    default null               comment '入库单名称',
  produce_id        bigint(20)      default null               comment '产出记录ID(关联qxx_wm_product_produce)',
  produce_code      varchar(64)     default null               comment '产出记录编码',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  source_warehouse_id bigint(20)    default null               comment '来源仓库(线边库)ID',
  warehouse_id      bigint(20)      not null                   comment '目标仓库(成品库)ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '目标仓库编码',
  warehouse_name    varchar(255)    default null               comment '目标仓库名称',
  recpt_date        datetime        default current_timestamp  comment '入库日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '入库总数量(个数)',
  total_box         int(11)         default 0                  comment '入库总箱数(纸张行业)',
  ipqc_id           bigint(20)      default null               comment '过程质检单ID(关联qxx_qc_ipqc)',
  ipqc_code         varchar(64)     default null               comment '过程质检单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (recpt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品入库单表';

-- ----------------------------
-- 25、产品入库单行表
-- ----------------------------
drop table if exists qxx_wm_product_recpt_line;
create table qxx_wm_product_recpt_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_id          bigint(20)      not null                   comment '入库单ID(关联qxx_wm_product_recpt)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_recpt    decimal(14,4)   default 0.0000             comment '入库数量(个数)',
  quantity_box      int(11)         default 0                  comment '入库箱数(纸张行业)',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '目标仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品入库单行表';

-- ----------------------------
-- 26、产品入库单明细表
-- ----------------------------
drop table if exists qxx_wm_product_recpt_detail;
create table qxx_wm_product_recpt_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_id          bigint(20)      not null                   comment '入库单ID(关联qxx_wm_product_recpt)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_product_recpt_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '入库数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '产品入库单明细表';

-- ============================================================
-- 五、销售发货流程 (纸张行业扩展: 物流追踪字段)
-- ============================================================

-- ----------------------------
-- 27、发货通知单表
-- ----------------------------
drop table if exists qxx_wm_sales_notice;
create table qxx_wm_sales_notice (
  notice_id         bigint(20)      not null auto_increment    comment '通知单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  notice_code       varchar(64)     not null                   comment '通知单编码',
  notice_name       varchar(255)    default null               comment '通知单名称',
  client_id         bigint(20)      not null                   comment '客户ID(关联qxx_md_client)',
  client_code       varchar(64)     default null               comment '客户编码',
  client_name       varchar(255)    default null               comment '客户名称',
  client_order_code varchar(64)     default null               comment '客户订单号(如PO#ORD66003MT)',
  salesperson       varchar(64)     default null               comment '业务员(如陈仁林/陈丽丽)',
  notice_date       datetime        default current_timestamp  comment '通知日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '发货总数量',
  total_box         int(11)         default 0                  comment '总箱数',
  oqc_id            bigint(20)      default null               comment '出货检验单ID(关联qxx_qc_oqc)',
  oqc_code          varchar(64)     default null               comment '出货检验单编码',
  shipping_address  varchar(500)    default null               comment '收货地址',
  receiver_name     varchar(64)     default null               comment '收货人',
  receiver_tel      varchar(64)     default null               comment '收货人电话',
  status            varchar(50)     default 'PENDING'          comment '状态:PENDING-待发货,SHIPPING-发货中,SHIPPED-已发货,CANCEL-取消',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (notice_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '发货通知单表';

-- ----------------------------
-- 28、发货通知单行表
-- ----------------------------
drop table if exists qxx_wm_sales_notice_line;
create table qxx_wm_sales_notice_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  notice_id         bigint(20)      not null                   comment '通知单ID(关联qxx_wm_sales_notice)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_notice   decimal(14,4)   default 0.0000             comment '发货通知数量',
  quantity_shipped  decimal(14,4)   default 0.0000             comment '已发货数量',
  quantity_box      int(11)         default 0                  comment '箱数',
  box_spec          varchar(100)    default null               comment '箱规(如250个/箱)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '发货通知单行表';

-- ----------------------------
-- 29、销售出库单表(Header)
-- 纸张行业扩展: 完整物流追踪字段
-- ----------------------------
drop table if exists qxx_wm_product_sales;
create table qxx_wm_product_sales (
  sales_id          bigint(20)      not null auto_increment    comment '出库单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  sales_code        varchar(64)     not null                   comment '出库单编码',
  sales_name        varchar(255)    default null               comment '出库单名称',
  notice_id         bigint(20)      default null               comment '发货通知单ID(关联qxx_wm_sales_notice)',
  notice_code       varchar(64)     default null               comment '发货通知单编码',
  client_id         bigint(20)      not null                   comment '客户ID(关联qxx_md_client)',
  client_code       varchar(64)     default null               comment '客户编码',
  client_name       varchar(255)    default null               comment '客户名称',
  client_order_code varchar(64)     default null               comment '客户订单号(PO号)',
  salesperson       varchar(64)     default null               comment '业务员',
  warehouse_id      bigint(20)      not null                   comment '出货仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  sales_date        datetime        default current_timestamp  comment '出库日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '出库总数量',
  total_box         int(11)         default 0                  comment '总箱数',
  total_volume      decimal(14,4)   default 0.0000             comment '总体积(立方米)',
  total_weight      decimal(14,4)   default 0.0000             comment '总重量(吨)',
  -- 物流追踪字段 (圣享工厂特有需求)
  logistics_company varchar(100)    default null               comment '物流公司名称',
  tracking_no       varchar(100)    default null               comment '快递/物流单号',
  logistics_fee     decimal(10,2)   default 0.00               comment '物流费用(元)',
  shipping_address  varchar(500)    default null               comment '收货详细地址',
  receiver_name     varchar(64)     default null               comment '收货人姓名',
  receiver_tel      varchar(64)     default null               comment '收货人电话',
  pallet_flag       char(1)         default '0'                comment '是否打托盘(1-是,0-否)',
  box_label         varchar(255)    default null               comment '箱唛/唛头描述',
  sales_type        varchar(50)     default 'FOREIGN'          comment '销售类型:FOREIGN-外贸,DOMESTIC-内贸,SPOT-现货',
  oqc_id            bigint(20)      default null               comment '出货检验单ID(关联qxx_qc_oqc)',
  oqc_code          varchar(64)     default null               comment '出货检验单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账,SHIPPED-已发货',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (sales_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '销售出库单表';

-- ----------------------------
-- 30、销售出库单行表
-- ----------------------------
drop table if exists qxx_wm_product_sales_line;
create table qxx_wm_product_sales_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  sales_id          bigint(20)      not null                   comment '出库单ID(关联qxx_wm_product_sales)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_sales    decimal(14,4)   default 0.0000             comment '出库数量(个数)',
  quantity_box      int(11)         default 0                  comment '出库箱数',
  box_spec          varchar(100)    default null               comment '装箱规格(如250个/箱)',
  box_length        decimal(10,2)   default 0.00               comment '箱长(cm)',
  box_width         decimal(10,2)   default 0.00               comment '箱宽(cm)',
  box_height        decimal(10,2)   default 0.00               comment '箱高(cm)',
  volume            decimal(14,4)   default 0.0000             comment '体积(立方米)',
  weight            decimal(14,4)   default 0.0000             comment '重量(吨)',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '销售出库单行表';

-- ----------------------------
-- 31、销售出库单明细表
-- ----------------------------
drop table if exists qxx_wm_product_sales_detail;
create table qxx_wm_product_sales_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  sales_id          bigint(20)      not null                   comment '出库单ID(关联qxx_wm_product_sales)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_product_sales_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '出库数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '销售出库单明细表';

-- ----------------------------
-- 32、销售退货单表
-- ----------------------------
drop table if exists qxx_wm_rt_sales;
create table qxx_wm_rt_sales (
  rt_id             bigint(20)      not null auto_increment    comment '退货单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rt_code           varchar(64)     not null                   comment '退货单编码',
  rt_name           varchar(255)    default null               comment '退货单名称',
  sales_id          bigint(20)      default null               comment '原销售出库单ID(关联qxx_wm_product_sales)',
  sales_code        varchar(64)     default null               comment '原销售出库单编码',
  client_id         bigint(20)      not null                   comment '客户ID(关联qxx_md_client)',
  client_code       varchar(64)     default null               comment '客户编码',
  client_name       varchar(255)    default null               comment '客户名称',
  warehouse_id      bigint(20)      not null                   comment '退货入库仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  rt_date           datetime        default current_timestamp  comment '退货日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '退货总数量',
  rqc_id            bigint(20)      default null               comment '退料质检单ID(关联qxx_qc_rqc)',
  rqc_code          varchar(64)     default null               comment '退料质检单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (rt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '销售退货单表';

-- ============================================================
-- 六、外协管理流程 (圣享核心业务: 100%带料外发)
-- ============================================================

-- ----------------------------
-- 33、外协领料单头表
-- 用途: 圣享将纸张/半成品/绳子发给外协厂(万隆/吉荣/浩卓/欧诺/圣皓)
-- ----------------------------
drop table if exists qxx_wm_outsource_issue;
create table qxx_wm_outsource_issue (
  issue_id          bigint(20)      not null auto_increment    comment '外协领料单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_code        varchar(64)     not null                   comment '外协领料单编码',
  issue_name        varchar(255)    default null               comment '外协领料单名称',
  vendor_id         bigint(20)      not null                   comment '外协厂ID(关联qxx_md_vendor,vendor_type=OUTSOURCE)',
  vendor_code       varchar(64)     default null               comment '外协厂编码',
  vendor_name       varchar(255)    default null               comment '外协厂名称',
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  outsource_type    varchar(50)     default 'PRODUCE'          comment '外发类型:PRODUCE-外发生产(印刷+制袋),ROPE-外发贴绳,OTHER-其他',
  warehouse_id      bigint(20)      not null                   comment '发料仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  issue_date        datetime        default current_timestamp  comment '发料日期',
  expected_return_date datetime    default null               comment '预计返回日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '发料总数量',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,ISSUED-已发出,IN_PROGRESS-外协生产中,RETURNED-已返回,CLOSED-关闭',
  remark            varchar(500)    default ''                 comment '备注(如:4月21日到纸,4月22日发到欧诺)',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (issue_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '外协领料单头表';

-- ----------------------------
-- 34、外协领料单行表
-- 支持三种物料同时发出: 纸张(原材料)+半成品袋子+绳子(辅料)
-- ----------------------------
drop table if exists qxx_wm_outsource_issue_line;
create table qxx_wm_outsource_issue_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_id          bigint(20)      not null                   comment '外协领料单ID(关联qxx_wm_outsource_issue)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  item_type         varchar(50)     default null               comment '物料类型:RAW-纸张原材料,SEMI-半成品袋子,AUX-辅料(绳子),PACK-包材',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  unit2             varchar(64)     default null               comment '辅助单位编码(纸张双单位)',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  quantity_issue    decimal(14,4)   default 0.0000             comment '发料数量(主单位)',
  quantity_issue2   decimal(14,4)   default 0.0000             comment '发料数量(辅助单位,如重量吨)',
  bundle_count      int(11)         default 0                  comment '卷数/捆数(纸张行业)',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  -- 纸卷明细见 qxx_wm_roll_detail 表
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '外协领料单行表';

-- ----------------------------
-- 35、外协领料单明细表
-- ----------------------------
drop table if exists qxx_wm_outsource_issue_detail;
create table qxx_wm_outsource_issue_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_id          bigint(20)      not null                   comment '外协领料单ID(关联qxx_wm_outsource_issue)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_outsource_issue_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '发料数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '外协领料单明细表';

-- ----------------------------
-- 36、外协入库单表
-- 用途: 外协厂完成加工后，圣享接收返回的成品/半成品
-- ----------------------------
drop table if exists qxx_wm_outsource_recpt;
create table qxx_wm_outsource_recpt (
  recpt_id          bigint(20)      not null auto_increment    comment '外协入库单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_code        varchar(64)     not null                   comment '外协入库单编码',
  recpt_name        varchar(255)    default null               comment '外协入库单名称',
  issue_id          bigint(20)      default null               comment '关联外协领料单ID(关联qxx_wm_outsource_issue)',
  issue_code        varchar(64)     default null               comment '关联外协领料单编码',
  vendor_id         bigint(20)      not null                   comment '外协厂ID(关联qxx_md_vendor)',
  vendor_code       varchar(64)     default null               comment '外协厂编码',
  vendor_name       varchar(255)    default null               comment '外协厂名称',
  outsource_factory_id  bigint(20)      default null               comment '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  warehouse_id      bigint(20)      not null                   comment '入库仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  recpt_date        datetime        default current_timestamp  comment '入库日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '入库总数量',
  total_box         int(11)         default 0                  comment '入库总箱数',
  ipqc_id           bigint(20)      default null               comment '过程质检单ID(关联qxx_qc_ipqc)',
  ipqc_code         varchar(64)     default null               comment '过程质检单编码',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_outsource_factory_id (outsource_factory_id),
  primary key (recpt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '外协入库单表';

-- ----------------------------
-- 37、外协入库单行表
-- ----------------------------
drop table if exists qxx_wm_outsource_recpt_line;
create table qxx_wm_outsource_recpt_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_id          bigint(20)      not null                   comment '外协入库单ID(关联qxx_wm_outsource_recpt)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_recpt    decimal(14,4)   default 0.0000             comment '入库数量',
  quantity_box      int(11)         default 0                  comment '入库箱数',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '外协入库单行表';

-- ----------------------------
-- 38、外协入库单明细表
-- ----------------------------
drop table if exists qxx_wm_outsource_recpt_detail;
create table qxx_wm_outsource_recpt_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_id          bigint(20)      not null                   comment '外协入库单ID(关联qxx_wm_outsource_recpt)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_outsource_recpt_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '入库数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '外协入库单明细表';

-- ============================================================
-- 七、调拨/盘点/条码/装箱/备料/杂项
-- ============================================================

-- ----------------------------
-- 39、调拨转移单表(Header)
-- ----------------------------
drop table if exists qxx_wm_transfer;
create table qxx_wm_transfer (
  transfer_id       bigint(20)      not null auto_increment    comment '调拨单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  transfer_code     varchar(64)     not null                   comment '调拨单编码',
  transfer_name     varchar(255)    default null               comment '调拨单名称',
  source_warehouse_id bigint(20)    not null                   comment '来源仓库ID(关联qxx_wm_warehouse)',
  source_warehouse_code varchar(64) default null               comment '来源仓库编码',
  source_warehouse_name varchar(255) default null              comment '来源仓库名称',
  target_warehouse_id bigint(20)    not null                   comment '目标仓库ID(关联qxx_wm_warehouse)',
  target_warehouse_code varchar(64) default null               comment '目标仓库编码',
  target_warehouse_name varchar(255) default null              comment '目标仓库名称',
  transfer_date     datetime        default current_timestamp  comment '调拨日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '调拨总数量',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (transfer_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '调拨转移单表';

-- ----------------------------
-- 40、调拨转移单行表
-- ----------------------------
drop table if exists qxx_wm_transfer_line;
create table qxx_wm_transfer_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  transfer_id       bigint(20)      not null                   comment '调拨单ID(关联qxx_wm_transfer)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_transfer decimal(14,4)   default 0.0000             comment '调拨数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  source_location_id bigint(20)     default null               comment '来源库区ID',
  source_area_id    bigint(20)      default null               comment '来源库位ID',
  target_location_id bigint(20)     default null               comment '目标库区ID',
  target_area_id    bigint(20)      default null               comment '目标库位ID',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '调拨转移单行表';

-- ----------------------------
-- 41、调拨转移单明细表
-- ----------------------------
drop table if exists qxx_wm_transfer_detail;
create table qxx_wm_transfer_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  transfer_id       bigint(20)      not null                   comment '调拨单ID(关联qxx_wm_transfer)',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_transfer_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '调拨数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  material_stock_id bigint(20)      not null                   comment '库存记录ID(关联qxx_wm_material_stock)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '调拨转移单明细表';

-- ----------------------------
-- 42、盘点方案表
-- ----------------------------
drop table if exists qxx_wm_stock_taking_plan;
create table qxx_wm_stock_taking_plan (
  plan_id           bigint(20)      not null auto_increment    comment '盘点方案ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  plan_code         varchar(64)     not null                   comment '盘点方案编码',
  plan_name         varchar(255)    not null                   comment '盘点方案名称',
  plan_type         varchar(50)     default 'FULL'             comment '盘点类型:FULL-全盘,PARTIAL-抽盘,CYCLE-循环盘点',
  warehouse_id      bigint(20)      default null               comment '盘点仓库ID(关联qxx_wm_warehouse)',
  warehouse_name    varchar(255)    default null               comment '盘点仓库名称',
  plan_date         datetime        default null               comment '计划盘点日期',
  status            varchar(50)     default 'PREPARE'          comment '状态:PREPARE-准备中,TAKING-盘点中,COMPLETED-已完成,ADJUSTED-已调整',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (plan_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '盘点方案表';

-- ----------------------------
-- 43、盘点任务表
-- ----------------------------
drop table if exists qxx_wm_stock_taking;
create table qxx_wm_stock_taking (
  taking_id         bigint(20)      not null auto_increment    comment '盘点任务ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  taking_code       varchar(64)     not null                   comment '盘点任务编码',
  plan_id           bigint(20)      not null                   comment '盘点方案ID(关联qxx_wm_stock_taking_plan)',
  plan_code         varchar(64)     default null               comment '盘点方案编码',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  warehouse_id      bigint(20)      not null                   comment '仓库ID',
  location_id       bigint(20)      default null               comment '库区ID',
  area_id           bigint(20)      default null               comment '库位ID',
  book_quantity     decimal(14,4)   default 0.0000             comment '账面数量',
  actual_quantity   decimal(14,4)   default 0.0000             comment '实际盘点数量',
  difference        decimal(14,4)   default 0.0000             comment '差异数量',
  diff_reason       varchar(500)    default null               comment '差异原因',
  taking_user       varchar(64)     default null               comment '盘点人',
  taking_date       datetime        default null               comment '盘点日期',
  status            varchar(50)     default 'PENDING'          comment '状态:PENDING-待盘点,TAKEN-已盘点,ADJUSTED-已调整',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (taking_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '盘点任务表';

-- ----------------------------
-- 44、条码清单表
-- ----------------------------
drop table if exists qxx_wm_barcode;
create table qxx_wm_barcode (
  barcode_id        bigint(20)      not null auto_increment    comment '条码ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  barcode_code      varchar(255)    not null                   comment '条码内容',
  barcode_type      varchar(50)     default 'PRODUCT'          comment '条码类型:PRODUCT-产品,PACKAGE-装箱,ITEM-物料,PALLET-托盘',
  item_id           bigint(20)      default null               comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     default null               comment '物料编码',
  item_name         varchar(255)    default null               comment '物料名称',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  quantity          decimal(14,4)   default 1.0000             comment '数量',
  unit_of_measure   varchar(64)     default null               comment '单位',
  batch_code        varchar(64)     default null               comment '批次号',
  print_count       int(11)         default 0                  comment '打印次数',
  last_print_time   datetime        default null               comment '最后打印时间',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (barcode_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '条码清单表';

-- ----------------------------
-- 45、装箱单表
-- ----------------------------
drop table if exists qxx_wm_package;
create table qxx_wm_package (
  package_id        bigint(20)      not null auto_increment    comment '装箱单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  package_code      varchar(64)     not null                   comment '装箱单编码',
  package_name      varchar(255)    default null               comment '装箱单名称',
  parent_id         bigint(20)      default 0                  comment '父装箱ID(层级装箱,0=顶层)',
  ancestors         varchar(500)    default null               comment '所有父节点ID',
  package_type      varchar(50)     default 'CARTON'           comment '装箱类型:CARTON-纸箱,PALLET-托盘,CONTAINER-集装箱',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  quantity_per_box  int(11)         default 0                  comment '每箱标准数量(如250个/箱)',
  box_count         int(11)         default 0                  comment '箱数',
  total_quantity    decimal(14,4)   default 0.0000             comment '总数量',
  weight            decimal(14,4)   default 0.0000             comment '重量(吨)',
  length            decimal(10,2)   default 0.00               comment '长(cm)',
  width             decimal(10,2)   default 0.00               comment '宽(cm)',
  height            decimal(10,2)   default 0.00               comment '高(cm)',
  volume            decimal(14,4)   default 0.0000             comment '体积(立方米)',
  box_label         varchar(255)    default null               comment '箱唛/唛头',
  status            varchar(50)     default 'OPEN'             comment '状态:OPEN-开放,CLOSED-关闭,SHIPPED-已发货',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (package_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '装箱单表';

-- ----------------------------
-- 46、装箱明细表
-- ----------------------------
drop table if exists qxx_wm_package_line;
create table qxx_wm_package_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  package_id        bigint(20)      not null                   comment '装箱单ID(关联qxx_wm_package)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '装箱数量',
  batch_id          bigint(20)      default null               comment '批次ID',
  batch_code        varchar(64)     default null               comment '批次编码',
  barcode_code      varchar(255)    default null               comment '条码内容',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '装箱明细表';

-- ----------------------------
-- 47、SN码管理表
-- ----------------------------
drop table if exists qxx_wm_sn;
create table qxx_wm_sn (
  sn_id             bigint(20)      not null auto_increment    comment 'SN ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  sn_code           varchar(255)    not null                   comment 'SN码(序列号,唯一)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  batch_id          bigint(20)      default null               comment '批次ID(关联qxx_wm_batch)',
  batch_code        varchar(64)     default null               comment '批次编码',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  status            varchar(50)     default 'CREATED'          comment '状态:CREATED-已创建,USED-已使用,SCRAP-已报废',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (sn_id),
  unique key uk_sn_code (sn_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = 'SN码管理表';

-- ----------------------------
-- 48、备料通知单表
-- ----------------------------
drop table if exists qxx_wm_materialrequest_notice;
create table qxx_wm_materialrequest_notice (
  notice_id         bigint(20)      not null auto_increment    comment '备料通知单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  notice_code       varchar(64)     not null                   comment '备料通知单编码',
  notice_name       varchar(255)    default null               comment '备料通知单名称',
  workorder_id      bigint(20)      not null                   comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  workstation_id    bigint(20)      default null               comment '工作站ID(关联qxx_md_workstation)',
  warehouse_id      bigint(20)      not null                   comment '仓库ID(关联qxx_wm_warehouse)',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  request_date      datetime        default current_timestamp  comment '需求日期',
  status            varchar(50)     default 'PENDING'          comment '状态:PENDING-待备料,PREPARED-已备料,ISSUED-已发料,CLOSED-关闭',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (notice_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '备料通知单表';

-- ----------------------------
-- 49、杂项入库单表
-- ----------------------------
drop table if exists qxx_wm_misc_recpt;
create table qxx_wm_misc_recpt (
  recpt_id          bigint(20)      not null auto_increment    comment '杂项入库单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  recpt_code        varchar(64)     not null                   comment '杂项入库单编码',
  recpt_name        varchar(255)    default null               comment '杂项入库单名称',
  recpt_type        varchar(50)     default null               comment '入库原因:OPENING-期初导入,ADJUST-盘盈调整,OTHER-其他',
  warehouse_id      bigint(20)      not null                   comment '仓库ID(关联qxx_wm_warehouse)',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  recpt_date        datetime        default current_timestamp  comment '入库日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '入库总数量',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (recpt_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '杂项入库单表';

-- ----------------------------
-- 50、杂项出库单表
-- ----------------------------
drop table if exists qxx_wm_misc_issue;
create table qxx_wm_misc_issue (
  issue_id          bigint(20)      not null auto_increment    comment '杂项出库单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_code        varchar(64)     not null                   comment '杂项出库单编码',
  issue_name        varchar(255)    default null               comment '杂项出库单名称',
  issue_type        varchar(50)     default null               comment '出库原因:ADJUST-盘亏调整,SCRAP-报废,OTHER-其他',
  warehouse_id      bigint(20)      not null                   comment '仓库ID(关联qxx_wm_warehouse)',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  issue_date        datetime        default current_timestamp  comment '出库日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '出库总数量',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (issue_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '杂项出库单表';

-- ============================================================
-- 八、纸张行业特有：纸卷明细
-- ============================================================

-- ----------------------------
-- 51、纸卷明细表
-- 用途：纸张行业特有。每卷纸到货后建立独立记录，追踪每卷的实际规格和消耗状态。
--       解决"50卷纸入库，每卷门幅/克重/长度不同，领料时选哪卷"的问题。
--
-- === 与 qxx_md_item / qxx_wm_material_stock 的关系 ===
-- qxx_md_item          = 标准规格（"应该是什么"）：联盛A2箱板纸，门幅=925mm，克重=120g
-- qxx_wm_roll_detail   = 到货实绩（"实际是什么"）：卷号RX001，实际门幅=923mm，克重=118g，长=3200m
-- qxx_wm_material_stock = 库存聚合（"总共多少"）：联盛A2箱板纸，库存=50卷/34.85吨
--
-- === 生命周期 ===
-- 入库时创建 → 领料时扣减 → 部分消耗标记PARTIAL → 全部消耗标记CONSUMED
-- 一卷一行，永不合并。退料时原卷恢复剩余量。
-- ----------------------------
drop table if exists qxx_wm_roll_detail;
create table qxx_wm_roll_detail (
  roll_id           bigint(20)      not null auto_increment    comment '纸卷明细ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  roll_code         varchar(64)     not null                   comment '纸卷号(唯一,如RX20260608-001,可贴条码)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  -- 源头追溯
  batch_id          bigint(20)      default null               comment '批次ID(关联qxx_wm_batch)',
  batch_code        varchar(64)     default null               comment '批次编码',
  recpt_id          bigint(20)      not null                   comment '入库单ID(关联qxx_wm_item_recpt,哪张入库单收的)',
  recpt_detail_id   bigint(20)      default null               comment '入库明细ID(关联qxx_wm_item_recpt_detail)',
  vendor_id         bigint(20)      default null               comment '供应商ID(关联qxx_md_vendor)',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  vendor_roll_no    varchar(64)     default null               comment '供应商原始卷号(来料时的编号)',
  -- 实际规格（到货实测值，≠物料主数据的标准值）
  actual_width      varchar(20)     default null               comment '实际门幅(mm),如923mm',
  actual_weight_gsm varchar(20)     default null               comment '实际克重(g/㎡),如118g',
  actual_length     decimal(14,2)   default 0.00               comment '实际长度(米)',
  actual_weight     decimal(14,4)   default 0.0000             comment '实际重量(吨),到货过磅值',
  -- 库存追踪（随消耗递减）
  unit_of_measure   varchar(64)     not null                   comment '计量单位(如TON-吨,KG-千克,M-米)',
  original_quantity decimal(14,4)   default 0.0000             comment '原始数量(入库时)',
  remaining_quantity decimal(14,4)  default 0.0000             comment '剩余数量(扣减后)',
  -- 存储位置
  warehouse_id      bigint(20)      not null                   comment '所在仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID(关联qxx_wm_storage_location)',
  area_id           bigint(20)      default null               comment '库位ID(关联qxx_wm_storage_area)',
  material_stock_id bigint(20)      default null               comment '库存记录ID(关联qxx_wm_material_stock,汇总库存行)',
  -- 消耗追踪
  last_issue_id     bigint(20)      default null               comment '最近领料单ID(关联qxx_wm_issue_header)',
  last_workorder_id bigint(20)      default null               comment '最近生产工单ID(关联qxx_pro_workorder)',
  last_workorder_code varchar(64)   default null               comment '最近生产工单编码',
  status            varchar(50)     default 'IN_STOCK'         comment '纸卷状态:IN_STOCK-在库,PARTIAL-部分已用,CONSUMED-已用完,RETURNED-已退回,SCRAPPED-已报废',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (roll_id),
  unique key uk_roll_code (roll_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '纸卷明细表';
