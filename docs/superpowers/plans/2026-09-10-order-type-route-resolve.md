# 订单类型 + 两标志位驱动默认工艺路线 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 销售订单按「订单类型（5 值）+ 是否外发 + 是否包装」开单时自动带出默认工艺路线（可人工改选），五条业务线共用同一套订单/工单/工序任务，外发订单路线必含外发工序。

**Architecture:** 在现有产品-路线绑定表 `qxx_pro_route_product` 上加三个适用维度列（NULL=通配）+默认列；新增纯解析服务按"精确匹配数优先"打分选路线；订单保存时后端逐行只补空、不覆盖手选，外发=Y 无含外发节点路线时硬阻断；订单行落 routeProductId 快照，转工单沿用。无新表（除加列）、无新菜单、无工单级工序快照。

**Tech Stack:** Spring Boot 4 / MyBatis（XML）/ Flyway / JUnit5+Mockito / Testcontainers；Vue 3.5 `<script setup>` 与 Options API 混用 / Element Plus / Vitest。

设计文档：`docs/superpowers/specs/2026-09-10-order-type-route-resolve-design.md`

## Global Constraints

- 工作目录：worktree `/Users/huangwenhua/company/self/qixiaoxia-mes-b2`（分支 `B2_feature`），所有命令在此目录执行。
- Flyway：下一个版本号 **V150**（当前最新 V149）；文件放 `backend/ruoyi-admin/src/main/resources/db/migration/`；DML 必须幂等；除「存量枚举值重映射」的两条 UPDATE（系统级跨工厂变更，文件内注释说明）外，所有 INSERT 显式写 `factory_id=1`。
- SQL WHERE 业务查询必须带 factory_id 条件（MyBatis 拦截器注入参数值，XML 里仍写 `<if>`/条件）；业务代码 INSERT 不写 factory_id（拦截器注入），但 **Flyway 裸 JDBC 必须显式写 factory_id**。
- 标志位取值：订单域 Y/N（`outsource_flag/package_flag`，与同表 `sample_flag` 一致）；路线工序外发判定沿用现有 `is_outsource='1'/'0'`；绑定维度列 `apply_outsource/apply_package` 用 Y/N，NULL=通配。
- 订单类型 5 值：`STANDARD` 标品 / `SMALL_BATCH` 小批量 / `GIFT` 礼品 / `STOCK` 备货订单 / `PLATE` 制版；默认 STANDARD。
- 后端函数 ≤50 行、前端组件 ≤300 行（超限必须拆）；业务状态用枚举不用裸字符串；不新增第三方依赖。
- 后端单测：Mockito mock 全部 Mapper/Service，断网可跑，命名 `should_xxx_when_xxx` + `@DisplayName` 中文；前端组件测试必须 `vi.mock` API，禁止真实请求。
- 每个 Task 结束跑对应测试并提交一次；后端 Java 改动最终必须重新打包重启 + 真实接口实测（最后一个 Task），中间 Task 以 `mvn test`/`compile` 为准。
- 只改本 worktree 内文件，不动其他 worktree/仓库。

## File Structure

**后端新建**
- `backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/SalOrderType.java` — 订单类型枚举
- `backend/ruoyi-system/.../service/mes/pro/ProRouteResolveService.java`（接口）
- `backend/ruoyi-system/.../service/mes/pro/impl/ProRouteResolveServiceImpl.java` — 纯解析打分逻辑
- `backend/ruoyi-system/.../domain/mes/pro/dto/RouteResolveResult.java` — 解析结果 DTO
- `backend/ruoyi-system/.../domain/mes/pro/dto/RouteBatchResolveRequest.java` — 批量预览请求体
- 测试：`ProRouteResolveServiceTest.java`、扩展 `SalOrderServiceImplTest.java`、`V150MigrationIT.java`

**后端修改**
- `db/migration/V150__order_type_flags_route_resolve.sql`（新建）
- `domain/mes/sal/SalOrder.java`、`SalOrderLine.java` + 两个 Mapper XML
- `domain/mes/pro/ProRouteProduct.java` + `ProRouteProductMapper.xml`
- `service/mes/sal/impl/SalOrderServiceImpl.java`
- `service/mes/pro/impl/ProRouteProductServiceImpl.java`（is_default 唯一校验）
- `web/controller/mes/pro/ProRouteProductController.java`（resolve 端点）
- 两个导出器 `SalOrderDetailExcelExporter.java`、`SalOrderPdfExporter.java`

**前端新建**
- `src/views/mes/sal/order/ToWorkorderDialog.vue`（从 index.vue 抽出的转工单向导）
- `src/views/mes/sal/order/__tests__/LineEdit.spec.ts`

**前端修改**
- `src/types/api/mes/sal/order.ts`、`src/api/mes/pro/routeproduct.ts`
- `src/views/mes/sal/order/index.vue`、`LineEdit.vue`、`detail.vue`
- `src/views/mes/pro/proroute/index.vue`、`src/views/mes/pro/workorder/index.vue`

---

### Task 1: Flyway V150 — 加列、字典 5 值、存量迁移、验收种子

**Files:**
- Create: `backend/ruoyi-admin/src/main/resources/db/migration/V150__order_type_flags_route_resolve.sql`

**Interfaces:**
- Produces: DB 列 `qxx_sal_order.outsource_flag/package_flag`、`qxx_sal_order_line.route_product_id/route_code/route_name`、`qxx_pro_route_product.apply_order_type/apply_outsource/apply_package/is_default`；字典 5 值；种子路线 `RT-PLATE/RT-SMALL/RT-GIFT`；种子物料 `ITEM-PLATE-DEMO/ITEM-SMALL-DEMO/ITEM-GIFT-DEMO`；外发工序 5 个 `PRC-DESIGN/PRC-PLATE/PRC-DIECUT/PRC-GLUE/PRC-INSPECT`。

- [ ] **Step 1: 写迁移文件（完整内容如下）**

