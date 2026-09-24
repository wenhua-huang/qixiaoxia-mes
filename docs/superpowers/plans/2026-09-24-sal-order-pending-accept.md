# 销售订单【待接单】状态 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 销售订单新增 PENDING_ACCEPT 待接单状态，所有新建单（手工 + CRM 推单）默认待接单，人工"接单"后才进入已确认态并解锁转工单/出库。

**Architecture:** 纯 sal 域状态机前置扩展：枚举加首位状态、Service 默认值/闸门/接单方法、Controller 加 `PUT /accept/{id}`、Flyway V161 改字典与列默认；下游出库/工单/事件链零改动（现有 `status in ('CONFIRMED','PRODUCING')` 集合天然拦截待接单）。前端列表加接单按钮并放宽改/删/取消显隐，E2E 把 V151 后失效的"确认"步骤更新为"接单"。

**Tech Stack:** Spring Boot 4 + JDK 17 + MyBatis（JUnit5/Mockito 单测，Testcontainers 集成测试）、Flyway、Vue 3 Options API + Element Plus、Playwright E2E。

## Global Constraints

- 分支：`feature/sal-order-pending-accept`（已建）；只改本项目文件。
- 状态码用枚举 `SalOrderStatus`，禁止散落字符串；错误文案用中文。
- Flyway：新文件 `V161__sal_order_pending_accept.sql`，不可改已执行迁移；`sys_dict_data` 是系统表无 factory_id（仿 V151 不带）；INSERT 必须 `WHERE NOT EXISTS` 幂等；不刷存量业务数据。
- Service 函数 ≤ 50 行；接单无库存影响，不需要 Redisson 锁（与 closeOrder/cancelOrder 同模式）。
- 接单接口权限沿用 `mes:sal:order:edit`，不新增菜单/权限。
- 后端单测离线可跑（禁连 DB）；集成测试需本地 Docker（`docker compose up -d redis`，MySQL 用 Testcontainers）。
- 后端改完必须重新打包 + 重启运行中进程 + token 实测，禁止只 `mvn compile` 就宣称完成。
- 提交粒度：每个 Task 一次提交，提交信息用 `feat(sal): ...` / `test(sal): ...` 前缀。

**参考文件**：设计文档 `docs/superpowers/specs/2026-09-24-sal-order-pending-accept-design.md`；先例迁移 `backend/ruoyi-admin/src/main/resources/db/migration/V151__sal_order_four_status.sql`。

---

### Task 1: 后端状态机核心——枚举/默认值/接单/闸门（单元测试 TDD）

**Files:**
- Modify: `backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/SalOrderStatus.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/ISalOrderService.java:38-39`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java`（:145-146, :177-178, :229-231, :302-311, :321-325）
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderServiceImplTest.java`

**Interfaces:**
- Produces: `ISalOrderService.acceptOrder(Long orderId): int`——仅 `PENDING_ACCEPT` → `CONFIRMED`，其他态抛 `ServiceException("仅待接单订单可接单")`；Task 2 Controller 调用它。
- Produces: `SalOrderStatus.PENDING_ACCEPT`（code=`"PENDING_ACCEPT"`，info=`"待接单"`）。
- 行为契约：`createWithLines`（含 CRM 推单）强制落 `PENDING_ACCEPT`；`updateWithLines`/删除允许 PENDING_ACCEPT+CONFIRMED；取消对 PENDING_ACCEPT 放行；转工单仍只允许 CONFIRMED/PRODUCING。

- [ ] **Step 1: 改测试——默认态断言 + 新闸门用例（红）**

修改 `SalOrderServiceImplTest.java`：

1. 类注释（57-61 行附近）改为：

```java
/**
 * 销售订单Service单元测试（五态模型：PENDING_ACCEPT/CONFIRMED/PRODUCING/SHIPPED/CLOSED + CANCEL）
 * 覆盖:createWithLines/createFromCrm(建单即 PENDING_ACCEPT) / acceptOrder(接单) /
 *      updateWithLines/closeOrder/cancelOrder/deleteSalOrderByOrderIds(状态守卫) /
 *      toWorkorder(CONFIRMED+PRODUCING 两态可转, PENDING_ACCEPT 拦截, 可转量校验, 工单回填)
 *
 * @author qixiaoxia
 */
```

2. 把现有 `createWithLines_forcesConfirmed`（112-130 行）整体替换为：

