# 订单类型 + 两标志位驱动默认工艺路线 设计文档

- 日期：2026-09-10
- 分支：`B2_feature`（基于 main f6aa06b）
- 状态：已通过头脑风暴设计评审，待出实施计划

## 1. 背景与目标

工厂有五条业务线：完稿制版、小批量、礼品、柔印标品、外发。五条线的差别只有三个开关：**订单类型、厂内还是外发、要不要包装**（"谁跟单"由订单上现有业务员字段 `salesperson` 承载，不新增维度）。

需求要求：

- 订单类型字典扩为：**标品、小批量、礼品、备货订单、制版**（制版为评审中确认新增的第 5 个值，用于在单据层直接识别完稿制版线）；
- 销售订单增加两个标志位：**是否外发、是否包装**；
- 开单时按「订单类型 + 两标志位」带出默认工艺路线，带出后允许人工调整；
- **不建 5 套流程、不建 5 套页面**——五条线走同一套订单/工单/工序任务对象；
- 验收：建 5 张不同类型/标志位的订单，全部能生成工单和工序任务，页面无分叉；外发标志打开的订单，工艺路线里带出外发工序。

### 业务线映射

| 业务线 | 订单类型 | 是否外发 | 是否包装 |
|---|---|---|---|
| 柔印标品 | 标品 STANDARD | N | 视产品/订单 |
| 小批量 | 小批量 SMALL_BATCH | N | 视产品/订单 |
| 礼品 | 礼品 GIFT | N | Y（礼品路线末道为包装节点） |
| 完稿制版 | 制版 PLATE | N | N |
| 外发 | 任一类型 | **Y** | 视产品/订单 |
| 备货（A2 已有） | 备货订单 STOCK | N/N | 视产品/订单 |

## 2. 现状（已核对代码与本地库数据，HEAD=f6aa06b）

1. 订单类型字典 `mes_sal_order_type` 已存在（V146）：新单 NEW / 返单 REPEAT / 备货 STOCK。PR#59 对 STOCK 无任何业务分支，仅字典+文案，与本设计"属性化、不分叉"一致。
2. 存量数据：`qxx_sal_order` NEW 47 单、STOCK 2 单、REPEAT 0 单；`qxx_pro_workorder` 冗余存 order_type。
3. `qxx_sal_order` 仅有 `sample_flag char(1) Y/N` 风格标志位；**无外发/包装标志**。`qxx_sal_order_line` 仅有包装要求**文本** `package_req`，无布尔位；订单头/行**均无路线字段**。
4. 路线即模板、实时引用：`qxx_pro_route` 无类型/模板字段；工单只存 `route_product_id`，不复制工序，排产时实时读 `qxx_pro_route_process` 生成 `qxx_pro_task`。
5. 产品-路线多对多绑定表 `qxx_pro_route_product`（线上 34 行）只有 route_id↔item_id，**无适用条件/默认概念**。同一产品已可绑多条路线（如奔趣纸袋绑了 RT-STANDARD/RT-SLIT/RT-OUTSRC）。
6. 外发全链路已按路线节点 `qxx_pro_route_process.is_outsource='1'`（+vendor）自动驱动，零代码待写：排产生成 VENDOR 虚拟站任务（`ScheduleServiceImpl.java:149-170`）、开工生成草稿外协单（`OutsourceIssueHelper`）、厂内报工拦截、收货回写报工推进流转卡。实测 RT-OUTSRC 节点为 外发生产(1)→贴绳(1)→包装(0)，但 **vendor 为空**，验收种子必须补。
7. 销售转工单向导路线目前纯手选（`sal/order/index.vue:437`，默认 null）；Step2 只能改 BOM/参数，工单页工序增删按钮纯展示不入库。
8. 销售转工单链路：`POST /mes/sal/order/toWorkorder` → `SalOrderServiceImpl.toWorkorder`（先锁后事务）→ `buildWorkorderFromLine`（:380-420）→ `createWorkorderWithBom`；销售路径不直接触发排产，任务在甘特加载/手工触发时惰性生成。
9. app 端无销售订单/建工单入口，本次只改 backend + frontend。
10. Flyway 当前最新 V149。

