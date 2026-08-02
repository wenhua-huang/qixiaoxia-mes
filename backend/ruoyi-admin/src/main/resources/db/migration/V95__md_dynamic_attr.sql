-- =====================================================================
-- V95: 物料分类动态扩展属性（元数据层 + JSON 列 + 旧子表数据迁移）
-- 设计文档: docs/设计文档/物料分类动态扩展属性设计.md
-- 说明: attr_def(全局属性字典, factory_id=0) + item_type_attr(分类绑定,实现继承)
--       物料 ext_attrs / 销售行·工单 line_attrs 统一扁平 {attrCode:value}
-- =====================================================================

-- 1. 属性字典表（全局共享，factory_id 恒为 0）
CREATE TABLE IF NOT EXISTS qxx_md_attr_def (
    attr_id        bigint       NOT NULL AUTO_INCREMENT COMMENT '属性ID',
    factory_id     bigint       NOT NULL DEFAULT 0      COMMENT '工厂ID(恒为0,全局共享)',
    attr_code      varchar(64)  NOT NULL                COMMENT '属性编码(唯一),如 PAPER_WIDTH',
    attr_name      varchar(100) NOT NULL                COMMENT '属性显示名,如 门幅',
    attr_type      varchar(20)  NOT NULL DEFAULT 'TEXT' COMMENT '类型:TEXT/NUMBER/SELECT/BOOL/DATE',
    attr_unit      varchar(20)  DEFAULT NULL            COMMENT '单位,如 mm/g',
    options_json   json         DEFAULT NULL            COMMENT 'SELECT 类型的可选值(JSON数组)',
    sort_order     int          DEFAULT 0               COMMENT '排序号',
    enable_flag    char(1)      DEFAULT '1' NOT NULL    COMMENT '是否启用(1-是,0-否)',
    remark         varchar(500) DEFAULT ''              COMMENT '备注',
    create_by      varchar(64)  DEFAULT ''              COMMENT '创建者',
    create_time    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      varchar(64)  DEFAULT ''              COMMENT '更新者',
    update_time    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (attr_id),
    UNIQUE KEY uk_factory_code (factory_id, attr_code),
    KEY idx_attr_code (attr_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料扩展属性字典(全局)';

-- 2. 分类-属性绑定表（实现继承：子类查祖先链聚合有效属性集）
CREATE TABLE IF NOT EXISTS qxx_md_item_type_attr (
    id             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id     bigint       NOT NULL                COMMENT '工厂ID',
    item_type_id   bigint       NOT NULL                COMMENT '分类ID(关联qxx_md_item_type)',
    attr_id        bigint       NOT NULL                COMMENT '属性ID(关联qxx_md_attr_def)',
    required       char(1)      DEFAULT '0' NOT NULL    COMMENT '是否必填(1-是,0-否)',
    sort_order     int          DEFAULT 0               COMMENT '本分类内排序',
    enable_flag    char(1)      DEFAULT '1' NOT NULL    COMMENT '是否启用(1-是,0-否)',
    create_by      varchar(64)  DEFAULT ''              COMMENT '创建者',
    create_time    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      varchar(64)  DEFAULT ''              COMMENT '更新者',
    update_time    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_attr (item_type_id, attr_id),
    KEY idx_factory_type (factory_id, item_type_id),
    KEY idx_attr (attr_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料分类-扩展属性绑定(实现继承)';

-- 3. 物料主数据新增 ext_attrs JSON 列（存扁平 {attrCode:value}）
ALTER TABLE qxx_md_item
    ADD COLUMN ext_attrs JSON DEFAULT NULL COMMENT '扩展属性(JSON扁平结构): {PAPER_WIDTH:925, PAPER_WEIGHT:120, ...}';

-- 4. 销售订单明细新增 line_attrs JSON 列（分类驱动的动态扩展属性，与订单业务字段平铺列并存）
ALTER TABLE qxx_sal_order_line
    ADD COLUMN line_attrs JSON DEFAULT NULL COMMENT '扩展属性(JSON扁平结构),分类驱动的动态属性快照';

-- 5. 工单新增 line_attrs JSON 列（仅装分类动态属性；订单业务字段仍用平铺列,见设计文档12.2）
ALTER TABLE qxx_pro_workorder
    ADD COLUMN line_attrs JSON DEFAULT NULL COMMENT '扩展属性(JSON扁平结构),从销售明细继承的分类动态属性';

-- =====================================================================
-- 6. 注册初始属性字典（从旧纸张/纸袋子表字段逆推，7条；礼品盒无业务数据不注册）
-- Flyway 裸 JDBC 绕过 FactoryIdInterceptor，显式写 factory_id=0(全局)
-- =====================================================================
INSERT IGNORE INTO qxx_md_attr_def (factory_id, attr_code, attr_name, attr_type, attr_unit, options_json, sort_order, enable_flag, remark) VALUES
    (0, 'PAPER_WIDTH',     '门幅',      'NUMBER', 'mm',   NULL,                            1, '1', '纸张门幅(旧 attr_paper.paper_width)'),
    (0, 'PAPER_WEIGHT',    '克重',      'NUMBER', 'g',    NULL,                            2, '1', '纸张克重(旧 attr_paper.paper_weight)'),
    (0, 'PAPER_SOURCE',    '来源/品牌', 'TEXT',   NULL,   NULL,                            3, '1', '纸张来源品牌(旧 attr_paper.paper_source)'),
    (0, 'PAPER_TYPE',      '种类',      'TEXT',   NULL,   NULL,                            4, '1', '纸张种类(旧 attr_paper.paper_type)'),
    (0, 'BAG_ROPE_SPEC',   '绳料规格',  'TEXT',   NULL,   NULL,                            5, '1', '纸袋绳料规格(旧 attr_paper_bag.rope_spec)'),
    (0, 'BAG_MOUTH_TYPE',  '口部提拔',  'SELECT', NULL,   '["锯齿口","平口","翻口"]',      6, '1', '纸袋口部(旧 attr_paper_bag.mouth_type)'),
    (0, 'BAG_BOTTOM_TYPE', '底板类型',  'TEXT',   NULL,   NULL,                            7, '1', '纸袋底板(旧 attr_paper_bag.bottom_type)');

-- 7. 绑定到分类（按 item_type_code 子查询，不硬编码 id；显式 factory_id=1 圣享）
INSERT IGNORE INTO qxx_md_item_type_attr (factory_id, item_type_id, attr_id, required, sort_order, enable_flag)
SELECT 1, t.item_type_id, d.attr_id, '0', d.sort_order, '1'
  FROM qxx_md_item_type t
  JOIN qxx_md_attr_def d
 WHERE t.factory_id = 1
   AND ( (t.item_type_code = 'RAW-PAPER'     AND d.attr_code IN ('PAPER_WIDTH','PAPER_WEIGHT','PAPER_SOURCE','PAPER_TYPE'))
      OR (t.item_type_code = 'FINISHED-BAG'  AND d.attr_code IN ('BAG_ROPE_SPEC','BAG_MOUTH_TYPE','BAG_BOTTOM_TYPE')) );

-- =====================================================================
-- 8. 迁移物料旧子表数据进 ext_attrs（扁平 {attrCode:value}）
-- 旧字段是 varchar("925mm"/"120g")，NUMBER 属性提取数值部分；空值跳过
-- 用 JSON_OBJECT 构造，JSON_MERGE_PATCH 合并 paper/paperBag 两源避免覆盖
-- =====================================================================

-- 8a. 纸张属性 → ext_attrs
UPDATE qxx_md_item i
JOIN qxx_md_item_attr_paper p ON p.item_id = i.item_id
SET i.ext_attrs = JSON_MERGE_PATCH(
    COALESCE(i.ext_attrs, '{}'),
    JSON_OBJECT(
        'PAPER_WIDTH',  NULLIF(REGEXP_REPLACE(IFNULL(p.paper_width,''), '[^0-9.]', ''), ''),
        'PAPER_WEIGHT', NULLIF(REGEXP_REPLACE(IFNULL(p.paper_weight,''), '[^0-9.]', ''), ''),
        'PAPER_SOURCE', NULLIF(p.paper_source, ''),
        'PAPER_TYPE',   NULLIF(p.paper_type, '')
    )
)
WHERE p.paper_width IS NOT NULL OR p.paper_weight IS NOT NULL
   OR p.paper_source IS NOT NULL OR p.paper_type IS NOT NULL;

-- 8b. 纸袋属性 → ext_attrs
UPDATE qxx_md_item i
JOIN qxx_md_item_attr_paper_bag b ON b.item_id = i.item_id
SET i.ext_attrs = JSON_MERGE_PATCH(
    COALESCE(i.ext_attrs, '{}'),
    JSON_OBJECT(
        'BAG_ROPE_SPEC',   NULLIF(b.rope_spec, ''),
        'BAG_MOUTH_TYPE',  NULLIF(b.mouth_type, ''),
        'BAG_BOTTOM_TYPE', NULLIF(b.bottom_type, '')
    )
)
WHERE b.rope_spec IS NOT NULL OR b.mouth_type IS NOT NULL OR b.bottom_type IS NOT NULL;

-- 注：礼品盒子表无业务数据，不迁移；旧三表的 DROP 推迟到后续单独版本（设计文档12.3，先观察一个发布周期）
