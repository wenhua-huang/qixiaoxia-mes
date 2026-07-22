-- =====================================================================
-- V73: 采购订单行属性改造 - 纸张平铺列迁移为 line_attrs JSON
-- 设计文档: docs/设计文档/采购订单行属性存储方案.md
-- =====================================================================

-- 1. 新增 line_attrs JSON 列（存储所有行业属性: paper/paperBag/product）
ALTER TABLE qxx_pur_order_line
    ADD COLUMN line_attrs JSON DEFAULT NULL COMMENT '行业属性(JSON): {paper:{width,weight,source,type,rollCount}, paperBag:{ropeSpec,mouthType,bottomType}, product:{size,packageSpec,printingReq}}';

-- 2. 将现有纸张平铺列数据迁移进 line_attrs JSON
UPDATE qxx_pur_order_line
SET line_attrs = JSON_OBJECT(
    'paper', JSON_OBJECT(
        'width',     paper_width,
        'weight',    paper_weight,
        'type',      paper_type,
        'rollCount', roll_count
    )
)
WHERE paper_width IS NOT NULL
   OR paper_weight IS NOT NULL
   OR paper_type IS NOT NULL
   OR roll_count IS NOT NULL;

-- 3. 删除旧平铺列（数据已迁移进 JSON）
ALTER TABLE qxx_pur_order_line
    DROP COLUMN paper_width,
    DROP COLUMN paper_weight,
    DROP COLUMN paper_type,
    DROP COLUMN roll_count;
