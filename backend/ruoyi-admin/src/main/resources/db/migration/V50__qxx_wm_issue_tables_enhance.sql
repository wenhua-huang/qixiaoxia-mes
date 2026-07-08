-- ============================================================
-- 生产领料单 5 张表建表（幂等）+ 字段增强（支撑完整生命周期）
-- 说明：
--   1. 表在部分环境已手工创建，故全部用 CREATE TABLE IF NOT EXISTS 幂等兜底
--   2. 新增字段（approve_by/quantity_issued_total/version 等）用存储过程判列存在才加，幂等
--   3. issue_code 唯一索引用判存在才建，幂等
--   4. 全部 WHERE/操作带 factory_id（MyBatis 拦截器自动注入，DDL 层无需）
-- 依赖：V05__qxx_wm_core.sql（qxx_wm_material_stock / qxx_wm_transaction 已建）
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 生产领料单头表 qxx_wm_issue_header
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_wm_issue_header (
  issue_id          bigint(20)      not null auto_increment    comment '领料单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  issue_code        varchar(64)     not null                   comment '领料单编码',
  issue_name        varchar(255)    default null               comment '领料单名称',
  workorder_id      bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  workorder_name    varchar(255)    default null               comment '生产工单名称',
  task_id           bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code         varchar(64)     default null               comment '生产任务编码',
  workstation_id    bigint(20)      default null               comment '工作站ID(关联qxx_md_workstation)',
  workstation_code  varchar(64)     default null               comment '工作站编码',
  workstation_name  varchar(255)    default null               comment '工作站名称',
  warehouse_id      bigint(20)      not null                   comment '发料仓库ID(关联qxx_wm_warehouse)',
  warehouse_code    varchar(64)     default null               comment '仓库编码',
  warehouse_name    varchar(255)    default null               comment '仓库名称',
  location_id       bigint(20)      default null               comment '库区ID',
  location_code     varchar(64)     default null               comment '库区编码',
  location_name     varchar(255)    default null               comment '库区名称',
  area_id           bigint(20)      default null               comment '库位ID',
  area_code         varchar(64)     default null               comment '库位编码',
  area_name         varchar(255)    default null               comment '库位名称',
  issue_date        datetime        default current_timestamp  comment '领料日期',
  total_quantity    decimal(14,4)   default 0.0000             comment '领料总数量(申请量)',
  issue_type        varchar(50)     default 'PRODUCE'          comment '领料类型:PRODUCE-生产领料,MISC-杂项领料',
  status            varchar(50)     default 'DRAFT'            comment '状态:DRAFT-草稿,PENDING-待审核,APPROVED-已下达,ALLOCATED-已预占,PARTIAL_ISSUED-部分发料,ISSUED-已发料,CLOSED-已关闭,CANCELED-已作废',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (issue_id),
  key idx_factory_id (factory_id)
) engine=innodb DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产领料单头表';

-- ============================================================
-- 2. 生产领料单行表 qxx_wm_issue_line
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_wm_issue_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',
  factory_id        bigint(20)      not null                   comment '工厂ID',
  issue_id          bigint(20)      not null                   comment '领料单ID(关联qxx_wm_issue_header)',
  issue_code        varchar(64)     default null               comment '领料单编码(冗余)',
  item_id           bigint(20)      not null                   comment '物料ID(关联qxx_md_item)',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '主单位编码',
  unit_name         varchar(64)     not null                   comment '主单位名称',
  quantity_issue    decimal(14,4)   default 0.0000             comment '申请领料数量',
  quantity_issued   decimal(14,4)   default 0.0000             comment '实际已发料数量(累计)',
  unit2             varchar(64)     default null               comment '辅单位编码',
  unit2_name        varchar(64)     default null               comment '辅单位名称',
  conversion_rate   decimal(18,6)   default null               comment '主辅单位转换率',
  quantity_issue2   decimal(14,4)   default 0.0000             comment '辅单位申请数量',
  quantity_issued2  decimal(14,4)   default 0.0000             comment '辅单位已发料数量',
  process_id        bigint(20)      default null               comment '工序ID',
  process_code      varchar(64)     default null               comment '工序编码',
  process_name      varchar(255)    default null               comment '工序名称',
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
  primary key (line_id),
  key idx_factory_id (factory_id),
  key idx_issue_id (issue_id)
) engine=innodb DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产领料单行表';

