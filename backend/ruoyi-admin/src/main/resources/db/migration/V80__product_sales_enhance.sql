-- =====================================================
-- V80: 销售出库功能完善
-- 背景：
--   1. 销售出库(qxx_wm_product_sales) 当前仅 CRUD 壳子，无状态机/无库存扣减/无批次拣货。
--      本迁移补齐：表字段(来源关联+已出库量追踪)、自动编码规则、状态/类型字典、菜单权限。
--   2. PM 决策：无预占，过账时一次性扣减 onhand；支持部分出库。
-- 依赖：V05(建表)、V36(销售发货菜单 mes:wm:sales:list)、V43(事务类型字典)、V71(sal_order 表)
-- 幂等策略：加列用存储过程+information_schema 检查；字典/规则/菜单用 INSERT IGNORE 或 WHERE NOT EXISTS
-- ID 段：dict_type 206~207 / dict_data 328~ / auto_code_rule 204 / auto_code_part 310~312
-- 字符集：utf8mb4
-- 日期：2026-07-22
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- Part 1: 表字段 — 来源关联 + 已出库量追踪
-- =====================================================

-- 1.1 头表：关联销售订单 + 已过账出库量
DROP PROCEDURE IF EXISTS proc_add_product_sales_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_product_sales_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales' AND COLUMN_NAME='sales_order_id') THEN
        ALTER TABLE qxx_wm_product_sales
          ADD COLUMN sales_order_id   BIGINT       DEFAULT NULL COMMENT '销售订单ID(关联qxx_sal_order)' AFTER oqc_code,
          ADD COLUMN sales_order_code VARCHAR(64)  DEFAULT NULL COMMENT '销售订单编码' AFTER sales_order_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales' AND COLUMN_NAME='posted_quantity') THEN
        ALTER TABLE qxx_wm_product_sales
          ADD COLUMN posted_quantity DECIMAL(14,4) DEFAULT 0.0000 COMMENT '已过账出库量(部分出库追踪)' AFTER total_quantity;
    END IF;
END$$
DELIMITER ;
CALL proc_add_product_sales_cols();
DROP PROCEDURE IF EXISTS proc_add_product_sales_cols;

-- 1.2 行表：关联销售订单行 + 已过账出库量
DROP PROCEDURE IF EXISTS proc_add_product_sales_line_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_product_sales_line_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales_line' AND COLUMN_NAME='sales_order_line_id') THEN
        ALTER TABLE qxx_wm_product_sales_line
          ADD COLUMN sales_order_line_id BIGINT       DEFAULT NULL COMMENT '销售订单行ID(关联qxx_sal_order_line)' AFTER sales_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales_line' AND COLUMN_NAME='quantity_posted') THEN
        ALTER TABLE qxx_wm_product_sales_line
          ADD COLUMN quantity_posted DECIMAL(14,4) DEFAULT 0.0000 COMMENT '已过账出库量(部分出库追踪)' AFTER quantity_sales;
    END IF;
END$$
DELIMITER ;
CALL proc_add_product_sales_line_cols();
DROP PROCEDURE IF EXISTS proc_add_product_sales_line_cols;


-- =====================================================
-- Part 2: 自动编码规则 SALES_NO（前端已调用，但规则未配置）
-- 格式：XS + yyyyMMdd + 3位流水号(按日循环) → XS20260722001
-- 参照 V14 的 ORDER_NO 结构
-- =====================================================

INSERT IGNORE INTO sys_auto_code_rule (rule_id, factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(204, 1, 'SALES_NO', '销售出库单号', '格式:XS20260722001', 15, '1', '0', 'L', '1', 'admin', NOW());

INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(310, 1, 204, 1, 'FIXCHAR',  'PREFIX_XS',   '固定前缀XS',    2, NULL, NULL, 'XS', NULL, NULL, NULL, NULL,   'admin', NOW()),
(311, 1, 204, 2, 'NOWDATE',  'DATE_PART',   '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, 'admin', NOW()),
(312, 1, 204, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)',    3, NULL, NULL, NULL, 1,    1,    '1',   'DAY', 'admin', NOW());


-- =====================================================
-- Part 3: 字典数据
-- =====================================================

-- 3.1 销售出库单状态 mes_wm_sales_status
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(206, '销售出库单状态', 'mes_wm_sales_status', '0', 'admin', NOW(), '销售出库单生命周期：草稿/部分过账/已过账/已发货/已关闭/已作废');

INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(328, 1, '草稿',       'DRAFT',           'mes_wm_sales_status', '', 'info',    'Y', '0', 'admin', NOW(), '制单中，可编辑'),
(329, 2, '部分过账',   'PARTIAL_POSTED',  'mes_wm_sales_status', '', 'warning', 'N', '0', 'admin', NOW(), '分批出库中，部分库存已扣减'),
(330, 3, '已过账',     'POSTED',          'mes_wm_sales_status', '', 'success', 'N', '0', 'admin', NOW(), '全量出库完成，已扣减库存'),
(331, 4, '已发货',     'SHIPPED',         'mes_wm_sales_status', '', 'primary', 'N', '0', 'admin', NOW(), '已登记物流发货'),
(332, 5, '已关闭',     'CLOSED',          'mes_wm_sales_status', '', 'info',    'N', '0', 'admin', NOW(), '已关闭，终态'),
(333, 6, '已作废',     'CANCELED',        'mes_wm_sales_status', '', 'danger',  'N', '0', 'admin', NOW(), '已作废，终态');

-- 3.2 销售类型 mes_product_sales_type
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(207, '销售类型', 'mes_product_sales_type', '0', 'admin', NOW(), '外贸/内贸/现货');

INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(334, 1, '外贸', 'FOREIGN',   'mes_product_sales_type', '', 'primary', 'Y', '0', 'admin', NOW(), '出口外贸订单'),
(335, 2, '内贸', 'DOMESTIC',  'mes_product_sales_type', '', 'success', 'N', '0', 'admin', NOW(), '国内销售订单'),
(336, 3, '现货', 'SPOT',      'mes_product_sales_type', '', 'warning', 'N', '0', 'admin', NOW(), '现货销售');


-- =====================================================
-- Part 4: 菜单权限 — 销售出库生命周期动作
-- 依赖：V36 建的主菜单 perms='mes:wm:sales:list' (menu_type='C')
-- 参照：V51 issue 模块菜单格式
-- =====================================================

SET @salesMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'mes:wm:sales:list' AND menu_type = 'C' LIMIT 1);

-- 4.1 补 export 权限（V36 遗漏）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '销售出库导出', @salesMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:export', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:export' AND menu_type = 'F');

-- 4.2 生命周期动作权限（order_num 续号 6~9）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '销售出库过账', @salesMenuId, 6, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:post', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:post' AND menu_type = 'F');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '销售出库发货', @salesMenuId, 7, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:ship', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:ship' AND menu_type = 'F');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '销售出库关闭', @salesMenuId, 8, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:close', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:close' AND menu_type = 'F');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '销售出库作废', @salesMenuId, 9, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:cancel', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:cancel' AND menu_type = 'F');

-- 4.3 给管理员角色授权（role_id=1，factory_id=0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE perms IN ('mes:wm:sales:export','mes:wm:sales:post','mes:wm:sales:ship','mes:wm:sales:close','mes:wm:sales:cancel')
  AND menu_type = 'F'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.menu_id = sys_menu.menu_id AND rm.role_id = 1 AND rm.factory_id = 0
  );
