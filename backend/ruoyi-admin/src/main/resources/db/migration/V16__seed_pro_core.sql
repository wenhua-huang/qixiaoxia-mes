-- ============================================================
-- 生产管理核心种子数据：工序 + 工艺路线 + 路线-产品关联
-- 与 workstation 种子数据对齐（PRINT-印刷, BAG_MAKE-制袋等）
-- ============================================================

-- 1. 生产工序
INSERT IGNORE INTO qxx_pro_process (process_id, factory_id, process_code, process_name, process_type, attention, enable_flag, create_by, create_time) VALUES
(200, 1, 'PRINT',     '印刷',      'INTERNAL', '检查版面整洁度，确保油墨均匀',                       '1', 'admin', NOW()),
(201, 1, 'BAG_MAKE',  '制袋',      'INTERNAL', '控制热封温度160-180℃，确保封口牢固',                 '1', 'admin', NOW()),
(202, 1, 'SLITTING',  '分切',      'INTERNAL', '按BOM规格精准裁切，误差≤±1mm',                        '1', 'admin', NOW()),
(203, 1, 'INSPECT',   '检验',      'INTERNAL', '逐只检查外观/尺寸/印刷/封口，不合格品隔离标记',        '1', 'admin', NOW()),
(204, 1, 'PACK',      '包装',      'INTERNAL', '按客户要求装箱，贴唛头',                              '1', 'admin', NOW()),
(205, 1, 'DIE_CUT',   '模切',      'INTERNAL', '检查模具压力，首件确认成形尺寸后方可量产',             '1', 'admin', NOW()),
(206, 1, 'GLUE',      '上胶',      'INTERNAL', '控制胶量均匀，防止溢胶和脱胶',                        '1', 'admin', NOW()),
(207, 1, 'OUTSOURCE', '外发加工',  'OUTSOURCE','外发前确认加工厂产能和交期',                          '1', 'admin', NOW());

-- 2. 工艺路线
INSERT IGNORE INTO qxx_pro_route (route_id, factory_id, route_code, route_name, enable_flag, remark, create_by, create_time) VALUES
(200, 1, 'ROUTE_BAG_STD',    '纸袋标准工艺路线',   '1', '印刷→制袋→检验→包装',           'admin', NOW()),
(201, 1, 'ROUTE_BOX_STD',    '礼品盒标准工艺路线', '1', '模切→上胶→检验→包装',           'admin', NOW()),
(202, 1, 'ROUTE_BAG_OUTSRC', '纸袋外发工艺路线',   '1', '印刷→外发→分切→制袋→检验→包装','admin', NOW());

-- 3. 路线-工序关联
INSERT IGNORE INTO qxx_pro_route_process (record_id, factory_id, route_id, process_id, process_code, process_name, order_num, create_by, create_time) VALUES
-- 纸袋标准：印刷→制袋→检验→包装
(300, 1, 200, 200, 'PRINT',    '印刷', 1, 'admin', NOW()),
(301, 1, 200, 201, 'BAG_MAKE', '制袋', 2, 'admin', NOW()),
(302, 1, 200, 203, 'INSPECT',  '检验', 3, 'admin', NOW()),
(303, 1, 200, 204, 'PACK',     '包装', 4, 'admin', NOW()),
-- 礼品盒标准：模切→上胶→检验→包装
(304, 1, 201, 205, 'DIE_CUT',  '模切', 1, 'admin', NOW()),
(305, 1, 201, 206, 'GLUE',     '上胶', 2, 'admin', NOW()),
(306, 1, 201, 203, 'INSPECT',  '检验', 3, 'admin', NOW()),
(307, 1, 201, 204, 'PACK',     '包装', 4, 'admin', NOW()),
-- 纸袋外发：印刷→外发加工→分切→制袋→检验→包装
(308, 1, 202, 200, 'PRINT',     '印刷',   1, 'admin', NOW()),
(309, 1, 202, 207, 'OUTSOURCE', '外发加工',2, 'admin', NOW()),
(310, 1, 202, 202, 'SLITTING',  '分切',   3, 'admin', NOW()),
(311, 1, 202, 201, 'BAG_MAKE',  '制袋',   4, 'admin', NOW()),
(312, 1, 202, 203, 'INSPECT',   '检验',   5, 'admin', NOW()),
(313, 1, 202, 204, 'PACK',      '包装',   6, 'admin', NOW());

-- 4. 路线-产品关联（绑定纸袋路线到第一个纸袋产品）
INSERT IGNORE INTO qxx_pro_route_product (record_id, factory_id, route_id, item_id, item_code, item_name, create_by, create_time)
SELECT 400, 1, 200, item_id, item_code, item_name, 'admin', NOW() FROM qxx_md_item WHERE item_code LIKE 'PAPER%' LIMIT 1;
