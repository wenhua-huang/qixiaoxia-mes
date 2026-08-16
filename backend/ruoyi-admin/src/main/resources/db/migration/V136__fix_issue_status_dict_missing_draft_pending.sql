-- ============================================================
-- 修复领料单状态字典缺失 DRAFT/PENDING
-- V51 用 dict_code 310/311 插入草稿/待审核，但这两个 code 后来被
-- mes_workstation_type 占用（分切/贴绳），INSERT IGNORE 静默跳过，
-- 导致字典只有6个状态，91张草稿单和1张待审核单无法正确显示/筛选。
-- 幂等：WHERE NOT EXISTS
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 480, 1, '草稿', 'DRAFT', 'mes_wm_issue_status', '', 'info', 'Y', '0', 'admin', NOW(), '制单中，可编辑'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_issue_status' AND dict_value = 'DRAFT'
);

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 481, 2, '待审核', 'PENDING', 'mes_wm_issue_status', '', 'warning', 'N', '0', 'admin', NOW(), '已提交审核，等待计划员/仓管主管审核'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_issue_status' AND dict_value = 'PENDING'
);
