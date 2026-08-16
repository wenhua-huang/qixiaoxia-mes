-- ============================================================
-- V134: 销售出库流程优化 — 文案改名 + SHIPPING 死状态清理
--
-- 背景：
--   1. "过账"语义在仓库一线易被理解为"事后记账"，而系统中过账实际是
--      "仓库出库扣库存"动作；"发货"实际是"登记物流信息"。命名反直觉，
--      改为"出库确认/发运登记"，使 DRAFT → 已出库 → 已发运 链路顺畅。
--      后端字段名(postedQuantity)、状态值(POSTED/SHIPPED)、权限串
--      (mes:wm:sales:post/ship)、接口路径全部保持不变，仅改用户可见文案。
--   2. 发运单列默认值与字典含 SHIPPING(待发运)，但服务层建单即写
--      IN_TRANSIT，SHIPPING 永不落库，cancel 端点因此对真实数据不可用。
--      清理 SHIPPING，真实撤销走 delete(回滚箱+头表发运量)。
--
-- 幂等：UPDATE/DELETE 均带 WHERE，可重复执行。
-- 字符集：utf8mb4
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- ────────────────────────────────────────────────────────────
-- Part 1: 字典 label 改名（mes_wm_sales_status）
-- ────────────────────────────────────────────────────────────
UPDATE sys_dict_data
SET dict_label = '部分出库', update_by = 'admin', update_time = NOW()
WHERE dict_type = 'mes_wm_sales_status' AND dict_value = 'PARTIAL_POSTED'
  AND dict_label <> '部分出库';

UPDATE sys_dict_data
SET dict_label = '已出库', update_by = 'admin', update_time = NOW()
WHERE dict_type = 'mes_wm_sales_status' AND dict_value = 'POSTED'
  AND dict_label <> '已出库';

UPDATE sys_dict_data
SET dict_label = '已发运', update_by = 'admin', update_time = NOW()
WHERE dict_type = 'mes_wm_sales_status' AND dict_value = 'SHIPPED'
  AND dict_label <> '已发运';

UPDATE sys_dict_type
SET remark = '销售出库单生命周期：草稿/部分出库/已出库/已发运/已关闭/已作废',
    update_by = 'admin', update_time = NOW()
WHERE dict_type = 'mes_wm_sales_status';

-- ────────────────────────────────────────────────────────────
-- Part 2: 菜单按钮名改名（perms 串不变）
-- ────────────────────────────────────────────────────────────
UPDATE sys_menu SET menu_name = '销售出库确认', update_by = 'admin', update_time = NOW()
WHERE perms = 'mes:wm:sales:post' AND menu_type = 'F' AND menu_name <> '销售出库确认';

UPDATE sys_menu SET menu_name = '销售出库发运', update_by = 'admin', update_time = NOW()
WHERE perms = 'mes:wm:sales:ship' AND menu_type = 'F' AND menu_name <> '销售出库发运';

-- 发运工作台页面菜单（C 类型，侧栏可见），与页面标题保持一致
UPDATE sys_menu SET menu_name = '发运登记工作台', update_by = 'admin', update_time = NOW()
WHERE perms = 'mes:wm:sales:ship:view' AND menu_type = 'C' AND menu_name <> '发运登记工作台';

-- ────────────────────────────────────────────────────────────
-- Part 3: 清理发运单 SHIPPING 死状态
-- ────────────────────────────────────────────────────────────

-- 3.1 兜底历史数据：若存在 SHIPPING 单据，统一提升为 IN_TRANSIT
UPDATE qxx_wm_product_sales_shipment
SET status = 'IN_TRANSIT', update_by = 'admin', update_time = NOW()
WHERE status = 'SHIPPING';

-- 3.2 列默认值改为 IN_TRANSIT，并收窄注释
ALTER TABLE qxx_wm_product_sales_shipment
    MODIFY COLUMN status varchar(20) DEFAULT 'IN_TRANSIT'
    COMMENT '发运单状态：IN_TRANSIT-在途,RECEIVED-已签收,CANCELED-已取消';

-- 3.3 删除字典中的 SHIPPING(待发运) 选项
DELETE FROM sys_dict_data
WHERE dict_type = 'mes_wm_shipment_status' AND dict_value = 'SHIPPING';

UPDATE sys_dict_type
SET remark = '发运单生命周期：在途/已签收/已取消',
    update_by = 'admin', update_time = NOW()
WHERE dict_type = 'mes_wm_shipment_status';
