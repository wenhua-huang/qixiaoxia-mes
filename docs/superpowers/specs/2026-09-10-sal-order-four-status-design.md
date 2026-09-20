# 销售订单状态收敛为四态 + 报工只更新进度（B3 / D1）设计

- 日期：2026-09-10
- 分支：B3_feature（基于 main f6aa06b）
- 关联需求：销售订单状态只保留四态「已确认 → 生产中 → 已出货 → 已结单」；工序任务报工不改订单状态，只更新进度百分比（D1）

## 1. 背景与现状

### 1.1 现状事实（代码核查结论）

- 销售订单当前是**五态审核机**，不是被报工打碎的状态机：
  `PREPARE 待提交 → PENDING 待审核 → CONFIRMED 已确认 → CLOSED 已关闭`，外加链外 `CANCEL 已取消`。
  - 枚举：`backend/ruoyi-common/.../enums/SalOrderStatus.java`
  - 字典：`mes_sal_order_status`，种子在 `V124__sal_order_approve.sql`
  - 字段：`qxx_sal_order.status` varchar(64)，列默认值 `'PREPARE'`（`V71__sal_order_tables.sql`）
- **生产报工链路当前就不写销售订单状态**（pro 域对 SalOrder 零引用）。报工审核 `ProFeedbackServiceImpl.auditFeedback` 只更新任务产量、任务/工单/流转卡状态。因此"六道工序报工订单状态跳六次"在现网不成立；销售侧真正的缺口是：
  1. 订单没有「生产中」「已出货」状态，也没有任何事件把订单推进过去（工单开工、出库发货均不回写订单）；
  2. 订单维度没有进度百分比（PR #55 的进度只到工单/任务/流转卡层）。
- 工单产量 `qxx_pro_workorder.quantity_produced` **只在末工序审核时累加**（防多工序重复计数），故订单进度不能用工单产量口径，否则中间工序报工期间进度恒为 0。
- 销售出库单 `qxx_wm_product_sales` 有 `sales_order_id`；出库明细 `qxx_wm_product_sales_line` 有 `sales_order_line_id`；发运明细 `qxx_wm_product_sales_detail` 有 `line_id`（=出库明细行）+ `quantity`。出库单自身已有"累计发运量 ≥ 总量 → ship_status=SHIPPED"的推导（`WmProductSalesShipmentServiceImpl.updateHeaderAfterShip`），但一张订单可拆多张出库单，订单维度发齐必须重新聚合判定。
- sal 域 Mapper 已有跨域聚合先例：`SalOrderLineMapper.xml` 的 `sumProducedQtyByLineId` 直接 SUM pro 工单表。
- 项目当前**未使用** Spring 事件机制，本设计为首个使用点。
- 移动端 app 无销售订单页面；报表无按销售订单状态的统计。两端零改动。

### 1.2 数据模型链路（进度/发齐聚合路径）

```
qxx_sal_order (id)
  └─ qxx_sal_order_line (order_id, line_id, quantity)
       ├─ qxx_pro_workorder (sales_order_line_id, status, quantity, quantity_produced)
       │    └─ qxx_pro_task (workorder_id, status, quantity, quantity_produced)
       └─ qxx_wm_product_sales (sales_order_id, status != 'CANCEL')
            └─ qxx_wm_product_sales_line (sales_id, line_id→sales_order_line_id)
                 └─ qxx_wm_product_sales_detail (line_id, quantity)
```

## 2. 目标与非目标

### 2.1 目标

1. 销售订单主线四态：**已确认 CONFIRMED → 生产中 PRODUCING → 已出货 SHIPPED → 已结单 CLOSED**。
2. 报工不改订单状态；同一张订单连续多道工序报工审核，订单状态始终「生产中」，进度百分比逐次上升。
3. 订单列表/详情可见实时生产进度百分比（0–100）。
4. 删除审核流：建单（含 CRM 推单）即「已确认」。
5. 保留 CANCEL 作为链外作废态。