```java
    @Test
    @DisplayName("createWithLines - 忽略前端传入状态，强制落 PENDING_ACCEPT")
    void createWithLines_forcesPendingAccept() {
        SalOrder order = new SalOrder();
        order.setOrderCode("SO001");
        order.setStatus("CONFIRMED"); // 旧前端可能仍传已确认
        SalOrderLine line = new SalOrderLine();
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("100"));
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(order); req.setLines(Collections.singletonList(line));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(200L); return 1;
        });

        SalOrder result = salOrderService.createWithLines(req);

        assertThat(result.getStatus()).isEqualTo("PENDING_ACCEPT");
    }
```

3. 把 `createFromCrm_confirmed`（132-153 行）整体替换为：

```java
    @Test
    @DisplayName("createFromCrm - 推单即 PENDING_ACCEPT")
    void createFromCrm_pendingAccept() {
        // mdItemMapper 反查物料 + insert 回填 id；断言落库订单 status=PENDING_ACCEPT
        com.ruoyi.system.domain.mes.md.MdItem item = new com.ruoyi.system.domain.mes.md.MdItem();
        item.setItemId(9L); item.setItemCode("P1"); item.setItemName("产品");
        when(mdItemMapper.selectMdItemList(any())).thenReturn(Collections.singletonList(item));
        when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
            ((SalOrder) inv.getArgument(0)).setOrderId(300L); return 1;
        });
        com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest crm =
                new com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest();
        crm.setOrderCode("CRM1"); crm.setOrderName("n"); crm.setClientName("c");
        com.ruoyi.system.domain.mes.sal.CrmOrderLineDTO dto = new com.ruoyi.system.domain.mes.sal.CrmOrderLineDTO();
        dto.setProductCode("P1"); dto.setQuantity(new BigDecimal("10"));
        crm.setLines(Collections.singletonList(dto));

        SalOrder result = salOrderService.createFromCrm(crm);

        assertThat(result.getStatus()).isEqualTo("PENDING_ACCEPT");
        verify(salOrderMapper).insertSalOrder(argThat(o -> "PENDING_ACCEPT".equals(o.getStatus())));
    }
```

4. 在 `cancelOrder_gate`（188 行）之后新增 4 个测试方法：

```java
    @Test
    @DisplayName("acceptOrder - PENDING_ACCEPT 接单 -> CONFIRMED")
    void acceptOrder_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        when(salOrderMapper.updateSalOrder(any())).thenReturn(1);

        salOrderService.acceptOrder(1L);

        verify(salOrderMapper).updateSalOrder(argThat(x -> "CONFIRMED".equals(x.getStatus())));
    }

    @Test
    @DisplayName("acceptOrder - 非 PENDING_ACCEPT（CONFIRMED）拒绝接单且不写库")
    void acceptOrder_gate() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "CONFIRMED"));

        assertThatThrownBy(() -> salOrderService.acceptOrder(1L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("待接单");
        verify(salOrderMapper, never()).updateSalOrder(any());
    }

    @Test
    @DisplayName("updateWithLines - PENDING_ACCEPT 无派生工单时可改，状态以库中值为准")
    void update_pendingAccept_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList())).thenReturn(java.util.Collections.emptyList());
        SalOrderCreateRequest req = new SalOrderCreateRequest();
        req.setOrder(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        req.setLines(Collections.emptyList());

        salOrderService.updateWithLines(req);

        verify(salOrderMapper).updateSalOrder(argThat(o -> "PENDING_ACCEPT".equals(o.getStatus())));
        verify(salOrderLineMapper).deleteSalOrderLineByOrderId(1L);
    }

    @Test
    @DisplayName("delete - PENDING_ACCEPT 无派生工单时可删")
    void delete_pendingAccept_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        when(salOrderMapper.selectWorkorderCountsByOrderIds(anyList())).thenReturn(java.util.Collections.emptyList());
        when(salOrderMapper.deleteSalOrderByOrderIds(any())).thenReturn(1);

        salOrderService.deleteSalOrderByOrderIds(new Long[]{1L});

        verify(salOrderLineMapper).deleteSalOrderLineByOrderId(1L);
        verify(salOrderMapper).deleteSalOrderByOrderIds(any());
    }

    @Test
    @DisplayName("cancelOrder - PENDING_ACCEPT 可取消 -> CANCEL")
    void cancelOrder_pendingAccept_ok() {
        when(salOrderMapper.selectSalOrderByOrderId(3L)).thenReturn(buildOrder(3L, "SO3", "PENDING_ACCEPT"));
        when(salOrderMapper.updateSalOrder(any())).thenReturn(1);

        salOrderService.cancelOrder(3L);

        verify(salOrderMapper).updateSalOrder(argThat(x -> "CANCEL".equals(x.getStatus())));
    }

    @Test
    @DisplayName("toWorkorder - PENDING_ACCEPT 未接单拒绝转工单")
    void toWorkorder_pendingAccept_rejected() {
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(buildLine(10L, 1L, new BigDecimal("100")));
        when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PENDING_ACCEPT"));
        SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
        req.setLineId(10L); req.setQuantity(new BigDecimal("10")); req.setWorkorderCode("W1");

        assertThatThrownBy(() -> salOrderService.toWorkorder(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已确认/生产中");
        verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
    }
```

