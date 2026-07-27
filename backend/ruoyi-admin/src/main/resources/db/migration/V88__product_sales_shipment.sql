-- =====================================================================
-- V85: 销售出库 — 发运单 + 装箱明细 + 头表发运汇总字段
-- 依据：docs/设计文档/销售出库-发货流程设计.md（P1：多次发运 + 装箱 + 签收回单 + 打印）
-- 范式参照：V05(建表)、V71(头行三层)、V80(字典/编码规则/菜单幂等种子)
-- 约束：① Flyway 裸 JDBC，INSERT 显式 factory_id；② DML 幂等；③ 不可改已执行文件
-- FactoryIdInterceptor 自动注入 qxx_* 表 factory_id，XML 不写 factory_id
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================
-- Part 1: 建发运单表 qxx_wm_product_sales_shipment
-- 一张出库单可多次发运（部分发货），每次发运 = 一条 shipment
-- =====================================================
CREATE TABLE IF NOT EXISTS qxx_wm_product_sales_shipment (
    shipment_id        bigint(20)     not null auto_increment  comment '发运单ID',
    factory_id         bigint(20)     not null                 comment '工厂ID(关联qxx_md_factory)',
    sales_id           bigint(20)     not null                 comment '销售出库单ID(关联qxx_wm_product_sales)',
    shipment_code      varchar(64)    not null                 comment '发运单号(编码规则SHIP_NO)',
    ship_method        varchar(20)    default 'LOGISTICS'      comment '发货方式:LOGISTICS-物流,EXPRESS-快递,PICKUP-自提,SELF-客户自送',
    logistics_company  varchar(100)   default ''               comment '物流/承运商公司',
    tracking_no        varchar(100)   default ''               comment '运单号',
    logistics_fee      decimal(12,2)  default 0.00             comment '物流费用',
    vehicle_no         varchar(20)    default ''               comment '车牌号(自提/物流)',
    driver_name        varchar(50)    default ''               comment '司机姓名',
    driver_tel         varchar(30)    default ''               comment '司机电话',
    receiver_name      varchar(50)    default ''               comment '实际收货人',
    receiver_tel       varchar(30)    default ''               comment '收货人电话',
    shipping_address   varchar(500)   default ''               comment '收货详细地址',
    plan_ship_date     date           default null             comment '计划发货日期',
    actual_ship_date   datetime       default null             comment '实际发货时间',
    shipped_quantity   decimal(16,4)  default 0.0000           comment '本次发运数量(冗余,便于统计)',
    box_count          int(11)        default 0                comment '本次发运箱数',
    status             varchar(20)    default 'SHIPPING'       comment '发运单状态:SHIPPING-待发运,IN_TRANSIT-在途,RECEIVED-已签收,CANCELED-已取消',
    received_time      datetime       default null             comment '签收时间',
    received_by        varchar(50)    default ''               comment '签收人',
    received_remark    varchar(500)   default ''               comment '签收备注',
    attachment_url     varchar(500)   default ''               comment '回单附件URL(MinIO,逗号分隔多文件)',
    remark             varchar(500)   default ''               comment '备注',
    create_by          varchar(64)    default ''               comment '创建者',
    create_time        datetime       default current_timestamp comment '创建时间',
    update_by          varchar(64)    default ''               comment '更新者',
    update_time        datetime       default current_timestamp on update current_timestamp comment '更新时间',
    primary key (shipment_id),
    key idx_factory_id (factory_id),
    key idx_sales_id (sales_id),
    unique key uk_shipment_code (factory_id, shipment_code)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库-发运单(支持多次发运)';

-- =====================================================
-- Part 2: 建装箱明细表 qxx_wm_product_sales_box
-- 一箱一条；本期不支持混装（box.line_id 单行）
-- =====================================================
CREATE TABLE IF NOT EXISTS qxx_wm_product_sales_box (
    box_id             bigint(20)     not null auto_increment  comment '装箱ID',
    factory_id         bigint(20)     not null                 comment '工厂ID',
    sales_id           bigint(20)     not null                 comment '销售出库单ID',
    line_id            bigint(20)     default null             comment '出库行ID(关联qxx_wm_product_sales_line)',
    box_no             varchar(32)    not null                 comment '箱号(BOX-NNN 或扫码)',
    item_id            bigint(20)     default null             comment '物料ID',
    item_code          varchar(64)    default ''               comment '物料编码(快照)',
    item_name          varchar(200)   default ''               comment '物料名称(快照)',
    specification      varchar(200)   default ''               comment '规格(快照)',
    quantity           decimal(16,4)  default 0.0000           comment '本箱数量',
    unit_of_measure    varchar(20)    default ''               comment '计量单位编码',
    unit_name          varchar(20)    default ''               comment '单位名称',
    box_spec           varchar(50)    default ''               comment '箱规描述',
    box_length         decimal(10,2)  default 0.00             comment '箱长(cm)',
    box_width          decimal(10,2)  default 0.00             comment '箱宽(cm)',
    box_height         decimal(10,2)  default 0.00             comment '箱高(cm)',
    volume             decimal(12,4)  default 0.0000           comment '体积(m³,后端按长宽高算)',
    weight             decimal(12,4)  default 0.0000           comment '重量(kg)',
    shipment_id        bigint(20)     default null             comment '关联发运单ID(已发运则非空)',
    status             varchar(20)    default 'PACKED'         comment '装箱状态:PACKED-已装箱,SHIPPED-已发运',
    remark             varchar(500)   default ''               comment '备注(唛头等)',
    create_by          varchar(64)    default ''               comment '创建者',
    create_time        datetime       default current_timestamp comment '创建时间',
    update_by          varchar(64)    default ''               comment '更新者',
    update_time        datetime       default current_timestamp on update current_timestamp comment '更新时间',
    primary key (box_id),
    key idx_factory_id (factory_id),
    key idx_sales_id (sales_id),
    key idx_shipment_id (shipment_id),
    key idx_line_id (line_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库-装箱明细';

-- =====================================================
-- Part 3: 头表 qxx_wm_product_sales 加 3 列（幂等）
-- shipped_quantity 已发运量、ship_status 发运子状态、plan_ship_date 计划发货日期
-- =====================================================
DROP PROCEDURE IF EXISTS proc_add_sales_ship_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_sales_ship_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales' AND COLUMN_NAME='shipped_quantity') THEN
        ALTER TABLE qxx_wm_product_sales ADD COLUMN shipped_quantity decimal(16,4) default 0.0000 comment '已发运量' AFTER posted_quantity;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales' AND COLUMN_NAME='ship_status') THEN
        ALTER TABLE qxx_wm_product_sales ADD COLUMN ship_status varchar(20) default 'UN_SHIPPED' comment '发运状态:UN_SHIPPED-未发运,PARTIAL_SHIPPED-部分发运,SHIPPED-已发运,RECEIVED-已签收' AFTER shipped_quantity;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_product_sales' AND COLUMN_NAME='plan_ship_date') THEN
        ALTER TABLE qxx_wm_product_sales ADD COLUMN plan_ship_date date default null comment '计划发货日期' AFTER ship_status;
    END IF;
END$$
DELIMITER ;
CALL proc_add_sales_ship_cols();
DROP PROCEDURE IF EXISTS proc_add_sales_ship_cols;

-- =====================================================
-- Part 4: 字典种子（不固定显式 ID，按 dict_type 幂等，避免与既有 ID 段冲突）
-- =====================================================
-- 4.1 发货方式 mes_wm_ship_method
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '发货方式', 'mes_wm_ship_method', '0', 'admin', NOW(), '销售出库发货方式：物流/快递/自提/客户自送'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_ship_method');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '物流', 'LOGISTICS', 'mes_wm_ship_method', '', 'primary', 'Y', '0', 'admin', NOW(), '物流公司承运'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_method' AND dict_value='LOGISTICS');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '快递', 'EXPRESS', 'mes_wm_ship_method', '', 'success', 'N', '0', 'admin', NOW(), '快递承运'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_method' AND dict_value='EXPRESS');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '自提', 'PICKUP', 'mes_wm_ship_method', '', 'warning', 'N', '0', 'admin', NOW(), '客户自提'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_method' AND dict_value='PICKUP');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '客户自送', 'SELF', 'mes_wm_ship_method', '', 'info', 'N', '0', 'admin', NOW(), '客户安排车辆自送'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_method' AND dict_value='SELF');

-- 4.2 发运单状态 mes_wm_shipment_status
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '发运单状态', 'mes_wm_shipment_status', '0', 'admin', NOW(), '发运单生命周期：待发运/在途/已签收/已取消'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_shipment_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待发运', 'SHIPPING', 'mes_wm_shipment_status', '', 'info', 'Y', '0', 'admin', NOW(), '已登记待发出'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_shipment_status' AND dict_value='SHIPPING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '在途', 'IN_TRANSIT', 'mes_wm_shipment_status', '', 'primary', 'N', '0', 'admin', NOW(), '已发出在途'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_shipment_status' AND dict_value='IN_TRANSIT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已签收', 'RECEIVED', 'mes_wm_shipment_status', '', 'success', 'N', '0', 'admin', NOW(), '客户已签收'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_shipment_status' AND dict_value='RECEIVED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已取消', 'CANCELED', 'mes_wm_shipment_status', '', 'danger', 'N', '0', 'admin', NOW(), '已取消'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_shipment_status' AND dict_value='CANCELED');

