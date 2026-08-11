-- V113: Code review 修复
-- 1. qxx_wm_outsource_order.status 默认值 ISSUED → DRAFT（与生命周期一致）
-- 2. 清理 V92 残留的 sys_role_menu 脏授权（role 11 获得了工序管理 2310* 的按钮权限）
-- 3. qxx_wm_outsource_order 补 outsource_factory_id（与外协 8 表约定一致）

-- 1. status 默认值改为 DRAFT
ALTER TABLE qxx_wm_outsource_order
  MODIFY COLUMN status varchar(20) DEFAULT 'DRAFT'
  COMMENT '状态:DRAFT-草稿,ISSUED-已发料,VENDOR_RCVD-厂商已签收,PROCESSING-加工中,FINISHED-已完工,SHIPPED-已发货,RECEIVED-已收货,CLOSED-已关闭';

-- 2. 清理 V92 误授：role 1/11 在 factory_id=0 下获得了工序管理(menu 2310)及其子按钮权限。
--    这些 menu_id 属于"工序管理"而非"分切作业"，V92 因 ID 冲突误插。
DELETE FROM sys_role_menu
WHERE role_id IN (1, 11)
  AND menu_id IN (2310, 23101, 23102, 23103, 23104, 23105)
  AND factory_id = 0;

-- 3. outsource_order 补 outsource_factory_id（从 vendor 回填）
ALTER TABLE qxx_wm_outsource_order
  ADD COLUMN outsource_factory_id bigint DEFAULT NULL COMMENT '外协场景:供应商对应系统工厂ID' AFTER factory_id,
  ADD KEY idx_outsource_factory_id (outsource_factory_id);

-- 回填已有单据的 outsource_factory_id（从厂商主数据）
UPDATE qxx_wm_outsource_order o
  LEFT JOIN qxx_md_vendor v ON v.vendor_id = o.vendor_id
SET o.outsource_factory_id = v.outsource_factory_id
WHERE o.outsource_factory_id IS NULL;
