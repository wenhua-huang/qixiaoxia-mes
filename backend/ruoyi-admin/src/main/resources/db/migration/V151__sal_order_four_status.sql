-- ============================================================
-- V151: 销售订单状态收敛四态（已确认→生产中→已出货→已结单，CANCEL 链外保留）
-- ① 停用旧审核态字典项 PREPARE/PENDING（保留行以翻译历史数据）
-- ② CLOSED 文案「已关闭」→「已结单」，新增 PRODUCING/SHIPPED
-- ③ 存量在途单（PREPARE/PENDING）一律刷为 CONFIRMED（审核流已废弃，无审核入口）
-- ④ 列默认值 'PREPARE' → 'CONFIRMED'
-- ⑤ 删除提交/审核按钮权限（menu_id 2909/2916，V124 建）及其角色授权
-- 说明：③ 跨工厂全量刷是有意为之（在途单无工厂能再审），仿 V149 不限 factory_id 先例。
-- 幂等：UPDATE/DELETE 天然幂等；INSERT 用 WHERE NOT EXISTS。
-- 日期：2026-09-10
-- ============================================================

SET NAMES utf8mb4;

-- ① 停用旧审核态
UPDATE sys_dict_data SET status = '1'
WHERE dict_type = 'mes_sal_order_status' AND dict_value IN ('PREPARE', 'PENDING');

-- ② CLOSED 改文案/配色（dict_label 无唯一约束，按 type+value 定位）
UPDATE sys_dict_data SET dict_label = '已结单', list_class = 'info'
WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CLOSED';

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '生产中', 'PRODUCING', 'mes_sal_order_status', '', 'warning', 'N', '0', 'admin', sysdate(), '关联工单已开工'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PRODUCING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已出货', 'SHIPPED', 'mes_sal_order_status', '', 'primary', 'N', '0', 'admin', sysdate(), '订单全部明细已发齐'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'SHIPPED');

-- 活跃项重排展示顺序（旧 PREPARE/PENDING 已停用不显示）并设默认
UPDATE sys_dict_data SET dict_sort = 1, is_default = 'Y'
WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CONFIRMED';
UPDATE sys_dict_data SET dict_sort = 4 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CLOSED';
UPDATE sys_dict_data SET dict_sort = 5 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'CANCEL';
-- 旧审核态取消默认标记并沉底，保证唯一默认项与生命周期排序
UPDATE sys_dict_data SET is_default = 'N', dict_sort = 8 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PREPARE';
UPDATE sys_dict_data SET is_default = 'N', dict_sort = 9 WHERE dict_type = 'mes_sal_order_status' AND dict_value = 'PENDING';

-- ③ 存量刷态（跨工厂全量，有意）
UPDATE qxx_sal_order SET status = 'CONFIRMED' WHERE status IN ('PREPARE', 'PENDING');

-- ③-b 存量推导 PRODUCING：已派生且已开工（PRODUCING/COMPLETED）的未取消工单。
-- 事件链只覆盖迁移后的新开工，历史已开工单不会再发事件，必须一次性补齐。显式 factory_id（Flyway 不走拦截器）。
UPDATE qxx_sal_order o
SET o.status = 'PRODUCING', o.update_by = 'flyway', o.update_time = sysdate()
WHERE o.status = 'CONFIRMED'
  AND EXISTS (
      SELECT 1 FROM qxx_sal_order_line l
      INNER JOIN qxx_pro_workorder w ON w.sales_order_line_id = l.line_id
          AND w.factory_id = l.factory_id AND w.status IN ('PRODUCING', 'COMPLETED')
      WHERE l.order_id = o.order_id AND l.factory_id = o.factory_id
  );

-- ③-c 存量推导 SHIPPED：全部订单行在非作废出库单上的 SHIPPED 箱量已发齐。
-- 与 SalOrderMapper.markShippedIfFullyDelivered 同箱量口径；发运事件已是历史，不会再触发。
-- MySQL 派生表(FROM 子查询)不允许引用外层 o/l，行已发箱量改用「关联标量子查询」表达（WHERE 中的子查询可关联）。
UPDATE qxx_sal_order o
SET o.status = 'SHIPPED', o.update_by = 'flyway', o.update_time = sysdate()
WHERE o.status IN ('CONFIRMED', 'PRODUCING')
  AND EXISTS (SELECT 1 FROM qxx_sal_order_line l WHERE l.order_id = o.order_id AND l.factory_id = o.factory_id)
  AND NOT EXISTS (
      SELECT 1
      FROM qxx_sal_order_line l
      WHERE l.order_id = o.order_id AND l.factory_id = o.factory_id
        AND l.quantity > COALESCE((
            SELECT SUM(b.quantity)
            FROM qxx_wm_product_sales s
            INNER JOIN qxx_wm_product_sales_line sl ON sl.sales_id = s.sales_id AND sl.factory_id = s.factory_id
            INNER JOIN qxx_wm_product_sales_box b ON b.line_id = sl.line_id AND b.factory_id = sl.factory_id
                 AND b.status = 'SHIPPED'
            WHERE s.sales_order_id = o.order_id AND s.factory_id = o.factory_id AND s.status <> 'CANCELED'
              AND sl.sales_order_line_id = l.line_id
        ), 0)
  );

-- ④ 列默认值 + 注释（保持原 varchar(64) 可空定义）
ALTER TABLE qxx_sal_order
  MODIFY COLUMN status varchar(64) DEFAULT 'CONFIRMED' COMMENT '订单状态：CONFIRMED已确认/PRODUCING生产中/SHIPPED已出货/CLOSED已结单/CANCEL已取消';

-- ⑤ 删按钮角色授权 + 按钮菜单（幂等）
DELETE FROM sys_role_menu WHERE menu_id IN (2909, 2916);
DELETE FROM sys_menu WHERE menu_id IN (2909, 2916);
