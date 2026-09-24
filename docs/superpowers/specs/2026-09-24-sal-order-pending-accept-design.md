# 销售订单新增【待接单】状态 设计

- 日期：2026-09-24
- 模块：`sal` 销售订单（`qxx_sal_order` / `qxx_sal_order_line`）
- 关联：取代性扩展 [2026-09-10-sal-order-four-status-design.md](2026-09-10-sal-order-four-status-design.md)（V151 四态收敛）
- Flyway：V161（当前最大 V160）

## 1. 背景与目标

销售订单当前建单即 `CONFIRMED` 已确认，缺少"工厂内部尚未接单"的前置环节。要求：

1. 新增状态【待接单】，码值 `PENDING_ACCEPT`；
2. **所有新建销售订单默认待接单**——手工建单与 CRM open-api 推单一视同仁；
3. 接单是执行前闸门：接单前不能转工单、不能被成品出库选单引用；可编辑、删除、取消；
4. 存量 `CONFIRMED` 订单保持不动，不做刷态。

## 2. 状态机

```
PENDING_ACCEPT 待接单 ──人工接单──▶ CONFIRMED 已确认 ──任一工单开工──▶ PRODUCING 生产中
      │                                  │                                    │
      │                                  └──────────── 取消 ─────────────────┘
      └──────────── 取消 ───────────────────────────────────────▶ CANCEL 已取消
                                                                   │
CONFIRMED ──全部明细发齐──▶ SHIPPED 已出货 ──人工结单──▶ CLOSED 已结单
```

- 新主线：`PENDING_ACCEPT → CONFIRMED → PRODUCING → SHIPPED → CLOSED`，`CANCEL` 为链外终态。
- 接单只允许从 `PENDING_ACCEPT` 单向推进到 `CONFIRMED`，不可回退。
- 工序报工不改变订单状态，仅驱动进度百分比（现状不变）。

## 3. 方案选择

| 方案 | 结论 | 理由 |
|------|------|------|
| A. 新枚举 `PENDING_ACCEPT` + 专用接单接口 | **采用** | 语义独立清晰；不复活 V151 已废弃的审核流；改动集中在 sal 域 |
| B. 复活 PREPARE/PENDING 审核态 | 否决 | V151 刚停用审核流；`approve_*` 留存列语义是"审核"不是"接单"；与历史数据翻译混淆 |
| C. 只加字典/默认值，不加接单入口 | 否决 | 状态无法在系统内流转，是死状态 |

## 4. 后端改动

### 4.1 枚举

`ruoyi-common` 的 `SalOrderStatus`：

- 枚举常量首位新增 `PENDING_ACCEPT("PENDING_ACCEPT", "待接单")`；
- 更新类注释状态图（五态主线 + CANCEL）；
- PDF / 明细 Excel 导出器通过 `SalOrderStatus.fromCode().getInfo()` 翻译，加枚举值即自动覆盖。

### 4.2 默认值（后端权威，忽略前端传值）

- `SalOrderServiceImpl.createWithLines()`：`order.setStatus(...)` 由 `CONFIRMED` 改为 `PENDING_ACCEPT`，更新注释。手工单与 CRM 推单（`createFromCrm` 最终也调 `createWithLines`）同时生效。
- `createFromCrm()` 中冗余的 `setStatus(CONFIRMED)` 改为 `PENDING_ACCEPT` 并更新注释（虽会被 createWithLines 覆盖，保持两处口径一致可读）。

### 4.3 接单动作

- `ISalOrderService` / `SalOrderServiceImpl` 新增 `acceptOrder(Long orderId)`：
  - `mustExist` 取单；非 `PENDING_ACCEPT` 抛 `ServiceException("仅待接单订单可接单")`；
  - 复用现有 `updateStatus(orderId, CONFIRMED)`；
  - 无库存/数量影响，不需要 Redisson 锁（与 `closeOrder`/`cancelOrder` 同模式）。
- Controller 新增 `PUT /mes/sal/order/accept/{orderId}`：
  - 权限沿用 `mes:sal:order:edit`（**不新增按钮权限与菜单行**，避免全量角色重新授权；将来需要"接单岗与建单岗分离"再开专用权限）；
  - `@Log(title = "销售订单接单", businessType = BusinessType.UPDATE)`。

### 4.4 既有状态闸门调整

| 动作 | 现状 | 改动 |
|------|------|------|
| 编辑 `updateWithLines` | 仅 CONFIRMED（且无派生工单） | 放宽为 PENDING_ACCEPT / CONFIRMED；错误文案改"仅待接单/已确认订单可修改" |
| 删除 `deleteSalOrderByOrderIds` | 仅 CONFIRMED（且无派生工单） | 放宽为 PENDING_ACCEPT / CONFIRMED；错误文案同步 |
| 取消 `cancelOrder` | 拦 SHIPPED/CLOSED/CANCEL | 不改代码（PENDING_ACCEPT 天然放行），仅补注释明确 |
| 转工单 `doToWorkorder` | 仅 CONFIRMED/PRODUCING | **不改**，待接单被拦，错误文案"仅已确认/生产中订单可转工单"保持 |
| 可转工单下拉 `selectSalOrderAllConvertible` | `status in ('CONFIRMED','PRODUCING')` | **不改** |
| 出库选单 SaleOrderSelect 固定 statusList | CONFIRMED/PRODUCING | **不改** |
| 开工推进 confirmProducing | `where status='CONFIRMED'` | **不改**（待接单不可能有派生工单） |
| 发齐推进 / 冲销降级 | CONFIRMED/PRODUCING 集合 | **不改** |
| 生命周期监听器 | — | **不改** |