-- ============================================================
-- 3. 生产领料单明细表 qxx_wm_issue_detail（按批次/库位拆分的实际发料明细）
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_wm_issue_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',
  factory_id        bigint(20)      not null                   comment '工厂ID',
  issue_id          bigint(20)      not null                   comment '领料单ID',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_issue_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '本次发料数量',
  quantity2         decimal(14,4)   default 0.0000             comment '辅单位发料数量',
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
  primary key (detail_id),
  key idx_factory_id (factory_id),
  key idx_issue_id (issue_id),
  key idx_line_id (line_id)
) engine=innodb DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产领料单明细表(分批发料记录)';

-- ============================================================
-- 4. 生产退料单头表 qxx_wm_rt_issue
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_wm_rt_issue (
  rt_id             bigint(20)      not null auto_increment    comment '退料单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID',
  rt_code           varchar(64)     not null                   comment '退料单编码',
  rt_name           varchar(255)    default null               comment '退料单名称',
  issue_id          bigint(20)      default null               comment '原领料单ID(关联qxx_wm_issue_header)',
  issue_code        varchar(64)     default null               comment '原领料单编码',
  workorder_id      bigint(20)      default null               comment '生产工单ID',
  workorder_code    varchar(64)     default null               comment '生产工单编码',
  workstation_id    bigint(20)      default null               comment '工作站ID',
  warehouse_id      bigint(20)      not null                   comment '退入仓库ID',
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
  primary key (rt_id),
  key idx_factory_id (factory_id)
) engine=innodb DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产退料单头表';

-- ============================================================
-- 5. 生产退料单行表 qxx_wm_rt_issue_line
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_wm_rt_issue_line (
  line_id           bigint(20)      not null auto_increment    comment '行ID',
  factory_id        bigint(20)      not null                   comment '工厂ID',
  rt_id             bigint(20)      not null                   comment '退料单ID(关联qxx_wm_rt_issue)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity_rt       decimal(14,4)   default 0.0000             comment '退料数量',
  quantity_rted     decimal(14,4)   default 0.0000             comment '已退料数量',
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
  primary key (line_id),
  key idx_factory_id (factory_id)
) engine=innodb DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产退料单行表';

-- ============================================================
-- 6. 生产退料单明细表 qxx_wm_rt_issue_detail
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_wm_rt_issue_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细ID',
  factory_id        bigint(20)      not null                   comment '工厂ID',
  rt_id             bigint(20)      not null                   comment '退料单ID',
  line_id           bigint(20)      not null                   comment '行ID(关联qxx_wm_rt_issue_line)',
  item_id           bigint(20)      not null                   comment '物料ID',
  item_code         varchar(64)     not null                   comment '物料编码',
  item_name         varchar(255)    not null                   comment '物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     not null                   comment '单位编码',
  unit_name         varchar(64)     not null                   comment '单位名称',
  quantity          decimal(14,4)   default 0.0000             comment '本次退料数量',
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
  primary key (detail_id),
  key idx_factory_id (factory_id)
) engine=innodb DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '生产退料单明细表';

-- ============================================================
-- 7. header 表字段增强（幂等：列存在则跳过）
--    为完整生命周期新增：审核信息 / 已发料累计 / 已收料累计 / 乐观锁 / 作废原因
-- ============================================================

-- 7.1 approve_by / approve_time — 审核人与审核时间
DROP PROCEDURE IF EXISTS proc_add_issue_approve_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_issue_approve_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'approve_by') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN approve_by varchar(64) default null comment '审核人' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'approve_time') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN approve_time datetime default null comment '审核时间' AFTER approve_by;
    END IF;
END$$
DELIMITER ;
CALL proc_add_issue_approve_cols();
DROP PROCEDURE IF EXISTS proc_add_issue_approve_cols;

