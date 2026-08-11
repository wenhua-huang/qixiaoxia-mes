-- ============================================================
-- V111：库存事务类型补充「外协发料 / 外协收货」
--
-- 背景：外协发货/收货写入 qxx_wm_transaction.transaction_type 为
--       OUTSOURCE_ISSUE / OUTSOURCE_RECPT（见 TransactionTypeEnum），
--       但 mes_wm_transaction_type 字典缺这两项，前端 dict-tag 匹配不到
--       标签，库存事务页「事务类型」列显示原始值（生产旧数据为 '0'）。
-- 策略：幂等补录两条字典数据；并把历史 source_doc_type='OUTSOURCE' 的
--       异常 transaction_type（'0'/空/NULL）按 quantity 正负修复为对应枚举码。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 外协发料（quantity < 0，红色 danger）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 13, '外协发料', 'OUTSOURCE_ISSUE', 'mes_wm_transaction_type', '', 'danger', 'N', '0', 'admin', NOW(), '外协发货扣减供应商库存'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE dict_type = 'mes_wm_transaction_type' AND dict_value = 'OUTSOURCE_ISSUE'
);

-- 2. 外协收货（quantity > 0，绿色 success）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 14, '外协收货', 'OUTSOURCE_RECPT', 'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), '外协收货入原料仓'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE dict_type = 'mes_wm_transaction_type' AND dict_value = 'OUTSOURCE_RECPT'
);

-- 3. 修复历史异常数据：外协单据事务类型写成 '0'/空/NULL 的，按数量正负回填
UPDATE qxx_wm_transaction
SET transaction_type = CASE WHEN quantity < 0 THEN 'OUTSOURCE_ISSUE' ELSE 'OUTSOURCE_RECPT' END,
    update_time = NOW()
WHERE source_doc_type = 'OUTSOURCE'
  AND (transaction_type = '0' OR transaction_type = '' OR transaction_type IS NULL);
