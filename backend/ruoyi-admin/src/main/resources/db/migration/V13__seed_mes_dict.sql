-- ============================================================
-- 企小侠文化纸盒MES系统 — MES字典种子数据
-- 用途：预置MES业务字典类型及数据（入库类型、入库单状态等）
-- 重要：导入前确保客户端字符集为 utf8mb4（mysql --default-character-set=utf8mb4）
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 字典类型表 (sys_dict_type) — 使用 INSERT IGNORE 兼容重复执行
-- ============================================================

-- 入库类型
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(201, '入库类型', 'mes_recpt_type', '0', 'admin', NOW(), '物料入库单的入库类型：采购入库、生产入库、外协入库、杂项入库、退货入库');

-- 入库单状态
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(202, '入库单状态', 'mes_itemrecpt_status', '0', 'admin', NOW(), '物料入库单的状态：草稿、已确认、已过账、取消');

-- ============================================================
-- 2. 字典数据表 (sys_dict_data) — 使用 INSERT IGNORE 兼容重复执行
-- ============================================================

-- 入库类型 (mes_recpt_type) — 对应 qxx_wm_item_recpt.recpt_type
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(301, 1, '采购入库', 'PURCHASE',  'mes_recpt_type', '', 'primary', 'Y', '0', 'admin', NOW(), '采购入库'),
(302, 2, '生产入库', 'PRODUCE',  'mes_recpt_type', '', 'success', 'N', '0', 'admin', NOW(), '生产入库'),
(303, 3, '外协入库', 'OUTSOURCE', 'mes_recpt_type', '', 'warning', 'N', '0', 'admin', NOW(), '外协入库'),
(304, 4, '杂项入库', 'MISC',     'mes_recpt_type', '', 'info',    'N', '0', 'admin', NOW(), '杂项入库'),
(305, 5, '退货入库', 'RETURN',   'mes_recpt_type', '', 'danger',  'N', '0', 'admin', NOW(), '退货入库');

-- 入库单状态 (mes_itemrecpt_status) — 对应 qxx_wm_item_recpt.status
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(306, 1, '草稿',   'DRAFT',     'mes_itemrecpt_status', '', 'info',    'Y', '0', 'admin', NOW(), '草稿状态'),
(307, 2, '已确认', 'CONFIRMED', 'mes_itemrecpt_status', '', 'primary', 'N', '0', 'admin', NOW(), '已确认状态'),
(308, 3, '已过账', 'POSTED',    'mes_itemrecpt_status', '', 'success', 'N', '0', 'admin', NOW(), '已过账状态'),
(309, 4, '取消',   'CANCEL',    'mes_itemrecpt_status', '', 'danger',  'N', '0', 'admin', NOW(), '取消状态');