- [ ] **Step 2: 运行测试确认红**

Run（先装改动过的 ruoyi-common 到本地仓库，否则 -pl 单模块拿到旧 jar）：

```bash
cd backend
mvn install -pl ruoyi-common -am -DskipTests -q
mvn test -pl ruoyi-system -Dtest=SalOrderServiceImplTest
```

Expected: 编译失败（`acceptOrder()` 方法不存在）或断言失败（默认态仍 CONFIRMED）。

- [ ] **Step 3: 枚举加 PENDING_ACCEPT**

`SalOrderStatus.java` 类注释整体替换为：

```java
/**
 * 销售订单状态枚举（主线五态 + 链外作废）
 * <pre>
 *   PENDING_ACCEPT(待接单) ──人工接单──▶ CONFIRMED(已确认) ──任一工单开工──▶ PRODUCING(生产中) ──全部明细发齐──▶ SHIPPED(已出货) ──人工结单──▶ CLOSED(已结单)
 *        │                                    │                                   │
 *        └────────────── 取消 ────────────────┴───────────────────────────────────┘
 *                                         ▼
 *                                   CANCEL(已取消，链外终态)
 * </pre>
 * 工序任务报工不改变订单状态，只驱动进度百分比。
 * 对应字典：sys_dict_type = 'mes_sal_order_status'（V124 建，V151 收敛，V161 新增待接单）
 *
 * @author qixiaoxia
 * @date 2026-08-13
 */
```

枚举常量首位加一行：

```java
    PENDING_ACCEPT("PENDING_ACCEPT", "待接单"),
    CONFIRMED("CONFIRMED", "已确认"),
```

- [ ] **Step 4: 默认值改 PENDING_ACCEPT（手工 + CRM 同一入口）**

`SalOrderServiceImpl.java`：

`:145-146` 改为：

```java
        // 建单即待接单（V161），忽略前端可能传入的状态；人工接单后才 CONFIRMED
        order.setStatus(SalOrderStatus.PENDING_ACCEPT.getCode());
```

`:177-178`（createFromCrm 内）改为：

```java
        // CRM 推单同样落待接单，需 MES 内人工接单（createWithLines 还会再强制一次）
        order.setStatus(SalOrderStatus.PENDING_ACCEPT.getCode());
```

- [ ] **Step 5: 编辑/删除闸门放宽，新增 acceptOrder**

`:229-231`（updateWithLines 校验）替换为：

```java
        if (!SalOrderStatus.PENDING_ACCEPT.is(existing.getStatus())
                && !SalOrderStatus.CONFIRMED.is(existing.getStatus())) {
            throw new ServiceException("仅待接单/已确认订单可修改,生产中已派生工单不可改,如需调整请取消后重建");
        }
```

`:302-311` `cancelOrder` 不改逻辑，仅在方法上一行补注释：

```java
    // PENDING_ACCEPT/CONFIRMED/PRODUCING 均可取消（SHIPPED/CLOSED/CANCEL 拦截）
```

在 `cancelOrder` 方法之后（`:311` 后）新增方法：

```java
    @Override
    public int acceptOrder(Long orderId)
    {
        SalOrder order = mustExist(orderId);
        if (!SalOrderStatus.PENDING_ACCEPT.is(order.getStatus())) {
            throw new ServiceException("仅待接单订单可接单");
        }
        return updateStatus(orderId, SalOrderStatus.CONFIRMED.getCode());
    }
```

`:321-325`（删除循环内校验）替换为：

```java
            // 仅待接单/已确认可删:转工单后(PRODUCING 起)删除会使工单 sales_order_line_id 成孤儿
            if (!SalOrderStatus.PENDING_ACCEPT.is(order.getStatus())
                    && !SalOrderStatus.CONFIRMED.is(order.getStatus()))
            {
                throw new ServiceException("订单 " + order.getOrderCode() + " 非待接单/已确认状态,不可删除");
            }
```

