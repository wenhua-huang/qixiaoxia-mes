-- ============================================================
-- V150: 订单类型(5值) + 是否外发/是否包装 标志位 + 按维度匹配默认工艺路线
--
-- 含:
--  ① qxx_sal_order 加 outsource_flag/package_flag(Y/N)
--  ② qxx_sal_order_line 加 route_product_id/route_code/route_name(带出留痕)
--  ③ qxx_pro_route_product 加 apply_order_type/apply_outsource/apply_package
--     (NULL=通配) + is_default
--  ④ 字典 mes_sal_order_type 调整为 标品/小批量/礼品/备货/制版;
--     NEW/REPEAT 存量重映射为 STANDARD
--  ⑤ 验收种子: 新工序(完稿/制版/模切/上胶/检验)、3 条路线、3 个演示物料及绑定,
--     RT-OUTSRC 外发节点补供应商
--
-- 幂等: DDL 用 information_schema 守卫的存储过程(裸 CREATE PROCEDURE, 无 DELIMITER,
--   与 V100/V106 同款, Flyway 已验证可跑); DML 全部 WHERE NOT EXISTS / 按唯一键 UPDATE。
-- 注意: ④ 的两条存量 UPDATE 是系统级枚举值重命名, intentionally 跨全部工厂、
--   不带 factory_id(属 factory_id 约束的明确例外); 其余 DML 均显式 factory_id=1。
-- 日期: 2026-09-10
-- ============================================================

SET NAMES utf8mb4;

-- ---------- ①②③ DDL(列存在则跳过; 风格与 V106/V100 一致) ----------
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_sal_order', 'outsource_flag', 'ALTER TABLE qxx_sal_order ADD COLUMN outsource_flag char(1) DEFAULT ''N'' COMMENT ''是否外发 Y/N'' AFTER sample_flag');
CALL add_col_if_missing('qxx_sal_order', 'package_flag', 'ALTER TABLE qxx_sal_order ADD COLUMN package_flag char(1) DEFAULT ''N'' COMMENT ''是否包装 Y/N'' AFTER outsource_flag');
CALL add_col_if_missing('qxx_sal_order_line', 'route_product_id', 'ALTER TABLE qxx_sal_order_line ADD COLUMN route_product_id bigint DEFAULT NULL COMMENT ''产品-路线绑定ID qxx_pro_route_product.record_id'' AFTER shipping_req');
CALL add_col_if_missing('qxx_sal_order_line', 'route_code', 'ALTER TABLE qxx_sal_order_line ADD COLUMN route_code varchar(50) DEFAULT NULL COMMENT ''路线编码快照'' AFTER route_product_id');
CALL add_col_if_missing('qxx_sal_order_line', 'route_name', 'ALTER TABLE qxx_sal_order_line ADD COLUMN route_name varchar(200) DEFAULT NULL COMMENT ''路线名称快照'' AFTER route_code');
CALL add_col_if_missing('qxx_pro_route_product', 'apply_order_type', 'ALTER TABLE qxx_pro_route_product ADD COLUMN apply_order_type varchar(50) DEFAULT NULL COMMENT ''适用订单类型;NULL=通配''');
CALL add_col_if_missing('qxx_pro_route_product', 'apply_outsource', 'ALTER TABLE qxx_pro_route_product ADD COLUMN apply_outsource char(1) DEFAULT NULL COMMENT ''适用是否外发 Y/N;NULL=通配''');
CALL add_col_if_missing('qxx_pro_route_product', 'apply_package', 'ALTER TABLE qxx_pro_route_product ADD COLUMN apply_package char(1) DEFAULT NULL COMMENT ''适用是否包装 Y/N;NULL=通配''');
CALL add_col_if_missing('qxx_pro_route_product', 'is_default', 'ALTER TABLE qxx_pro_route_product ADD COLUMN is_default char(1) DEFAULT ''N'' COMMENT ''同分时默认首选 Y/N''');

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- ---------- ④ 字典: 删旧两值, 备新五值 ----------
DELETE FROM sys_dict_data WHERE dict_type = 'mes_sal_order_type' AND dict_value IN ('NEW','REPEAT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '标品', 'STANDARD', 'mes_sal_order_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '柔印标品常规订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='STANDARD');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '小批量', 'SMALL_BATCH', 'mes_sal_order_type', '', 'info', 'N', '0', 'admin', sysdate(), '小批量订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='SMALL_BATCH');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '礼品', 'GIFT', 'mes_sal_order_type', '', 'danger', 'N', '0', 'admin', sysdate(), '礼品盒订单(含包装)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='GIFT');
UPDATE sys_dict_data SET dict_sort=4 WHERE dict_type='mes_sal_order_type' AND dict_value='STOCK';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '制版', 'PLATE', 'mes_sal_order_type', '', 'success', 'N', '0', 'admin', sysdate(), '完稿制版订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='PLATE');