### 2.2 非目标（YAGNI）

- 不做状态时间线/操作日志；不做订单进度冗余落库（坚持实时聚合）。
- 不做移动端销售订单页；不做报表按订单状态统计。
- 不做签收自动结单（结单保持人工）。
- 不物理删除 approve_by/approve_time/approve_remark 列（保留历史数据，代码不再写入、页面不再展示）。
- 不做退货对订单状态的回退（退货走出货退货流程，另行设计）。

## 3. 状态机（已确认决策）

```
建单 / CRM推单 ──▶ 已确认 CONFIRMED ──任一关联工单开工──▶ 生产中 PRODUCING ──订单全部明细发齐──▶ 已出货 SHIPPED ──人工结单──▶ 已结单 CLOSED
                      │                                     │
                      └────────────── 取消 ─────────────────┘
                                     ▼
                               已取消 CANCEL（链外终态）
```

| 状态 | 编码 | 进入条件 | 配色 |
|---|---|---|---|
| 已确认 | `CONFIRMED`（复用） | 建单/CRM 推单直接落；存量 PREPARE/PENDING 刷入 | success |
| 生产中 | `PRODUCING`（新增） | 任一关联工单开工成功（事件） | warning |
| 已出货 | `SHIPPED`（新增） | 订单全部明细累计发运量 ≥ 订单量（事件后订单维度重算） | primary |
| 已结单 | `CLOSED`（复用，文案"已关闭"→"已结单"） | 人工结单，仅 SHIPPED 可结 | info |
| 已取消 | `CANCEL`（链外保留） | 仅 CONFIRMED/PRODUCING 可取消 | danger |

规则细则：

- 现货直发（未建工单即发货）允许 CONFIRMED → SHIPPED：发货推进的前置态条件为 `status IN ('CONFIRMED','PRODUCING')`。
- 已出货允许在 PRODUCING 时达成（部分工单仍在制、货已发齐的业务场景），不要求工单全部完工。
- 取消**不级联取消工单**（与现状一致，工单归生产域管理）；取消接口成功提示"关联工单需另行处理"。
- 已出货/已结单/已取消不可再取消；已结单为终态。
- 转工单：CONFIRMED 与 PRODUCING 均允许（支持生产中追加拆单，沿用部分转单校验）。
- 编辑/删除订单行：仅 CONFIRMED（生产中已派生工单，禁改）。

## 4. 跨域推进机制：领域事件（方案 A）

pro/wm 只发布事件，不依赖 sal；sal 监听事件推进自身状态。依赖箭头保持单向（sal → pro/wm 只读聚合）。

### 4.1 事件与发布点

事件类放新包 `com.ruoyi.system.event.mes`（ruoyi-system 模块）。

| 事件 | 发布点 | 载荷 | 发布条件 |
|---|---|---|---|
| `WorkorderStartedEvent` | `ProWorkorderServiceImpl.startProduction(Long)`（约 :734，工单置 PRODUCING 成功后） | workorderId, salesOrderLineId（非订单来源为 null） | 每次开工成功都发 |
| `SalesShipmentCompletedEvent` | `WmProductSalesShipmentServiceImpl.updateHeaderAfterShip()`（约 :191） | salesId, salesOrderId（非订单来源为 null） | **仅当本次推导出库单 shipStatus=SHIPPED（出库单维度发齐）时**才发；部分发货不发 |

注入 `ApplicationEventPublisher` 发布；事件为不可变 POJO。

### 4.2 监听器

新建 `SalOrderLifecycleListener`（sal 域），两个方法均为：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

- AFTER_COMMIT 保证开工/发货事务回滚时订单绝不动；REQUIRES_NEW 让状态推进独立提交，失败不影响已完成的主业务，仅记 error 日志（状态推进本身幂等，可人工补偿/重放）。
- **onWorkorderStarted**：经 salesOrderLineId 反查订单 id（`qxx_sal_order_line.line_id → order_id`）；载荷 null / 查不到订单直接跳过；执行条件 UPDATE：`status='CONFIRMED' → 'PRODUCING'`。多工单分别开工、重复事件均幂等。
- **onSalesShipped**：载荷 null 跳过；执行"前置态 + 发齐"二合一条件 UPDATE（见 4.3），单条 SQL 原子完成，无需先查后更。
- CANCEL/CLOSED 态收到事件因不命中前置态自然不动。报工链路零改动，D1 在结构上成立。