## 3. 数据模型设计（Flyway V150，单个迁移）

### 3.1 `qxx_sal_order` 加两列

沿用同表 `sample_flag` 的 Y/N 风格：

```sql
outsource_flag char(1) DEFAULT 'N' COMMENT '是否外发 Y/N',
package_flag   char(1) DEFAULT 'N' COMMENT '是否包装 Y/N'
```

### 3.2 `qxx_sal_order_line` 加三列

开单时带出的路线选择落库留痕，code/name 冗余快照与该表 product_code/product_name 风格一致：

```sql
route_product_id bigint       NULL COMMENT '产品-路线绑定ID qxx_pro_route_product.record_id',
route_code       varchar(50)  NULL COMMENT '路线编码冗余快照',
route_name       varchar(200) NULL COMMENT '路线名称冗余快照'
```

### 3.3 `qxx_pro_route_product` 加四列（匹配元数据）

```sql
apply_order_type varchar(50) NULL COMMENT '适用订单类型 STANDARD/SMALL_BATCH/GIFT/STOCK/PLATE；NULL=通配',
apply_outsource  char(1)     NULL COMMENT '适用是否外发 Y/N；NULL=通配',
apply_package    char(1)     NULL COMMENT '适用是否包装 Y/N；NULL=通配',
is_default       char(1) DEFAULT 'N' COMMENT '同分时默认首选 Y/N'
```

NULL=通配是关键：存量 34 行迁移后默认全 NULL，行为与今天完全一致；打标签只是收窄候选。

### 3.4 字典调整为 5 值

`mes_sal_order_type`：

| value | label | list_class | is_default |
|---|---|---|---|
| STANDARD | 标品 | primary | Y |
| SMALL_BATCH | 小批量 | info | N |
| GIFT | 礼品 | danger | N |
| STOCK | 备货订单 | warning | N |
| PLATE | 制版 | success | N |

删除 NEW/REPEAT 字典行（DML 按 dict_type+value 幂等）。存量值重映射：

```sql
UPDATE qxx_sal_order     SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT') OR order_type IS NULL;
UPDATE qxx_pro_workorder SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT');
```

枚举值是系统级变更，这两条 UPDATE 跨全部工厂、不带 factory_id 过滤，属 AGENTS.md「所有 SQL WHERE 带 factory_id」约束的合理例外，迁移注释中写明原因。其余 DML（字典、种子）全部显式 factory_id。

### 3.5 枚举

`ruoyi-common` 新增 `SalOrderType` 枚举（STANDARD/SMALL_BATCH/GIFT/STOCK/PLATE），替换散落字面量：SalOrder Excel 注解、PDF/Excel 导出器 switch、CRM 推单硬编码 NEW、Service 默认值。

### 3.6 验收种子数据（幂等 WHERE NOT EXISTS，显式 factory_id=1）

- 外协供应商：无 OUTSOURCE 类型供应商则建一个；给 RT-OUTSRC 的「外发生产/贴绳」节点补 `vendor_id/vendor_code/vendor_name` + `outsource_factory_id`；
- 新路线：制版路线（完稿→制版→检验）、小批量路线、礼品路线（末道为「包装」工序节点）；标品/外发复用 RT-STANDARD/RT-OUTSRC；
- 新演示产品：制版、小批量、礼品各一个物料并绑定对应路线；
- 给现有绑定打维度标签：RT-OUTSRC 行 apply_outsource='Y'，RT-STANDARD 行 apply_order_type='STANDARD' 等，保证五条线都有精确匹配项。

## 4. 后端设计

### 4.1 路线解析服务（新增 pro 域 `ProRouteResolveService`，单一职责）

入参 `itemId + orderType + outsourceFlag + packageFlag`，在该产品的绑定行中选一条：

1. **排除**：某维度 `apply_*` 非 NULL 且与订单值不一致的候选；NULL 视为通配；
2. **外发硬约束**：订单 outsourceFlag=Y 时，候选路线必须在 `qxx_pro_route_process` 实际存在 is_outsource='1' 的节点（查节点，不只信标签，防止标签打了节点没配）；不满足则跳过该候选；
3. **排序**：三维度精确匹配数多者优先，通配命中排后；同分 is_default='Y' 优先，再按 record_id 稳定排序，取第一条；
4. 返回 routeProductId + routeCode/routeName + matched；无候选返回空；**外发=Y 且全部候选出局**返回 hardBlocked + 原因文案。