-- 存量枚举值重映射(系统级, 跨工厂, 无 factory_id 过滤 —— 见文件头说明)
UPDATE qxx_sal_order     SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT') OR order_type IS NULL;
UPDATE qxx_pro_workorder SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT');

-- ---------- ⑤a 验收用工序(按 code 幂等, id 自增) ----------
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-DESIGN',  '完稿', '核对客户文件与版式要求', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-DESIGN');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-PLATE',   '制版', '输出印版并核对套印精度', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-PLATE');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-DIECUT',  '模切', '检查模具压力,首件确认尺寸', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-DIECUT');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-GLUE',    '上胶', '控制胶量均匀,防溢胶脱胶', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-GLUE');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-INSPECT', '检验', '按标准抽检,不合格品隔离', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-INSPECT');

-- ---------- ⑤b 三条新路线(按 route_code 幂等) ----------
INSERT INTO qxx_pro_route (factory_id, route_code, route_name, enable_flag, remark, create_by, create_time)
SELECT 1, 'RT-PLATE', '制版工艺路线', '1', '完稿→制版→检验', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route WHERE factory_id=1 AND route_code='RT-PLATE');
INSERT INTO qxx_pro_route (factory_id, route_code, route_name, enable_flag, remark, create_by, create_time)
SELECT 1, 'RT-SMALL', '小批量工艺路线', '1', '印刷→制袋→检验→包装', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route WHERE factory_id=1 AND route_code='RT-SMALL');
INSERT INTO qxx_pro_route (factory_id, route_code, route_name, enable_flag, remark, create_by, create_time)
SELECT 1, 'RT-GIFT', '礼品工艺路线', '1', '模切→上胶→检验→包装', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route WHERE factory_id=1 AND route_code='RT-GIFT');

-- ---------- ⑤c 路线工序(按 route+process 幂等) ----------
-- 制版: 完稿(1)→制版(2,关键)→检验(3)
INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, link_type, key_flag, is_check, is_outsource, create_by, create_time)
SELECT 1, r.route_id, p.process_id, p.process_code, p.process_name, n.n, 'SS',
       CASE WHEN p.process_code='PRC-PLATE' THEN 'Y' ELSE 'N' END,
       CASE WHEN p.process_code='PRC-INSPECT' THEN 'Y' ELSE 'N' END,
       '0', 'admin', NOW()
FROM qxx_pro_route r
JOIN (SELECT 'PRC-DESIGN' AS code, 1 AS n UNION ALL SELECT 'PRC-PLATE',2 UNION ALL SELECT 'PRC-INSPECT',3) n
JOIN qxx_pro_process p ON p.factory_id=1 AND p.process_code=n.code
WHERE r.factory_id=1 AND r.route_code='RT-PLATE'
  AND NOT EXISTS (SELECT 1 FROM qxx_pro_route_process rp WHERE rp.route_id=r.route_id AND rp.process_id=p.process_id);

-- 小批量: 印刷(PRC-PRINT,1)→制袋(PRC-BAG,2,关键)→检验(3)→包装(PRC-PACK,4)
INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, link_type, key_flag, is_check, is_outsource, create_by, create_time)
SELECT 1, r.route_id, p.process_id, p.process_code, p.process_name, n.n, 'SS',
       CASE WHEN p.process_code='PRC-BAG' THEN 'Y' ELSE 'N' END,
       CASE WHEN p.process_code='PRC-INSPECT' THEN 'Y' ELSE 'N' END,
       '0', 'admin', NOW()
FROM qxx_pro_route r
JOIN (SELECT 'PRC-PRINT' AS code, 1 AS n UNION ALL SELECT 'PRC-BAG',2 UNION ALL SELECT 'PRC-INSPECT',3 UNION ALL SELECT 'PRC-PACK',4) n
JOIN qxx_pro_process p ON p.factory_id=1 AND p.process_code=n.code
WHERE r.factory_id=1 AND r.route_code='RT-SMALL'
  AND NOT EXISTS (SELECT 1 FROM qxx_pro_route_process rp WHERE rp.route_id=r.route_id AND rp.process_id=p.process_id);