### 4.3 发齐判定 SQL（订单维度）

UPDATE 的 WHERE 同时要求：

1. `id = #{orderId}` 且 `status IN ('CONFIRMED','PRODUCING')`；
2. NOT EXISTS 任一订单行未发齐：不存在满足下式的订单行——
   `行订单量 > 该行在全部非取消出库单上的累计已发运箱量（b.status='SHIPPED'）`

聚合路径（实现口径，2026-09-11 订正）：子查询按订单行汇总**已发运箱量**——
`wm_product_sales s INNER JOIN wm_product_sales_line sl ON sl.sales_id=s.sales_id AND sl.factory_id=s.factory_id INNER JOIN wm_product_sales_box b ON b.line_id=sl.line_id AND b.factory_id=sl.factory_id AND b.status='SHIPPED' WHERE s.sales_order_id=#{orderId} AND s.status<>'CANCELED' GROUP BY sl.sales_order_line_id` 得 `SUM(b.quantity)`；
外层 `sal_order_line l LEFT JOIN 该子查询 x ON x.sol_id=l.line_id`，判 `l.quantity > COALESCE(x.shipped_qty,0)`。

口径说明：以**箱（box）为发运事实表**而非出库行/明细——只有装箱并随发运单置为 SHIPPED 的箱才算真实出货，与 wm 出库侧 `updateHeaderAfterShip` 仅在箱回写 SHIPPED 后发事件的时点一致；同一订单行跨多张出库单的箱量累计比较，故"出库单发齐 ≠ 订单发齐"。出库行 quantity_sales 只是计划量，不作数。

SQL 显式写全所有表的 factory_id 等值条件并标 `@SkipFactoryId`（监听器以 REQUIRES_NEW 独立事务执行，事件载荷携带 factoryId，不依赖线程参数注入，口径更显式）。

Mapper 方法 `markShippedIfFullyDelivered(orderId, factoryId, updateBy, updateTime)`，返回受影响行数：0 表示未发齐或状态不符，监听器不报错。

## 5. 数据库迁移 V151

新建 `backend/ruoyi-admin/src/main/resources/db/migration/V151__sal_order_four_status.sql`。Flyway 裸 JDBC，**所有 DML 显式 factory_id**；DML 需幂等（可重入）。

1. 字典 `mes_sal_order_status`：
   - 停用 PREPARE、PENDING（`sys_dict_data.status='1'`），不删除（保留历史值可翻译）。
   - CLOSED 字典 label 改「已结单」，listClass 改 `info`。
   - 新增 PRODUCING（label 生产中，listClass warning）、SHIPPED（label 已出货，listClass primary）。
   - dict_data 的 dict_code 用法、factory_id 口径照抄 V124/V146 写法（先 SELECT 校验再 INSERT，幂等）。
2. 存量数据：`UPDATE qxx_sal_order SET status='CONFIRMED' WHERE status IN ('PREPARE','PENDING')`（按 factory_id 全量；在途审核单视为直接确认）。
3. DDL：`qxx_sal_order.status` 列默认值 `'PREPARE'` → `'CONFIRMED'`，更新列注释为四态说明。
4. 菜单按钮权限：删除/停用 `mes:sal:order:submit`、`mes:sal:order:approve` 两个按钮权限项（若 V124 或其他迁移建过；先查 sys_menu 实际数据再写幂等 DELETE）。`mes:sal:order:workorder`、`:edit`（结单复用）保留。

## 6. 后端改造清单

### 6.1 枚举与常量

