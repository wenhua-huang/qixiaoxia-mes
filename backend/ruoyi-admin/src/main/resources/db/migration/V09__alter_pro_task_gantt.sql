-- ============================================================
-- 甘特图排产功能 — 增量 DDL 脚本
-- 用途：ProTask 扩展字段 + 换型时间表 + 排产快照表
-- 日期：2026-06-27
-- ============================================================

-- 1. ProTask 扩展字段（ALTER TABLE，不破坏现有数据）
ALTER TABLE `qxx_pro_task`
  ADD COLUMN `predecessor_id`  bigint(20)  DEFAULT NULL  COMMENT '前置任务ID(同工单内依赖链，NULL=首工序)',
  ADD COLUMN `snapshot_id`     bigint(20)  DEFAULT NULL  COMMENT '排产快照ID(NULL=正式排产)',
  ADD COLUMN `gantt_sort`      int(11)     DEFAULT 0     COMMENT '甘特图行排序号',
  ADD INDEX `idx_predecessor_id` (`predecessor_id`),
  ADD INDEX `idx_snapshot_id` (`snapshot_id`);

-- 2. 换型时间表（新增）
DROP TABLE IF EXISTS `qxx_pro_changeover`;
CREATE TABLE IF NOT EXISTS `qxx_pro_changeover` (
  `id`                bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '换型ID',
  `factory_id`        bigint(20)    NOT NULL                 COMMENT '工厂ID(关联qxx_md_factory)',
  `name`              varchar(255)  DEFAULT NULL             COMMENT '换型名称(如"印刷→制袋换型")',
  `from_process_id`   bigint(20)    DEFAULT NULL             COMMENT '从工序ID(NULL=任意工序)',
  `to_process_id`     bigint(20)    NOT NULL                 COMMENT '到工序ID(关联qxx_pro_process)',
  `workstation_id`    bigint(20)    DEFAULT NULL             COMMENT '工作站ID(NULL=通用换型规范)',
  `duration_minutes`  int(11)       NOT NULL DEFAULT 0       COMMENT '换型时长(分钟)',
  `remark`            varchar(500)  DEFAULT ''               COMMENT '备注',
  `create_by`         varchar(64)   DEFAULT ''               COMMENT '创建者',
  `create_time`       datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         varchar(64)   DEFAULT ''               COMMENT '更新者',
  `update_time`       datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_from_to_ws` (`from_process_id`, `to_process_id`, `workstation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工序换型时间规范表';

-- 3. 排产快照表（新增）
DROP TABLE IF EXISTS `qxx_pro_snapshot`;
CREATE TABLE IF NOT EXISTS `qxx_pro_snapshot` (
  `id`              bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '快照ID',
  `factory_id`      bigint(20)    NOT NULL                 COMMENT '工厂ID(关联qxx_md_factory)',
  `name`            varchar(255)  DEFAULT NULL             COMMENT '快照名称',
  `scope_type`      varchar(32)   DEFAULT NULL             COMMENT '排产范围类型(WORKSTATION/WORKSHOP)',
  `scope_id`        bigint(20)    DEFAULT NULL             COMMENT '排产范围ID(工作站ID/车间ID)',
  `status`          varchar(32)   DEFAULT 'DRAFT'          COMMENT 'DRAFT/PUBLISHED/DISCARDED',
  `snapshot_data`   json          DEFAULT NULL             COMMENT '快照完整数据(JSON备份)',
  `remark`          varchar(500)  DEFAULT ''               COMMENT '备注',
  `create_by`       varchar(64)   DEFAULT ''               COMMENT '创建者',
  `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       varchar(64)   DEFAULT ''               COMMENT '更新者',
  `update_time`     datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排产快照表(支持预览→确认→回滚)';
