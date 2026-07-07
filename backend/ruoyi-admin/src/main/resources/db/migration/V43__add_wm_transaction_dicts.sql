-- ============================================================
-- V43：库存事务类型 & 来源单据类型字典
-- 用途：前端用 dict-tag 替换硬编码映射，显示中文
-- ============================================================

-- 1. 字典类型（幂等：不存在才插入，不指定 dict_id 用自增）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '来源单据类型', 'mes_wm_source_doc_type', '0', 'admin', NOW(), '库存事务来源单据类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_source_doc_type');

-- 更新已有 dict_type 的备注
UPDATE sys_dict_type SET remark = '库存事务类型：物料入库/杂项入库/杂项出库/供应商退货/销售出库/销售退货/调拨出库/调拨入库/分配/释放/领料出库/退货入库', update_time = NOW()
WHERE dict_type = 'mes_wm_transaction_type';

-- 2. 清除旧的库存事务类型字典数据（旧 seed 数据使用了不匹配 TransactionTypeEnum 的 dict_value）
DELETE FROM sys_dict_data WHERE dict_type = 'mes_wm_transaction_type';

-- 3. 事务类型字典数据（list_class 控制 tag 颜色）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(1,  '物料入库',   'ITEM_RECPT',    'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), ''),
(2,  '杂项入库',   'MISC_RECPT',    'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), ''),
(3,  '杂项出库',   'MISC_ISSUE',    'mes_wm_transaction_type', '', 'danger',  'N', '0', 'admin', NOW(), ''),
(4,  '供应商退货', 'ITEM_RTV',      'mes_wm_transaction_type', '', 'danger',  'N', '0', 'admin', NOW(), ''),
(5,  '销售出库',   'PRODUCT_SALES', 'mes_wm_transaction_type', '', 'danger',  'N', '0', 'admin', NOW(), ''),
(6,  '销售退货',   'PRODUCT_RT',    'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), ''),
(7,  '调拨出库',   'TRANS_OUT',     'mes_wm_transaction_type', '', 'warning', 'N', '0', 'admin', NOW(), ''),
(8,  '调拨入库',   'TRANS_IN',      'mes_wm_transaction_type', '', 'warning', 'N', '0', 'admin', NOW(), ''),
(9,  '分配',       'ALLOCATE',      'mes_wm_transaction_type', '', 'info',    'N', '0', 'admin', NOW(), ''),
(10, '释放',       'RELEASE',       'mes_wm_transaction_type', '', 'info',    'N', '0', 'admin', NOW(), ''),
(11, '领料出库',   'ISSUE_OUT',     'mes_wm_transaction_type', '', 'danger',  'N', '0', 'admin', NOW(), ''),
(12, '退货入库',   'RETURN_IN',     'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), '');

-- 4. 来源单据类型字典数据（幂等：不存在才插入）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '物料入库单', 'wm_item_recpt', 'mes_wm_source_doc_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_source_doc_type' AND dict_value = 'wm_item_recpt');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '领料出库单', 'ISSUE', 'mes_wm_source_doc_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_source_doc_type' AND dict_value = 'ISSUE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '退料单', 'RTISSUE', 'mes_wm_source_doc_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_source_doc_type' AND dict_value = 'RTISSUE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '调拨单', 'wm_transfer', 'mes_wm_source_doc_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_source_doc_type' AND dict_value = 'wm_transfer');