-- 7.1b 补齐早期手工建表缺失的基础列（幂等）
--     部分生产环境 issue_header 是 V50 之前手工建表，缺 issue_date / total_quantity /
--     unit_of_measure / unit_name；CREATE TABLE IF NOT EXISTS 不会补列，后续 §7.2 的
--     AFTER total_quantity 会 1054。此处统一补齐，列已存在则跳过。
DROP PROCEDURE IF EXISTS proc_add_issue_header_base_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_issue_header_base_cols()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'issue_date') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN issue_date datetime default current_timestamp comment '领料日期' AFTER area_name;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'total_quantity') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN total_quantity decimal(14,4) default 0.0000 comment '领料总数量(申请量)' AFTER issue_date;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'unit_of_measure') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN unit_of_measure varchar(64) default null comment '主单位编码' AFTER total_quantity;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'unit_name') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN unit_name varchar(64) default null comment '主单位名称' AFTER unit_of_measure;
    END IF;
END$$
DELIMITER ;
CALL proc_add_issue_header_base_cols();
DROP PROCEDURE IF EXISTS proc_add_issue_header_base_cols;

-- 7.2 quantity_issued_total — 已发料累计（分批发料累加）
DROP PROCEDURE IF EXISTS proc_add_qty_issued_total;
DELIMITER $$
CREATE PROCEDURE proc_add_qty_issued_total()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'quantity_issued_total') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN quantity_issued_total decimal(14,4) default 0.0000 comment '已发料累计数量' AFTER unit_name;
    END IF;
END$$
DELIMITER ;
CALL proc_add_qty_issued_total();
DROP PROCEDURE IF EXISTS proc_add_qty_issued_total;

-- 7.3 quantity_received — 已收料累计（产线收料确认）
DROP PROCEDURE IF EXISTS proc_add_qty_received;
DELIMITER $$
CREATE PROCEDURE proc_add_qty_received()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'quantity_received') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN quantity_received decimal(14,4) default 0.0000 comment '已收料累计数量' AFTER quantity_issued_total;
    END IF;
END$$
DELIMITER ;
CALL proc_add_qty_received();
DROP PROCEDURE IF EXISTS proc_add_qty_received;

-- 7.4 version — 乐观锁版本号（并发状态流转防护）
DROP PROCEDURE IF EXISTS proc_add_issue_version;
DELIMITER $$
CREATE PROCEDURE proc_add_issue_version()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'version') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN version int(11) default 0 comment '乐观锁版本号' AFTER quantity_received;
    END IF;
END$$
DELIMITER ;
CALL proc_add_issue_version();
DROP PROCEDURE IF EXISTS proc_add_issue_version;

-- 7.5 cancel_reason — 作废原因
DROP PROCEDURE IF EXISTS proc_add_cancel_reason;
DELIMITER $$
CREATE PROCEDURE proc_add_cancel_reason()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'cancel_reason') THEN
        ALTER TABLE qxx_wm_issue_header ADD COLUMN cancel_reason varchar(500) default null comment '作废原因' AFTER approve_time;
    END IF;
END$$
DELIMITER ;
CALL proc_add_cancel_reason();
DROP PROCEDURE IF EXISTS proc_add_cancel_reason;

-- ============================================================
-- 8. issue_code 唯一索引（幂等：索引存在则跳过，防止重复编码）
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_uk_issue_code;
DELIMITER $$
CREATE PROCEDURE proc_add_uk_issue_code()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND INDEX_NAME = 'uk_issue_code') THEN
        ALTER TABLE qxx_wm_issue_header ADD UNIQUE KEY uk_issue_code (factory_id, issue_code);
    END IF;
END$$
DELIMITER ;
CALL proc_add_uk_issue_code();
DROP PROCEDURE IF EXISTS proc_add_uk_issue_code;

-- ============================================================
-- 9. line 表补发料累计字段（幂等）— 老库可能只有 quantity_issue
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_line_qty_issued;
DELIMITER $$
CREATE PROCEDURE proc_add_line_qty_issued()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_line' AND COLUMN_NAME = 'quantity_issued') THEN
        ALTER TABLE qxx_wm_issue_line ADD COLUMN quantity_issued decimal(14,4) default 0.0000 comment '实际已发料数量(累计)' AFTER quantity_issue;
    END IF;
END$$
DELIMITER ;
CALL proc_add_line_qty_issued();
DROP PROCEDURE IF EXISTS proc_add_line_qty_issued;