接口 `ISalOrderService.java` 在 `cancelOrder` 声明（39 行）之后加：

```java
    /** 接单:PENDING_ACCEPT->CONFIRMED（仅待接单可接，其他态拒绝） */
    public int acceptOrder(Long orderId);
```

- [ ] **Step 6: 运行单测确认绿**

Run:

```bash
cd backend
mvn test -pl ruoyi-system -Dtest=SalOrderServiceImplTest
```

Expected: `Tests run: 31, Failures: 0, Error: 0`（原 25 个用例中 2 个改断言不增减 + 新增 6 个 = 31；若数量对不上以全绿且含新增用例为准）。

- [ ] **Step 7: 提交**

```bash
git add backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/SalOrderStatus.java \
  backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/ISalOrderService.java \
  backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java \
  backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderServiceImplTest.java
git commit -m "feat(sal): 新增待接单状态与接单动作，建单默认待接单"
```

---

### Task 2: 接单 HTTP 接口 + Flyway V161 + Excel 文案 + 集成测试

**Files:**
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/SalOrderController.java:165-171`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/SalOrder.java:77`
- Create: `backend/ruoyi-admin/src/main/resources/db/migration/V161__sal_order_pending_accept.sql`
- Test: `backend/ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/sal/SalOrderIT.java`

**Interfaces:**
- Consumes: Task 1 的 `salOrderService.acceptOrder(Long)`。
- Produces: `PUT /mes/sal/order/accept/{orderId}`，权限 `mes:sal:order:edit`，返回标准 `{code:200}`；非法态 `code:500` 且 msg 含"待接单"。
- Produces: V161 迁移——字典项 `PENDING_ACCEPT 待接单`（info、is_default=Y、sort=1），列默认改 `PENDING_ACCEPT`。

- [ ] **Step 1: 改集成测试——默认态断言 + 接单闸门（红）**

修改 `SalOrderIT.java`：

1. 类 javadoc（33-49 行）首句改为：

```java
 * <p>验收链：建单即 PENDING_ACCEPT（factory_id=1 由 FactoryIdInterceptor 注入）→ accept 接单
 * → CONFIRMED → toWorkorder → 开工事务（AFTER_COMMIT 事件同步投递）→ PRODUCING → ...
```

2. `createOrderAndWorkorder` 中 366-370 行的建单断言块替换为：

```java
        // 建单即 PENDING_ACCEPT（V161 起），factory_id 由 FactoryIdInterceptor 注入为线程工厂 1
        Map<String, Object> created = jdbcTemplate.queryForMap(
                "select status, factory_id from qxx_sal_order where order_id=?", orderId);
        assertThat(created.get("status")).isEqualTo("PENDING_ACCEPT");
        assertThat(((Number) created.get("factory_id")).longValue()).isEqualTo(FACTORY.longValue());

        // 接单后才 CONFIRMED，方可转工单
        ResponseEntity<Map> accepted = restTemplate.exchange(baseUrl() + "/accept/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(accepted.getBody().get("code")).isEqualTo(200);
        assertThat(queryStatus(orderId)).isEqualTo("CONFIRMED");
```

3. 把 `createOrderOnly`（402-429 行）重构为"原始建单 + 接单"两个方法，整体替换为：

```java
    /** 仅建单并接单（CONFIRMED，无工单）——用于无工单降级 CONFIRMED 分支 */
    private Long createOrderOnly(String orderCode)
    {
        Long orderId = createOrderRaw(orderCode);
        ResponseEntity<Map> accepted = restTemplate.exchange(baseUrl() + "/accept/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(accepted.getBody().get("code")).isEqualTo(200);
        assertThat(queryStatus(orderId)).isEqualTo("CONFIRMED");
        return orderId;
    }

    /** 仅原始建单（V161 起落 PENDING_ACCEPT），不接单不转工单 */
    @SuppressWarnings("unchecked")
    private Long createOrderRaw(String orderCode)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("productId", 1);
        line.put("productCode", "P001");
        line.put("productName", "产品");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantity", LINE_QTY);

        Map<String, Object> order = new HashMap<>();
        order.put("orderCode", orderCode);
        order.put("orderName", "订单-" + orderCode);
        order.put("clientCode", "C001");
        order.put("clientName", "测试客户");
        order.put("businessLine", "DOMESTIC");

        Map<String, Object> createReq = new HashMap<>();
        createReq.put("order", order);
        createReq.put("lines", List.of(line));

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl() + "/createWithLines", authRequest(createReq), Map.class);
        assertThat(createResp.getBody().get("code")).isEqualTo(200);
        return ((Number) ((Map<?, ?>) createResp.getBody().get("data")).get("orderId")).longValue();
    }
```