-- 礼品: 模切(1)→上胶(2)→检验(3)→包装(4); 模切/上胶为关键工序
INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, link_type, key_flag, is_check, is_outsource, create_by, create_time)
SELECT 1, r.route_id, p.process_id, p.process_code, p.process_name, n.n, 'SS',
       CASE WHEN p.process_code IN ('PRC-DIECUT','PRC-GLUE') THEN 'Y' ELSE 'N' END,
       CASE WHEN p.process_code='PRC-INSPECT' THEN 'Y' ELSE 'N' END,
       '0', 'admin', NOW()
FROM qxx_pro_route r
JOIN (SELECT 'PRC-DIECUT' AS code, 1 AS n UNION ALL SELECT 'PRC-GLUE',2 UNION ALL SELECT 'PRC-INSPECT',3 UNION ALL SELECT 'PRC-PACK',4) n
JOIN qxx_pro_process p ON p.factory_id=1 AND p.process_code=n.code
WHERE r.factory_id=1 AND r.route_code='RT-GIFT'
  AND NOT EXISTS (SELECT 1 FROM qxx_pro_route_process rp WHERE rp.route_id=r.route_id AND rp.process_id=p.process_id);

-- ---------- ⑤d 三个演示物料(按 item_code 幂等) ----------
INSERT INTO qxx_md_item (factory_id, item_code, item_name, specification, unit_of_measure, unit_name, enable_flag, create_by, create_time)
SELECT 1, 'ITEM-PLATE-DEMO', '制版演示印版', '标准版式', 'PCS', '个', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_item WHERE factory_id=1 AND item_code='ITEM-PLATE-DEMO');
INSERT INTO qxx_md_item (factory_id, item_code, item_name, specification, unit_of_measure, unit_name, enable_flag, create_by, create_time)
SELECT 1, 'ITEM-SMALL-DEMO', '小批量演示纸袋', '小批量规格', 'PCS', '个', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_item WHERE factory_id=1 AND item_code='ITEM-SMALL-DEMO');
INSERT INTO qxx_md_item (factory_id, item_code, item_name, specification, unit_of_measure, unit_name, enable_flag, create_by, create_time)
SELECT 1, 'ITEM-GIFT-DEMO', '礼品盒演示产品', '礼盒装', 'PCS', '个', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_item WHERE factory_id=1 AND item_code='ITEM-GIFT-DEMO');

-- ---------- ⑤e 物料-路线绑定 + 维度标签(按 route+item 幂等) ----------
INSERT INTO qxx_pro_route_product (factory_id, route_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, apply_order_type, apply_outsource, apply_package, is_default, create_by, create_time)
SELECT 1, r.route_id, i.item_id, i.item_code, i.item_name, i.unit_of_measure, i.unit_name, 1,
       m.apply_type, m.apply_out, m.apply_pkg, 'Y', 'admin', NOW()
FROM (
  SELECT 'RT-PLATE' AS rc, 'ITEM-PLATE-DEMO' AS ic, 'PLATE' AS apply_type, NULL AS apply_out, NULL AS apply_pkg
  UNION ALL SELECT 'RT-SMALL','ITEM-SMALL-DEMO','SMALL_BATCH', NULL, NULL
  UNION ALL SELECT 'RT-GIFT','ITEM-GIFT-DEMO','GIFT', NULL, 'Y'
) m
JOIN qxx_pro_route r ON r.factory_id=1 AND r.route_code=m.rc
JOIN qxx_md_item  i ON i.factory_id=1 AND i.item_code=m.ic
WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route_product rp WHERE rp.route_id=r.route_id AND rp.item_id=i.item_id);

-- ---------- ⑤f 存量绑定打标 ----------
-- 外发路线绑定: 适用于外发=Y
UPDATE qxx_pro_route_product rp JOIN qxx_pro_route r ON r.route_id=rp.route_id
SET rp.apply_outsource='Y'
WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.apply_outsource IS NULL;
-- 标准路线绑定: 适用标品且同分默认
UPDATE qxx_pro_route_product rp JOIN qxx_pro_route r ON r.route_id=rp.route_id
SET rp.apply_order_type='STANDARD', rp.is_default='Y'
WHERE rp.factory_id=1 AND r.route_code='RT-STANDARD' AND rp.apply_order_type IS NULL;

-- ---------- ⑤g RT-OUTSRC 外发节点补供应商(按 route+is_outsource 定位) ----------
UPDATE qxx_pro_route_process rp
JOIN qxx_pro_route r ON r.route_id=rp.route_id
JOIN qxx_md_vendor v ON v.factory_id=1 AND v.vendor_code='OUT-WANLONG'
SET rp.vendor_id=v.vendor_id, rp.vendor_code=v.vendor_code, rp.vendor_name=v.vendor_name
WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.is_outsource='1'
  AND rp.vendor_id IS NULL;
