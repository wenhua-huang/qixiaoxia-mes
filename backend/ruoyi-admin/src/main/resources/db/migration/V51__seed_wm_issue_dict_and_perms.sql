-- ============================================================
-- 领料单状态字典 + 领料类型字典 + 库存事务类型字典 + 领料单菜单权限补全
-- 依赖：V36__fix_wm_menus_and_paths.sql（库存发料菜单 mes:wm:issue:list 已存在）
-- 幂等：全部 INSERT IGNORE
-- 字典ID段：203~ 起续号（入库 201/202 之后），数据 310~ 起续号
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 字典类型 (sys_dict_type) — 全部 INSERT IGNORE
-- ============================================================

-- 领料单状态（8态完整生命周期）
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(203, '领料单状态', 'mes_wm_issue_status', '0', 'admin', NOW(), '领料单生命周期状态：草稿/待审核/已下达/已预占/部分发料/已发料/已关闭/已作废');

-- 领料类型
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(204, '领料类型', 'mes_wm_issue_type', '0', 'admin', NOW(), '领料单类型：生产领料、杂项领料');

-- 库存事务类型（修正 qxx_wm_transaction.transaction_type 注释与代码实际值的不一致）
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(205, '库存事务类型', 'mes_wm_transaction_type', '0', 'admin', NOW(), '库存流水事务类型：预占/释放/领料出库/退料入库/入库/调拨/调整/拆分');

-- ============================================================
-- 2. 字典数据 (sys_dict_data) — 全部 INSERT IGNORE
-- ============================================================

-- 领料单状态 mes_wm_issue_status（8态，含颜色：info草稿/warning待审/primary已下达/success预占发料/danger作废）
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(310, 1, '草稿',     'DRAFT',         'mes_wm_issue_status', '', 'info',    'Y', '0', 'admin', NOW(), '制单中，可编辑'),
(311, 2, '待审核',   'PENDING',       'mes_wm_issue_status', '', 'warning', 'N', '0', 'admin', NOW(), '已提交审核，等待计划员/仓管主管审核'),
(312, 3, '已下达',   'APPROVED',      'mes_wm_issue_status', '', 'primary', 'N', '0', 'admin', NOW(), '审核通过，仓库可见，未预占库存'),
(313, 4, '已预占',   'ALLOCATED',     'mes_wm_issue_status', '', 'success', 'N', '0', 'admin', NOW(), '已预占库存(扣减可用量)'),
(314, 5, '部分发料', 'PARTIAL_ISSUED','mes_wm_issue_status', '', 'success', 'N', '0', 'admin', NOW(), '分批发料中，部分库存已扣减'),
(315, 6, '已发料',   'ISSUED',        'mes_wm_issue_status', '', 'success', 'N', '0', 'admin', NOW(), '全量出库完成'),
(316, 7, '已关闭',   'CLOSED',        'mes_wm_issue_status', '', 'primary', 'N', '0', 'admin', NOW(), '已关闭(收料确认/手工关闭)，终态'),
(317, 8, '已作废',   'CANCELED',      'mes_wm_issue_status', '', 'danger',  'N', '0', 'admin', NOW(), '已作废，终态');

-- 领料类型 mes_wm_issue_type
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(318, 1, '生产领料', 'PRODUCE', 'mes_wm_issue_type', '', 'primary', 'Y', '0', 'admin', NOW(), '工单驱动的生产领料'),
(319, 2, '杂项领料', 'MISC',    'mes_wm_issue_type', '', 'info',    'N', '0', 'admin', NOW(), '非生产性领料(报废/办公等)');

-- 库存事务类型 mes_wm_transaction_type
INSERT IGNORE INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(320, 1, '预占',     'ALLOCATE',   'mes_wm_transaction_type', '', 'warning', 'N', '0', 'admin', NOW(), '领料确认时扣减可用库存'),
(321, 2, '释放预占', 'RELEASE',    'mes_wm_transaction_type', '', 'info',    'N', '0', 'admin', NOW(), '释放预占恢复可用库存'),
(322, 3, '领料出库', 'ISSUE_OUT',  'mes_wm_transaction_type', '', 'danger',  'N', '0', 'admin', NOW(), '领料执行扣减现有库存'),
(323, 4, '退料入库', 'RETURN_IN',  'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), '退料入库增加现有库存'),
(324, 5, '入库',     'RECEIPT',    'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', NOW(), '采购/生产入库'),
(325, 6, '调拨',     'TRANSFER',   'mes_wm_transaction_type', '', 'primary', 'N', '0', 'admin', NOW(), '仓库间调拨'),
(326, 7, '调整',     'ADJUST',     'mes_wm_transaction_type', '', 'warning', 'N', '0', 'admin', NOW(), '盘点调整'),
(327, 8, '拆分',     'SPLIT',      'mes_wm_transaction_type', '', 'info',    'N', '0', 'admin', NOW(), '库存拆分');

-- ============================================================
-- 3. 领料单菜单权限补全
--    V36 只建了 query/add/edit/remove，缺 export（前端已调用）
--    新增生命周期动作权限：submit/approve/issueOut/close/cancel
-- ============================================================

-- 找到领料单主菜单ID（perms='mes:wm:issue:list' AND menu_type='C'）
SET @issueMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'mes:wm:issue:list' AND menu_type = 'C' LIMIT 1);

-- 补 export 权限（V36 遗漏）
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('库存发料导出', @issueMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:export', '#', 'admin', sysdate(), '', NULL, '');

-- 新增生命周期动作权限（按 order_num 续号 6~10）
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('领料单提交审核', @issueMenuId, 6, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:submit', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('领料单审核', @issueMenuId, 7, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:approve', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('领料单发料出库', @issueMenuId, 8, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:issueOut', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('领料单关闭', @issueMenuId, 9, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:close', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('领料单作废', @issueMenuId, 10, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:issue:cancel', '#', 'admin', sysdate(), '', NULL, '');