- `SalOrderStatus`：删 PREPARE、PENDING；新增 PRODUCING、SHIPPED；保留 CONFIRMED、CLOSED（语义注释改已结单）、CANCEL；重写类注释状态机。
- 全部状态比较改用枚举常量，禁止新写裸字符串（现状裸字符串处一并替换）。

### 6.2 SalOrderServiceImpl / Controller

- 删除端点与 Service 方法：submit/approve/reject/batchSubmit/batchApprove（含 Controller 映射、接口声明、前端 API 同步删）。
- `createWithLines`、`createFromCrm`：直接落 CONFIRMED（删 PENDING/PREPARE 赋值）。
- 门控调整：
  - 编辑/删除：仅 CONFIRMED（原仅 PREPARE）。
  - 转工单 `doToWorkorder` 与 `selectSalOrderAllConvertible` SQL：CONFIRMED、PRODUCING 可转。
  - `closeOrder`：仅 SHIPPED → CLOSED（原仅 CONFIRMED），提示文案改"结单"。
  - `cancelOrder`：仅 CONFIRMED/PRODUCING，成功提示追加"关联工单需另行处理"。
- 编辑保存 `updateWithLines` 不再写任何审核字段。

### 6.3 进度聚合（D1 展示侧）

- `SalOrder` 增加非 DB 瞬态字段 `private Integer progressPercent;`（0–100）。
- 新增 SalOrderMapper 批量聚合查询（列表页 IN 批量，禁止 N+1；仿 `ProWorkorderServiceImpl.enrichProgress` 范式）：

  ```sql
  -- 入参：当前页订单 id 列表
  SELECT l.order_id AS orderId,
         LEAST(100, ROUND(COALESCE(SUM(t.quantity_produced),0)
               / NULLIF(SUM(t.quantity),0) * 100, 0)) AS progressPercent
  FROM qxx_sal_order_line l
  JOIN qxx_pro_workorder w ON w.sales_order_line_id = l.line_id AND w.status != 'CANCEL'
  JOIN qxx_pro_task t ON t.workorder_id = w.workorder_id AND t.status != 'CANCEL'
  WHERE l.order_id IN (...) AND l.factory_id = #{factoryId}
  GROUP BY l.order_id
  ```

- 口径：任务层**数量加权**，用 `quantity_produced`（报工审核即累加，含不合格——进度表达"做了多少"，与工单完成率口径一致），分母为 0（无工单/任务未下发）该订单无聚合行，回填 0。
- 列表接口支持 `includeProgress=true` 时批量回填（Controller 参考工单列表写法）；详情接口始终回填。

### 6.4 导出文案同步

三处旧五态翻译改新五码（CONFIRMED/PRODUCING/SHIPPED/CLOSED/CANCEL）：

- `SalOrder.java` status 字段 @Excel `readConverterExp`
- `SalOrderPdfExporter`（约 :168）
- `SalOrderDetailExcelExporter`（约 :136）

## 7. 前端改造清单（frontend，app 零改动）

### 7.1 `src/views/mes/sal/order/index.vue`

- 删顶部"批量提交""批量审核"按钮及 handle 方法（36-37 行）；删行内"提交审核/审核通过/驳回"按钮（59-61 行）与驳回弹窗（含硬编码文案 :258）。
- 行按钮新条件：
  - 改、删除图标：仅 CONFIRMED；顶部"修改" `canEditSelected` 同步改 CONFIRMED。
  - 生成工单：CONFIRMED、PRODUCING 可见。
  - 结单（原"关闭"改文案）：仅 SHIPPED 可见。
  - 取消：CONFIRMED、PRODUCING 可见。
- 新增「生产进度」列：`el-progress :percentage="row.progressPercent"`，不返进度时按 0 展示；一律显示任务口径真实聚合值，不按状态特判（允许生产中直接发齐出货，故 SHIPPED 时进度可能未满 100）。
- 新建表单默认 status 改 CONFIRMED（实际由后端落，前端仅占位）；状态筛选、tag 渲染已走字典，V151 后自动生效。