4. 在 `gates_rejected` 测试方法之后新增一个测试：

```java
    @Test
    @DisplayName("待接单闸门：建单PENDING_ACCEPT；未接单转工单500；接单200→CONFIRMED后可转工单")
    void pending_accept_gate_then_accept()
    {
        Long orderId = createOrderRaw("SO-IT-PA");
        Long lineId = getFirstLineId(orderId);
        assertThat(queryStatus(orderId)).isEqualTo("PENDING_ACCEPT");

        // 未接单转工单 → 500
        Map<String, Object> twReq = new HashMap<>();
        twReq.put("lineId", lineId);
        twReq.put("quantity", new BigDecimal("100"));
        twReq.put("workorderCode", "WO-IT-PA-BLOCKED");
        twReq.put("requestDate", "2026-07-30 00:00:00");
        ResponseEntity<Map> blocked = restTemplate.postForEntity(
                baseUrl() + "/toWorkorder", authRequest(twReq), Map.class);
        assertThat(blocked.getBody().get("code")).isEqualTo(500);
        assertThat(blocked.getBody().get("msg").toString()).contains("已确认/生产中");

        // 接单 → 200 / CONFIRMED
        ResponseEntity<Map> accept = restTemplate.exchange(baseUrl() + "/accept/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(accept.getBody().get("code")).isEqualTo(200);
        assertThat(queryStatus(orderId)).isEqualTo("CONFIRMED");

        // 重复接单 → 500
        ResponseEntity<Map> repeat = restTemplate.exchange(baseUrl() + "/accept/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(repeat.getBody().get("code")).isEqualTo(500);
        assertThat(repeat.getBody().get("msg").toString()).contains("待接单");

        // 接单后转工单 → 200
        twReq.put("workorderCode", "WO-IT-PA");
        ResponseEntity<Map> ok = restTemplate.postForEntity(
                baseUrl() + "/toWorkorder", authRequest(twReq), Map.class);
        assertThat(ok.getBody().get("code")).isEqualTo(200);
    }
```

- [ ] **Step 2: 运行集成测试确认红**

Run（需 Docker；集成测试库 Flyway baseline=136，V161 会在 Testcontainer 上真实执行）：

```bash
cd backend
docker compose up -d redis
mvn install -pl ruoyi-system -am -DskipTests -q
mvn failsafe:integration-test -pl ruoyi-admin -Dit.test=SalOrderIT
```

Expected: 编译失败（`/accept/` 404 或方法不存在）；新测试 404/500 不符预期。

- [ ] **Step 3: Controller 加接单接口**

`SalOrderController.java` 在 `cancel` 方法（165-171 行）之后插入：

```java
    @PreAuthorize("@ss.hasPermi('mes:sal:order:edit')")
    @Log(title = "销售订单接单", businessType = BusinessType.UPDATE)
    @PutMapping("/accept/{orderId}")
    public AjaxResult accept(@PathVariable("orderId") Long orderId)
    {
        return toAjax(salOrderService.acceptOrder(orderId));
    }
```

- [ ] **Step 4: Excel 导出注解补状态**

`SalOrder.java:77` 改为：

```java
    @Excel(name = "状态", readConverterExp = "PENDING_ACCEPT=待接单,CONFIRMED=已确认,PRODUCING=生产中,SHIPPED=已出货,CLOSED=已结单,CANCEL=已取消")
```

- [ ] **Step 5: 创建 V161 迁移**

创建 `backend/ruoyi-admin/src/main/resources/db/migration/V161__sal_order_pending_accept.sql`：

```sql
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
```

- [ ] **Step 6: 重跑集成测试确认绿**

Run:

```bash
cd backend
mvn failsafe:integration-test -pl ruoyi-admin -Dit.test=SalOrderIT
```

Expected: `Tests run: 6, Failures: 0, Error: 0`（原 5 个 + 新增 1 个；全部通过即说明 V161 在 Testcontainer 上执行成功）。

- [ ] **Step 7: 提交**

```bash
git add backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/SalOrderController.java \
  backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/SalOrder.java \
  backend/ruoyi-admin/src/main/resources/db/migration/V161__sal_order_pending_accept.sql \
  backend/ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/sal/SalOrderIT.java
git commit -m "feat(sal): 接单接口与V161字典/列默认迁移，补集成测试"
```

