-- 物料-供应商关系表
DROP TABLE IF EXISTS qxx_md_item_vendor;
CREATE TABLE qxx_md_item_vendor (
    record_id        bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    factory_id       bigint(20)   NOT NULL DEFAULT 1       COMMENT '工厂ID',
    item_id          bigint(20)   NOT NULL                 COMMENT '物料ID(关联qxx_md_item)',
    item_code        varchar(64)  DEFAULT NULL             COMMENT '物料编码',
    item_name        varchar(255) DEFAULT NULL             COMMENT '物料名称',
    vendor_id        bigint(20)   NOT NULL                 COMMENT '供应商ID(关联qxx_md_vendor)',
    vendor_code      varchar(64)  DEFAULT NULL             COMMENT '供应商编码',
    vendor_name      varchar(255) DEFAULT NULL             COMMENT '供应商名称',
    is_preferred     char(1)      DEFAULT 'N'             COMMENT '是否首选供应商(Y/N)',
    min_order_qty    decimal(14,2) DEFAULT NULL            COMMENT '最小起订量',
    lead_time_days   int(11)      DEFAULT NULL             COMMENT '采购提前期(天)',
    enable_flag      char(1)      DEFAULT '1'             COMMENT '是否启用(1-是,0-否)',
    remark           varchar(500) DEFAULT ''              COMMENT '备注',
    create_by        varchar(64)  DEFAULT ''              COMMENT '创建者',
    create_time      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by        varchar(64)  DEFAULT ''              COMMENT '更新者',
    update_time      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_item_vendor (factory_id, item_id, vendor_id),
    KEY idx_factory_id (factory_id),
    KEY idx_vendor_id (vendor_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '物料供应商关系表';
