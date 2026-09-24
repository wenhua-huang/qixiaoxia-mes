-- ============================================================
-- V161: 销售订单新增「待接单」前置状态
-- ① 字典 mes_sal_order_status 新增 PENDING_ACCEPT（待接单/info/默认/排序1）
-- ② 默认项 CONFIRMED -> PENDING_ACCEPT；活跃项排序整体后移一位
-- ③ qxx_sal_order.status 列默认值改 PENDING_ACCEPT（存量行不动）
-- 说明：sys_dict_data 为系统表无 factory_id（仿 V151）；INSERT 幂等。
-- 日期：2026-09-24
-- ============================================================

SET NAMES utf8mb4;

-- ① 新增待接单字典项
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待接单', 'PENDING_ACCEPT', 'mes_sal_order_status', '', 'info', 'Y', '0', 'admin', sysdate(), '新建订单默认状态，接单后转已确认'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PENDING_ACCEPT');

-- 重复执行兜底规范化（Flyway 仅跑一次，此行保证脏环境下口径唯一）
UPDATE sys_dict_data SET dict_sort = 1, dict_label = '待接单', list_class = 'info', is_default = 'Y', status = '0'
WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PENDING_ACCEPT';

-- ② 默认项移交 + 活跃项排序顺延（停用的 PREPARE/PENDING 保持 sort 8/9）
UPDATE sys_dict_data SET is_default = 'N', dict_sort = 2
WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CONFIRMED';
UPDATE sys_dict_data SET dict_sort = 3 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PRODUCING';
UPDATE sys_dict_data SET dict_sort = 4 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'SHIPPED';
UPDATE sys_dict_data SET dict_sort = 5 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CLOSED';
UPDATE sys_dict_data SET dict_sort = 6 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CANCEL';

-- ③ 列默认值 + 注释（存量 CONFIRMED 行不受影响）
ALTER TABLE qxx_sal_order
  MODIFY COLUMN status varchar(64) DEFAULT 'PENDING_ACCEPT' COMMENT '订单状态：PENDING_ACCEPT待接单/CONFIRMED已确认/PRODUCING生产中/SHIPPED已出货/CLOSED已结单/CANCEL已取消';