### 7.2 `src/views/mes/sal/order/detail.vue`

- `statusTagType` 映射改新五码配色；删驳回原因 alert（:18）与 approveHint（:119-124）；头部状态区加 el-progress（绑 progressPercent）。

### 7.3 跨域与 API/类型

- `src/views/mes/wm/product_sales/components/SaleOrderSelect.vue`：查询参数由单值 `status:'CONFIRMED'` 改为后端支持的状态列表（CONFIRMED、PRODUCING）；列表状态列接字典翻译（当前裸显 status 码，顺带修正）。
- 后端列表查询参数支持多状态（复用现有 statusList 模式；若无则 SalOrderQuery 加 `statusList`，Mapper 加 foreach）。
- `src/api/mes/sal/order.ts`：删 submit/approve/reject/batchSubmit/batchApprove。
- `src/types/api/mes/sal/order.ts`：`status` 收紧为联合类型 `'CONFIRMED'|'PRODUCING'|'SHIPPED'|'CLOSED'|'CANCEL'`；加 `progressPercent?: number`；approve* 三字段标记废弃（可保留可选，注释说明）。
- 重写已与实现脱节的组件测试 `__tests__/index.spec.ts`（旧断言 statusText/statusTag 已失效）。

## 8. 测试与验收

### 8.1 后端单元测试（Mockito，断网可跑）

- 扩 `SalOrderServiceImplTest`：建单/CRM 推单即 CONFIRMED；五個旧端点已移除（反射/编译保证）；编辑/删除/转工单/结单/取消各门控正反例。
- 新建 `SalOrderLifecycleListenerTest`：
  - 开工：CONFIRMED→PRODUCING；非 CONFIRMED 不动；null 载荷/查无订单跳过；重复事件幂等。
  - 发货：发齐且前置态命中才 SHIPPED；部分发货（受影响行 0）不动；CONFIRMED 直发可 SHIPPED；CANCEL/CLOSED 不动。
  - AFTER_COMMIT/REQUIRES_NEW 注解存在性断言（防被人改掉事务语义）。
- 报工回归：扩 `ProFeedbackServiceUnitTest`，断言审核链路不调用任何 SalOrder 更新（当前本就不调用，固化防线）。
- 进度算法：纯 SQL 聚合放集成测试验证；若抽出计算工具方法则单测加权/封顶/零分母/排 CANCEL。

### 8.2 集成测试（Testcontainers，重写 SalOrderIT）

**测试分层（实现实际形态，2026-09-11 订正）**：IT 只负责"状态机 + 聚合 SQL + 事件链路"的端到端正确性——任务进度以直插 `qxx_pro_task.quantity_produced` 模拟（两道任务分母恒 200，经真实 `/list?includeProgress=true` 读聚合值断言 0→25→50→50→100），不走报工审核接口。"报工不改订单状态"这一 D1 反面约束由两道更轻的防线固化：① 架构防线——监听器只订阅 WorkorderStarted / SalesShipmentCompleted 两个事件，报工链路无任何事件出口；② 单测防线——ProFeedbackServiceUnitTest 反射断言反馈服务不依赖 `com.ruoyi.system.**.mes.sal.**` 任何类型。发运事件由测试在真实事务内 publish，触发的是真实 markShippedIfFullyDelivered 5 表 SQL，故部分发货/发齐判定为真端到端。

落地为 3 个用例：主链路四态 + 进度递增；PRODUCING 中取消后重复开工事件幂等 noop；门控（超转/PRODUCING 结单/删除均 500）。上列第 5 步（工单完工仍 PRODUCING）由"进度 100% 时状态仍 PRODUCING"断言覆盖；第 10 步（V151 迁移）由迁移在 IT 容器内真实执行（Flyway repair + migrate 成功即建表/字典 DDL 有效）与 Task 1 单测覆盖，不另设数据预置用例。

主链路（核心验收点对照）：