-- 4.3 头表发运状态 mes_wm_ship_status
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '销售出库发运状态', 'mes_wm_ship_status', '0', 'admin', NOW(), '出库单发运子状态：未发运/部分发运/已发运/已签收'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_ship_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '未发运', 'UN_SHIPPED', 'mes_wm_ship_status', '', 'info', 'Y', '0', 'admin', NOW(), '尚未登记发运'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_status' AND dict_value='UN_SHIPPED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '部分发运', 'PARTIAL_SHIPPED', 'mes_wm_ship_status', '', 'warning', 'N', '0', 'admin', NOW(), '多次发运进行中'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_status' AND dict_value='PARTIAL_SHIPPED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已发运', 'SHIPPED', 'mes_wm_ship_status', '', 'primary', 'N', '0', 'admin', NOW(), '全部数量已发运'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_status' AND dict_value='SHIPPED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已签收', 'RECEIVED', 'mes_wm_ship_status', '', 'success', 'N', '0', 'admin', NOW(), '全部发运已签收'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_ship_status' AND dict_value='RECEIVED');

-- 4.4 装箱状态 mes_wm_box_status
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '装箱状态', 'mes_wm_box_status', '0', 'admin', NOW(), '装箱明细状态：已装箱/已发运'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_box_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '已装箱', 'PACKED', 'mes_wm_box_status', '', 'info', 'Y', '0', 'admin', NOW(), '已装箱待发运'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_box_status' AND dict_value='PACKED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已发运', 'SHIPPED', 'mes_wm_box_status', '', 'success', 'N', '0', 'admin', NOW(), '已随发运单发出'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_box_status' AND dict_value='SHIPPED');