包装=Y 只按 apply_package 标签参与匹配（系统无包装工序类型字段，V102 已删 process_type），靠种子数据保证礼品路线末道为包装节点；排产天然生成包装任务。

### 4.2 两个落地点

- **保存订单**（`createWithLines`/`updateWithLines`，含 CRM 推单 `createFromCrm`）：逐行处理——行 routeProductId 为空 → 解析服务自动回填（含 code/name 快照）；已有值（自动带出或人工选过）→ **保留不覆盖**，仅校验合法性（路线属于该产品；外发=Y 时含外发节点）。外发硬约束不满足 → 抛 `ServiceException`，整单保存阻断。
- **转工单兜底**（`buildWorkorderFromLine`，SalOrderServiceImpl.java:413 附近）：向导未传 routeProductId 时取订单行存值，再空才现场解析；外发=Y 仍无合规路线 → 阻断转工单。不信任前端，两处后端硬校验。
- CRM 推单：orderType 空默认 STANDARD、两标志 N，走同一套回填。

### 4.3 预览端点（前端联动用，纯读无副作用）

- `GET /mes/pro/routeproduct/resolve?itemId=&orderType=&outsourceFlag=&packageFlag=`
  返回 `{routeProductId, routeCode, routeName, matched, hardBlocked, message}`；
- 批量：`POST /mes/pro/routeproduct/resolveBatch`，body 为多行 itemId + 共同头维度，一次返回每行结果（头维度变更后一键重算用）。

### 4.4 改动清单

- SalOrder +2 字段、SalOrderLine +3 字段；两个 Mapper XML 的 resultMap/select/insert/update 同步；
- ProRouteProduct +4 字段、Mapper XML 同步；绑定保存校验同产品至多一条 is_default='Y'；
- 新增 SalOrderType 枚举并替换全部字面量引用点；
- SalOrderServiceImpl 保存两处 + CRM + buildWorkorderFromLine 接入解析/校验，私有方法抽离，守函数 ≤50 行；
- ProRouteProductController 加 resolve/resolveBatch 端点。

### 4.5 明确不做

- 不改 `workorder_type`（保持 SELF）：外协是工序级概念，一张工单可自制+外发混合，现有排产/VENDOR/外协单/收货回写零改动；
- 不做工单级工序快照、不做 BOM/任务的标志位特判；app 端不动。

## 5. 前端设计

### 5.1 订单头表单（`views/mes/sal/order/index.vue`）

- 订单类型下拉已字典化，字典改完自动出现 5 值；
- 「是否有样品」旁加两个 el-switch：是否外发、是否包装（Y/N，独立 `<span>` 标签）。

### 5.2 明细行弹窗（`LineEdit.vue`，核心交互）

- 新增「工艺路线」下拉：候选=该产品全部绑定路线（listRouteProduct(itemId)），人工可任选，推荐项加「默认」tag；
- 选产品后调 resolve 预览端点默认选中；头维度（类型/两标志）经 props 传入，行内只读展示匹配条件，不重复勾选；
- 外发硬阻断：下拉下方红字「该产品未配置外发路线，请先在工艺路线主数据配置」，确定按钮禁用（后端再拦一次）。

### 5.3 有值即确认的覆盖规则

- 行上一旦有 routeProductId，切头类型/标志不自动覆盖；
- 头维度切换且已有明细行时弹确认：「订单类型/标志已变更，是否按新条件重新匹配全部明细的工艺路线？」确认走 resolveBatch 批量重算，取消保持现状；空行照常自动补；新增行永远自动带出。

### 5.4 转工单向导

- 选中明细行时路线下拉默认选中订单行存的 routeProductId（现为 null 纯手选），仍可改选；改选沿用现有 onTwRouteChange 重载工序/BOM/参数，Step2 调整不变。

