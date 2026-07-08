-- ============================================================
-- V56: 对齐 schema 到测试环境(幂等)
-- 目的:让新环境跑完 V33-V56 = 测试 schema;
--      测试/生产已对齐,V56 执行为零操作(不动数据/schema)
-- 幂等:CREATE IF NOT EXISTS / 空表才重建 / 索引存在跳过 / 重复才删
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- Part 1: 15 张缺失表(CREATE IF NOT EXISTS)
-- ============================================================
CREATE TABLE IF NOT EXISTS `qxx_md_item_vendor` (

  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL DEFAULT '1' COMMENT '工厂ID',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料名称',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `is_preferred` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否首选供应商(Y/N)',
  `min_order_qty` decimal(14,2) DEFAULT NULL COMMENT '最小起订量',
  `lead_time_days` int DEFAULT NULL COMMENT '采购提前期(天)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_item_vendor` (`factory_id`,`item_id`,`vendor_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_vendor_id` (`vendor_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料供应商关系表';

CREATE TABLE IF NOT EXISTS `qxx_wm_materialrequest_notice` (

  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '备料通知单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `notice_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '备料通知单编码',
  `notice_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备料通知单名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `request_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '需求日期',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态:PENDING-待备料,PREPARED-已备料,ISSUED-已发料,CLOSED-关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备料通知单表';

CREATE TABLE IF NOT EXISTS `qxx_wm_materialrequest_notice_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `notice_id` bigint NOT NULL COMMENT '备料通知单ID(关联qxx_wm_materialrequest_notice)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_request` decimal(14,4) DEFAULT '0.0000' COMMENT '申请备料数量',
  `quantity_prepared` decimal(14,4) DEFAULT '0.0000' COMMENT '已备料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备料通知单行表';

CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue` (

  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '外协领料单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '外协领料单编码',
  `issue_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协领料单名称',
  `vendor_id` bigint NOT NULL COMMENT '外协厂ID(关联qxx_md_vendor,vendor_type=OUTSOURCE)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `outsource_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PRODUCE' COMMENT '外发类型:PRODUCE-外发生产(印刷+制袋),ROPE-外发贴绳,OTHER-其他',
  `warehouse_id` bigint NOT NULL COMMENT '发料仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `issue_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发料日期',
  `expected_return_date` datetime DEFAULT NULL COMMENT '预计返回日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '发料总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,ISSUED-已发出,IN_PROGRESS-外协生产中,RETURNED-已返回,CLOSED-关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注(如:4月21日到纸,4月22日发到欧诺)',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协领料单头表';

CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue_detail` (

  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '外协领料单ID(关联qxx_wm_outsource_issue)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_outsource_issue_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '发料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协领料单明细表';

CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '外协领料单ID(关联qxx_wm_outsource_issue)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `item_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料类型:RAW-纸张原材料,SEMI-半成品袋子,AUX-辅料(绳子),PACK-包材',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码(纸张双单位)',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `quantity_issue` decimal(14,4) DEFAULT '0.0000' COMMENT '发料数量(主单位)',
  `quantity_issue2` decimal(14,4) DEFAULT '0.0000' COMMENT '发料数量(辅助单位,如重量吨)',
  `bundle_count` int DEFAULT '0' COMMENT '卷数/捆数',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协领料单行表';

CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt` (

  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '外协入库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '外协入库单编码',
  `recpt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协入库单名称',
  `issue_id` bigint DEFAULT NULL COMMENT '关联外协领料单ID(关联qxx_wm_outsource_issue)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联外协领料单编码',
  `vendor_id` bigint NOT NULL COMMENT '外协厂ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `warehouse_id` bigint NOT NULL COMMENT '入库仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `recpt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库总数量',
  `total_box` int DEFAULT '0' COMMENT '入库总箱数',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程质检单ID(关联qxx_qc_ipqc)',
  `ipqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过程质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协入库单表';

CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt_detail` (

  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '外协入库单ID(关联qxx_wm_outsource_recpt)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_outsource_recpt_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协入库单明细表';

CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '外协入库单ID(关联qxx_wm_outsource_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `quantity_box` int DEFAULT '0' COMMENT '入库箱数',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协入库单行表';

CREATE TABLE IF NOT EXISTS `qxx_wm_product_produce` (

  `produce_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产出记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `produce_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产出记录编码',
  `produce_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产出记录名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `feedback_id` bigint DEFAULT NULL COMMENT '报工记录ID(关联qxx_pro_feedback)',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '线边库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '线边库名称',
  `produce_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '产出日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '产出总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`produce_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品产出记录表';

CREATE TABLE IF NOT EXISTS `qxx_wm_product_produce_detail` (

  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `produce_id` bigint NOT NULL COMMENT '产出记录ID(关联qxx_wm_product_produce)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_product_produce_line)',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '产出数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品产出记录明细表';

CREATE TABLE IF NOT EXISTS `qxx_wm_product_produce_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `produce_id` bigint NOT NULL COMMENT '产出记录ID(关联qxx_wm_product_produce)',
  `item_id` bigint NOT NULL COMMENT '产品物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_produce` decimal(14,4) DEFAULT '0.0000' COMMENT '产出数量',
  `quantity_qualified` decimal(14,4) DEFAULT '0.0000' COMMENT '合格数量',
  `quantity_unqualified` decimal(14,4) DEFAULT '0.0000' COMMENT '不合格数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品产出记录行表';

CREATE TABLE IF NOT EXISTS `qxx_wm_product_recpt` (

  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '入库单编码',
  `recpt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入库单名称',
  `produce_id` bigint DEFAULT NULL COMMENT '产出记录ID(关联qxx_wm_product_produce)',
  `produce_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产出记录编码',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `source_warehouse_id` bigint DEFAULT NULL COMMENT '来源仓库(线边库)ID',
  `warehouse_id` bigint NOT NULL COMMENT '目标仓库(成品库)ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标仓库名称',
  `recpt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库总数量(个数)',
  `total_box` int DEFAULT '0' COMMENT '入库总箱数',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程质检单ID(关联qxx_qc_ipqc)',
  `ipqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过程质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品入库单表';

CREATE TABLE IF NOT EXISTS `qxx_wm_product_recpt_detail` (

  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_product_recpt)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_product_recpt_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品入库单明细表';

CREATE TABLE IF NOT EXISTS `qxx_wm_product_recpt_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_product_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(个数)',
  `quantity_box` int DEFAULT '0' COMMENT '入库箱数',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '目标仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品入库单行表';

-- ============================================================
-- Part 2: 15 张结构差异表(仅当表为空时 DROP+CREATE 到测试版,非空跳过)
-- ============================================================
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_cal_teamshift;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_cal_teamshift()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_cal_teamshift') THEN
        IF (SELECT COUNT(*) FROM `qxx_cal_teamshift`) = 0 THEN
            DROP TABLE `qxx_cal_teamshift`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_cal_teamshift();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_cal_teamshift;
CREATE TABLE IF NOT EXISTS `qxx_cal_teamshift` (

  `teamshift_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排班明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `shift_date` date NOT NULL COMMENT '排班日期',
  `team_id` bigint NOT NULL COMMENT '班组ID(关联qxx_cal_team)',
  `team_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组编码',
  `team_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组名称',
  `shift_id` bigint NOT NULL COMMENT '班次ID(关联qxx_cal_shift)',
  `shift_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班次名称',
  `plan_id` bigint NOT NULL COMMENT '排班计划ID(关联qxx_cal_plan)',
  `plan_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '排班计划编码',
  `plan_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '排班计划名称',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `order_num` int DEFAULT '1' COMMENT '序号(1白班/2夜班)',
  PRIMARY KEY (`teamshift_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班组排班明细表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pro_process_content;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_pro_process_content()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pro_process_content') THEN
        IF (SELECT COUNT(*) FROM `qxx_pro_process_content`) = 0 THEN
            DROP TABLE `qxx_pro_process_content`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_pro_process_content();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pro_process_content;
CREATE TABLE IF NOT EXISTS `qxx_pro_process_content` (

  `content_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `order_num` int DEFAULT '0' COMMENT '顺序编号(作业步骤序号)',
  `content_text` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业内容说明',
  `device` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助设备/工具',
  `material` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助材料/辅料',
  `doc_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业指导书URL(SOP文件)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_check` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否需要质检(Y-是,N-否)',
  PRIMARY KEY (`content_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工序作业内容表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pro_workorder_change;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_pro_workorder_change()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pro_workorder_change') THEN
        IF (SELECT COUNT(*) FROM `qxx_pro_workorder_change`) = 0 THEN
            DROP TABLE `qxx_pro_workorder_change`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_pro_workorder_change();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pro_workorder_change;
CREATE TABLE IF NOT EXISTS `qxx_pro_workorder_change` (

  `change_id` bigint NOT NULL AUTO_INCREMENT COMMENT '变更ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  `workorder_id` bigint NOT NULL COMMENT '工单ID',
  `change_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '变更类型:BOM/QTY/PARAM/STATUS',
  `field_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '变更字段',
  `old_value` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原值',
  `new_value` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新值',
  `change_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变更原因',
  `approver` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` varchar(32) COLLATE utf8mb4_general_ci DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`change_id`),
  KEY `idx_workorder_id` (`workorder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单变更记录表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_tm_tool;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_tm_tool()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_tm_tool') THEN
        IF (SELECT COUNT(*) FROM `qxx_tm_tool`) = 0 THEN
            DROP TABLE `qxx_tm_tool`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_tm_tool();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_tm_tool;
CREATE TABLE IF NOT EXISTS `qxx_tm_tool` (

  `tool_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `tool_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工装夹具编码(唯一)',
  `tool_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工装夹具名称',
  `brand` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌',
  `spec` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `tool_type_id` bigint NOT NULL COMMENT '工装夹具类型ID(关联qxx_tm_tool_type)',
  `tool_type_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '工装夹具类型编码',
  `tool_type_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '工装夹具类型名称',
  `quantity` int DEFAULT '1' COMMENT '总数量',
  `available_quantity` int DEFAULT '1' COMMENT '可用数量(总数量减去使用中和保养中的数量)',
  `mainten_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数',
  `next_mainten_date` date DEFAULT NULL COMMENT '下次保养日期',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STORE' COMMENT '状态:STORE-在库,USING-使用中,MAINTENANCE-保养中,SCRAPPED-报废',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工装夹具清单表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_tm_tool_type;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_tm_tool_type()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_tm_tool_type') THEN
        IF (SELECT COUNT(*) FROM `qxx_tm_tool_type`) = 0 THEN
            DROP TABLE `qxx_tm_tool_type`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_tm_tool_type();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_tm_tool_type;
CREATE TABLE IF NOT EXISTS `qxx_tm_tool_type` (

  `tool_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具类型ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `tool_type_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码(唯一)',
  `tool_type_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `need_code_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否需要编码(1-需要,0-不需要)',
  `mainten_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数',
  `mainten_cycle` int DEFAULT '0' COMMENT '保养周期(与保养类型配合,如:月+3=每3个月保养一次)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_type_id`),
  UNIQUE KEY `uk_tool_type_code` (`tool_type_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工装夹具类型表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_issue_header;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_issue_header()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_header') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_issue_header`) = 0 THEN
            DROP TABLE `qxx_wm_issue_header`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_issue_header();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_issue_header;
CREATE TABLE IF NOT EXISTS `qxx_wm_issue_header` (

  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '领料单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '领料单编码',
  `issue_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '领料单名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `warehouse_id` bigint NOT NULL COMMENT '发料仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL,
  `location_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_id` bigint DEFAULT NULL,
  `area_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领料日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '领料总数量',
  `quantity_issued_total` decimal(14,4) DEFAULT '0.0000' COMMENT '已发料累计数量',
  `quantity_received` decimal(14,4) DEFAULT '0.0000' COMMENT '已收料累计数量',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PRODUCE' COMMENT '领料类型:PRODUCE-生产领料,MISC-杂项领料',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `approve_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核人',
  `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
  `cancel_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作废原因',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `uk_issue_code` (`factory_id`,`issue_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单头表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_issue_detail;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_issue_detail()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_detail') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_issue_detail`) = 0 THEN
            DROP TABLE `qxx_wm_issue_detail`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_issue_detail();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_issue_detail;
CREATE TABLE IF NOT EXISTS `qxx_wm_issue_detail` (

  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '领料单ID(关联qxx_wm_issue_header)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_issue_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '领料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单明细表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_issue_line;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_issue_line()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_issue_line') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_issue_line`) = 0 THEN
            DROP TABLE `qxx_wm_issue_line`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_issue_line();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_issue_line;
CREATE TABLE IF NOT EXISTS `qxx_wm_issue_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '领料单ID(关联qxx_wm_issue_header)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '领料单编码(冗余)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_issue` decimal(14,4) DEFAULT '0.0000' COMMENT '申请领料数量',
  `quantity_issued` decimal(14,4) DEFAULT '0.0000' COMMENT '实际已发料数量(累计)',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅单位编码',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅单位名称',
  `conversion_rate` decimal(18,6) DEFAULT NULL COMMENT '主辅单位转换率',
  `quantity_issue2` decimal(14,4) DEFAULT '0.0000' COMMENT '辅单位申请数量',
  `quantity_issued2` decimal(14,4) DEFAULT '0.0000' COMMENT '辅单位已发料数量',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '实际发料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单行表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_rt_issue;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_rt_issue()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_issue') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_rt_issue`) = 0 THEN
            DROP TABLE `qxx_wm_rt_issue`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_rt_issue();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_rt_issue;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_issue` (

  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退料单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退料单编码',
  `rt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退料单名称',
  `issue_id` bigint DEFAULT NULL COMMENT '原领料单ID(关联qxx_wm_issue_header)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原领料单编码',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `warehouse_id` bigint NOT NULL COMMENT '退入仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `rt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '退料日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退料总数量',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rqc_id` bigint DEFAULT NULL COMMENT '退料质检单ID(关联qxx_qc_rqc)',
  `rqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退料质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单头表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_rt_issue_detail;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_rt_issue_detail()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_issue_detail') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_rt_issue_detail`) = 0 THEN
            DROP TABLE `qxx_wm_rt_issue_detail`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_rt_issue_detail();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_rt_issue_detail;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_issue_detail` (

  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退料单ID(关联qxx_wm_rt_issue)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_rt_issue_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单明细表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_rt_issue_line;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_rt_issue_line()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_issue_line') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_rt_issue_line`) = 0 THEN
            DROP TABLE `qxx_wm_rt_issue_line`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_rt_issue_line();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_rt_issue_line;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_issue_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退料单ID(关联qxx_wm_rt_issue)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_rt` decimal(14,4) DEFAULT '0.0000' COMMENT '退料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单行表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pro_card_process;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_pro_card_process()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pro_card_process') THEN
        IF (SELECT COUNT(*) FROM `qxx_pro_card_process`) = 0 THEN
            DROP TABLE `qxx_pro_card_process`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_pro_card_process();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pro_card_process;
CREATE TABLE IF NOT EXISTS `qxx_pro_card_process` (

  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `card_id` bigint NOT NULL COMMENT '流转卡ID(关联qxx_pro_card)',
  `card_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流转卡编码',
  `seq_num` int DEFAULT '1' COMMENT '工序序号(第几道工序)',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `process_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'INTERNAL' COMMENT '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  `task_id` bigint DEFAULT NULL COMMENT '排产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '排产任务编码',
  `input_time` datetime DEFAULT NULL COMMENT '进入工序时间(上道工序完成时间)',
  `output_time` datetime DEFAULT NULL COMMENT '出工序时间(本道工序完成时间)',
  `quantity_input` decimal(12,2) DEFAULT NULL COMMENT '投入数量',
  `quantity_output` decimal(12,2) DEFAULT NULL COMMENT '产出数量',
  `quantity_unqualified` decimal(12,2) DEFAULT NULL COMMENT '不合格品数量',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `user_id` bigint NOT NULL COMMENT '操作人用户ID',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人用户名',
  `nick_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人昵称',
  `vendor_id` bigint DEFAULT NULL COMMENT '外协厂ID(关联qxx_md_vendor,外发工序专用)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程检验单ID(关联qxx_qc_ipqc)',
  `ipqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过程检验单编码',
  `feedback_id` bigint DEFAULT NULL COMMENT '报工记录ID(关联qxx_pro_feedback)',
  `issue_detail_id` bigint DEFAULT NULL COMMENT '领料明细ID(关联qxx_wm_issue_detail,建立card↔原料批次直接追溯)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流转卡工序信息表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pur_order;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_pur_order()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order') THEN
        IF (SELECT COUNT(*) FROM `qxx_pur_order`) = 0 THEN
            DROP TABLE `qxx_pur_order`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_pur_order();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_pur_order;
CREATE TABLE IF NOT EXISTS `qxx_pur_order` (

  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `order_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '采购订单编码(唯一)',
  `order_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购订单名称',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `purchase_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PAPER' COMMENT '采购类型:PAPER-纸张,AUX-辅料(绳子/胶水/油墨),PACK-包材(纸箱),OTHER-其他',
  `order_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '下单日期',
  `expected_date` datetime DEFAULT NULL COMMENT '预计到货日期',
  `purchaser` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购员(申购人)',
  `approver` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '采购总数量(主单位)',
  `total_amount` decimal(14,2) DEFAULT '0.00' COMMENT '采购总金额(元)',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY' COMMENT '币种:CNY-人民币,USD-美元',
  `source_order_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联客户订单号(如PO#ORD66003MT)',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `workorder_id` bigint DEFAULT NULL COMMENT '关联工单ID',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联工单编码',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_code` (`order_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单头表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_batch;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_batch()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_batch') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_batch`) = 0 THEN
            DROP TABLE `qxx_wm_batch`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_batch();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_batch;
CREATE TABLE IF NOT EXISTS `qxx_wm_batch` (

  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次编码(唯一)',
  `batch_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次名称',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `quality_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '质量状态:NORMAL-正常,HOLD-冻结,REJECT-不合格',
  `lot_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产批号',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '批次状态:ACTIVE-活跃,CLOSED-关闭,EXPIRED-过期',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次记录表';

DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_item_recpt_line;
DELIMITER $$
CREATE PROCEDURE proc_rebuild_qxx_wm_item_recpt_line()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_item_recpt_line') THEN
        IF (SELECT COUNT(*) FROM `qxx_wm_item_recpt_line`) = 0 THEN
            DROP TABLE `qxx_wm_item_recpt_line`;
        END IF;
    END IF;
END$$
DELIMITER ;
CALL proc_rebuild_qxx_wm_item_recpt_line();
DROP PROCEDURE IF EXISTS proc_rebuild_qxx_wm_item_recpt_line;
CREATE TABLE IF NOT EXISTS `qxx_wm_item_recpt_line` (

  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_item_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000' COMMENT '换算率',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(主单位)',
  `quantity_recpt2` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(辅助单位,如重量吨)',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID(关联qxx_wm_batch)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `lot_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产批号',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `notice_line_id` bigint DEFAULT NULL COMMENT '到货通知行ID(关联qxx_wm_arrival_notice_line)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料入库单行表';

-- ============================================================
-- Part 3: 删除废弃表 tm_tool_borrow/maintenance(代码无引用)
-- ============================================================
DROP TABLE IF EXISTS `qxx_tm_tool_borrow`;
DROP TABLE IF EXISTS `qxx_tm_tool_maintenance`;

-- ============================================================
-- Part 4: sys_auto_code_rule 对齐(删重复 rule 保留最小 rule_id + 加 uk_rule_code)
-- ============================================================
-- 4.1 删除重复 rule_code 的多余记录(保留每个 rule_code 最小 rule_id),先删其 part
DELETE p FROM sys_auto_code_part p
INNER JOIN (
    SELECT rule_id FROM sys_auto_code_rule r1
    WHERE rule_id > (SELECT MIN(rule_id) FROM sys_auto_code_rule r2 WHERE r2.rule_code = r1.rule_code)
) dup ON p.rule_id = dup.rule_id;
-- 4.2 删除重复 rule(保留最小 rule_id)
DELETE FROM sys_auto_code_rule
WHERE rule_id > (SELECT min_id FROM (
    SELECT rule_code, MIN(rule_id) AS min_id FROM sys_auto_code_rule GROUP BY rule_code
) t WHERE t.rule_code = sys_auto_code_rule.rule_code);
-- 4.3 加 uk_rule_code 唯一索引(存在则跳过)
SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_auto_code_rule' AND INDEX_NAME='uk_rule_code');
SET @sql := IF(@idx=0, 'ALTER TABLE sys_auto_code_rule ADD UNIQUE INDEX uk_rule_code (rule_code)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- Part 5: cal_teamshift.order_num 对齐测试版(int DEFAULT 1;V55 加的 bigint 会被改)
-- ============================================================
DROP PROCEDURE IF EXISTS proc_fix_order_num_type;
DELIMITER $$
CREATE PROCEDURE proc_fix_order_num_type()
BEGIN
    DECLARE ct VARCHAR(100);
    SELECT COLUMN_TYPE INTO ct FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_cal_teamshift' AND COLUMN_NAME='order_num';
    IF ct IS NOT NULL AND ct <> 'int' THEN
        ALTER TABLE qxx_cal_teamshift MODIFY COLUMN order_num int DEFAULT 1 COMMENT '序号(1白班/2夜班)';
    END IF;
END$$
DELIMITER ;
CALL proc_fix_order_num_type();
DROP PROCEDURE IF EXISTS proc_fix_order_num_type;