-- =====================================================
-- Part 5: 自动编码规则 SHIP_NO（不固定 rule_id/part_id，按 rule_code 幂等）
-- 格式：SHP + yyyyMMdd + 3位流水（每日循环），例 SHP20260727001
-- =====================================================
INSERT INTO sys_auto_code_rule
    (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time)
SELECT 1, 'SHIP_NO', '发运单号', '格式:SHP20260727001', 15, 'N', '0', 'L', '1', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code = 'SHIP_NO' AND factory_id = 1);
SET @shipRuleId = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'SHIP_NO' AND factory_id = 1);

INSERT INTO sys_auto_code_part
    (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, create_by, create_time)
SELECT 1, @shipRuleId, 1, 'FIXCHAR', 'PREFIX_SHP', '固定前缀SHP', 3, 'SHP', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @shipRuleId AND part_index = 1);
INSERT INTO sys_auto_code_part
    (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, create_by, create_time)
SELECT 1, @shipRuleId, 2, 'NOWDATE', 'DATE_PART', '日期(yyyyMMdd)', 8, 'yyyyMMdd', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @shipRuleId AND part_index = 2);
INSERT INTO sys_auto_code_part
    (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, @shipRuleId, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)', 3, 1, 1, '1', 'DAY', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @shipRuleId AND part_index = 3);

-- =====================================================
-- Part 6: 菜单权限 — 发货工作台 + 签收按钮
-- 依赖：V36 建的 sales 主菜单 perms='mes:wm:sales:list' (menu_type='C', parent_id=2002)
-- 参照：V80 Part 4 菜单幂等范式
-- =====================================================
SET @salesMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'mes:wm:sales:list' AND menu_type = 'C' LIMIT 1);

-- 6.1 发货工作台（独立菜单页，type=C，独立 perms=mes:wm:sales:ship:view 避免与 V80 按钮冲突）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '发货工作台', 2002, 13, 'product_sales_ship', 'mes/wm/product_sales_ship/index', 1, 0, 'C', '0', '0', 'mes:wm:sales:ship:view', 'van', 'admin', NOW(), '', NULL, '发货工作台（独立菜单页）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:ship:view' AND menu_type = 'C');

-- 取发货工作台菜单 ID（动态）
SET @shipMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'mes:wm:sales:ship:view' AND menu_type = 'C' LIMIT 1);

-- 6.2 签收按钮（type=F，挂在发货工作台下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '发运签收', @shipMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:wm:sales:receive', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:sales:receive' AND menu_type = 'F');

-- 6.3 给管理员角色授权（role_id=1，factory_id=0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE ((perms = 'mes:wm:sales:ship:view' AND menu_type = 'C')
    OR (perms = 'mes:wm:sales:receive' AND menu_type = 'F'))
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.menu_id = sys_menu.menu_id AND rm.role_id = 1 AND rm.factory_id = 0
  );
