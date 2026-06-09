-- ============================================================
-- 企小侠文化纸盒MES系统 — 采购管理模块(pur)表设计
-- 版本: v1.0
-- 日期: 2026-06-08
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_pur_ (Purchase 采购管理)
-- 说明: 采购订单 + 采购行，纸张行业特有字段（门幅/克重/卷数/吨价）
--       串联到货通知→IQC→入库流程
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1、采购订单头表
-- 用途：管理原材料/辅料/包材的采购订单。纸张行业特有：按吨计价、关联客户订单
-- 流程：申购→审批→下单→到货通知→IQC→入库
-- ----------------------------
drop table if exists qxx_pur_order;
create table qxx_pur_order (
  order_id          bigint(20)      not null auto_increment    comment '采购订单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  order_code        varchar(64)     not null                   comment '采购订单编码(唯一)',
  order_name        varchar(255)    default null               comment '采购订单名称',
  vendor_id         bigint(20)      not null                   comment '供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)',
  vendor_code       varchar(64)     default null               comment '供应商编码',
  vendor_name       varchar(255)    default null               comment '供应商名称',
  purchase_type     varchar(50)     default 'PAPER'            comment '采购类型:PAPER-纸张,AUX-辅料(绳子/胶水/油墨),PACK-包材(纸箱),OTHER-其他',
  order_date        datetime        default current_timestamp  comment '下单日期',
  expected_date     datetime        default null               comment '预计到货日期',
  purchaser         varchar(64)     default null               comment '采购员(申购人)',
  approver          varchar(64)     default null               comment '审批人',
  total_quantity    decimal(14,4)   default 0.0000             comment '采购总数量(主单位)',
  total_amount      decimal(14,2)   default 0.00               comment '采购总金额(元)',
  currency          varchar(10)     default 'CNY'              comment '币种:CNY-人民币,USD-美元',
  -- 关联客户订单（圣享特有：每笔采购追溯到具体客户订单）
  source_order_code varchar(64)     default null               comment '关联客户订单号(如PO#ORD66003MT)',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (order_id),
  unique key uk_order_code (order_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '采购订单头表';

-- ----------------------------
-- 2、采购订单行表
-- 用途：采购订单的物料明细行。纸张行业可选字段（门幅/克重/卷数），非纸张物料留NULL。
-- ----------------------------
drop table if exists qxx_pur_order_line;
create table qxx_pur_order_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  order_id          bigint(20)      not null                   comment '采购订单ID(关联qxx_pur_order)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克)',
  unit_name         varchar(64)     not null                   comment '主单位名称',
  -- 双单位（纸张行业:ROLL-卷/TON-吨，其他行业可按需使用）
  unit2             varchar(64)     default null               comment '辅助单位编码(如ROLL-卷,与主单位联动)',
  unit2_name        varchar(64)     default null               comment '辅助单位名称',
  conversion_rate   decimal(10,4)   default 1.0000             comment '主单位→辅助单位换算率',
  quantity_ordered  decimal(14,4)   default 0.0000             comment '订购数量(主单位)',
  quantity_ordered2 decimal(14,4)   default 0.0000             comment '订购数量(辅助单位,如卷数)',
  quantity_received decimal(14,4)   default 0.0000             comment '已收货数量(主单位)',
  quantity_received2 decimal(14,4)  default 0.0000             comment '已收货数量(辅助单位)',
  unit_price        decimal(14,4)   default 0.0000             comment '单价(元/主单位,如元/吨)',
  amount            decimal(14,2)   default 0.00               comment '行金额(不含税)',
  tax_rate          decimal(5,2)    default 0.00               comment '税率(%)',
  -- 纸张行业可选字段（采购纸张时填写规格要求，到货后可对比实际值；非纸张物料留NULL）
  paper_width       varchar(20)     default null               comment '门幅要求(mm),如925mm',
  paper_weight      varchar(20)     default null               comment '克重要求(g),如120g',
  paper_type        varchar(50)     default null               comment '纸张种类:乌卡/俄卡/箱板纸/白牛皮/TC箱板纸/瑞典赤牛',
  roll_count        int(11)         default 0                  comment '预计卷数(纸张行业用，其他行业=0)',
  -- 关联客户订单（圣享特有：每笔采购追溯到具体客户订单）
  source_order_code varchar(64)     default null               comment '关联客户订单号',
  expected_date     datetime        default null               comment '预计到货日期',
  arrival_notice_id bigint(20)      default null               comment '到货通知单ID(关联qxx_wm_arrival_notice,收货后回写)',
  status            varchar(50)     default 'ORDERED'          comment '行状态:ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '采购订单行表';
