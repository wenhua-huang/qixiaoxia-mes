-- =====================================================================
-- V96: 采购订单行 line_attrs 分组结构 → 扁平化 {attrCode:value}
-- 设计文档: docs/设计文档/物料分类动态扩展属性设计.md (第三节·结构升级)
-- 前置: V73 建的 line_attrs 是分组 {paper:{width,weight,source,type,rollCount},
--        paperBag:{ropeSpec,mouthType,bottomType}, product:{size,packageSpec,printingReq}}
-- 本迁移: 重组为扁平 {PAPER_WIDTH, PAPER_WEIGHT, ...} 与物料 ext_attrs/销售行 line_attrs 统一
-- product 分组(size/packageSpec/printingReq)不迁——属物料通用字段,采购行按 item_id 关联显示即可
-- =====================================================================

UPDATE qxx_pur_order_line
SET line_attrs = JSON_OBJECT(
    'PAPER_WIDTH',      JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paper.width')),
    'PAPER_WEIGHT',     JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paper.weight')),
    'PAPER_SOURCE',     JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paper.source')),
    'PAPER_TYPE',       JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paper.type')),
    'PAPER_ROLL_COUNT', JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paper.rollCount')),
    'BAG_ROPE_SPEC',    JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paperBag.ropeSpec')),
    'BAG_MOUTH_TYPE',   JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paperBag.mouthType')),
    'BAG_BOTTOM_TYPE',  JSON_UNQUOTE(JSON_EXTRACT(line_attrs, '$.paperBag.bottomType'))
)
WHERE line_attrs IS NOT NULL
  AND (
      JSON_EXTRACT(line_attrs, '$.paper.width')      IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paper.weight')     IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paper.source')     IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paper.type')       IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paper.rollCount')  IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paperBag.ropeSpec')   IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paperBag.mouthType')  IS NOT NULL OR
      JSON_EXTRACT(line_attrs, '$.paperBag.bottomType') IS NOT NULL
  );

-- 清理：迁移后纯空对象(全 null)的置 NULL，保持干净
UPDATE qxx_pur_order_line
SET line_attrs = NULL
WHERE line_attrs IS NOT NULL
  AND CAST(line_attrs AS CHAR) = '{}';