---

### Task 3: 前端接单入口与 E2E 规格更新

**Files:**
- Modify: `frontend/src/types/api/mes/sal/order.ts:3-4`
- Modify: `frontend/src/api/mes/sal/order.ts:25-27`
- Modify: `frontend/src/views/mes/sal/order/index.vue`（:57-65 行按钮、:145 import、:225 reset、:299-302 methods、:311-314 computed）
- Modify: `frontend/src/views/mes/sal/order/detail.vue`（statusTagType map）
- Test: `e2e/tests/sal/sal-order-lifecycle.spec.ts`（V151 后已失效，更新为接单语义）

**Interfaces:**
- Consumes: Task 2 的 `PUT /mes/sal/order/accept/{id}`。
- 不新增 Vitest 组件测试：列表页为既有 Options API 大文件，状态交互由 Task 4 的 Playwright 正式覆盖（项目该页本无 Vitest 规格，不为本次改动新建重型 mock）。

- [ ] **Step 1: 类型与 API**

`frontend/src/types/api/mes/sal/order.ts` 3-4 行替换为：

```ts
/** 销售订单状态（五态主线 + CANCEL 链外） */
export type SalOrderStatus = 'PENDING_ACCEPT' | 'CONFIRMED' | 'PRODUCING' | 'SHIPPED' | 'CLOSED' | 'CANCEL'
```

`frontend/src/api/mes/sal/order.ts` 在 `cancelOrder`（25-27 行）之后加：

```ts
export function acceptOrder(id: number): Promise<AjaxResult> {
  return request({ url: '/mes/sal/order/accept/' + id, method: 'put' })
}
```

- [ ] **Step 2: 列表页按钮显隐与接单动作**

`frontend/src/views/mes/sal/order/index.vue`：

1. import 行（145 行）改为：

```javascript
import { listOrder, getOrderDetail, createOrderWithLines, updateOrderWithLines, closeOrder, cancelOrder, acceptOrder, delOrder } from '@/api/mes/sal/order'
```

2. 操作列（60-65 行）整段按钮替换为：

```html
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button v-if="scope.row.status==='PENDING_ACCEPT'" link type="success" size="small" @click="handleAccept(scope.row)" v-hasPermi="['mes:sal:order:edit']">接单</el-button>
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED'].includes(scope.row.status) && !hasWorkorder(scope.row)" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:sal:order:edit']">改</el-button>
          <el-button v-if="scope.row.status==='CONFIRMED' || scope.row.status==='PRODUCING'" link type="warning" size="small" @click="handleToWorkorder(scope.row)" v-hasPermi="['mes:sal:order:workorder']">生成工单</el-button>
          <el-button v-if="scope.row.status==='SHIPPED'" link type="success" size="small" @click="handleClose(scope.row)" v-hasPermi="['mes:sal:order:edit']">结单</el-button>
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED','PRODUCING'].includes(scope.row.status)" link type="danger" size="small" @click="handleCancel(scope.row)" v-hasPermi="['mes:sal:order:edit']">取消</el-button>
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED'].includes(scope.row.status) && !hasWorkorder(scope.row)" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sal:order:remove']"></el-button>
```

3. methods 中在 `handleClose`（299 行）之前插入：

```javascript
    handleAccept(row) { this.$modal.confirm('确认接单 "' + row.orderCode + '"？接单后进入已确认状态，可转工单生产。').then(() => acceptOrder(row.orderId)).then(() => { this.getList(); this.$modal.msgSuccess('接单成功') }).catch(() => {}) },
```

4. `reset()`（225 行）中 `status: 'CONFIRMED'` 改为：

```javascript
status: 'PENDING_ACCEPT'
```

5. computed（311-314 行）替换为：

```javascript
    /** 顶部「修改」：选中行均为待接单/已确认且均未派生工单（含未开工）才可用 */
    canEditSelected() { return this.selectedRows.length > 0 && this.selectedRows.every(r => ['PENDING_ACCEPT','CONFIRMED'].includes(r.status) && !this.hasWorkorder(r)) },
    /** 顶部「删除」：同修改口径，待接单/已确认且未派生工单 */
    canDeleteSelected() { return this.selectedRows.length > 0 && this.selectedRows.every(r => ['PENDING_ACCEPT','CONFIRMED'].includes(r.status) && !this.hasWorkorder(r)) }
```

- [ ] **Step 3: 详情页颜色兜底 map**

