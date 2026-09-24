-- ============================================================
-- V158: 异常单增加「已作废 VOID」终态（E1：挂错对象作废重开）
--   1. 状态字典补 VOID（台账可见、留痕；不参与完工硬拦）
--   2. 状态列注释补充 VOID 语义（无 CHECK 约束，仅注释）
-- 幂等：字典 INSERT 带 WHERE NOT EXISTS；注释语句重复执行无副作用。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 字典：生产异常单状态增加「已作废」
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已作废', 'VOID', 'mes_pro_exception_status', '', 'info', 'N', '0', 'admin', NOW(), '挂错对象作废重开,终态,不硬拦完工'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_status' AND dict_value='VOID');

-- 2. 列注释补充 VOID
ALTER TABLE qxx_pro_exception
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'OPEN'
        COMMENT '状态 OPEN待处理/PROCESSING处理中/CLOSED已关闭/VOID已作废';