### 5.5 路线主数据维护页（`pro/proroute/index.vue` 关联产品 tab 绑定弹窗）

- 加：适用订单类型下拉（5 值+「不限」）、是否外发（不限/是/否）、是否包装（不限/是/否）、默认路线 switch。

### 5.6 其他

- 订单详情 detail.vue：头展示外发/包装两 tag，每行加「工艺路线」列；
- 工单页 workorder/index.vue:150 写死的订单类型下拉改字典加载；
- `types/api/mes/sal/order.ts` 补 5 个新字段；
- index.vue 已 541 行超 300 行规范：本次新增逻辑收敛为 `useRouteResolve` composable，并把转工单向导抽成 `ToWorkorderDialog.vue` 子组件（仅容纳新逻辑，不重排既有功能）；
- 列表不加标志位列/筛选（YAGNI）；不新增页面和菜单；app 不动。

## 6. 错误处理

| 场景 | 处理 |
|---|---|
| 外发=Y 但产品无含外发节点的路线 | 保存订单、转工单两处后端硬阻断，ServiceException 明确文案；前端红字+禁用确定 |
| 其他维度无精确匹配 | 通配兜底；通配也没有则路线留空不阻断，转工单时人工选（现状行为） |
| 同产品多条 is_default=Y | 绑定保存后端校验拒绝 |
| 人工改选不属于该产品的路线 | 保存校验拒绝 |
| 头维度变更后不点重算 | 旧路线保留，空行照常自动补，不静默覆盖已存值 |
| CRM 推单 | 类型空默认 STANDARD、标志 N，同一套回填 |
| CONFIRMED 订单/已转工单 | 只有 PREPARE 可编辑（现状）；工单存 routeProductId 引用，主数据后续调整不影响在产工单（实时引用语义不变，不引入快照表） |

## 7. 测试策略（遵循 docs/设计文档/测试约定.md）

- **后端单元测试**（Mockito，扩展 `SalOrderServiceImplTest`，新增 `ProRouteResolveServiceTest`，should_xxx_when_xxx + 中文 DisplayName）：
  - 匹配矩阵：三维精确 > 部分通配 > 全通配；维度不符出局；同分 is_default 胜出；record_id 稳定兜底；
  - 外发=Y 候选路线实际无 is_outsource=1 节点 → hardBlocked（标签打了节点没配也算出局）；
  - 保存：空路线自动回填、已选不覆盖、非法路线拒绝；转工单兜底取订单行路线。
- **前端组件测试**（Vitest，vi.mock API）：LineEdit 选产品调 resolve 并默认选中、外发阻断态禁用提交、头维度变更重算确认弹窗。
- **集成测试**（Testcontainers，`*IT.java`）：V150 在空库与预置 NEW 订单的存量库两种初始态可执行且幂等；NEW→STANDARD、字典 5 值、种子路线可查。
- **手工实测红线**：重新打包重启后端（mvn -pl ruoyi-admin -am package -DskipTests + kill/nohup），token 实测真实接口；前端浏览器实际点击。curl ≠ E2E。

## 8. 验收清单

1. 建 5 张订单：标品 / 小批量 / 礼品（包装=Y）/ 制版 / 外发（外发=Y），保存后每行自动带出对应路线；
2. 全部走完 提交→审核→生成工单→甘特打开触发排产→工序任务生成，始终同一套页面，无分叉；
3. 外发订单：路线含外发节点 → 排产后该任务挂 VENDOR 虚拟站且带供应商 → 开工生成草稿外协单；
4. 礼品订单路线末道为「包装」任务；
5. 行内改选其他产品路线后保存，选择保留、重开仍在；切头标志弹重算确认，确认后全行刷新；
6. 外发=Y 且产品未配外发路线时保存被阻断并有明确提示；
7. 存量 47 张 NEW 订单打开显示「标品」，历史工单同样显示标品；
8. 列表/详情/转工单/导出 PDF·Excel 中 5 种类型文案正确。

## 9. 范围边界

- **不做**：工单级工序增删落库、列表标志位筛选、新菜单/新实体、app 改动、workorder_type 语义改造；
- 本期无新建实体，不涉及自动编码规则种子。