```sql
-- ============================================================
-- V150: 订单类型(5值) + 是否外发/是否包装 标志位 + 按维度匹配默认工艺路线
--
-- 含:
--  ① qxx_sal_order 加 outsource_flag/package_flag(Y/N)
--  ② qxx_sal_order_line 加 route_product_id/route_code/route_name(带出留痕)
--  ③ qxx_pro_route_product 加 apply_order_type/apply_outsource/apply_package
--     (NULL=通配) + is_default
--  ④ 字典 mes_sal_order_type 调整为 标品/小批量/礼品/备货/制版;
--     NEW/REPEAT 存量重映射为 STANDARD
--  ⑤ 验收种子: 新工序(完稿/制版/模切/上胶/检验)、3 条路线、3 个演示物料及绑定,
--     RT-OUTSRC 外发节点补供应商
--
-- 幂等: DDL 用 information_schema 守卫的存储过程(裸 CREATE PROCEDURE, 无 DELIMITER,
--   与 V100/V106 同款, Flyway 已验证可跑); DML 全部 WHERE NOT EXISTS / 按唯一键 UPDATE。
-- 注意: ④ 的两条存量 UPDATE 是系统级枚举值重命名, intentionally 跨全部工厂、
--   不带 factory_id(属 AGENTS.md factory_id 约束的明确例外); 其余 DML 均显式 factory_id=1。
-- 日期: 2026-09-10
-- ============================================================

SET NAMES utf8mb4;

-- ---------- ①②③ DDL(列存在则跳过; 裸 CREATE PROCEDURE, 风格与 V106/V100 一致) ----------
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_sal_order', 'outsource_flag', 'ALTER TABLE qxx_sal_order ADD COLUMN outsource_flag char(1) DEFAULT ''N'' COMMENT ''是否外发 Y/N'' AFTER sample_flag');
CALL add_col_if_missing('qxx_sal_order', 'package_flag', 'ALTER TABLE qxx_sal_order ADD COLUMN package_flag char(1) DEFAULT ''N'' COMMENT ''是否包装 Y/N'' AFTER outsource_flag');
CALL add_col_if_missing('qxx_sal_order_line', 'route_product_id', 'ALTER TABLE qxx_sal_order_line ADD COLUMN route_product_id bigint DEFAULT NULL COMMENT ''产品-路线绑定ID qxx_pro_route_product.record_id'' AFTER shipping_req');
CALL add_col_if_missing('qxx_sal_order_line', 'route_code', 'ALTER TABLE qxx_sal_order_line ADD COLUMN route_code varchar(50) DEFAULT NULL COMMENT ''路线编码快照'' AFTER route_product_id');
CALL add_col_if_missing('qxx_sal_order_line', 'route_name', 'ALTER TABLE qxx_sal_order_line ADD COLUMN route_name varchar(200) DEFAULT NULL COMMENT ''路线名称快照'' AFTER route_code');
CALL add_col_if_missing('qxx_pro_route_product', 'apply_order_type', 'ALTER TABLE qxx_pro_route_product ADD COLUMN apply_order_type varchar(50) DEFAULT NULL COMMENT ''适用订单类型;NULL=通配''');
CALL add_col_if_missing('qxx_pro_route_product', 'apply_outsource', 'ALTER TABLE qxx_pro_route_product ADD COLUMN apply_outsource char(1) DEFAULT NULL COMMENT ''适用是否外发 Y/N;NULL=通配''');
CALL add_col_if_missing('qxx_pro_route_product', 'apply_package', 'ALTER TABLE qxx_pro_route_product ADD COLUMN apply_package char(1) DEFAULT NULL COMMENT ''适用是否包装 Y/N;NULL=通配''');
CALL add_col_if_missing('qxx_pro_route_product', 'is_default', 'ALTER TABLE qxx_pro_route_product ADD COLUMN is_default char(1) DEFAULT ''N'' COMMENT ''同分时默认首选 Y/N''');

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- ---------- ④ 字典: 删旧两值, 备新五值 ----------
DELETE FROM sys_dict_data WHERE dict_type = 'mes_sal_order_type' AND dict_value IN ('NEW','REPEAT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '标品', 'STANDARD', 'mes_sal_order_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '柔印标品常规订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='STANDARD');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '小批量', 'SMALL_BATCH', 'mes_sal_order_type', '', 'info', 'N', '0', 'admin', sysdate(), '小批量订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='SMALL_BATCH');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '礼品', 'GIFT', 'mes_sal_order_type', '', 'danger', 'N', '0', 'admin', sysdate(), '礼品盒订单(含包装)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='GIFT');
-- STOCK 由 V146 播种, 仅校正排序
UPDATE sys_dict_data SET dict_sort=4 WHERE dict_type='mes_sal_order_type' AND dict_value='STOCK';
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '制版', 'PLATE', 'mes_sal_order_type', '', 'success', 'N', '0', 'admin', sysdate(), '完稿制版订单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value='PLATE');

-- 存量枚举值重映射(系统级, 跨工厂, 无 factory_id 过滤 —— 见文件头说明)
UPDATE qxx_sal_order     SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT') OR order_type IS NULL;
UPDATE qxx_pro_workorder SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT');

-- ---------- ⑤a 验收用工序(按 code 幂等, id 自增) ----------
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-DESIGN',  '完稿', '核对客户文件与版式要求', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-DESIGN');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-PLATE',   '制版', '输出印版并核对套印精度', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-PLATE');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-DIECUT',  '模切', '检查模具压力,首件确认尺寸', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-DIECUT');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-GLUE',    '上胶', '控制胶量均匀,防溢胶脱胶', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-GLUE');
INSERT INTO qxx_pro_process (factory_id, process_code, process_name, attention, enable_flag, create_by, create_time)
SELECT 1, 'PRC-INSPECT', '检验', '按标准抽检,不合格品隔离', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_process WHERE factory_id=1 AND process_code='PRC-INSPECT');

-- ---------- ⑤b 三条新路线(按 route_code 幂等) ----------
INSERT INTO qxx_pro_route (factory_id, route_code, route_name, enable_flag, remark, create_by, create_time)
SELECT 1, 'RT-PLATE', '制版工艺路线', '1', '完稿→制版→检验', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route WHERE factory_id=1 AND route_code='RT-PLATE');
INSERT INTO qxx_pro_route (factory_id, route_code, route_name, enable_flag, remark, create_by, create_time)
SELECT 1, 'RT-SMALL', '小批量工艺路线', '1', '印刷→制袋→检验→包装', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route WHERE factory_id=1 AND route_code='RT-SMALL');
INSERT INTO qxx_pro_route (factory_id, route_code, route_name, enable_flag, remark, create_by, create_time)
SELECT 1, 'RT-GIFT', '礼品工艺路线', '1', '模切→上胶→检验→包装', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route WHERE factory_id=1 AND route_code='RT-GIFT');

-- ---------- ⑤c 路线工序(按 route+process 幂等, id/order_num 用子查询) ----------
-- 制版: 完稿(1)→制版(2)→检验(3)
INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, link_type, key_flag, is_check, is_outsource, create_by, create_time)
SELECT 1, r.route_id, p.process_id, p.process_code, p.process_name, n.n, 'SS',
       CASE WHEN p.process_code='PRC-PLATE' THEN 'Y' ELSE 'N' END,
       CASE WHEN p.process_code='PRC-INSPECT' THEN 'Y' ELSE 'N' END,
       '0', 'admin', NOW()
FROM qxx_pro_route r
JOIN (SELECT 'PRC-DESIGN' AS code, 1 AS n UNION ALL SELECT 'PRC-PLATE',2 UNION ALL SELECT 'PRC-INSPECT',3) n
JOIN qxx_pro_process p ON p.factory_id=1 AND p.process_code=n.code
WHERE r.factory_id=1 AND r.route_code='RT-PLATE'
  AND NOT EXISTS (SELECT 1 FROM qxx_pro_route_process rp WHERE rp.route_id=r.route_id AND rp.process_id=p.process_id);

-- 小批量: 印刷(PRC-PRINT,1)→制袋(PRC-BAG,2)→检验(3)→包装(PRC-PACK,4)
INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, link_type, key_flag, is_check, is_outsource, create_by, create_time)
SELECT 1, r.route_id, p.process_id, p.process_code, p.process_name, n.n, 'SS',
       CASE WHEN p.process_code='PRC-BAG' THEN 'Y' ELSE 'N' END,
       CASE WHEN p.process_code='PRC-INSPECT' THEN 'Y' ELSE 'N' END,
       '0', 'admin', NOW()
FROM qxx_pro_route r
JOIN (SELECT 'PRC-PRINT' AS code, 1 AS n UNION ALL SELECT 'PRC-BAG',2 UNION ALL SELECT 'PRC-INSPECT',3 UNION ALL SELECT 'PRC-PACK',4) n
JOIN qxx_pro_process p ON p.factory_id=1 AND p.process_code=n.code
WHERE r.factory_id=1 AND r.route_code='RT-SMALL'
  AND NOT EXISTS (SELECT 1 FROM qxx_pro_route_process rp WHERE rp.route_id=r.route_id AND rp.process_id=p.process_id);

-- 礼品: 模切(1)→上胶(2)→检验(3)→包装(4)
INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, link_type, key_flag, is_check, is_outsource, create_by, create_time)
SELECT 1, r.route_id, p.process_id, p.process_code, p.process_name, n.n, 'SS',
       CASE WHEN p.process_code IN ('PRC-DIECUT','PRC-GLUE') THEN 'Y' ELSE 'N' END,
       CASE WHEN p.process_code='PRC-INSPECT' THEN 'Y' ELSE 'N' END,
       '0', 'admin', NOW()
FROM qxx_pro_route r
JOIN (SELECT 'PRC-DIECUT' AS code, 1 AS n UNION ALL SELECT 'PRC-GLUE',2 UNION ALL SELECT 'PRC-INSPECT',3 UNION ALL SELECT 'PRC-PACK',4) n
JOIN qxx_pro_process p ON p.factory_id=1 AND p.process_code=n.code
WHERE r.factory_id=1 AND r.route_code='RT-GIFT'
  AND NOT EXISTS (SELECT 1 FROM qxx_pro_route_process rp WHERE rp.route_id=r.route_id AND rp.process_id=p.process_id);

-- ---------- ⑤d 三个演示物料(按 item_code 幂等) ----------
INSERT INTO qxx_md_item (factory_id, item_code, item_name, specification, unit_of_measure, unit_name, enable_flag, create_by, create_time)
SELECT 1, 'ITEM-PLATE-DEMO', '制版演示印版', '标准版式', 'PCS', '个', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_item WHERE factory_id=1 AND item_code='ITEM-PLATE-DEMO');
INSERT INTO qxx_md_item (factory_id, item_code, item_name, specification, unit_of_measure, unit_name, enable_flag, create_by, create_time)
SELECT 1, 'ITEM-SMALL-DEMO', '小批量演示纸袋', '小批量规格', 'PCS', '个', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_item WHERE factory_id=1 AND item_code='ITEM-SMALL-DEMO');
INSERT INTO qxx_md_item (factory_id, item_code, item_name, specification, unit_of_measure, unit_name, enable_flag, create_by, create_time)
SELECT 1, 'ITEM-GIFT-DEMO', '礼品盒演示产品', '礼盒装', 'PCS', '个', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_md_item WHERE factory_id=1 AND item_code='ITEM-GIFT-DEMO');

-- ---------- ⑤e 物料-路线绑定 + 维度标签(按 route+item 幂等) ----------
INSERT INTO qxx_pro_route_product (factory_id, route_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, apply_order_type, apply_outsource, apply_package, is_default, create_by, create_time)
SELECT 1, r.route_id, i.item_id, i.item_code, i.item_name, i.unit_of_measure, i.unit_name, 1,
       m.apply_type, m.apply_out, m.apply_pkg, 'Y', 'admin', NOW()
FROM (
  SELECT 'RT-PLATE' AS rc, 'ITEM-PLATE-DEMO' AS ic, 'PLATE' AS apply_type, NULL AS apply_out, NULL AS apply_pkg
  UNION ALL SELECT 'RT-SMALL','ITEM-SMALL-DEMO','SMALL_BATCH', NULL, NULL
  UNION ALL SELECT 'RT-GIFT','ITEM-GIFT-DEMO','GIFT', NULL, 'Y'
) m
JOIN qxx_pro_route r ON r.factory_id=1 AND r.route_code=m.rc
JOIN qxx_md_item  i ON i.factory_id=1 AND i.item_code=m.ic
WHERE NOT EXISTS (SELECT 1 FROM qxx_pro_route_product rp WHERE rp.route_id=r.route_id AND rp.item_id=i.item_id);

-- ---------- ⑤f 存量绑定打标 ----------
-- 外发路线绑定: 适用于外发=Y
UPDATE qxx_pro_route_product rp JOIN qxx_pro_route r ON r.route_id=rp.route_id
SET rp.apply_outsource='Y'
WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.apply_outsource IS NULL;
-- 标准路线绑定: 适用标品且同分默认
UPDATE qxx_pro_route_product rp JOIN qxx_pro_route r ON r.route_id=rp.route_id
SET rp.apply_order_type='STANDARD', rp.is_default='Y'
WHERE rp.factory_id=1 AND r.route_code='RT-STANDARD' AND rp.apply_order_type IS NULL;

-- ---------- ⑤g RT-OUTSRC 外发节点补供应商(节点本身按 route+process 定位) ----------
UPDATE qxx_pro_route_process rp
JOIN qxx_pro_route r ON r.route_id=rp.route_id
JOIN qxx_md_vendor v ON v.factory_id=1 AND v.vendor_code='OUT-WANLONG'
SET rp.vendor_id=v.vendor_id, rp.vendor_code=v.vendor_code, rp.vendor_name=v.vendor_name
WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.is_outsource='1'
  AND rp.vendor_id IS NULL;
-- 注: outsource_factory_id 保持 NULL, 与现网既有万隆外协节点口径一致(万隆无对应系统工厂)
```

> DDL 写法已对齐 V100/V106 生产先例（裸 CREATE PROCEDURE，无 DELIMITER，Flyway 可直接执行）。

- [ ] **Step 2: 本地库执行验证（Flyway 随后端启动执行，也可手动）**

注意：`mysql` 客户端**不能**直接执行裸 CREATE PROCEDURE（内部分号会被当语句结束，报 1064）；Flyway 能识别 BEGIN…END 块，提交文件必须保持 V100/V106 风格（无 DELIMITER）。本地 CLI 手测时用 awk 包一层 DELIMITER 生成临时文件：

Run:
```bash
awk '/^DROP PROCEDURE IF EXISTS add_col_if_missing;$/&&!d{print;print "DELIMITER //";p=1;next} p&&/^END;$/{print "END //";print "DELIMITER ;";p=0;d=1;next}{print}' \
  backend/ruoyi-admin/src/main/resources/db/migration/V150__order_type_flags_route_resolve.sql > /tmp/V150_cli_test.sql
docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes < /tmp/V150_cli_test.sql 2>&1 | grep -v "Using a password" ; echo "exit=$?"
docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes -e "
SELECT dict_value,dict_label FROM sys_dict_data WHERE dict_type='mes_sal_order_type' ORDER BY dict_sort;
SELECT route_code, COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.route_code IN ('RT-PLATE','RT-SMALL','RT-GIFT') GROUP BY route_code;
SELECT item_code, apply_order_type, apply_outsource, apply_package, is_default FROM qxx_pro_route_product rp JOIN qxx_md_item i ON i.item_id=rp.item_id WHERE i.item_code LIKE 'ITEM-%-DEMO';
SELECT order_type, COUNT(*) FROM qxx_sal_order GROUP BY order_type;
SELECT COUNT(*) AS still_null_vendor FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.route_code='RT-OUTSRC' AND rp.is_outsource='1' AND rp.vendor_id IS NULL;"
```
Expected: 字典返回 5 行（STANDARD/SMALL_BATCH/GIFT/STOCK/PLATE）；3 条新路线工序数分别为 3/4/4；3 个演示绑定标签正确（GIFT 行 apply_package=Y）；订单只有 STANDARD/STOCK；still_null_vendor=0。

- [ ] **Step 3: 再跑一次验证幂等**

Run: `docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes < /tmp/V150_cli_test.sql`（沿用 Step 2 包装文件）
Expected: 无报错；重复 Step 2 的查询，行数/计数不变（字典仍 5 行、绑定不重复）。
说明：手动 mysql 执行不写 flyway_schema_history；后端首次启动时 Flyway 会再跑一遍同一文件并登记历史——因全部语句幂等（存储过程判列存在、DML 均 WHERE NOT EXISTS），二遍执行安全无副作用，不需要 repair。

- [ ] **Step 4: Commit**

```bash
git add backend/ruoyi-admin/src/main/resources/db/migration/V150__order_type_flags_route_resolve.sql
git commit -m "feat(sal): V150 订单类型5值+外发/包装标志+路线维度列与验收种子"
```

---

### Task 2: SalOrderType 枚举并替换全部 NEW/REPEAT 字面量

**Files:**
- Create: `backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/SalOrderType.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/SalOrder.java:30`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java`（约 :121、:147、:410）
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/export/SalOrderDetailExcelExporter.java:157-160`
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/export/SalOrderPdfExporter.java:189-192`

**Interfaces:**
- Produces: `SalOrderType.STANDARD.getCode()` 等，供 Task 7 与导出器使用。

- [ ] **Step 1: 新建枚举（仿 SalOrderStatus 风格）**

```java
package com.ruoyi.common.enums;

/**
 * 销售订单类型枚举（字典 mes_sal_order_type）
 *
 * <p>五条业务线由 订单类型 + 是否外发 + 是否包装 三个开关组合区分，
 * 不走五套流程：
 * <ul>
 *   <li>STANDARD 标品：柔印标品（含完稿制版线按制版产品+制版路线区分前的常规口径）</li>
 *   <li>SMALL_BATCH 小批量 / GIFT 礼品(含包装) / PLATE 制版(完稿制版线)</li>
 *   <li>STOCK 备货订单：无客户备货生产，走同一审核与转工单链路</li>
 * </ul>
 * 外发不是独立类型，而是订单头 outsource_flag=Y，可与任一类型组合。
 *
 * @author qixiaoxia
 * @date 2026-09-10
 */
public enum SalOrderType {

    STANDARD("STANDARD", "标品"),
    SMALL_BATCH("SMALL_BATCH", "小批量"),
    GIFT("GIFT", "礼品"),
    STOCK("STOCK", "备货订单"),
    PLATE("PLATE", "制版");

    private final String code;
    private final String info;

    SalOrderType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    public boolean is(String code) { return this.code.equals(code); }