1. 建单 → CONFIRMED，进度 0；
2. 转工单（多工序工艺路线）→ 仍 CONFIRMED，进度 0；
3. 工单开工 → PRODUCING；
4. 同一订单连续审核多道工序报工（≥2 道、分批数量）：**每次后订单状态恒为 PRODUCING，progressPercent 严格递增**（D1 验收点）；
5. 工单完工 → 仍 PRODUCING；
6. 部分发货（第一张出库单只发部分量）→ 仍 PRODUCING；
7. 剩余发齐 → SHIPPED；
8. 人工结单 → CLOSED；
9. 另一单：PRODUCING 途中取消 → CANCEL，再收开工/发货事件状态不变；
10. V151 迁移校验：预置 PREPARE/PENDING 单，迁移后均 CONFIRMED；字典 5 个有效项齐全。

### 8.3 实测红线（AGENTS.md，不可跳过）

- `cd backend && mvn -pl ruoyi-admin -am package -DskipTests` → kill 旧进程 → 重启 jar → 等 captchaImage 200。
- token 调真实接口走 8.2 主链路（可用后端脚本/ curl 连续报工），眼见状态不跳、进度逐次上升。
- 前端浏览器实际操作：订单列表进度列、按钮显隐、出库选单、结单流程各点一次。

## 9. 风险与对策

| 风险 | 对策 |
|---|---|
| 事件监听失败（如 DB 抖动）导致状态停在旧态 | 条件 UPDATE 幂等，后续关联事件（下一工单开工、下次发货）会自然补偿；影响仅限状态展示，不阻塞主业务；日志 error 可查 |
| 一张订单多张出库单，误把"出库单发齐"当"订单发齐" | 只在出库单 SHIPPED 时发事件，是否转 SHIPPED 由 sal 侧订单维度 NOT EXISTS 重算（4.3），两层判定 |
| 取消订单后关联出库/工单数据悬挂 | 取消前置态仅 CONFIRMED/PRODUCING（SHIPPED 后不可取消）；不级联，提示用户另行处理工单；出库选单只列 CONFIRMED/PRODUCING，CANCEL 单无法再建出库 |
| 进度用任务数量口径跨工序重复计数质疑 | 该字段仅作"工序平均完工率"展示，命名/文档明确不是产量；产量口径仍以工单 quantity_produced 为准；分子分母同时包含全部任务，加权后语义自洽，全部任务完成恰为 100% |
| V151 刷存量 PENDING 单"绕过审核" | 审核流已废弃，在途单无审核入口；刷为 CONFIRMED 是唯一可继续业务的选择，上线说明中注明 |

## 10. 影响文件一览

**新增**

- `V151__sal_order_four_status.sql`
- `event/mes/WorkorderStartedEvent.java`、`event/mes/SalesShipmentCompletedEvent.java`
- `service/mes/sal/.../SalOrderLifecycleListener.java`（+ 单测）
- 后端：发齐判定/进度聚合 Mapper 方法与 XML

**修改（后端）**

- `SalOrderStatus.java`、`SalOrder.java`（瞬态字段 + @Excel）、`SalOrderController.java`、`ISalOrderService.java`、`SalOrderServiceImpl.java`、`SalOrderMapper(.java/.xml)`、`SalOrderLineMapper.xml`（如需反查 orderId）
- `ProWorkorderServiceImpl.java`（发布开工事件）、`WmProductSalesShipmentServiceImpl.java`（发布发货事件）
- `SalOrderPdfExporter.java`、`SalOrderDetailExcelExporter.java`
- 测试：`SalOrderServiceImplTest`、`ProFeedbackServiceUnitTest`、`SalOrderIT`

**修改（前端）**

- `views/mes/sal/order/index.vue`、`detail.vue`、`__tests__/index.spec.ts`
- `views/mes/wm/product_sales/components/SaleOrderSelect.vue`
- `api/mes/sal/order.ts`、`types/api/mes/sal/order.ts`

**不动**：app/、报表/看板、报工链路业务代码、approve_* 数据库列。
