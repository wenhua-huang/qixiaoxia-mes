-- ============================================================
-- 工单齐套分析 → 采购单/领料单/退料单/入库单联动
-- DB 迁移脚本
-- 日期: 2026-06-30
-- ============================================================

-- 1. qxx_pur_order 增加工单关联字段
ALTER TABLE qxx_pur_order ADD COLUMN workorder_id BIGINT DEFAULT NULL COMMENT '关联工单ID';
ALTER TABLE qxx_pur_order ADD COLUMN workorder_code VARCHAR(64) DEFAULT NULL COMMENT '关联工单编码';

-- 2. qxx_wm_item_recpt 增加工单关联字段（生产入库追溯）
ALTER TABLE qxx_wm_item_recpt ADD COLUMN workorder_id BIGINT DEFAULT NULL COMMENT '关联工单ID(生产入库)';
ALTER TABLE qxx_wm_item_recpt ADD COLUMN workorder_code VARCHAR(64) DEFAULT NULL COMMENT '关联工单编码';

-- 3. 新增工单单据生成日志表（幂等保障）
DROP TABLE IF EXISTS qxx_pro_doc_generation_log;
CREATE TABLE qxx_pro_doc_generation_log (
    log_id           BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '日志ID',
    factory_id       BIGINT(20)      NOT NULL                   COMMENT '工厂ID(关联qxx_md_factory)',
    workorder_id     BIGINT(20)      NOT NULL                   COMMENT '工单ID(关联qxx_pro_workorder)',
    doc_type         VARCHAR(20)     NOT NULL                   COMMENT '单据类型:ISSUE-领料单,RETURN-退料单,RECPT-入库单,PUR_ORDER-采购单',
    doc_id           BIGINT(20)      NOT NULL                   COMMENT '生成的目标单据ID',
    doc_code         VARCHAR(64)     DEFAULT NULL               COMMENT '生成的目标单据编码',
    generation_batch VARCHAR(64)     NOT NULL                   COMMENT '生成批次号(UUID,同一批操作共享)',
    status           VARCHAR(20)     DEFAULT 'ACTIVE'           COMMENT 'ACTIVE-有效,REVOKED-已撤销',
    create_time      DATETIME        DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    PRIMARY KEY (log_id),
    UNIQUE KEY uk_wo_doc (workorder_id, doc_type, doc_id),
    KEY idx_workorder_id (workorder_id),
    KEY idx_generation_batch (generation_batch),
    KEY idx_factory_id (factory_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '工单单据生成日志(幂等保障)';