    /** 未匹配返回 null */
    public static SalOrderType fromCode(String code) {
        if (code == null) return null;
        for (SalOrderType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
```

- [ ] **Step 2: 替换实体 Excel 注解**

`SalOrder.java:30` 改为：
```java
    @Excel(name = "订单类型", readConverterExp = "STANDARD=标品,SMALL_BATCH=小批量,GIFT=礼品,STOCK=备货订单,PLATE=制版")
    private String orderType;
```

- [ ] **Step 3: 替换 Service 三处默认值**

`SalOrderServiceImpl.java` 顶部加 import：
```java
import com.ruoyi.common.enums.SalOrderType;
```
- createWithLines 中 `if (order.getOrderType() == null) order.setOrderType("NEW");` →
  `if (order.getOrderType() == null) order.setOrderType(SalOrderType.STANDARD.getCode());`
- createFromCrm 中 `order.setOrderType("NEW");` → `order.setOrderType(SalOrderType.STANDARD.getCode());`
- buildWorkorderFromLine 中 `wo.setOrderType(StringUtils.isNotEmpty(order.getOrderType()) ? order.getOrderType() : "NEW");` →
  `wo.setOrderType(StringUtils.isNotEmpty(order.getOrderType()) ? order.getOrderType() : SalOrderType.STANDARD.getCode());`

- [ ] **Step 4: 两个导出器 switch 换 5 值**

两个文件中相同的 switch 段（Excel 导出器约 :157-160、PDF 导出器约 :189-192）替换为：
```java
            case "STANDARD" -> "标品";
            case "SMALL_BATCH" -> "小批量";
            case "GIFT" -> "礼品";
            case "STOCK" -> "备货订单";
            case "PLATE" -> "制版";
            default -> str(s);
```

- [ ] **Step 5: 全局兜底搜索残留字面量**

Run: `grep -rn '"NEW"\|"REPEAT"' backend/ruoyi-system/src/main/java backend/ruoyi-admin/src/main/java | grep -iv 'newline\|renew' | grep -i 'order\|sal\|workorder' || echo "无残留"`
Expected: 输出「无残留」（或仅剩与订单类型无关的命中，逐个确认无关）。

- [ ] **Step 6: 编译 + 跑销售订单单测**

Run: `cd backend && mvn -q -pl ruoyi-common,ruoyi-system,ruoyi-admin -am compile 2>&1 | tail -5 && mvn -q -pl ruoyi-system test -Dtest=SalOrderServiceImplTest 2>&1 | tail -15`
Expected: BUILD SUCCESS；既有 SalOrderServiceImplTest 全绿（其构造的订单未设 orderType 时不影响断言；若有用例显式断言 "NEW"，同步改为 STANDARD）。

- [ ] **Step 7: Commit**

```bash
git add backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/SalOrderType.java backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/SalOrder.java backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/export/
git commit -m "refactor(sal): 新增 SalOrderType 枚举并切换为 5 值订单类型"
```

---

### Task 3: ProRouteProduct 增加四个维度字段（实体 + XML）

**Files:**
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProRouteProduct.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProRouteProductMapper.xml`

**Interfaces:**
- Produces: `ProRouteProduct.getApplyOrderType()/getApplyOutsource()/getApplyPackage()/getIsDefault()`（String，维度列可为 null）。Task 4 打分逻辑、Task 5 唯一校验、Task 12 前端维护都依赖。

- [ ] **Step 1: 实体加 4 字段与访问器**

在 `timeUnitType` 字段后加：
```java
    /** 适用订单类型 STANDARD/SMALL_BATCH/GIFT/STOCK/PLATE；null=通配 */
    private String applyOrderType;

    /** 适用是否外发 Y/N；null=通配 */
    private String applyOutsource;

    /** 适用是否包装 Y/N；null=通配 */
    private String applyPackage;

    /** 同产品同分时候选首选 Y/N */
    private String isDefault;
```
并补 4 对 getter/setter（仿同文件现有紧凑风格），toString 里追加 4 个 append。

- [ ] **Step 2: XML 四处同步**

`ProRouteProductMapper.xml`：
1. resultMap 在 `timeUnitType` 后加：
```xml
        <result property="applyOrderType"  column="apply_order_type"  />
        <result property="applyOutsource"  column="apply_outsource"  />
        <result property="applyPackage"    column="apply_package"    />
        <result property="isDefault"       column="is_default"       />
```
2. `selectProRouteProductVo` 的列清单加 `, apply_order_type, apply_outsource, apply_package, is_default`（加在 time_unit_type 后、remark 前）。
3. insert 两个 `<trim>` 各加（注意 update 不能把 null 维度"跳过"导致无法清空——本项目 update 用 `<if>` 非空才写，清空维度走"设为其他值"，可接受；但 is_default='N' 必须能写入，'N' 非空不受影响）：
```xml
            <if test="applyOrderType != null">apply_order_type,</if>
            <if test="applyOutsource != null">apply_outsource,</if>
            <if test="applyPackage != null">apply_package,</if>
            <if test="isDefault != null">is_default,</if>
```
对应 values trim：
```xml
            <if test="applyOrderType != null">#{applyOrderType},</if>
            <if test="applyOutsource != null">#{applyOutsource},</if>
            <if test="applyPackage != null">#{applyPackage},</if>
            <if test="isDefault != null">#{isDefault},</if>
```
4. update SET 加同样 4 行（`col = #{prop},`）。

- [ ] **Step 3: 编译**

Run: `cd backend && mvn -q -pl ruoyi-system -am compile 2>&1 | tail -5`
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProRouteProduct.java backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProRouteProductMapper.xml
git commit -m "feat(pro): 产品路线绑定增加适用订单类型/外发/包装/默认四个维度"
```

---

### Task 4: 路线解析服务 ProRouteResolveService（TDD）

**Files:**
- Create: `backend/ruoyi-system/.../domain/mes/pro/dto/RouteResolveResult.java`
- Create: `backend/ruoyi-system/.../service/mes/pro/ProRouteResolveService.java`
- Create: `backend/ruoyi-system/.../service/mes/pro/impl/ProRouteResolveServiceImpl.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProRouteResolveServiceTest.java`

**Interfaces:**
- Consumes: `ProRouteProductMapper.selectProRouteProductList(ProRouteProduct query)`（按 itemId 过滤，Task 3 后含 4 维度）、`ProRouteProcessMapper.selectProRouteProcessByRouteId(Long routeId)`（返回节点含 `getIsOutsource()`，'1'=外发）。
- Produces:
  - `RouteResolveResult`：`Long routeProductId, Long routeId, String routeCode, String routeName, boolean matched, boolean hardBlocked, String message`（全字段 getter/setter + 静态工厂 `matched(ProRouteProduct rp)`、`empty()`、`blocked(message)`）。
  - `RouteResolveResult resolve(Long itemId, String orderType, String outsourceFlag, String packageFlag)`
  - `java.util.Map<Long,RouteResolveResult> resolveBatch(java.util.Collection<Long> itemIds, String orderType, String outsourceFlag, String packageFlag)`

**匹配规则（唯一权威实现，后续 Task 7 保存回填与 Task 5 预览端点都调它）：**
1. 归一化入参：orderType 空→`STANDARD`；outsource/package 空→`N`。
2. 取该 itemId 全部绑定行；空列表 → `empty()`。
3. 逐候选：`applyOrderType/applyOutsource/applyPackage` 任一非 null 且与订单值不等 → 淘汰。
4. 订单外发=Y 时：该路线节点中无 `is_outsource='1'` → 淘汰，并标记"见过候选但全因外发出局"。
5. 存活候选打分：三维度各自"列非 null（精确指定）"加 1 分（0-3 分，精确越多越优先，通配排后）；排序 score desc, isDefault='Y' 优先, recordId asc，取首。
6. 外发=Y 且候选原本非空但第 4 步全部淘汰 → `blocked("产品[itemId]未配置含外发工序的工艺路线，请先在工艺路线主数据配置")`；其余无存活 → `empty()`。

- [ ] **Step 1: 先写失败测试（完整用例如下）**

```java
package com.ruoyi.system.service.mes.pro;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.service.mes.pro.impl.ProRouteResolveServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("默认工艺路线解析服务测试")
class ProRouteResolveServiceTest
{
    @Mock private ProRouteProductMapper routeProductMapper;
    @Mock private ProRouteProcessMapper routeProcessMapper;
    @InjectMocks private ProRouteResolveServiceImpl service;

    private ProRouteProduct rp(long id, long routeId, String type, String out, String pkg, String isDefault) {
        ProRouteProduct p = new ProRouteProduct();
        p.setRecordId(id); p.setRouteId(routeId); p.setItemId(100L);
        p.setRouteCodeViaSnapshot(routeId + "-RC"); // 见 Step 2 说明: 实体无 routeCode,用 routeName 承载断言
        p.setItemName("演示产品");
        p.setApplyOrderType(type); p.setApplyOutsource(out); p.setApplyPackage(pkg); p.setIsDefault(isDefault);
        return p;
    }

    private void stubRoutes(List<ProRouteProduct> rows) {
        when(routeProductMapper.selectProRouteProductList(any())).thenReturn(rows);
    }

    private void stubNodes(long routeId, boolean hasOutsource) {
        ProRouteProcess n = new ProRouteProcess();
        n.setIsOutsource(hasOutsource ? "1" : "0");
        when(routeProcessMapper.selectProRouteProcessByRouteId(routeId)).thenReturn(List.of(n));
    }

    @Test
    @DisplayName("三维精确匹配优先于通配候选")
    void should_pickExact_when_bothWildcardAndExactExist() {
        ProRouteProduct wildcard = rp(1L, 10L, null, null, null, "Y");
        ProRouteProduct exact = rp(2L, 11L, "GIFT", "N", "Y", "N");
        stubRoutes(List.of(wildcard, exact));
        stubNodes(10L, false); stubNodes(11L, false);
        RouteResolveResult r = service.resolve(100L, "GIFT", "N", "Y");
        assertThat(r.isMatched()).isTrue();
        assertThat(r.getRouteProductId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("维度不符的候选被淘汰")
    void should_excludeCandidate_when_dimensionMismatch() {
        stubRoutes(List.of(rp(1L, 10L, "PLATE", null, null, "N")));
        stubNodes(10L, false);
        RouteResolveResult r = service.resolve(100L, "STANDARD", "N", "N");
        assertThat(r.isMatched()).isFalse();
        assertThat(r.isHardBlocked()).isFalse();
    }

    @Test
    @DisplayName("同分时 isDefault=Y 胜出")
    void should_pickDefault_when_scoresTie() {
        ProRouteProduct a = rp(1L, 10L, "STANDARD", null, null, "N");
        ProRouteProduct b = rp(2L, 11L, "STANDARD", null, null, "Y");
        stubRoutes(List.of(a, b));
        stubNodes(10L, false); stubNodes(11L, false);
        assertThat(service.resolve(100L, "STANDARD", "N", "N").getRouteProductId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("同分时均非默认, recordId 小者稳定胜出")
    void should_pickLowerRecordId_when_tieAndNoDefault() {
        stubRoutes(List.of(rp(2L, 11L, null, null, null, "N"), rp(1L, 10L, null, null, null, "N")));
        stubNodes(10L, false); stubNodes(11L, false);
        assertThat(service.resolve(100L, "STANDARD", "N", "N").getRouteProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("外发=Y 且无任何含外发节点路线 -> hardBlocked")
    void should_block_when_outsourceOrderButNoOutsourceRoute() {
        ProRouteProduct tagged = rp(1L, 10L, null, "Y", null, "N");
        stubRoutes(List.of(tagged));
        stubNodes(10L, false); // 标签打了但节点没配外发
        RouteResolveResult r = service.resolve(100L, "STANDARD", "Y", "N");
        assertThat(r.isHardBlocked()).isTrue();
        assertThat(r.isMatched()).isFalse();
        assertThat(r.getMessage()).contains("外发");
    }

    @Test
    @DisplayName("外发=Y 命中含外发节点的路线")
    void should_matchOutsourceRoute_when_nodeFlagged() {
        stubRoutes(List.of(rp(1L, 10L, null, "Y", null, "N")));
        stubNodes(10L, true);
        RouteResolveResult r = service.resolve(100L, "STANDARD", "Y", "N");
        assertThat(r.isMatched()).isTrue();
        assertThat(r.getRouteProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("产品无任何绑定 -> 空结果不阻断")
    void should_returnEmpty_when_noBinding() {
        stubRoutes(List.of());
        RouteResolveResult r = service.resolve(100L, "STANDARD", "N", "N");
        assertThat(r.isMatched()).isFalse();
        assertThat(r.isHardBlocked()).isFalse();
    }

    @Test
    @DisplayName("入参空值归一化为 STANDARD/N/N")
    void should_normalizeInputs_when_nullArgs() {
        stubRoutes(List.of(rp(1L, 10L, "STANDARD", "N", "N", "Y")));
        stubNodes(10L, false);
        assertThat(service.resolve(100L, null, null, null).isMatched()).isTrue();
    }

    @Test
    @DisplayName("resolveBatch 为每个 itemId 返回独立结果")
    void should_returnMap_when_resolveBatch() {
        ProRouteProduct p1 = rp(1L, 10L, null, null, null, "Y"); p1.setItemId(100L);
        ProRouteProduct p2 = rp(2L, 20L, null, null, null, "Y"); p2.setItemId(200L);
        when(routeProductMapper.selectProRouteProductList(any())).thenAnswer(inv -> {
            ProRouteProduct q = inv.getArgument(0);
            return q.getItemId() == 100L ? List.of(p1) : q.getItemId() == 200L ? List.of(p2) : List.of();
        });
        stubNodes(10L, false); stubNodes(20L, false);
        var map = service.resolveBatch(List.of(100L, 200L, 300L), null, null, null);
        assertThat(map.get(100L).isMatched()).isTrue();
        assertThat(map.get(200L).isMatched()).isTrue();
        assertThat(map.get(300L).isMatched()).isFalse();
    }
}
```

> 说明：`ProRouteProduct` 没有 routeCode 字段（route_code 在 route 主表）。测试里删掉 `setRouteCodeViaSnapshot(...)` 行，`rp()` 中不设 code；matched 结果的 routeCode/routeName 由实现中按 routeId 补查（见 Step 2）。RouteResolveResult 需带 routeId 即可，routeCode/routeName 允许为 null（调用方需要时自行补，保存时由订单行冗余写入——见 Task 6/7，回填时调用方用 route 信息填充）。**实现选择（本计划定案）**：解析服务只返回 `routeProductId/routeId/matched/hardBlocked/message`，不 join 路线名称；route_code/route_name 快照由 Service 层回填时用 `ProRouteMapper.selectProRouteByRouteId` 补。故 DTO 不含 routeCode/routeName 字段，测试也不断言它们。

- [ ] **Step 2: 跑测试确认失败（类不存在）**

Run: `cd backend && mvn -q -pl ruoyi-system test -Dtest=ProRouteResolveServiceTest 2>&1 | tail -10`
Expected: 编译失败（找不到 ProRouteResolveServiceImpl / RouteResolveResult）。

- [ ] **Step 3: 实现 DTO 与服务**

`RouteResolveResult.java`：
```java
package com.ruoyi.system.domain.mes.pro.dto;

import com.ruoyi.system.domain.mes.pro.ProRouteProduct;

/** 路线解析结果。matched=命中;hardBlocked=外发硬约束不满足;两者皆否=留空手选 */
public class RouteResolveResult
{
    private Long routeProductId;
    private Long routeId;
    private boolean matched;
    private boolean hardBlocked;
    private String message;

    public static RouteResolveResult matched(ProRouteProduct rp) {
        RouteResolveResult r = new RouteResolveResult();
        r.routeProductId = rp.getRecordId();
        r.routeId = rp.getRouteId();
        r.matched = true;
        return r;
    }

    public static RouteResolveResult empty() { return new RouteResolveResult(); }

    public static RouteResolveResult blocked(String message) {
        RouteResolveResult r = new RouteResolveResult();
        r.hardBlocked = true;
        r.message = message;
        return r;
    }

    public Long getRouteProductId() { return routeProductId; }
    public Long getRouteId() { return routeId; }
    public boolean isMatched() { return matched; }
    public boolean isHardBlocked() { return hardBlocked; }
    public String getMessage() { return message; }
}
```

接口 `ProRouteResolveService.java`：
```java
package com.ruoyi.system.service.mes.pro;

import java.util.Collection;
import java.util.Map;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;

/** 按 订单类型+是否外发+是否包装 在产品绑定路线中解析默认路线 */
public interface ProRouteResolveService
{
    RouteResolveResult resolve(Long itemId, String orderType, String outsourceFlag, String packageFlag);

    Map<Long, RouteResolveResult> resolveBatch(Collection<Long> itemIds, String orderType,
                                               String outsourceFlag, String packageFlag);
}
```

实现 `ProRouteResolveServiceImpl.java`（≤50 行/方法，打分用私有方法拆分）：
```java
package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.enums.SalOrderType;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.service.mes.pro.ProRouteResolveService;

@Service
public class ProRouteResolveServiceImpl implements ProRouteResolveService
{
    private static final String YES = "Y";
    private static final String OUTSOURCE_NODE = "1";

    @Autowired private ProRouteProductMapper routeProductMapper;
    @Autowired private ProRouteProcessMapper routeProcessMapper;

    @Override
    public RouteResolveResult resolve(Long itemId, String orderType, String outsourceFlag, String packageFlag) {
        String type = orderType == null ? SalOrderType.STANDARD.getCode() : orderType;
        String out = outsourceFlag == null ? "N" : outsourceFlag;
        String pkg = packageFlag == null ? "N" : packageFlag;
        if (itemId == null) return RouteResolveResult.empty();

        List<ProRouteProduct> all = bindingsOf(itemId);
        if (all.isEmpty()) return RouteResolveResult.empty();

        List<ProRouteProduct> survivors = new ArrayList<>();
        for (ProRouteProduct rp : all) {
            if (dimensionMismatch(rp, type, out, pkg)) continue;
            if (YES.equals(out) && !routeHasOutsourceNode(rp.getRouteId())) continue;
            survivors.add(rp);
        }
        if (survivors.isEmpty() && YES.equals(out)) {
            return RouteResolveResult.blocked("产品[" + itemId + "]未配置含外发工序的工艺路线，请先在工艺路线主数据配置");
        }
        if (survivors.isEmpty()) return RouteResolveResult.empty();

        survivors.sort(comparator());
        return RouteResolveResult.matched(survivors.get(0));
    }

    @Override
    public Map<Long, RouteResolveResult> resolveBatch(Collection<Long> itemIds, String orderType,
                                                      String outsourceFlag, String packageFlag) {
        Map<Long, RouteResolveResult> map = new LinkedHashMap<>();
        if (itemIds != null) {
            for (Long itemId : itemIds) {
                map.put(itemId, resolve(itemId, orderType, outsourceFlag, packageFlag));
            }
        }
        return map;
    }

    private List<ProRouteProduct> bindingsOf(Long itemId) {
        ProRouteProduct query = new ProRouteProduct();
        query.setItemId(itemId);
        return routeProductMapper.selectProRouteProductList(query);
    }

    private boolean dimensionMismatch(ProRouteProduct rp, String type, String out, String pkg) {
        return rp.getApplyOrderType() != null && !rp.getApplyOrderType().equals(type)
            || rp.getApplyOutsource() != null && !rp.getApplyOutsource().equals(out)
            || rp.getApplyPackage() != null && !rp.getApplyPackage().equals(pkg);
    }

    private boolean routeHasOutsourceNode(Long routeId) {
        List<ProRouteProcess> nodes = routeProcessMapper.selectProRouteProcessByRouteId(routeId);
        return nodes != null && nodes.stream().anyMatch(n -> OUTSOURCE_NODE.equals(n.getIsOutsource()));
    }

    /** 精确维度多者优先; 同分 isDefault=Y; 再同分 recordId 升序稳定 */
    private Comparator<ProRouteProduct> comparator() {
        return Comparator
            .comparingInt((ProRouteProduct rp) -> exactDimensionCount(rp)).reversed()
            .thenComparing(rp -> YES.equals(rp.getIsDefault()) ? 0 : 1)
            .thenComparing(ProRouteProduct::getRecordId);
    }

    private int exactDimensionCount(ProRouteProduct rp) {
        int score = 0;
        if (rp.getApplyOrderType() != null) score++;
        if (rp.getApplyOutsource() != null) score++;
        if (rp.getApplyPackage() != null) score++;
        return score;
    }
}
```

- [ ] **Step 4: 测试转绿**

Run: `cd backend && mvn -q -pl ruoyi-system test -Dtest=ProRouteResolveServiceTest 2>&1 | tail -10`
Expected: BUILD SUCCESS，9 个用例全过。若排序用例失败，检查 `exactDimensionCount` 与 comparator 顺序。

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/dto/ backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/ backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProRouteResolveServiceTest.java
git commit -m "feat(pro): 默认工艺路线解析服务(维度打分+外发硬阻断)"
```

---

### Task 5: 解析预览端点 + 同产品 is_default 唯一校验

**Files:**
- Create: `backend/ruoyi-system/.../domain/mes/pro/dto/RouteBatchResolveRequest.java`
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProRouteProductController.java`
- Modify: `backend/ruoyi-system/.../service/mes/pro/impl/ProRouteProductServiceImpl.java`
- Modify: `backend/ruoyi-system/.../service/mes/pro/IProRouteProductService.java`（如需新增校验方法签名则加；校验直接放 insert/update 内部则不动接口）

**Interfaces:**
- Produces: `GET /mes/pro/routeproduct/resolve?itemId=&orderType=&outsourceFlag=&packageFlag=` → AjaxResult(data=RouteResolveResult)；`POST /mes/pro/routeproduct/resolveBatch`（body `{itemIds:[...], orderType, outsourceFlag, packageFlag}`）→ AjaxResult(data=Map)。权限用 `mes:sal:order:list`（能看销售订单即可预览）。

- [ ] **Step 1: 写批量请求 DTO**

```java
package com.ruoyi.system.domain.mes.pro.dto;

import java.util.List;

/** 批量默认路线预览请求(头维度对多行相同) */
public class RouteBatchResolveRequest
{
    private List<Long> itemIds;
    private String orderType;
    private String outsourceFlag;
    private String packageFlag;

    public List<Long> getItemIds() { return itemIds; }
    public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getOutsourceFlag() { return outsourceFlag; }
    public void setOutsourceFlag(String outsourceFlag) { this.outsourceFlag = outsourceFlag; }
    public String getPackageFlag() { return packageFlag; }
    public void setPackageFlag(String packageFlag) { this.packageFlag = packageFlag; }
}
```

- [ ] **Step 2: Controller 加两端点**

类上注入（与已有 proRouteProductService 并列）：
```java
    @Autowired
    private com.ruoyi.system.service.mes.pro.ProRouteResolveService proRouteResolveService;
```
方法（放在 listByRouteId 之后）：
```java
    /**
     * 按 订单类型+外发+包装 预览某产品的默认路线（开单联动用，纯读）
     */
    @PreAuthorize("@ss.hasPermi('mes:sal:order:list')")
    @GetMapping("/resolve")
    public AjaxResult resolve(@org.springframework.web.bind.annotation.RequestParam Long itemId,
                              @RequestParam(required = false) String orderType,
                              @RequestParam(required = false) String outsourceFlag,
                              @RequestParam(required = false) String packageFlag) {
        return success(proRouteResolveService.resolve(itemId, orderType, outsourceFlag, packageFlag));
    }

    /**
     * 批量预览默认路线（头维度变更后一键重算用，纯读）
     */
    @PreAuthorize("@ss.hasPermi('mes:sal:order:list')")
    @PostMapping("/resolveBatch")
    public AjaxResult resolveBatch(@RequestBody com.ruoyi.system.domain.mes.pro.dto.RouteBatchResolveRequest req) {
        return success(proRouteResolveService.resolveBatch(req.getItemIds(), req.getOrderType(),
                req.getOutsourceFlag(), req.getPackageFlag()));
    }
```
（@RequestParam/@RequestBody/@GetMapping/@PostMapping 按文件既有 import 决定是否展开为 import 行。）

- [ ] **Step 3: is_default 同产品唯一校验**

在 `ProRouteProductServiceImpl` 注入 `ProRouteProductMapper`（已有字段 `qxxProRouteProductMapper`，直接复用）。新增私有方法并在 insert/update 开头调用：
```java
    /** 同一产品至多一条 is_default='Y'，保证同分解析结果确定 */
    private void validateSingleDefault(ProRouteProduct p) {
        if (!"Y".equals(p.getIsDefault()) || p.getItemId() == null) return;
        ProRouteProduct query = new ProRouteProduct();
        query.setItemId(p.getItemId());
        List<ProRouteProduct> siblings = qxxProRouteProductMapper.selectProRouteProductList(query);
        for (ProRouteProduct sib : siblings) {
            if ("Y".equals(sib.getIsDefault()) && !sib.getRecordId().equals(p.getRecordId())) {
                throw new com.ruoyi.common.exception.ServiceException(
                        "产品[" + p.getItemName() + "]已存在默认路线，同产品只能设一条默认路线");
            }
        }
    }
```
`insertProRouteProduct` 首行调 `validateSingleDefault(p);`；`updateProRouteProduct` 首行同样调用（此时 recordId 为当前行自身，更新场景排除自身）。

- [ ] **Step 4: 编译 + 路由冒烟（打包前先 compile）**

Run: `cd backend && mvn -q -pl ruoyi-admin -am compile 2>&1 | tail -5`
Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProRouteProductController.java backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProRouteProductServiceImpl.java backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/dto/RouteBatchResolveRequest.java
git commit -m "feat(pro): 默认路线预览端点 + 同产品默认路线唯一校验"
```

---

### Task 6: SalOrder/SalOrderLine 新字段（实体 + Mapper XML）

**Files:**
- Modify: `backend/ruoyi-system/.../domain/mes/sal/SalOrder.java`
- Modify: `backend/ruoyi-system/.../domain/mes/sal/SalOrderLine.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderMapper.xml`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderLineMapper.xml`

**Interfaces:**
- Produces: `SalOrder.getOutsourceFlag()/getPackageFlag()`；`SalOrderLine.getRouteProductId()/getRouteCode()/getRouteName()`。Task 7 回填依赖。

- [ ] **Step 1: SalOrder 实体加两标志**

`sampleFlag` 字段后加（含 Excel 注解，与样品一致）：
```java
    @Excel(name = "是否外发", readConverterExp = "Y=是,N=否")
    private String outsourceFlag;

    @Excel(name = "是否包装", readConverterExp = "Y=是,N=否")
    private String packageFlag;
```
补两对 getter/setter（仿 sampleFlag）。

- [ ] **Step 2: SalOrderLine 实体加三字段**

`shippingReq` 后加：
```java
    /** 开单时带出的产品-路线绑定ID(qxx_pro_route_product.record_id)，可人工改选 */
    private Long routeProductId;

    /** 路线编码冗余快照 */
    private String routeCode;

    /** 路线名称冗余快照 */
    private String routeName;
```
补三对 getter/setter。

- [ ] **Step 3: SalOrderMapper.xml 同步**

resultMap 在 sampleFlag 后：
```xml
        <result property="outsourceFlag"    column="outsource_flag"    />
        <result property="packageFlag"     column="package_flag"       />
```
selectSalOrderVo 列清单在 `sample_flag` 后加 `, outsource_flag, package_flag`；insert 两个 trim、update SET 各加：
```xml
            <if test="outsourceFlag != null">outsource_flag,</if>
            <if test="packageFlag != null">package_flag,</if>
```
```xml
            <if test="outsourceFlag != null">#{outsourceFlag},</if>
            <if test="packageFlag != null">#{packageFlag},</if>
```
update SET：`<if test="outsourceFlag != null">outsource_flag = #{outsourceFlag},</if>`、`<if test="packageFlag != null">package_flag = #{packageFlag},</if>`。
（注意 update 的 `<if>` 非空才写：N 是非空字符串可正常写入；两列 DDL 默认 N，历史行无需补齐。）

- [ ] **Step 4: SalOrderLineMapper.xml 同步**

resultMap 在 shippingReq 后：
```xml
        <result property="routeProductId"  column="route_product_id"  />
        <result property="routeCode"       column="route_code"        />
        <result property="routeName"       column="route_name"        />
```
selectSalOrderLineVo 列清单在 shipping_req 后加 `, route_product_id, route_code, route_name`；insert 两 trim、update SET 各加三项（routeProductId 用 `!= null` 判断）：
```xml
            <if test="routeProductId != null">route_product_id,</if>
            <if test="routeCode != null">route_code,</if>
            <if test="routeName != null">route_name,</if>
```
对应 values 与 update SET 同构。

- [ ] **Step 5: 编译**

Run: `cd backend && mvn -q -pl ruoyi-system -am compile 2>&1 | tail -5`
Expected: BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/ backend/ruoyi-system/src/main/resources/mapper/mes/sal/
git commit -m "feat(sal): 订单头加外发/包装标志, 订单行加路线绑定快照三列"
```

---

### Task 7: 订单保存回填路线 + 两处外发硬校验（TDD）

**Files:**
- Modify: `backend/ruoyi-system/.../service/mes/sal/impl/SalOrderServiceImpl.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderServiceImplTest.java`

**Interfaces:**
- Consumes: Task 4 `ProRouteResolveService.resolve(...)`；`ProRouteProductMapper.selectProRouteProductByRecordId(Long)`（校验手选路线归属，已有 XML 方法）；`ProRouteMapper.selectProRouteByRouteId(Long)`（取路线 code/name 快照）；`ProRouteProcessMapper`（或复用 resolveService 不直接用——外发节点校验通过解析结果/节点查询）。
- 规则：
  - 保存逐行：routeProductId 为空 → resolve 自动填（含 route_code/name 快照）；非空 → 校验归属本产品，外发=Y 时校验该路线含外发节点，回填快照。
  - resolve 返回 hardBlocked → 抛 ServiceException 整单阻断。
  - update 路径同样（该服务 update 是删行重插，统一走 saveLines）。
  - 转工单 buildWorkorderFromLine：req.routeProductId 为空时取 line.routeProductId；外发=Y 最终仍无合规路线则阻断。

- [ ] **Step 1: 先加失败单测（追加到 SalOrderServiceImplTest）**

测试类新增 mock 字段（与现有 @Mock 并列）：
```java
    @Mock private com.ruoyi.system.service.mes.pro.ProRouteResolveService proRouteResolveService;
    @Mock private com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper proRouteProductMapper;
    @Mock private com.ruoyi.system.mapper.mes.pro.ProRouteMapper proRouteMapper;
```
setUp() 中无需默认打桩（LENIENT）。新增用例：
```java
    private SalOrderCreateRequest orderWithOneLine(String orderType, String outFlag, Long lineRouteId) {
        SalOrder order = new SalOrder();
        order.setOrderCode("SO-T-" + System.nanoTime());
        order.setOrderType(orderType);
        order.setOutsourceFlag(outFlag);
        order.setPackageFlag("N");
        order.setStatus("PREPARE");
        SalOrderLine line = new SalOrderLine();
        line.setProductId(100L); line.setProductCode("P1"); line.setProductName("演示");
        line.setQuantity(new BigDecimal("10"));
        line.setRouteProductId(lineRouteId);
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order); req.setLines(java.util.List.of(line));
        return req;
    }

    @Test
    @DisplayName("createWithLines - 行无路线时按头维度自动回填, 手选路线不被覆盖")
    void should_fillRoute_when_lineRouteBlank_andKeepWhenPresent() {
        // 场景1: 空 -> resolve 命中 recordId=2
        com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult hit =
            com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult.empty() == null ? null : null; // 占位, 下行使⽤⼯⼚
        when(proRouteResolveService.resolve(eq(100L), eq("STANDARD"), eq("N"), eq("N")))
            .thenReturn(matchedResult(2L, 20L));
        when(salOrderMapper.insertSalOrder(any())).thenAnswer(inv -> { ((SalOrder) inv.getArgument(0)).setOrderId(1L); return 1; });
        when(proRouteMapper.selectProRouteByRouteId(20L)).thenReturn(route(20L, "RT-STANDARD", "标准"));

        SalOrderCreateRequest req = orderWithOneLine("STANDARD", "N", null);
        salOrderService.createWithLines(req);

        ArgumentCaptor<SalOrderLine> cap = ArgumentCaptor.forClass(SalOrderLine.class);
        verify(salOrderLineMapper).insertSalOrderLine(cap.capture());
        assertThat(cap.getValue().getRouteProductId()).isEqualTo(2L);
        assertThat(cap.getValue().getRouteCode()).isEqualTo("RT-STANDARD");

        // 场景2: 已选 recordId=9 不被覆盖, 仅校验+补快照
        reset(proRouteResolveService, proRouteProductMapper, proRouteMapper, salOrderLineMapper);
        com.ruoyi.system.domain.mes.pro.ProRouteProduct own = new com.ruoyi.system.domain.mes.pro.ProRouteProduct();
        own.setRecordId(9L); own.setRouteId(90L); own.setItemId(100L);
        when(proRouteProductMapper.selectProRouteProductByRecordId(9L)).thenReturn(own);
        when(proRouteMapper.selectProRouteByRouteId(90L)).thenReturn(route(90L, "RT-MANUAL", "手选"));
        when(salOrderMapper.insertSalOrder(any())).thenAnswer(inv -> { ((SalOrder) inv.getArgument(0)).setOrderId(2L); return 1; });
        SalOrderCreateRequest req2 = orderWithOneLine("STANDARD", "N", 9L);
        salOrderService.createWithLines(req2);
        verify(proRouteResolveService, never()).resolve(any(), any(), any(), any());
        ArgumentCaptor<SalOrderLine> cap2 = ArgumentCaptor.forClass(SalOrderLine.class);
        verify(salOrderLineMapper).insertSalOrderLine(cap2.capture());
        assertThat(cap2.getValue().getRouteProductId()).isEqualTo(9L);
        assertThat(cap2.getValue().getRouteName()).isEqualTo("手选");
    }

    @Test
    @DisplayName("createWithLines - 外发=Y 解析被硬阻断 -> 整单拒绝")
    void should_rejectOrder_when_outsourceBlocked() {
        when(proRouteResolveService.resolve(any(), any(), any(), any()))
            .thenReturn(com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult.blocked("未配置外发路线"));
        when(salOrderMapper.insertSalOrder(any())).thenAnswer(inv -> { ((SalOrder) inv.getArgument(0)).setOrderId(3L); return 1; });
        SalOrderCreateRequest req = orderWithOneLine("STANDARD", "Y", null);
        assertThatThrownBy(() -> salOrderService.createWithLines(req))
            .isInstanceOf(ServiceException.class).hasMessageContaining("外发");
        verify(salOrderLineMapper, never()).insertSalOrderLine(any());
    }

    // ---- helpers ----
    private com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult matchedResult(long rpId, long routeId) {
        com.ruoyi.system.domain.mes.pro.ProRouteProduct rp = new com.ruoyi.system.domain.mes.pro.ProRouteProduct();
        rp.setRecordId(rpId); rp.setRouteId(routeId);
        return com.ruoyi.system.domain.mes.pro.dto.RouteResolveResult.matched(rp);
    }
    private com.ruoyi.system.domain.mes.pro.ProRoute route(long id, String code, String name) {
        com.ruoyi.system.domain.mes.pro.ProRoute r = new com.ruoyi.system.domain.mes.pro.ProRoute();
        r.setRouteId(id); r.setRouteCode(code); r.setRouteName(name);
        return r;
    }
```
补静态导入：`import static org.mockito.ArgumentMatchers.eq;`、`import static org.mockito.Mockito.never;`、`import static org.mockito.Mockito.reset;`（其余 any/verify/when 已有）。删除占位行 `RouteResolveResult hit = ...`。

- [ ] **Step 2: 跑测试确认失败**

Run: `cd backend && mvn -q -pl ruoyi-system test -Dtest=SalOrderServiceImplTest 2>&1 | tail -12`
Expected: 新两用例失败（routeProductId 为 null / 未抛异常），既有用例仍通过。

- [ ] **Step 3: 实现保存回填与校验**

`SalOrderServiceImpl` 注入三个依赖：
```java
    @Autowired private com.ruoyi.system.service.mes.pro.ProRouteResolveService proRouteResolveService;
    @Autowired private com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper proRouteProductMapper;
    @Autowired private com.ruoyi.system.mapper.mes.pro.ProRouteMapper proRouteMapper;
```
createWithLines 默认值段补两标志（在 sampleFlag 默认旁）：
```java
        if (order.getOutsourceFlag() == null) order.setOutsourceFlag("N");
        if (order.getPackageFlag() == null) order.setPackageFlag("N");
```
createFromCrm 中 `order.setSampleFlag("N");` 旁补：
```java
        order.setOutsourceFlag("N");
        order.setPackageFlag("N");
```
`saveLines` 签名由 `saveLines(Long orderId, List<SalOrderLine> lines, boolean isCreate)` 改为 `saveLines(SalOrder order, List<SalOrderLine> lines, boolean isCreate)`；两个调用点（createWithLines:127、updateWithLines:210，当前均为 `saveLines(order.getOrderId(), req.getLines(), true)`，两处 `order` 都在作用域）统一改为 `saveLines(order, req.getLines(), true);`。方法体（行号会随后续插入移动）替换为：
```java
    private void saveLines(SalOrder order, List<SalOrderLine> lines, boolean isCreate)
    {
        if (lines == null || lines.isEmpty()) return;
        int lineNo = 1;
        for (SalOrderLine line : lines)
        {
            if (!isCreate) line.setLineId(null);
            line.setOrderId(order.getOrderId());
            line.setLineNo(lineNo++);
            if (line.getQuantity() == null) throw new ServiceException("明细行订单数量不能为空");
            if (line.getLineAmount() == null && line.getUnitPrice() != null)
            {
                line.setLineAmount(line.getUnitPrice().multiply(line.getQuantity()));
            }
            resolveLineRoute(order, line);
            line.setCreateBy(SecurityUtils.getUsername());
            line.setCreateTime(DateUtils.getNowDate());
            salOrderLineMapper.insertSalOrderLine(line);
        }
    }

    /** 行路线: 空则按头维度解析带出; 非空仅校验归属并补快照; 外发硬约束不满足整单阻断 */
    private void resolveLineRoute(SalOrder order, SalOrderLine line)
    {
        if (line.getRouteProductId() == null) {
            var result = proRouteResolveService.resolve(line.getProductId(),
                    order.getOrderType(), order.getOutsourceFlag(), order.getPackageFlag());
            if (result.isHardBlocked()) throw new ServiceException(result.getMessage());
            if (result.isMatched()) {
                line.setRouteProductId(result.getRouteProductId());
                fillRouteSnapshot(line, result.getRouteId());
            }
            return;
        }
        ProRouteProduct binding = proRouteProductMapper.selectProRouteProductByRecordId(line.getRouteProductId());
        if (binding == null || !binding.getItemId().equals(line.getProductId())) {
            throw new ServiceException("明细行选择的工艺路线不属于该产品");
        }
        if ("Y".equals(order.getOutsourceFlag()) && !routeHasOutsourceNode(binding.getRouteId())) {
            throw new ServiceException("订单标记外发，但所选路线不含外发工序: " + line.getProductName());
        }
        fillRouteSnapshot(line, binding.getRouteId());
    }

    private void fillRouteSnapshot(SalOrderLine line, Long routeId) {
        ProRoute route = proRouteMapper.selectProRouteByRouteId(routeId);
        if (route != null) { line.setRouteCode(route.getRouteCode()); line.setRouteName(route.getRouteName()); }
    }

    private boolean routeHasOutsourceNode(Long routeId) {
        // 复用解析服务同口径判定, 避免重复依赖 routeProcessMapper: 以该路线任一绑定再解析不可行,
        // 故直接注入 ProRouteProcessMapper(字段见下)
        List<ProRouteProcess> nodes = proRouteProcessMapper.selectProRouteProcessByRouteId(routeId);
        return nodes != null && nodes.stream().anyMatch(n -> "1".equals(n.getIsOutsource()));
    }
```
再注入 `@Autowired private com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper proRouteProcessMapper;`，import `ProRouteProduct/ProRoute/ProRouteProcess`。
（函数行数：resolveLineRoute ≤ 50 行，满足约束。）

- [ ] **Step 4: 转工单兜底 + 外发复校**

`buildWorkorderFromLine` 中把 `wo.setRouteProductId(req.getRouteProductId());` 改为：
```java
        Long effectiveRouteProductId = req.getRouteProductId() != null ? req.getRouteProductId()
                : line.getRouteProductId();
        if ("Y".equals(order.getOutsourceFlag()) && effectiveRouteProductId != null) {
            ProRouteProduct binding = proRouteProductMapper.selectProRouteProductByRecordId(effectiveRouteProductId);
            if (binding == null || !routeHasOutsourceNode(binding.getRouteId())) {
                throw new ServiceException("订单标记外发，但工艺路线不含外发工序，不能转工单");
            }
        }
        if ("Y".equals(order.getOutsourceFlag()) && effectiveRouteProductId == null) {
            throw new ServiceException("外发订单必须选择含外发工序的工艺路线后再转工单");
        }
        wo.setRouteProductId(effectiveRouteProductId);
```

- [ ] **Step 5: 全部销售/路线相关单测转绿**

Run: `cd backend && mvn -q -pl ruoyi-system test -Dtest=SalOrderServiceImplTest,ProRouteResolveServiceTest 2>&1 | tail -15`
Expected: 全绿。既有"正常创建头+行"等用例因新依赖 resolve 未打桩：空 routeProductId 分支会调 mock（默认返回 null）→ NPE 风险。在 setUp() 给默认 lenient 打桩：`when(proRouteResolveService.resolve(any(),any(),any(),any())).thenReturn(RouteResolveResult.empty());`，使既有用例走"留空"分支。

- [ ] **Step 6: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderServiceImplTest.java
git commit -m "feat(sal): 开单按头维度自动带出路线(只补空), 外发硬校验保存/转工单双拦截"
```

---

### Task 8: 前端类型/API 与后端全量编译

**Files:**
- Modify: `frontend/src/types/api/mes/sal/order.ts`
- Modify: `frontend/src/api/mes/pro/routeproduct.ts`

**Interfaces:**
- Produces: 类型 `SalOrder.outsourceFlag/packageFlag`、`SalOrderLine.routeProductId/routeCode/routeName`；API `resolveRouteProduct(params)`、`resolveRouteProductBatch(payload)`。

- [ ] **Step 1: 类型补字段**

`types/api/mes/sal/order.ts`：
- SalOrderQueryParams 的 orderType 注释改为 5 值；SalOrder 接口在 sampleFlag 后加：
```ts
  /** 是否外发 Y/N */
  outsourceFlag?: string
  /** 是否包装 Y/N */
  packageFlag?: string
```
- SalOrderLine 在 shippingReq 后加：
```ts
  /** 绑定的产品-路线 record_id */
  routeProductId?: number
  routeCode?: string
  routeName?: string
```

- [ ] **Step 2: routeproduct API 加两函数**

`api/mes/pro/routeproduct.ts` 末尾加：
```ts
// 按 订单类型+是否外发+是否包装 预览默认路线（开单联动）
export function resolveRouteProduct(query: { itemId: number; orderType?: string; outsourceFlag?: string; packageFlag?: string }) {
  return request({ url: '/mes/pro/routeproduct/resolve', method: 'get', params: query })
}
// 头维度变更后批量重算
export function resolveRouteProductBatch(data: { itemIds: number[]; orderType?: string; outsourceFlag?: string; packageFlag?: string }) {
  return request({ url: '/mes/pro/routeproduct/resolveBatch', method: 'post', data })
}
```

- [ ] **Step 3: 后端全量编译兜底**

Run: `cd backend && mvn -q -pl ruoyi-admin -am compile 2>&1 | tail -5`
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/api/mes/sal/order.ts frontend/src/api/mes/pro/routeproduct.ts
git commit -m "feat(fe): 订单标志位/行路线字段类型与路线预览 API"
```

---

### Task 9: 订单头两个开关 + LineEdit 路线下拉 + 头维度变更批量重算

**Files:**
- Modify: `frontend/src/views/mes/sal/order/index.vue`
- Modify: `frontend/src/views/mes/sal/order/LineEdit.vue`

**Interfaces:**
- Consumes: Task 8 API；LineEdit 新增 props `orderType/outsourceFlag/packageFlag`，emit confirm 行内带 routeProductId/routeCode/routeName。

- [ ] **Step 1: LineEdit.vue 增加路线选择**

props 扩展：
```ts
const props = defineProps<{
  modelValue: boolean
  line: SalOrderLine | null
  orderType?: string
  outsourceFlag?: string
  packageFlag?: string
}>()
```
script 中 `api/mes/pro/routeproduct` 导入：
```ts
import { listRouteProduct, resolveRouteProduct } from '@/api/mes/pro/routeproduct'
import { listRoute } from '@/api/mes/pro/proroute'
```
新增状态与逻辑：
```ts
const routeOptions = ref<any[]>([])
const routeBlocked = ref(false)
const routeBlockMsg = ref('')

async function loadRouteOptions(selectResolved = true) {
  routeBlocked.value = false; routeBlockMsg.value = ''
  if (!form.productId) { routeOptions.value = []; return }
  const [bindRes, routeRes] = await Promise.all([
    listRouteProduct({ itemId: form.productId, pageSize: 100 }),
    listRoute({ pageSize: 1000 })
  ])
  const routeMap: Record<number, string> = {}
  ;(routeRes.rows || []).forEach((rt: any) => { routeMap[rt.routeId] = rt.routeName || rt.routeCode })
  routeOptions.value = (bindRes.rows || []).map((rp: any) => ({ ...rp, _routeName: routeMap[rp.routeId] || ('路线#' + rp.routeId) }))
  if (selectResolved) await applyResolvedRoute()
}

async function applyResolvedRoute() {
  if (!form.productId) return
  const r = await resolveRouteProduct({
    itemId: form.productId, orderType: props.orderType,
    outsourceFlag: props.outsourceFlag, packageFlag: props.packageFlag
  })
  const d = r.data || {}
  if (d.hardBlocked) {
    routeBlocked.value = true
    routeBlockMsg.value = d.message || '该产品未配置外发路线，请先在工艺路线主数据配置'
    form.routeProductId = null
  } else if (d.matched && d.routeProductId) {
    form.routeProductId = d.routeProductId
    const hit = routeOptions.value.find(o => o.recordId === d.routeProductId)
    if (hit) { form.routeCode = hit.routeCode || null; form.routeName = hit._routeName || null }
  }
}

function onRouteChange(recordId: number) {
  const hit = routeOptions.value.find(o => o.recordId === recordId)
  form.routeCode = hit?.routeCode || null
  form.routeName = hit?._routeName || null
}
```
initForm 中 Object.assign 的默认对象补 `routeProductId: null, routeCode: null, routeName: null`；编辑已有行（src.routeProductId）时不调 applyResolvedRoute（保留手选），仅 `loadRouteOptions(false)`；新增行选产品后调 `loadRouteOptions(true)`。
onProductSelected 末尾把 `if (row.itemId) loadExtAttrsByProduct(row.itemId)` 后追加 `loadRouteOptions(true)`。
模板在「产品尺寸/行交期」el-row 后插入一行：
```html
          <el-row>
            <el-col :span="16">
              <el-form-item label="工艺路线">
                <el-select v-model="form.routeProductId" clearable placeholder="留空则转工单时手选" style="width:100%" @change="onRouteChange">
                  <el-option v-for="r in routeOptions" :key="r.recordId"
                    :label="r._routeName + ' — ' + r.itemName" :value="r.recordId">
                    <span>{{ r._routeName }} — {{ r.itemName }}</span>
                    <el-tag v-if="r.isDefault === 'Y'" size="small" type="success" style="margin-left:6px">默认</el-tag>
                  </el-option>
                </el-select>
                <div v-if="routeBlocked" style="color:#f56c6c;font-size:12px;line-height:1.4">{{ routeBlockMsg }}</div>
              </el-form-item>
            </el-col>
          </el-row>
```
confirm() 中外发阻断时禁止提交：在扩展属性校验前加
```ts
  if (routeBlocked.value) { ElMessage.error(routeBlockMsg.value || '外发路线未配置'); return }
```

- [ ] **Step 2: index.vue 头表单加两开关并传 props**

头表单「业务线/订单类型/是否有样品/付款方式」el-row 中，把 6 列改造成新增一行容纳两开关（在该 el-row 后新增一行，避免挤布局）：
```html
        <el-row>
          <el-col :span="6"><el-form-item label="是否外发" prop="outsourceFlag"><el-switch v-model="form.outsourceFlag" active-value="Y" inactive-value="N" @change="onHeadDimensionChange" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="是否包装" prop="packageFlag"><el-switch v-model="form.packageFlag" active-value="Y" inactive-value="N" @change="onHeadDimensionChange" /></el-form-item></el-col>
        </el-row>
```
订单类型下拉加 `@change="onHeadDimensionChange"`。
reset() 的 form 对象：orderType 改 `'STANDARD'`，补 `outsourceFlag: 'N', packageFlag: 'N'`。
LineEdit 组件使用处（搜索 `<LineEdit` 或 `line-edit`）补传三个 prop 与 v-model：
```html
:order-type="form.orderType" :outsource-flag="form.outsourceFlag" :package-flag="form.packageFlag"
```
methods 中新增：
```js
    async onHeadDimensionChange() {
      if (!this.lineList.length) return
      try {
        await this.$modal.confirm('订单类型/标志已变更，是否按新条件重新匹配全部明细的工艺路线？')
      } catch { return }
      const itemIds = [...new Set(this.lineList.map((l: any) => l.productId).filter(Boolean))]
      if (!itemIds.length) return
      const r = await resolveRouteProductBatch({ itemIds, orderType: this.form.orderType, outsourceFlag: this.form.outsourceFlag, packageFlag: this.form.packageFlag })
      const map = r.data || {}
      // 用候选列表补 routeName：批量结果仅含 id/routeId, 名称逐产品用已有缓存或留 code
      this.lineList.forEach((l: any) => {
        const m = map[l.productId]
        if (m && m.matched) { l.routeProductId = m.routeProductId; l.routeCode = null; l.routeName = null }
      })
      // 名称快照由后端保存时补全；前端列表即时展示用详情接口刷新
      this.$modal.msgSuccess('已按新条件重新匹配工艺路线')
    },
```
import 段（文件顶部 `<script>` 既有 import 区）加：
```js
import { resolveRouteProductBatch } from '@/api/mes/pro/routeproduct'
```
（routeName 即时展示：onLineConfirm 已有行对象；批量重算后若列表需要显示名称，可在该方法末尾对每个命中产品调一次 listRouteProduct 拉名——实施时若明细表格展示路线列，按产品分组补名称；本期明细编辑表格不展示路线列时，保存后后端补快照即可，不额外请求。）

- [ ] **Step 3: 前端类型检查与构建**

Run: `cd frontend && npx vue-tsc --noEmit 2>&1 | tail -15`
Expected: 无新增 TS 错误（若仓库该命令本身有历史错误，确认与本次文件无关）。

- [ ] **Step 4: 浏览器手测（此时后端尚未重启，仅看 UI 交互需先完成 Task 10 后统一打包重启；本步先做静态检查）**

跳过运行时验证，统一在最后 Task 执行；本 Task 以类型检查通过为准。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/mes/sal/order/index.vue frontend/src/views/mes/sal/order/LineEdit.vue
git commit -m "feat(fe): 订单头外发/包装开关, 明细行路线自动带出与可改选, 头变更批量重算"
```

---

### Task 10: 转工单向导默认选中订单行路线，并抽离 ToWorkorderDialog 组件

**Files:**
- Create: `frontend/src/views/mes/sal/order/ToWorkorderDialog.vue`
- Modify: `frontend/src/views/mes/sal/order/index.vue`

**Interfaces:**
- Produces: `<ToWorkorderDialog v-model="twOpen" :order="twOrder" @created="getList" />`，index.vue 只保留打开动作与当前订单概要；向导内部完成选行→路线（默认行路线）→BOM/参数→偏离检测→提交。

- [ ] **Step 1: 抽组件**

新建 `ToWorkorderDialog.vue`（`<script>` Options API 与原 index.vue 向导同源），把 index.vue 中：
- 模板：`<!-- 生成工单(2步向导...) -->` 整个 `<el-dialog title="销售订单转工单">…</el-dialog>`（含行内 BOM 编辑/SKU 子弹窗若在同模板一并搬迁；搜索 `twBomEditOpen`、`twSkuDialogOpen` 定位相关弹窗整体剪切）；
- data：所有 `tw*` 字段（twOpen 通过 prop modelValue 计算 get/set）；
- methods：`twSelectLine/onTwRouteChange/getTwProcessName/handleTwAddBom/handleTwEditBom/handleTwDelBom/onTwBomItemSelected/twConfirmBomEdit/twGenSkuCode/twAutoGen/twConfirmSku/handleTwCheck/buildTwPayload/doTwSubmit/twRowClass/twNext/twCancel`；
- computed：`twProcessGroupList`；
- 相关 import：toWorkorder、listRouteProduct、listRoute、listRouteProcessByRouteId、listRouteProductBomByRouteId、listRouteProcessParamByRouteProductId、listParamTemplate、checkDeviation、genSerialCode。

组件契约：
```js
props: { modelValue: Boolean, order: { type: Object, default: () => ({}) } },
emits: ['update:modelValue', 'created'],
```
watch modelValue 为 true 时执行原 handleToWorkorder 的初始化（用 props.order.orderCode/requestDate；getOrderDetail(props.order.orderId) 拉行）；提交成功 emit('created')。
index.vue 删除上述模板/data/methods/computed/import，模板中放：
```html
    <ToWorkorderDialog v-model="twOpen" :order="twOrder" @created="getList" />
```
handleToWorkorder 瘦身为：
```js
    handleToWorkorder(row) { this.twOrder = row; this.twOpen = true },
```
data 仅留 `twOpen: false, twOrder: {}`。

- [ ] **Step 2: 向导默认选中订单行路线**

`twSelectLine(line)` 中把 `this.twForm.routeProductId = null;` 改为：
```js
      this.twForm.routeProductId = line.routeProductId || null
```
并在 routeOptions 装载完成后，若默认值存在则主动触发装载（在 listRoute 回调末尾）：
```js
              this.twRouteOptions = rows.map(...)
              if (this.twForm.routeProductId) this.$nextTick(() => this.onTwRouteChange(this.twForm.routeProductId))
```

- [ ] **Step 3: 类型检查 + 既有组件测试不破**

Run:
```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | tail -10
npx vitest run src/views/mes/sal/order/__tests__/index.spec.ts 2>&1 | tail -15
```
Expected: 无新增 TS 错误；既有 index.spec.ts 全绿（其 mock 了向导用到的全部 API；若因抽组件导致 mount index 时 ToWorkorderDialog 真实解析子链，在 spec 的 vi.mock 中追加 `vi.mock('../ToWorkorderDialog.vue', ...)` 桩，或 stubs 加 `ToWorkorderDialog: true`）。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/mes/sal/order/
git commit -m "refactor(fe): 抽离转工单向导组件并默认带出订单行路线"
```

---

### Task 11: 工艺路线维护页「关联产品」绑定弹窗加维度维护

**Files:**
- Modify: `frontend/src/views/mes/pro/proroute/index.vue`

**Interfaces:**
- 绑定 prodForm 增加 applyOrderType/applyOutsource/applyPackage/isDefault，提交走既有 addRouteProduct/updateRouteProduct（Task 3 后后端可接收）。

- [ ] **Step 1: 弹窗模板加四控件**

在「基准批量」el-form-item 之前（约 :280 行）插入：
```html
        <el-form-item label="适用订单类型">
          <el-select v-model="prodForm.applyOrderType" clearable placeholder="不限" style="width:100%">
            <el-option v-for="d in orderTypeOptions" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-row>
          <el-col :span="8"><el-form-item label="适用外发"><el-select v-model="prodForm.applyOutsource" clearable placeholder="不限" style="width:100%"><el-option label="是" value="Y" /><el-option label="否" value="N" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="适用包装"><el-select v-model="prodForm.applyPackage" clearable placeholder="不限" style="width:100%"><el-option label="是" value="Y" /><el-option label="否" value="N" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="默认路线"><el-switch v-model="prodForm.isDefault" active-value="Y" inactive-value="N" /></el-form-item></el-col>
        </el-row>
```
关联产品表格加一列（在"标准工时"后）：
```html
            <el-table-column label="适用维度" align="center" min-width="140">
              <template #default="scope">
                <el-tag v-if="scope.row.applyOrderType" size="small">{{ orderTypeLabel(scope.row.applyOrderType) }}</el-tag>
                <el-tag v-if="scope.row.applyOutsource==='Y'" size="small" type="warning" style="margin-left:2px">外发</el-tag>
                <el-tag v-if="scope.row.applyPackage==='Y'" size="small" type="success" style="margin-left:2px">包装</el-tag>
                <el-tag v-if="scope.row.isDefault==='Y'" size="small" type="primary" style="margin-left:2px">默认</el-tag>
                <span v-if="!scope.row.applyOrderType && scope.row.applyOutsource!=='Y' && scope.row.applyPackage!=='Y'" style="color:#909399;font-size:12px">不限</span>
              </template>
            </el-table-column>
```

- [ ] **Step 2: data 加字典、初始值、label 方法**

data 中 productSearchKey 附近加 `orderTypeOptions: []`；created/mounted 里加载（与页面既有 getDicts 用法一致；若该页已 import getDicts 复用）：
```js
import { getDicts } from '@/api/system/dict/data'
// created 或 mounted:
getDicts('mes_sal_order_type').then(r => { this.orderTypeOptions = r.data || [] })
```
methods 加：
```js
    orderTypeLabel(v) { const d = this.orderTypeOptions.find((o: any) => o.dictValue === v); return d ? d.dictLabel : v },
```
handleAddProduct 的 prodForm 初始对象补 `applyOrderType: null, applyOutsource: null, applyPackage: null, isDefault: 'N'`（约 :560）；handleUpdateProduct 用 `{ ...row }` 已自动带新字段。

- [ ] **Step 3: 提交时把空串归为 null（通配）**

找到 submitProdForm（约 :565 之后），构造提交对象前归一：
```js
      const payload = { ...this.prodForm }
      ;['applyOrderType','applyOutsource','applyPackage'].forEach(k => { if (!payload[k]) payload[k] = null })
```
用 payload 替换原直接提交的 prodForm。

- [ ] **Step 4: 类型检查**

Run: `cd frontend && npx vue-tsc --noEmit 2>&1 | tail -8`
Expected: 无新增错误。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/mes/pro/proroute/index.vue
git commit -m "feat(fe): 路线-产品绑定维护适用类型/外发/包装维度与默认标记"
```

---

### Task 12: 订单详情展示 + 工单页订单类型字典化

**Files:**
- Modify: `frontend/src/views/mes/sal/order/detail.vue`
- Modify: `frontend/src/views/mes/pro/workorder/index.vue`

- [ ] **Step 1: detail.vue 头加两标志、明细加路线列**

头部 descriptions 在「是否有样品」item（:32）后加：
```html
          <el-descriptions-item label="是否外发">{{ order.outsourceFlag === 'Y' ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="是否包装">{{ order.packageFlag === 'Y' ? '是' : '否' }}</el-descriptions-item>
```
明细表格在「包装要求」列（:80）后加：
```html
          <el-table-column label="工艺路线" align="center" prop="routeName" min-width="130" :show-overflow-tooltip="true">
            <template #default="s">{{ s.row.routeName || '-' }}</template>
          </el-table-column>
```

- [ ] **Step 2: workorder/index.vue 订单类型下拉字典化**

:150 写死的 select 替换为：
```html
<el-select v-model="form.orderType" style="width:100%" :disabled="optType==='view'">
  <el-option v-for="d in salOrderTypeOptions" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
</el-select>
```
在该页 data 加 `salOrderTypeOptions: []`，created/mounted（或已有的字典加载处）加 `getDicts('mes_sal_order_type').then(r => { this.salOrderTypeOptions = r.data || [] })`（import getDicts 路径 `@/api/system/dict/data`，与 sal/order/index.vue 一致）。

- [ ] **Step 3: 类型检查**

Run: `cd frontend && npx vue-tsc --noEmit 2>&1 | tail -8`
Expected: 无新增错误。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/mes/sal/order/detail.vue frontend/src/views/mes/pro/workorder/index.vue
git commit -m "feat(fe): 订单详情展示外发/包装与行路线, 工单页订单类型字典化"
```

---

### Task 13: 前端组件测试（LineEdit 路线带出与外发阻断）

**Files:**
- Create: `frontend/src/views/mes/sal/order/__tests__/LineEdit.spec.ts`

**Interfaces:** 参照现有 `index.spec.ts` 的 mock 风格；必须 mock 全部 API 与子组件。

- [ ] **Step 1: 写测试（完整）**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LineEdit from '../LineEdit.vue'

const mockResolve = vi.fn()
const mockListBindings = vi.fn()
vi.mock('@/api/mes/pro/routeproduct', () => ({
  listRouteProduct: (...a: any[]) => mockListBindings(...a),
  resolveRouteProduct: (...a: any[]) => mockResolve(...a),
}))
vi.mock('@/api/mes/pro/proroute', () => ({ listRoute: vi.fn().mockResolvedValue({ rows: [] }) }))
vi.mock('@/api/mes/md/item', () => ({ getItem: vi.fn().mockResolvedValue({ data: {} }) }))
vi.mock('@/api/mes/md/attr', () => ({ getEffAttrSchema: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/components/itemSelect/single.vue', () => ({ default: { name: 'ItemSelect', template: '<div />' } }))

function mountLine(line: any = null, extraProps: any = {}) {
  return mount(LineEdit, {
    props: { modelValue: true, line, orderType: 'STANDARD', outsourceFlag: 'N', packageFlag: 'N', ...extraProps },
    global: {
      stubs: { ExtAttrForm: true },
      mocks: { parseTime: (t: any) => (t ? String(t) : '') },
      config: { globalProperties: {} } as any,
    },
  })
}

function bindingRow(id: number, routeId: number, name: string, isDefault = 'N') {
  return { recordId: id, routeId, itemId: 100, itemCode: 'P1', itemName: '演示产品', _routeName: name, isDefault }
}

describe('SalOrder LineEdit.vue 工艺路线带出', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('选产品后按头维度解析并默认选中命中路线', async () => {
    mockListBindings.mockResolvedValue({ rows: [bindingRow(2, 20, '标准路线', 'Y')] })
    mockResolve.mockResolvedValue({ data: { matched: true, hardBlocked: false, routeProductId: 2, routeId: 20 } })
    const wrapper = mountLine()
    ;(wrapper.vm as any).onProductSelected({ itemId: 100, itemCode: 'P1', itemName: '演示产品', unitOfMeasure: 'GE', unitName: '个' })
    await nextTick(); await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect((wrapper.vm as any).form.routeProductId).toBe(2)
    expect((wrapper.vm as any).routeBlocked).toBe(false)
  })

  it('外发硬阻断时置阻断态且 confirm 不提交', async () => {
    mockListBindings.mockResolvedValue({ rows: [] })
    mockResolve.mockResolvedValue({ data: { matched: false, hardBlocked: true, message: '未配置外发路线' } })
    const wrapper = mountLine(null, { outsourceFlag: 'Y' })
    ;(wrapper.vm as any).onProductSelected({ itemId: 100, itemCode: 'P1', itemName: '演示产品', unitOfMeasure: 'GE', unitName: '个' })
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect((wrapper.vm as any).routeBlocked).toBe(true)
    const before = wrapper.emitted('confirm')?.length || 0
    ;(wrapper.vm as any).confirm()
    await nextTick()
    expect((wrapper.emitted('confirm')?.length || 0)).toBe(before)
  })

  it('编辑已有行保留手选路线, 不重新解析', async () => {
    mockListBindings.mockResolvedValue({ rows: [bindingRow(9, 90, '手选路线')] })
    const wrapper = mountLine({ lineId: 5, productId: 100, productCode: 'P1', productName: '演示产品', quantity: 1, routeProductId: 9, routeName: '手选路线' })
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect(mockResolve).not.toHaveBeenCalled()
    expect((wrapper.vm as any).form.routeProductId).toBe(9)
  })
})
```
（ElMessage 在阻断分支会弹消息；测试环境 element-plus 全局未注册时 `ElMessage.error` 仍可调用其纯 JS 实现，无需 DOM。若报错，`vi.mock('element-plus', ...)` 仅替换 ElMessage。）

- [ ] **Step 2: 跑测试**

Run: `cd frontend && npx vitest run src/views/mes/sal/order/__tests__/LineEdit.spec.ts 2>&1 | tail -20`
Expected: 3 个用例通过。若第一个用例时序不稳，把等待改为 `await flushPromises()`（`@vue/test-utils` 导出）。

- [ ] **Step 3: 全量前端组件测试回归**

Run: `cd frontend && npx vitest run 2>&1 | tail -15`
Expected: 全部通过（含既有 index.spec.ts）。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/mes/sal/order/__tests__/LineEdit.spec.ts
git commit -m "test(fe): 明细行路线带出/外发阻断/保留手选组件测试"
```

---

### Task 14: 集成测试 — V150 迁移结果与枚举重映射

**Files:**
- Create: `backend/ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/sal/V150MigrationIT.java`

**Interfaces:** 继承 `BaseIntegrationTest`（Testcontainers，Flyway 自动执行全部迁移）。需要本地 Redis（`docker compose up -d redis` 或项目既有 redis 容器）。

- [ ] **Step 1: 写 IT（断言迁移产物 + 存量 UPDATE 语义）**

```java
package com.ruoyi.web.controller.mes.sal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ruoyi.BaseIntegrationTest;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V150 迁移结果集成测试")
class V150MigrationIT extends BaseIntegrationTest
{
    @Test
    @DisplayName("字典为 5 值且无 NEW/REPEAT")
    void should_haveFiveOrderTypes_and_noLegacyValues() {
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_dict_data WHERE dict_type='mes_sal_order_type'", Integer.class);
        assertThat(cnt).isEqualTo(5);
        Integer legacy = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value IN ('NEW','REPEAT')",
            Integer.class);
        assertThat(legacy).isZero();
    }

    @Test
    @DisplayName("新列存在且订单行路线三列可写可读")
    void should_haveNewColumns() {
        Integer cols = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
            "AND ((table_name='qxx_sal_order' AND column_name IN ('outsource_flag','package_flag')) " +
            "OR (table_name='qxx_sal_order_line' AND column_name IN ('route_product_id','route_code','route_name')) " +
            "OR (table_name='qxx_pro_route_product' AND column_name IN ('apply_order_type','apply_outsource','apply_package','is_default')))",
            Integer.class);
        assertThat(cols).isEqualTo(9);
    }

    @Test
    @DisplayName("种子: 三条新路线工序数 3/4/4, 礼品绑定带包装维度")
    void should_seedNewRoutesAndGiftBinding() {
        Integer plate = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.factory_id=1 AND r.route_code='RT-PLATE'", Integer.class);
        Integer small = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.factory_id=1 AND r.route_code='RT-SMALL'", Integer.class);
        Integer gift = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.factory_id=1 AND r.route_code='RT-GIFT'", Integer.class);
        assertThat(plate).isEqualTo(3);
        assertThat(small).isEqualTo(4);
        assertThat(gift).isEqualTo(4);

        Integer giftPkg = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_product rp JOIN qxx_md_item i ON i.item_id=rp.item_id " +
            "WHERE rp.factory_id=1 AND i.item_code='ITEM-GIFT-DEMO' AND rp.apply_order_type='GIFT' AND rp.apply_package='Y'",
            Integer.class);
        assertThat(giftPkg).isEqualTo(1);
    }

    @Test
    @DisplayName("外发节点均回填了供应商")
    void should_backfillVendorForOutsourceNodes() {
        Integer missing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id " +
            "WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.is_outsource='1' AND rp.vendor_id IS NULL",
            Integer.class);
        assertThat(missing).isZero();
    }

    @Test
    @DisplayName("存量重映射 SQL 语义: 插入 NEW 订单执行 UPDATE 后变 STANDARD")
    void should_remapLegacyOrderType_when_updateRuns() {
        jdbcTemplate.update("INSERT INTO qxx_sal_order (factory_id, order_code, order_type, status, create_time) " +
            "VALUES (1, 'SO-IT-LEGACY-1', 'NEW', 'PREPARE', NOW())");
        // 重放迁移中的重映射语句
        jdbcTemplate.update("UPDATE qxx_sal_order SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT') OR order_type IS NULL");
        String t = jdbcTemplate.queryForObject(
            "SELECT order_type FROM qxx_sal_order WHERE order_code='SO-IT-LEGACY-1'", String.class);
        assertThat(t).isEqualTo("STANDARD");
    }
}
```
> 若 Testcontainers 库中 RT-OUTSRC 不存在（V16 种子可能未进入 test schema——schema 来自 sql/ry_20260417.sql + manual_tables.sql，种子路线不一定有），则"回填供应商"用例断言需以"存在 RT-OUTSRC 时 missing=0；不存在则跳过"改写：先 SELECT COUNT RT-OUTSRC，为 0 时 assumeTrue 跳过（`org.junit.jupiter.api.Assumptions.assumeTrue`）。实施时先跑一遍确认，按结果二选一。

- [ ] **Step 2: 跑 IT**

Run（需 Docker + Redis）:
```bash
cd backend && mvn -q -pl ruoyi-admin -am verify -Dtest=V150MigrationIT -DfailIfNoTests=false 2>&1 | tail -25
```
Expected: 5 个用例通过（或外发回填 1 个按假设跳过）。若 Flyway 在 test schema 因缺前置表报错，记录缺失表名；V150 的 DDL 都是加列/插入，依赖 qxx_sal_order 等表已由 manual_tables.sql 提供——若某表缺失导致迁移失败，这是 test schema 与 Flyway 兼容的既有问题，参照最近一次能通过的 *IT 基线（SysConfigControllerIT）确认环境，再决定是否在 test sql 补表（仅测试资源，不算业务改动）。

- [ ] **Step 3: Commit**

```bash
git add backend/ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/sal/V150MigrationIT.java
git commit -m "test(it): V150 迁移产物/字典/种子/枚举重映射集成测试"
```

---

### Task 15: 打包重启 + 8 条验收手工实测（红线，不可跳过）

**Files:** 无代码改动（发现问题回到对应 Task 修复）。

- [ ] **Step 1: 全量单测回归**

Run: `cd backend && mvn -q -pl ruoyi-system,ruoyi-admin -am test 2>&1 | tail -15`
Expected: BUILD SUCCESS，全部单测通过。

- [ ] **Step 2: 重新打包并重启后端**

Run:
```bash
cd backend && mvn -q -pl ruoyi-admin -am package -DskipTests 2>&1 | tail -5
# 找旧进程并重启
PID=$(pgrep -f 'ruoyi-admin.jar' | head -1); [ -n "$PID" ] && kill "$PID"
sleep 3
nohup java -jar backend/ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
for i in $(seq 1 30); do sleep 2; curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/captchaImage | grep -q 200 && break; done
curl -s -o /dev/null -w 'captcha=%{http_code}\n' http://localhost:8081/captchaImage
```
Expected: captcha=200；日志 `/tmp/ruoyi-backend.log` 无 Flyway/Bean 启动错误（`grep -i error /tmp/ruoyi-backend.log | tail`）。若 8081 端口/路径与本项目实际不符，以 backend/AGENTS.md 与 dev-start skill 为准。

- [ ] **Step 3: 接口实测（token）**

Run:
```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
# 3a. 预览端点：标品+外发=Y 打 RT-OUTSRC 绑定的产品（奔趣 item_id 见种子，先用演示物料 ITEM-GIFT-DEMO 验正常命中）
curl -s "http://localhost:8081/mes/pro/routeproduct/resolve?itemId=<GIFT_ITEM_ID>&orderType=GIFT&outsourceFlag=N&packageFlag=Y" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# 3b. 外发阻断：对一个只绑了厂内路线的产品传 outsourceFlag=Y
curl -s "http://localhost:8081/mes/pro/routeproduct/resolve?itemId=<PLATE_ITEM_ID>&orderType=PLATE&outsourceFlag=Y&packageFlag=N" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```
Expected: 3a data.matched=true 且 routeProductId 为 ITEM-GIFT-DEMO↔RT-GIFT 绑定；3b hardBlocked=true、message 含"外发"。
itemId 用 `docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes -e "SELECT item_id,item_code FROM qxx_md_item WHERE item_code LIKE 'ITEM-%-DEMO'"` 取。

- [ ] **Step 4: 前端启动并浏览器实测（dev-start 或既有 vite）**

启动前端（若未运行）：`cd frontend && npm run dev`，浏览器登录后逐条执行验收清单（对应设计文档 §8）：

1. 新建 5 张订单：标品(STANDARD/N/N)、小批量(SMALL_BATCH)、礼品(GIFT + 包装=Y)、制版(PLATE)、外发（任一类产品 + 外发=Y）。每张：选产品后明细行工艺路线自动带出对应路线；记录截图。
2. 5 张订单全部 提交→审核通过→「生成工单」（向导路线默认带出且可改选）→生成成功；打开甘特图触发排产，确认每道工序生成任务，页面始终同一套，无分叉。
3. 外发订单：路线含外发节点 → 甘特上该任务为 VENDOR 虚拟站且带供应商名；工单开工检查生成草稿外协单（仓库-外协订单列表可见 DRAFT 单，供应商=万隆）。
4. 礼品订单排产后任务序列末道为「包装」。
5. 明细行改选其他绑定路线→保存→重开编辑，选择保留；修改头类型/标志弹重算确认，确认后各行 routeProductId 刷新（查库 `SELECT line_id,route_product_id,route_name FROM qxx_sal_order_line WHERE order_id=<id>`）。
6. 外发=Y 且给未配外发路线的产品（如 ITEM-PLATE-DEMO）开单：保存被拒、提示含"外发"。
7. 打开一张历史 NEW 时期订单（共 47 张），订单类型显示「标品」；历史工单同样显示标品。
8. 订单详情、列表 tag、导出 PDF 与 Excel 中 5 种类型文案正确（导出后打开核对）。

- [ ] **Step 5: 发现问题回到对应 Task 修复并重跑其测试，再回到本 Task；全绿后收尾**

Run:
```bash
git log --oneline main..HEAD
git status -s
```
Expected: 15 个 Task 的提交在 B2_feature 上；工作区无遗留改动（截图等未跟踪文件按需清理或加入 .gitignore，不入库业务目录）。
在最终汇报中逐条引用 8 条验收的实际结果（截图/接口响应/查询结果），不得仅凭"启动成功"宣称完成。
```

---

## Self-Review 结论（计划作者已核对）

- **Spec 覆盖**：数据模型（Task 1/3/6）、解析服务（4）、预览端点与默认唯一（5）、保存回填+双硬校验（7）、枚举与文案（2/12）、头开关/行路线/重算（9）、向导默认（10）、主数据维护（11）、详情展示（12）、四层测试（4/7/13/14）、8 条验收（15）——设计文档 §3–§8 每条均有落点。
- **取值一致性**：订单/绑定统一 Y/N，仅节点判定读 `is_outsource='1'`，计划全文统一。
- **类型/方法名一致性**：resolve/resolveBatch/RouteResolveResult 工厂方法 matched/empty/blocked 在 Task 4/5/7/13 中一致；字段 applyOrderType/applyOutsource/applyPackage/isDefault 跨 Task 3/4/9/11 一致；routeProductId 等 5 个前端字段跨 Task 6/8/9/10/12 一致。
- **已知实施时需现场确认项**（已写进对应 Step，不是占位）：① Flyway 幂等加列以本项目 V121/V124 既有写法为准；② IT 中 RT-OUTSRC 是否存在于 test schema；③ 后端端口/进程以 backend/AGENTS.md 为准。