`frontend/src/views/mes/sal/order/detail.vue` 的 `statusTagType` map 改为：

```typescript
  const map: Record<string, string> = { PENDING_ACCEPT: 'info', CONFIRMED: 'success', PRODUCING: 'warning', SHIPPED: 'primary', CLOSED: 'info', CANCEL: 'danger' }
```

（文案仍由 `useDict('mes_sal_order_status')` 字典驱动，无需改。）

- [ ] **Step 4: 更新 E2E 规格（确认 → 接单）**

`e2e/tests/sal/sal-order-lifecycle.spec.ts`：

1. 第一个测试标题（17 行）：

```typescript
  test('完整流程：建单 -> 接单 -> 转工单 -> 工单页验证来源', async ({ page }) => {
```

2. 91 行日志：

```typescript
    console.log('  ✅ 销售订单创建成功（PENDING_ACCEPT）')
```

3. 93-107 行 Step 2 整段替换：

```typescript
    // ==== Step 2：搜索 + 接单 ====
    await page.locator('input[placeholder*="销售订单号"]').first().fill(uniqueCode)
    await page.locator('button').filter({ hasText: '搜索' }).first().click()
    await page.waitForTimeout(2000)
    await expect(page.locator('.el-table__body tr').first()).toBeVisible({ timeout: 5000 })

    const acceptBtn = page.locator('.el-table__body .el-button').filter({ hasText: /^接单$/ }).first()
    await expect(acceptBtn).toBeVisible({ timeout: 5000 })
    acceptBtn.click()
    await page.waitForTimeout(500)
    const mb1 = page.locator('.el-message-box__btns button').filter({ hasText: '确定' }).first()
    await expect(mb1).toBeVisible({ timeout: 3000 })
    await mb1.click()
    await page.waitForTimeout(2000)
    console.log('  ✅ 接单成功（PENDING_ACCEPT -> CONFIRMED）')
```

4. 第二个测试（180 行起）：

- 标题：`'选路线流程：API建单(item_id=219有路线) → 接单 → 转工单选路线 → BOM提交'`
- 199 行 payload 中 `status: 'PREPARE'` 改为 `status: 'PENDING_ACCEPT'`
- 210-216 行"API确认"块替换为：

```typescript
    // API接单
    await page.evaluate(async (args) => {
      const r = await fetch(args.base + '/mes/sal/order/accept/' + args.oid, {
        method: 'PUT', headers: { 'Authorization': 'Bearer ' + args.token }
      })
      const d = await r.json()
      if (d.code !== 200) throw new Error('接单失败: ' + JSON.stringify(d))
    }, { base: API_BASE, token, oid: orderId })
    console.log('  ✅ API接单成功')
```

- [ ] **Step 5: 静态检查**

Run:

```bash
cd frontend
npx eslint src/views/mes/sal/order/index.vue src/api/mes/sal/order.ts
```

Expected: 无 error（warning 可接受；与本改动无关的基线 warning 不修）。不跑全量 vue-tsc（干净 main 基线即有 800+ 报错，无信号；类型面仅一行联合类型扩展）。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/types/api/mes/sal/order.ts frontend/src/api/mes/sal/order.ts \
  frontend/src/views/mes/sal/order/index.vue frontend/src/views/mes/sal/order/detail.vue \
  e2e/tests/sal/sal-order-lifecycle.spec.ts