### 4.5 Excel 注解

`SalOrder.java` 的 `@Excel(readConverterExp=...)` 加 `PENDING_ACCEPT=待接单,` 前缀。

### 4.6 测试

- 更新现有断言：`SalOrderServiceImplTest`、`SalOrderIT` 中"建单即 CONFIRMED"改为 PENDING_ACCEPT；
- 新增用例：
  1. 接单：PENDING_ACCEPT → CONFIRMED；
  2. 非待接单调接单被拒；
  3. 待接单转工单被拒；
  4. 待接单可编辑、可删除、可取消。

## 5. Flyway V161

文件：`backend/ruoyi-admin/src/main/resources/db/migration/V161__sal_order_pending_accept.sql`

要点（仿 V151 写法；`sys_dict_data` 为系统表，无 factory_id，INSERT 不带；幂等 `WHERE NOT EXISTS`）：

1. 插入字典项（`dict_type='mes_sal_order_status'`）：
   - dict_value=`PENDING_ACCEPT`，dict_label=`待接单`，list_class=`info`，is_default=`Y`，dict_sort=1，status='0'，remark='新建订单默认状态，接单后转已确认'；
2. 默认项与活跃排序重排：
   - CONFIRMED：is_default=`N`，dict_sort=2；PRODUCING=3；SHIPPED=4；CLOSED=5；CANCEL=6；停用项 PREPARE/PENDING 保持 sort 8/9；
3. 列默认值与注释：
   ```sql
   ALTER TABLE qxx_sal_order
     MODIFY COLUMN status varchar(64) DEFAULT 'PENDING_ACCEPT'
     COMMENT '订单状态：PENDING_ACCEPT待接单/CONFIRMED已确认/PRODUCING生产中/SHIPPED已出货/CLOSED已结单/CANCEL已取消';
   ```
4. **不刷存量数据**：现有 CONFIRMED 单保持 CONFIRMED。

## 6. 前端改动（frontend）

- `src/types/api/mes/sal/order.ts`：`SalOrderStatus` 联合类型首位加 `'PENDING_ACCEPT'`。
- `src/api/mes/sal/order.ts`：新增 `acceptOrder(id)` → `PUT /mes/sal/order/accept/{id}`。
- 列表页 `src/views/mes/sal/order/index.vue`：
  - 行操作：PENDING_ACCEPT 行显示【接单】按钮（type=success，`v-hasPermi="['mes:sal:order:edit']"`，确认弹窗后调接口刷新）；
  - 【改】【删除图标】显隐：`(status==='CONFIRMED' || status==='PENDING_ACCEPT') && !hasWorkorder`；
  - 【取消】显隐：加入 PENDING_ACCEPT；
  - 【生成工单】【结单】条件不变；
  - 顶部批量 `canEditSelected` / `canDeleteSelected`：允许状态集合同步加 PENDING_ACCEPT；
  - 新增表单 `reset()` 的 `status: 'CONFIRMED'` 改 `'PENDING_ACCEPT'`（后端仍强制覆盖）；
  - 状态搜索下拉、状态列 tag 由字典驱动，字典更新后自动出"待接单"，无需改渲染逻辑。
- 详情页 `detail.vue`：
  - 文案由字典自动覆盖；硬编码颜色兜底 map 加 `PENDING_ACCEPT: 'info'`；
  - 详情页保持只读，**不放接单按钮**（接单入口仅列表行）。
- app 端无销售订单模块（app 的 sales 是成品销售出库），**不改**。

## 7. 明确不做（YAGNI）

- 不增加 accepted_by / accepted_time 列，不记录接单人/时间；
- 不做批量接单；
- 不新增接单专用权限/菜单；
- 不做 CRM 推单后的接单回调通知；
- 不调整下游出库/工单模块任何代码。

## 8. 验证计划

按后端改动验证红线：

1. `mvn -pl ruoyi-admin -am package -DskipTests` 打包；
2. 重启 :8081 进程，`curl captchaImage` 200；
3. token 实测：
   - 手工建单 → status=PENDING_ACCEPT；
   - CRM open-api 推单 → PENDING_ACCEPT；
   - 接单 → CONFIRMED；CONFIRMED 再调接单报错；
   - 待接单调转工单被拒；待接单可编辑/删除/取消；
   - 出库选单弹窗查不到待接单；
4. 前端浏览器实测：列表 tag/搜索下拉出现待接单、接单按钮与各操作显隐、建单默认值；
5. 单测/集成测试 `SalOrderServiceImplTest`、`SalOrderIT` 全绿。