git commit -m "feat(sal): 列表接单入口与待接单操作显隐，E2E更新为接单语义"
```

---

### Task 4: 打包重启 + token 实测 + Playwright 全链路验证（红线）

**Files:** 无代码产出（若测出 bug 回对应 Task 修复后重新提交）。

**Interfaces:** 消费前 3 个 Task 的全部产物。

- [ ] **Step 1: 全量打包**

Run:

```bash
cd backend
mvn -pl ruoyi-admin -am package -DskipTests
```

Expected: `BUILD SUCCESS`，`ruoyi-admin/target/ruoyi-admin.jar` 时间戳为最新。

- [ ] **Step 2: 重启运行中后端，确认 V161 执行**

Run:

```bash
PID=$(lsof -ti:8081 | head -1); [ -n "$PID" ] && kill "$PID"; sleep 3
cd backend
nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
for i in $(seq 1 40); do curl -sf http://localhost:8081/captchaImage >/dev/null 2>&1 && break; sleep 2; done
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8081/captchaImage
grep -E 'V161|Flyway.*migrat' /tmp/ruoyi-backend.log | tail -5
```

Expected: 输出 `200`；日志含 `Migrating schema ... to version "161 - sal order pending accept"` 且无迁移异常。

- [ ] **Step 3: 清字典缓存（SQL 直插字典不经后台缓存刷新）**

Run（容器名按实际 `docker ps` 为准）：

```bash
RD=$(docker ps --format '{{.Names}}' | grep -i redis | head -1)
docker exec "$RD" sh -c "redis-cli --scan --pattern 'sys_dict:*' | xargs -r redis-cli del"
```

Expected: 输出删除行数 ≥ 1；下次查询字典时从库重载，"待接单"出现。

- [ ] **Step 4: token 冒烟建单→拦截→接单→重复接单→清理**

Run（requests 依赖已装；client/item 各取列表第一条做冒烟数据）：

```bash
python3 - <<'PY'
import subprocess, time, requests
BASE = 'http://localhost:8081'
token = subprocess.check_output(['python3', 'backend/scripts/get_token.py']).decode().strip()
h = {'Authorization': 'Bearer ' + token}
client = requests.get(BASE + '/mes/md/client/list', params={'pageSize': 1}, headers=h).json()['rows'][0]
item = requests.get(BASE + '/mes/md/item/list', params={'pageSize': 1}, headers=h).json()['rows'][0]
code = 'SMK-PA-' + str(int(time.time()))
payload = {
  'order': {'orderCode': code, 'orderName': '冒烟-待接单', 'clientId': client['clientId'],
            'clientCode': client['clientCode'], 'clientName': client['clientName'], 'businessLine': 'DOMESTIC'},
  'lines': [{'productId': item['itemId'], 'productCode': item['itemCode'], 'productName': item['itemName'],
             'unitOfMeasure': item.get('unitOfMeasure') or 'PCS', 'quantity': 1}]
}
r = requests.post(BASE + '/mes/sal/order/createWithLines', json=payload, headers=h).json()
assert r['code'] == 200, r
oid = r['data']['orderId']
status = lambda: requests.get(BASE + f'/mes/sal/order/detail/{oid}', headers=h).json()['data']['status']
assert status() == 'PENDING_ACCEPT', status()
ok = requests.put(BASE + f'/mes/sal/order/accept/{oid}', headers=h).json()
assert ok['code'] == 200, ok
assert status() == 'CONFIRMED', status()
dup = requests.put(BASE + f'/mes/sal/order/accept/{oid}', headers=h).json()
assert dup['code'] == 500 and '待接单' in dup['msg'], dup
d = requests.delete(BASE + f'/mes/sal/order/{oid}', headers=h).json()
assert d['code'] == 200, d
print('SMOKE OK', code)
PY
```

Expected: 打印 `SMOKE OK SMK-PA-...`。若 client/item 接口路径在本环境不同，先 `curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/mes/md/client/list?pageSize=1` 核实路径再调整，不许跳过断言。

- [ ] **Step 5: 启动前端（如未启动）并跑 Playwright**

Run:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:5173/ || true
# 若非 200：cd frontend && nohup npm run dev > /tmp/qxx-frontend.log 2>&1 &  等待 5173 就绪
cd e2e
npx playwright test tests/sal/sal-order-lifecycle.spec.ts --reporter=line
```

Expected: 2 个测试全过（建单后列表出现【接单】按钮 → 接单成功提示 → 生成工单按钮可用 → 工单页验证来源）。

- [ ] **Step 6: 浏览器人工点验补 E2E 盲区**

启动/打开前端页面（admin 登录），逐项确认：

1. 销售订单列表状态搜索下拉含"待接单"，新单 tag 为灰色"待接单"；
2. 待接单行按钮：有【接单】，无【生成工单】【结单】；【改】【删】【取消】可见可用；
3. 接单后按钮组切换为已确认口径（接单消失、生成工单出现）；
4. 详情页状态 tag 文案/颜色正确；
5. 成品销售出库选单弹窗（`wm/product_sales` 新增选销售订单）查不到待接单。

Expected: 5 项全部符合；任何一项不符即停下修 bug，不带病收尾。

- [ ] **Step 7: 收尾汇报**

- 跑一遍后端全量单测防回归：`cd backend && mvn test -q`（预期全绿；既有失败若与本次无关，停下汇报，不偷偷修）。
- `git log --oneline main..HEAD` 确认 3 个提交在分支上；向用户汇报：改动文件、验证证据（单测/IT/E2E/冒烟输出）、遗留事项。
- **不自行 merge/推送/部署**，等用户决定。
