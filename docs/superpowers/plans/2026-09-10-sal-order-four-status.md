# 销售订单四态收敛 + 报工只更新进度 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 销售订单状态收敛为 已确认 CONFIRMED → 生产中 PRODUCING → 已出货 SHIPPED → 已结单 CLOSED（+链外 CANCEL）；报工不动订单状态，订单列表/详情展示任务口径实时进度百分比。

**Architecture:** 删除审核流（建单即 CONFIRMED）；工单开工、出库发齐两个跨域动作通过 Spring 领域事件（AFTER_COMMIT + REQUIRES_NEW）驱动 sal 侧条件 UPDATE 推进状态；进度沿用 PR #55「实时聚合 + IN 批量回填」模式，从订单行→工单→工序任务数量加权计算。

**Tech Stack:** Spring Boot 4 / MyBatis / Flyway（后端），Vue 3 Options API + Element Plus（订单页）/ Vue 3 setup + TS（详情页与出库选单），JUnit5 + Mockito + Testcontainers。

设计依据：`docs/superpowers/specs/2026-09-10-sal-order-four-status-design.md`

## Global Constraints

- 分支 `B3_feature`（基于 main f6aa06b），每个 Task 结束一次 commit；提交信息中文，格式 `feat(sal): ...` / `test(sal): ...` / `fix(wm,sal): ...`。
- SQL：所有业务查询带 factory_id 条件；Flyway 迁移裸 JDBC，DML 显式处理 factory_id，DML 必须幂等；不得修改已执行的迁移文件，新增版本号 **V151**（当前最大 V149）。
- 后端函数 ≤50 行；状态比较一律用 `SalOrderStatus` 枚举常量，禁止新写裸状态字符串（pro/wm 域自身状态常量除外）。
- 后端改完必须 `mvn -pl ruoyi-admin -am package -DskipTests` 重新打包 + 重启 jar + token 实测接口（Task 8 红线）。
- 前端组件 ≤300 行；本功能不改 app/、不改报表。
- 测试命令：后端单测 `cd backend && mvn -pl ruoyi-system test -Dtest=类名`；集成测试需 Docker：`cd backend && mvn -pl ruoyi-admin -am verify -Dtest=SalOrderIT`；前端组件测试 `cd frontend && npx vitest run src/views/mes/sal/order`。
- **对 spec 的两处实现细化（已与代码核实，实现时同步修订 spec 对应行）：**
  1. 订单「发齐」口径 = 该订单行在全部非作废出库单下 **status='SHIPPED' 箱的 quantity 合计**（`qxx_wm_product_sales_box`），与出库单 header `shipped_quantity` 的累加口径（`sumBoxQuantity`）一致；spec 4.3 写的发运明细表 detail 是过账口径，不是发运口径，以本计划为准。
  2. D1「连续报工进度递增」用三层覆盖：Task 4 反射单测固化「报工服务零 sal 依赖」、Task 6 IT 验「任务产量累加期间订单状态恒 PRODUCING 且进度严格递增」、Task 8 真实 HTTP 连续审核报工手工实测。

---

### Task 1: V151 Flyway 迁移（字典四态 + 存量刷态 + 默认值 + 按钮权限回收）

**Files:**
- Create: `backend/ruoyi-admin/src/main/resources/db/migration/V151__sal_order_four_status.sql`

**Interfaces:**
- Produces: DB 状态——字典 `mes_sal_order_status` 有效项为 CONFIRMED(success,默认)/PRODUCING(warning)/SHIPPED(primary)/CLOSED(info,文案已结单)/CANCEL(danger)，PREPARE/PENDING 停用；`qxx_sal_order.status` 默认 'CONFIRMED'；菜单 2909/2916（submit/approve，V124 建）及其角色授权删除。

- [ ] **Step 1: 写迁移文件**

```sql
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

-- ④ 列默认值 + 注释（保持原 varchar(64) 可空定义）
ALTER TABLE qxx_sal_order
  MODIFY COLUMN status varchar(64) DEFAULT 'CONFIRMED' COMMENT '订单状态：CONFIRMED已确认/PRODUCING生产中/SHIPPED已出货/CLOSED已结单/CANCEL已取消';

-- ⑤ 删按钮角色授权 + 按钮菜单（幂等）
DELETE FROM sys_role_menu WHERE menu_id IN (2909, 2916);
DELETE FROM sys_menu WHERE menu_id IN (2909, 2916);
```

- [ ] **Step 2: 本地连开发库手工验证迁移可执行且可重入**

Run（mysql 别名按本机环境，端口 3306 见 AGENTS.md）：
```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
```
重启本地后端让 Flyway 执行，等 `curl -s http://localhost:8081/captchaImage` 返回 200，然后：
```bash
mysql -h127.0.0.1 -P3306 -uroot mes -e "SELECT dict_value,dict_label,list_class,status FROM sys_dict_data WHERE dict_type='mes_sal_order_status' ORDER BY dict_sort; SELECT menu_id FROM sys_menu WHERE menu_id IN (2909,2916); SHOW CREATE TABLE qxx_sal_order\G" 2>/dev/null | grep -E "PREPARE|PENDING|CONFIRMED|PRODUCING|SHIPPED|CLOSED|CANCEL|DEFAULT 'CONFIRMED'"
```
Expected: PREPARE/PENDING 的 status=1；PRODUCING/SHIPPED 存在；menu 查询无行；列默认 `DEFAULT 'CONFIRMED'`。日志无 Flyway 异常。

- [ ] **Step 3: Commit**

```bash
git add backend/ruoyi-admin/src/main/resources/db/migration/V151__sal_order_four_status.sql
git commit -m "feat(sal): V151 订单四态字典/存量刷态/默认值/回收审核按钮权限"
```

---

### Task 2: 枚举四态化 + 删除审核流 + 门控重写（Service/Controller/实体）

**Files:**
- Modify: `backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/SalOrderStatus.java`（全量重写枚举项与类注释）
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/SalOrder.java:71-82`（@Excel 文案；新增瞬态字段 progressPercent；新增查询字段 statusList）
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/ISalOrderService.java`（删 5 个审核方法签名）
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java`
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/SalOrderController.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderMapper.xml`（convertible 两态；statusList foreach）
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderServiceImplTest.java`（重写）

**Interfaces:**
- Produces: `SalOrderStatus {CONFIRMED, PRODUCING, SHIPPED, CLOSED, CANCEL}`；`SalOrder.getProgressPercent()/setStatusList(List<String>)`；Service 仅保留 createWithLines/createFromCrm/updateWithLines/getDetail/closeOrder/cancelOrder/deleteSalOrderByOrderIds/toWorkorder 等；`GET /mes/sal/order/list` 支持 `statusList` 多状态参数。
- Consumes: 无（基础任务）。

- [ ] **Step 1: 先重写单元测试（红灯）**

整体替换 `SalOrderServiceImplTest.java`：删去 submit/approve/reject/batch 全部用例；改为下列用例（保留现有 mock 骨架/SecurityUtils 静态 mock/lockTemplate/txTemplate 初始化，import 随之增删）：

```java
@Test
@DisplayName("createWithLines - 忽略前端旧状态，强制落 CONFIRMED")
void createWithLines_forcesConfirmed() {
    SalOrder order = new SalOrder();
    order.setOrderCode("SO001");
    order.setStatus("PREPARE"); // 旧前端可能仍传
    SalOrderLine line = new SalOrderLine();
    line.setProductId(1L);
    line.setQuantity(new BigDecimal("100"));
    SalOrderCreateRequest req = new SalOrderCreateRequest();
    req.setOrder(order); req.setLines(Collections.singletonList(line));
    when(salOrderMapper.insertSalOrder(any(SalOrder.class))).thenAnswer(inv -> {
        ((SalOrder) inv.getArgument(0)).setOrderId(200L); return 1;
    });

    SalOrder result = salOrderService.createWithLines(req);

    assertThat(result.getStatus()).isEqualTo("CONFIRMED");
}

@Test
@DisplayName("createFromCrm - 推单即 CONFIRMED")
void createFromCrm_confirmed() {
    // mdItemMapper 反查物料 + insert 回填 id；断言落库订单 status=CONFIRMED
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

    assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    verify(salOrderMapper).insertSalOrder(argThat(o -> "CONFIRMED".equals(o.getStatus())));
}

@Test
@DisplayName("updateWithLines - 仅 CONFIRMED 可改；PRODUCING 拒绝且不改状态")
void update_gate() {
    when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
    SalOrderCreateRequest req = new SalOrderCreateRequest();
    SalOrder o = buildOrder(1L, "SO1", "PRODUCING"); req.setOrder(o);
    assertThatThrownBy(() -> salOrderService.updateWithLines(req))
            .isInstanceOf(ServiceException.class).hasMessageContaining("已确认");
}

@Test
@DisplayName("closeOrder - 仅 SHIPPED 可结单")
void closeOrder_gate() {
    when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
    assertThatThrownBy(() -> salOrderService.closeOrder(1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("已出货");

    when(salOrderMapper.selectSalOrderByOrderId(2L)).thenReturn(buildOrder(2L, "SO2", "SHIPPED"));
    when(salOrderMapper.updateSalOrder(any())).thenReturn(1);
    salOrderService.closeOrder(2L);
    verify(salOrderMapper).updateSalOrder(argThat(x -> "CLOSED".equals(x.getStatus())));
}

@Test
@DisplayName("cancelOrder - CONFIRMED/PRODUCING 可取消，SHIPPED/CLOSED 拒绝")
void cancelOrder_gate() {
    when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "SHIPPED"));
    assertThatThrownBy(() -> salOrderService.cancelOrder(1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("不可取消");
    when(salOrderMapper.selectSalOrderByOrderId(2L)).thenReturn(buildOrder(2L, "SO2", "PRODUCING"));
    when(salOrderMapper.updateSalOrder(any())).thenReturn(1);
    salOrderService.cancelOrder(2L);
    verify(salOrderMapper).updateSalOrder(argThat(x -> "CANCEL".equals(x.getStatus())));
}

@Test
@DisplayName("delete - 非 CONFIRMED 不可删")
void delete_gate() {
    when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
    assertThatThrownBy(() -> salOrderService.deleteSalOrderByOrderIds(new Long[]{1L}))
            .isInstanceOf(ServiceException.class).hasMessageContaining("不可删除");
}

@Test
@DisplayName("toWorkorder - CONFIRMED 与 PRODUCING 均可转；SHIPPED 拒绝")
void toWorkorder_statusGate() {
    // PRODUCING 正向：保留现有 toWorkorder_ok_backfill 用例，把前置 mock 单状态保持 CONFIRMED 即可
    // 新增 SHIPPED 拒绝：
    when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(buildLine(10L, 1L, new BigDecimal("100")));
    when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "SHIPPED"));
    SalOrderToWorkorderRequest req = new SalOrderToWorkorderRequest();
    req.setLineId(10L); req.setQuantity(new BigDecimal("10")); req.setWorkorderCode("W1");
    assertThatThrownBy(() -> salOrderService.toWorkorder(req))
            .isInstanceOf(ServiceException.class);
    verify(proWorkorderService, never()).createWorkorderWithBom(any(), any(), any());
}
```
保留原 `toWorkorder_ok_backfill`、`toWorkorder_overConvertible_rejected` 两个用例不变。新增 mock 字段（若尚缺）：`@Mock private MdItemMapper mdItemMapper;`。删除未用 import（Consumer/Map 若只剩批量用例使用则一并删）。

- [ ] **Step 2: 跑测试确认编译失败/红灯**

Run: `cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderServiceImplTest -q`
Expected: 编译失败（枚举无 PREPARE/PENDING、Service 无 submitOrder 等）——这是预期红灯。

- [ ] **Step 3: 重写 SalOrderStatus 枚举**

全量替换枚举体（保留包名/fromCode/is）：

```java
/**
 * 销售订单状态枚举（主线四态 + 链外作废）
 * <pre>
 *   CONFIRMED(已确认) ──任一工单开工──▶ PRODUCING(生产中) ──全部明细发齐──▶ SHIPPED(已出货) ──人工结单──▶ CLOSED(已结单)
 *        │                                   │
 *        └──────────── 取消 ─────────────────┘
 *                       ▼
 *                 CANCEL(已取消，链外终态)
 * </pre>
 * 工序任务报工不改变订单状态，只驱动进度百分比。
 * 对应字典：sys_dict_type = 'mes_sal_order_status'（V124 建，V151 收敛）
 */
public enum SalOrderStatus {
    CONFIRMED("CONFIRMED", "已确认"),
    PRODUCING("PRODUCING", "生产中"),
    SHIPPED("SHIPPED", "已出货"),
    CLOSED("CLOSED", "已结单"),
    CANCEL("CANCEL", "已取消");
    // 构造/getCode/getInfo/is/fromCode 原样保留
```

- [ ] **Step 4: 改 SalOrder 实体**

- status 字段 @Excel 改为：
```java
@Excel(name = "状态", readConverterExp = "CONFIRMED=已确认,PRODUCING=生产中,SHIPPED=已出货,CLOSED=已结单,CANCEL=已取消")
private String status;
```
- approveRemark 字段注释改为 `/** 历史审核意见（审核流已废弃，保留列不写入） */`。
- lines 字段后新增：
```java
/** 生产进度百分比 0-100（任务口径实时聚合，非DB字段） */
private transient Integer progressPercent;

/** 列表查询：多状态过滤（非DB字段） */
private transient java.util.List<String> statusList;
```
- 补 getter/setter 四行（getProgressPercent/setProgressPercent/getStatusList/setStatusList）。

- [ ] **Step 5: 改 SalOrderServiceImpl**

- `createWithLines`：把 :120 改为无条件 `order.setStatus(SalOrderStatus.CONFIRMED.getCode());`（即无视前端传值）。
- `createFromCrm`：:149-150 注释与赋值改为 `order.setStatus(SalOrderStatus.CONFIRMED.getCode());`。
- `updateWithLines` 门控（:201-204）改为：
```java
if (!SalOrderStatus.CONFIRMED.is(existing.getStatus())) {
    throw new ServiceException("仅已确认(CONFIRMED)订单可修改,生产中已派生工单不可改,如需调整请取消后重建");
}
order.setStatus(existing.getStatus()); // 状态不允许经编辑接口篡改
```
- 删除方法：submitOrder、approveOrder、rejectOrder、batchSubmit、batchApprove、executeBatch（含 Map/Consumer/ArrayList/HashMap import，若全无用则删）。
- closeOrder 改为：
```java
@Override
public int closeOrder(Long orderId) {
    SalOrder order = mustExist(orderId);
    if (!SalOrderStatus.SHIPPED.is(order.getStatus())) throw new ServiceException("仅已出货订单可结单");
    return updateStatus(orderId, SalOrderStatus.CLOSED.getCode());
}
```
- cancelOrder 改为：
```java
@Override
public int cancelOrder(Long orderId) {
    SalOrder order = mustExist(orderId);
    if (SalOrderStatus.SHIPPED.is(order.getStatus())
            || SalOrderStatus.CLOSED.is(order.getStatus())
            || SalOrderStatus.CANCEL.is(order.getStatus())) {
        throw new ServiceException("已出货/已结单/已取消订单不可取消，关联工单需另行处理");
    }
    return updateStatus(orderId, SalOrderStatus.CANCEL.getCode());
}
```
- deleteSalOrderByOrderIds 门控（:328-331）改为 `if (!SalOrderStatus.CONFIRMED.is(order.getStatus())) throw new ServiceException("订单 " + order.getOrderCode() + " 非已确认状态,不可删除");`
- doToWorkorder（:355）改为：
```java
if (!SalOrderStatus.CONFIRMED.is(order.getStatus())
        && !SalOrderStatus.PRODUCING.is(order.getStatus())) {
    throw new ServiceException("仅已确认/生产中订单可转工单");
}
```
- buildWorkorderFromLine 中 `wo.setStatus("PREPARE")` 改为用 pro 域常量（保持字面量亦可，该串属 pro 域；最小改动保留 "PREPARE" 字面量并加注释 `// ProConstants 工单初态`）。

- [ ] **Step 6: 改 ISalOrderService 与 Controller**

- 接口删 submitOrder/approveOrder/rejectOrder/batchSubmit/batchApprove 五个声明与 Map import（若 Map 无其他用途）。
- Controller 删 submit/batchSubmit/approve/batchApprove/reject 五个端点（:150-189）及 `toIdArray` 私有方法、`java.util.List`/`java.util.Map` import 若不再使用。close 端点 @Log title 改「销售订单结单」。
- `list` 端点签名保持 `list(SalOrder salOrder)`（statusList 随实体绑定，无需动签名）。

- [ ] **Step 7: 改 SalOrderMapper.xml**

- selectSalOrderList 的 status 条件（:51）替换为：
```xml
<if test="status != null and status != ''"> and status = #{status}</if>
<if test="statusList != null and statusList.size() > 0">
    and status in
    <foreach collection="statusList" item="st" open="(" separator="," close=")">#{st}</foreach>
</if>
```
- selectSalOrderAllConvertible（:56-60）where 改为：
```sql
where status in ('CONFIRMED', 'PRODUCING')
```

- [ ] **Step 8: 跑单测转绿 + 全模块编译**

Run:
```bash
cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderServiceImplTest -q
mvn -pl ruoyi-admin -am compile -q
```
Expected: 单测全绿；全仓编译通过（注意 grep 是否还有别处引用被删的 5 个方法：`grep -rn "submitOrder\|approveOrder\|rejectOrder\|batchSubmit\|batchApprove" backend/ruoyi-admin/src backend/ruoyi-system/src/main`，应只剩无关注释/其他域同名方法）。

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat(sal): 删审核流建单即确认，四态门控（结单仅已出货/取消仅确认与生产中/转单两态）"
```

---

### Task 3: 订单生产进度聚合（任务口径，列表批量回填 + 详情回填）

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/sal/vo/SalOrderProgressRow.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/sal/SalOrderMapper.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderMapper.xml`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/ISalOrderService.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderServiceImpl.java`
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/sal/SalOrderController.java`（list 加 includeProgress；detail 回填）
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderServiceImplTest.java`

**Interfaces:**
- Produces: `GET /mes/sal/order/list?includeProgress=true` 每行带 `progressPercent`(0-100)；`GET /mes/sal/order/detail/{id}` 头节点始终带 progressPercent。
- 口径：`LEAST(100, ROUND(SUM(task.quantity_produced)/NULLIF(SUM(task.quantity),0)*100))`，join 链 sal_order_line→workorder(status!=CANCEL)→task(status!=CANCEL)，无任务订单回填 0。

- [ ] **Step 1: 写失败单测**

在 SalOrderServiceImplTest 增加：

```java
@Test
@DisplayName("enrichOrderProgress - 批量回填，无聚合行订单=0，封顶100由SQL保证")
void enrichOrderProgress_batch() {
    SalOrder o1 = buildOrder(1L, "SO1", "PRODUCING");
    SalOrder o2 = buildOrder(2L, "SO2", "CONFIRMED");
    SalOrder o3 = buildOrder(3L, "SO3", "PRODUCING");
    com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow row =
            new com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow();
    row.setOrderId(3L); row.setProgressPercent(42);
    when(salOrderMapper.selectProgressByOrderIds(anyList())).thenReturn(java.util.List.of(row));

    salOrderService.enrichOrderProgress(java.util.List.of(o1, o2, o3));

    assertThat(o1.getProgressPercent()).isZero();
    assertThat(o2.getProgressPercent()).isZero();
    assertThat(o3.getProgressPercent()).isEqualTo(42);
}

@Test
@DisplayName("enrichOrderProgress - 空列表直接返回不查库")
void enrichOrderProgress_empty() {
    salOrderService.enrichOrderProgress(java.util.Collections.emptyList());
    verify(salOrderMapper, never()).selectProgressByOrderIds(anyList());
}

@Test
@DisplayName("getDetail - 头节点带 progressPercent")
void getDetail_withProgress() {
    when(salOrderMapper.selectSalOrderByOrderId(1L)).thenReturn(buildOrder(1L, "SO1", "PRODUCING"));
    when(salOrderLineMapper.selectSalOrderLineByOrderId(1L)).thenReturn(java.util.Collections.emptyList());
    com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow row =
            new com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow();
    row.setOrderId(1L); row.setProgressPercent(88);
    when(salOrderMapper.selectProgressByOrderIds(anyList())).thenReturn(java.util.List.of(row));

    SalOrder d = salOrderService.getDetail(1L);

    assertThat(d.getProgressPercent()).isEqualTo(88);
}
```

- [ ] **Step 2: 跑测试确认红灯**

Run: `cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderServiceImplTest -q`
Expected: 编译失败（SalOrderProgressRow / selectProgressByOrderIds / enrichOrderProgress 不存在）。

- [ ] **Step 3: 新建 VO**

`SalOrderProgressRow.java`：
```java
package com.ruoyi.system.domain.mes.sal.vo;

/** 订单生产进度聚合行（任务口径实时计算，非持久化） */
public class SalOrderProgressRow {
    private Long orderId;
    /** 0-100，SUM(任务已报数)/SUM(任务计划数)，SQL 内 LEAST 封顶 */
    private Integer progressPercent;
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
}
```

- [ ] **Step 4: Mapper 接口 + XML**

接口加：
```java
/**
 * 批量聚合订单生产进度（任务数量口径，排除已取消工单/任务）。
 * 主表 l 的 factory_id 由 FactoryIdInterceptor 注入。
 */
java.util.List<com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow> selectProgressByOrderIds(
        @org.apache.ibatis.annotations.Param("ids") java.util.List<Long> orderIds);
```
XML 加（</mapper> 前）：
```xml
<!-- 订单进度=Σ任务已报工数/Σ任务计划数（排已取消工单/任务）。主表 l.factory_id 由拦截器注入 -->
<select id="selectProgressByOrderIds"
        resultType="com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow">
    SELECT l.order_id AS orderId,
           LEAST(100, ROUND(COALESCE(SUM(t.quantity_produced), 0)
                 / NULLIF(SUM(t.quantity), 0) * 100, 0)) AS progressPercent
    FROM qxx_sal_order_line l
    INNER JOIN qxx_pro_workorder w ON w.sales_order_line_id = l.line_id AND w.factory_id = l.factory_id
                                  AND w.status != 'CANCEL'
    INNER JOIN qxx_pro_task t ON t.workorder_id = w.workorder_id AND t.factory_id = w.factory_id
                             AND t.status != 'CANCEL'
    WHERE l.order_id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    GROUP BY l.order_id
</select>
```

- [ ] **Step 5: Service + Controller**

ISalOrderService 加 `void enrichOrderProgress(List<SalOrder> list);`。
Impl 加：
```java
@Override
public void enrichOrderProgress(List<SalOrder> list) {
    if (list == null || list.isEmpty()) return;
    List<Long> ids = list.stream().map(SalOrder::getOrderId).collect(java.util.stream.Collectors.toList());
    Map<Long, Integer> pmap = new HashMap<>();
    for (com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow r
            : salOrderMapper.selectProgressByOrderIds(ids)) {
        pmap.put(r.getOrderId(), r.getProgressPercent());
    }
    for (SalOrder o : list) {
        o.setProgressPercent(pmap.getOrDefault(o.getOrderId(), 0));
    }
}
```
getDetail 在 `order.setLines(lines);` 前加 `enrichOrderProgress(List.of(order));`。
Controller list 改为（仿 ProWorkorderController:77）：
```java
public TableDataInfo list(SalOrder salOrder,
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "false") boolean includeProgress) {
    startPage();
    List<SalOrder> list = salOrderService.selectSalOrderList(salOrder);
    if (includeProgress) salOrderService.enrichOrderProgress(list);
    return getDataTable(list);
}
```

- [ ] **Step 6: 单测转绿**

Run: `cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderServiceImplTest -q`
Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(sal): 订单生产进度聚合（任务数量口径，列表includeProgress批量回填+详情回填）"
```

---

### Task 4: 领域事件基础设施 + 工单开工推进「生产中」

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/event/mes/WorkorderStartedEvent.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/sal/SalOrderMapper.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderMapper.xml`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProWorkorderServiceImpl.java`（注入 publisher，startProduction 发事件）
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderLifecycleListener.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderLifecycleListenerTest.java`
- Test: 修改 `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProWorkorderServiceUnitTest.java`（开工发事件一例）
- Test: 修改 `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProFeedbackServiceUnitTest.java`（防回归反射断言）

**Interfaces:**
- Produces: `WorkorderStartedEvent(Long workorderId, Long salesOrderLineId, Long factoryId)`；开工事务提交后，订单 CONFIRMED→PRODUCING 的条件 UPDATE `confirmProducing`；非销售来源工单不发事件。

- [ ] **Step 1: 写监听器失败单测**

新建 `SalOrderLifecycleListenerTest.java`：
```java
package com.ruoyi.system.service.mes.sal;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.event.mes.WorkorderStartedEvent;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.sal.impl.SalOrderLifecycleListener;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalOrderLifecycleListenerTest {
    @Mock SalOrderMapper salOrderMapper;
    @Mock SalOrderLineMapper salOrderLineMapper;
    @InjectMocks SalOrderLifecycleListener listener;

    @Test
    void started_confirmed_order_becomes_producing() {
        SalOrderLine line = new SalOrderLine();
        line.setLineId(10L); line.setOrderId(1L);
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        listener.onWorkorderStarted(new WorkorderStartedEvent(99L, 10L, 1L));
        verify(salOrderMapper).confirmProducing(eq(1L), eq(1L), anyString(), any());
    }

    @Test
    void started_without_sales_line_is_ignored() {
        listener.onWorkorderStarted(new WorkorderStartedEvent(99L, null, 1L));
        verifyNoInteractions(salOrderMapper);
    }

    @Test
    void started_unknown_line_is_ignored() {
        when(salOrderLineMapper.selectSalOrderLineByLineId(404L)).thenReturn(null);
        listener.onWorkorderStarted(new WorkorderStartedEvent(99L, 404L, 1L));
        verify(salOrderMapper, never()).confirmProducing(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void listener_annotation_is_after_commit_requires_new() throws Exception {
        Method m = SalOrderLifecycleListener.class.getMethod("onWorkorderStarted", WorkorderStartedEvent.class);
        TransactionalEventListener a = m.getAnnotation(TransactionalEventListener.class);
        org.assertj.core.api.Assertions.assertThat(a.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        org.assertj.core.api.Assertions.assertThat(m.getAnnotation(Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }
}
```

- [ ] **Step 2: 跑测试确认红灯**（类不存在，编译失败）

Run: `cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderLifecycleListenerTest -q`

- [ ] **Step 3: 新建事件类**

```java
package com.ruoyi.system.event.mes;

/**
 * 工单开工事件：pro 域发布，sal 域监听后把销售订单 CONFIRMED→PRODUCING。
 * 非销售订单来源的工单不发布。不可变。
 */
public class WorkorderStartedEvent {
    private final Long workorderId;
    private final Long salesOrderLineId;
    private final Long factoryId;
    public WorkorderStartedEvent(Long workorderId, Long salesOrderLineId, Long factoryId) {
        this.workorderId = workorderId;
        this.salesOrderLineId = salesOrderLineId;
        this.factoryId = factoryId;
    }
    public Long getWorkorderId() { return workorderId; }
    public Long getSalesOrderLineId() { return salesOrderLineId; }
    public Long getFactoryId() { return factoryId; }
}
```

- [ ] **Step 4: Mapper 条件 UPDATE（@SkipFactoryId，factoryId 显式）**

接口加：
```java
/** 仅 CONFIRMED 订单推进 PRODUCING（开工事件）。跳过拦截器：跨表事件链路显式带 factoryId */
@com.ruoyi.common.annotation.SkipFactoryId
int confirmProducing(@org.apache.ibatis.annotations.Param("orderId") Long orderId,
                     @org.apache.ibatis.annotations.Param("factoryId") Long factoryId,
                     @org.apache.ibatis.annotations.Param("updateBy") String updateBy,
                     @org.apache.ibatis.annotations.Param("updateTime") java.util.Date updateTime);
```
XML 加：
```xml
<!-- 开工推进：仅 CONFIRMED→PRODUCING，幂等；factory_id 显式（事件链路 @SkipFactoryId） -->
<update id="confirmProducing">
    update qxx_sal_order
    set status = 'PRODUCING', update_by = #{updateBy}, update_time = #{updateTime}
    where order_id = #{orderId} and factory_id = #{factoryId} and status = 'CONFIRMED'
</update>
```

- [ ] **Step 5: 监听器**

```java
package com.ruoyi.system.service.mes.sal.impl;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.event.mes.WorkorderStartedEvent;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;

/**
 * 销售订单生命周期事件监听：跨域状态推进唯一入口。
 * AFTER_COMMIT：主业务回滚则订单绝不动；REQUIRES_NEW：推进独立提交，失败仅记日志（条件 UPDATE 幂等可补偿）。
 */
@Component
public class SalOrderLifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(SalOrderLifecycleListener.class);

    @Autowired private SalOrderMapper salOrderMapper;
    @Autowired private SalOrderLineMapper salOrderLineMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onWorkorderStarted(WorkorderStartedEvent e) {
        if (e.getSalesOrderLineId() == null) return;
        SalOrderLine line = salOrderLineMapper.selectSalOrderLineByLineId(e.getSalesOrderLineId());
        if (line == null || line.getOrderId() == null) return;
        int rows = salOrderMapper.confirmProducing(line.getOrderId(), e.getFactoryId(), currentUser(), DateUtils.getNowDate());
        log.info("开工事件推进订单生产中: orderId={}, workorderId={}, affected={}", line.getOrderId(), e.getWorkorderId(), rows);
    }

    private String currentUser() {
        try { return SecurityUtils.getUsername(); } catch (Exception ex) { return "system"; }
    }
}
```

- [ ] **Step 6: ProWorkorderServiceImpl 发布事件**

类字段区加：
```java
@org.springframework.beans.factory.annotation.Autowired
private org.springframework.context.ApplicationEventPublisher eventPublisher;
```
startProduction 在 `return rows;` 前插入：
```java
if (wo.getSalesOrderLineId() != null) {
    eventPublisher.publishEvent(new com.ruoyi.system.event.mes.WorkorderStartedEvent(
            workorderId, wo.getSalesOrderLineId(), wo.getFactoryId()));
}
```

- [ ] **Step 7: 开工发布单测一例**

ProWorkorderServiceUnitTest 的 mock 列表（:60-74）加一行（@InjectMocks 会按类型注入 impl 的 eventPublisher 字段）：
```java
@Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
```
在「4. 合法状态转换」用例后新增（复用类内 `testWorkorder` 夹具，setUp 已 mock selectProCardList 跳过建卡）：
```java
@Test
@DisplayName("4b. 销售来源工单开工：发布 WorkorderStartedEvent")
void startProduction_salesOrderWo_publishesEvent() {
    testWorkorder.setStatus("PREPARE");
    testWorkorder.setSalesOrderLineId(7L);
    testWorkorder.setFactoryId(1L);
    when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
    when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);

    workorderService.startProduction(1L);

    verify(eventPublisher).publishEvent(argThat(e -> e instanceof com.ruoyi.system.event.mes.WorkorderStartedEvent
            && ((com.ruoyi.system.event.mes.WorkorderStartedEvent) e).getWorkorderId().equals(1L)
            && ((com.ruoyi.system.event.mes.WorkorderStartedEvent) e).getSalesOrderLineId().equals(7L)
            && ((com.ruoyi.system.event.mes.WorkorderStartedEvent) e).getFactoryId().equals(1L)));
}

@Test
@DisplayName("4c. 非销售来源工单开工：不发事件")
void startProduction_manualWo_noEvent() {
    testWorkorder.setStatus("PREPARE");
    testWorkorder.setSalesOrderLineId(null);
    when(workorderMapper.selectProWorkorderByWorkorderId(1L)).thenReturn(testWorkorder);
    when(workorderMapper.updateProWorkorder(any(ProWorkorder.class))).thenReturn(1);

    workorderService.startProduction(1L);

    verify(eventPublisher, never()).publishEvent(any());
}
```
注意：既有用例 `testStatusTransitionLegal` 跑在同夹具上且未设 salesOrderLineId（默认 null），加事件逻辑后它自然走「不发事件」分支，不需改；若夹具默认带了 salesOrderLineId，则在该用例显式置 null。

- [ ] **Step 8: 报工零 sal 依赖防回归断言**

在 ProFeedbackServiceUnitTest 加：
```java
@Test
@DisplayName("架构防线：报工服务不得依赖任何 sal 域类型（D1 报工不动订单状态）")
void feedbackService_hasNoSalDependency() {
    for (java.lang.reflect.Field f :
            com.ruoyi.system.service.mes.pro.impl.ProFeedbackServiceImpl.class.getDeclaredFields()) {
        String typeName = f.getType().getName();
        org.assertj.core.api.Assertions.assertThat(typeName).doesNotContain(".mes.sal.");
    }
}
```

- [ ] **Step 9: 跑相关单测**

Run:
```bash
cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderLifecycleListenerTest,ProWorkorderServiceUnitTest,ProFeedbackServiceUnitTest -q
```
Expected: PASS。

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat(pro,sal): 工单开工领域事件推进订单CONFIRMED→PRODUCING（AFTER_COMMIT条件UPDATE）"
```

---

### Task 5: 出库发齐事件推进「已出货」（订单维度发齐判定）

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/event/mes/SalesShipmentCompletedEvent.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/sal/SalOrderMapper.java`（+markShippedIfFullyDelivered）
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderMapper.xml`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/impl/WmProductSalesShipmentServiceImpl.java`（发事件）
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/sal/impl/SalOrderLifecycleListener.java`（+onSalesShipped）
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/sal/SalOrderLifecycleListenerTest.java`
- Test: 新建 `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/wm/WmProductSalesShipmentServiceUnitTest.java`

**Interfaces:**
- Produces: `SalesShipmentCompletedEvent(Long salesId, Long salesOrderId, Long factoryId)`；出库单维度发齐（shipStatus=SHIPPED）才发布；监听器订单维度复核（SHIPPED 箱量按订单行汇总 ≥ 订单行数量）后 CONFIRMED/PRODUCING→SHIPPED。

- [ ] **Step 1: 写失败测试（监听器两侧）**

SalOrderLifecycleListenerTest 增加：
```java
@Test
void shipped_event_calls_conditional_update() {
    listener.onSalesShipped(new com.ruoyi.system.event.mes.SalesShipmentCompletedEvent(5L, 1L, 1L));
    verify(salOrderMapper).markShippedIfFullyDelivered(eq(1L), eq(1L), anyString(), any());
}

@Test
void shipped_event_null_order_ignored() {
    listener.onSalesShipped(new com.ruoyi.system.event.mes.SalesShipmentCompletedEvent(5L, null, 1L));
    verify(salOrderMapper, never()).markShippedIfFullyDelivered(anyLong(), anyLong(), anyString(), any());
}
```
新建 `WmProductSalesShipmentServiceUnitTest.java`（完整代码；静态 mock 仿 SalOrderServiceImplTest:69-90，wm 侧锁方法名为 `execute(String,long,Supplier)`，见 impl :85）：
```java
package com.ruoyi.system.service.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.WmProductSalesConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.event.mes.SalesShipmentCompletedEvent;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesBoxMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesShipmentMapper;
import com.ruoyi.system.service.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.impl.WmProductSalesShipmentServiceImpl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WmProductSalesShipmentServiceUnitTest {
    @Mock WmProductSalesShipmentMapper shipmentMapper;
    @Mock WmProductSalesBoxMapper boxMapper;
    @Mock WmProductSalesMapper salesMapper;
    @Mock AutoCodeGenerator autoCodeGenerator;
    @Mock RedisLockTemplate lockTemplate;
    @Mock PlatformTransactionManager transactionManager;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks WmProductSalesShipmentServiceImpl shipmentService;

    private MockedStatic<SecurityUtils> securityMock;
    private MockedStatic<DateUtils> dateMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("tester");
        dateMock = mockStatic(DateUtils.class);
        dateMock.when(DateUtils::getNowDate).thenReturn(new Date());
        // 锁直执行；真实 TransactionTemplate 套 mock manager（getTransaction/commit 默认 no-op）
        when(lockTemplate.execute(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
        ReflectionTestUtils.setField(shipmentService, "txTemplate", new TransactionTemplate(transactionManager));
    }

    @AfterEach
    void tearDown() { securityMock.close(); dateMock.close(); }

    @Test
    @DisplayName("整单发齐：header 转 SHIPPED 且发布 SalesShipmentCompletedEvent")
    void fullShipment_publishesEvent() {
        when(salesMapper.selectWmProductSalesBySalesId(1L)).thenReturn(
                buildHeader(new BigDecimal("100"), BigDecimal.ZERO));
        when(boxMapper.selectWmProductSalesBoxByBoxId(10L)).thenReturn(
                buildBox(10L, WmProductSalesConstants.BOX_STATUS_PACKED, new BigDecimal("100")));

        shipmentService.createShipment(buildRequest());

        verify(eventPublisher).publishEvent(argThat(e -> e instanceof SalesShipmentCompletedEvent
                && ((SalesShipmentCompletedEvent) e).getSalesOrderId().equals(5L)
                && ((SalesShipmentCompletedEvent) e).getSalesId().equals(1L)
                && ((SalesShipmentCompletedEvent) e).getFactoryId().equals(1L)));
        verify(salesMapper).updateWmProductSales(argThat(h ->
                WmProductSalesConstants.STATUS_SHIPPED.equals(h.getStatus())));
    }

    @Test
    @DisplayName("部分发运 60/100：PARTIAL_SHIPPED 且不发事件")
    void partialShipment_noEvent() {
        when(salesMapper.selectWmProductSalesBySalesId(1L)).thenReturn(
                buildHeader(new BigDecimal("100"), BigDecimal.ZERO));
        when(boxMapper.selectWmProductSalesBoxByBoxId(10L)).thenReturn(
                buildBox(10L, WmProductSalesConstants.BOX_STATUS_PACKED, new BigDecimal("60")));

        shipmentService.createShipment(buildRequest());

        verify(eventPublisher, never()).publishEvent(any());
        verify(salesMapper).updateWmProductSales(argThat(h ->
                WmProductSalesConstants.SHIP_STATUS_PARTIAL_SHIPPED.equals(h.getShipStatus())
                        && !WmProductSalesConstants.STATUS_SHIPPED.equals(h.getStatus())));
    }

    /** POSTED+UN_SHIPPED 可发运；total=100，已发 alreadyShipped，挂销售订单 5 */
    private WmProductSales buildHeader(BigDecimal totalQty, BigDecimal alreadyShipped) {
        WmProductSales h = new WmProductSales();
        h.setSalesId(1L); h.setFactoryId(1L); h.setSalesOrderId(5L);
        h.setStatus(WmProductSalesConstants.STATUS_POSTED);
        h.setShipStatus(WmProductSalesConstants.SHIP_STATUS_UN_SHIPPED);
        h.setTotalQuantity(totalQty); h.setPostedQuantity(totalQty); h.setShippedQuantity(alreadyShipped);
        return h;
    }
    private WmProductSalesBox buildBox(Long boxId, String status, BigDecimal qty) {
        WmProductSalesBox b = new WmProductSalesBox();
        b.setBoxId(boxId); b.setSalesId(1L); b.setBoxNo("B-" + boxId);
        b.setStatus(status); b.setQuantity(qty);
        return b;
    }
    private WmProductSalesShipment buildRequest() {
        WmProductSalesShipment e = new WmProductSalesShipment();
        e.setSalesId(1L); e.setShipmentCode("SHIP-UT-1");
        WmProductSalesBox ref = new WmProductSalesBox(); ref.setBoxId(10L);
        e.setBoxes(List.of(ref));
        return e;
    }
}
```

- [ ] **Step 2: 跑测试确认红灯**

Run: `cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderLifecycleListenerTest,WmProductSalesShipmentServiceUnitTest -q`

- [ ] **Step 3: 事件类（仿 WorkorderStartedEvent）**

```java
package com.ruoyi.system.event.mes;

/** 出库单维度全部发齐事件：wm 域发布，sal 域订单维度复核后 CONFIRMED/PRODUCING→SHIPPED。非订单来源出库单不发布。 */
public class SalesShipmentCompletedEvent {
    private final Long salesId;
    private final Long salesOrderId;
    private final Long factoryId;
    public SalesShipmentCompletedEvent(Long salesId, Long salesOrderId, Long factoryId) {
        this.salesId = salesId; this.salesOrderId = salesOrderId; this.factoryId = factoryId;
    }
    public Long getSalesId() { return salesId; }
    public Long getSalesOrderId() { return salesOrderId; }
    public Long getFactoryId() { return factoryId; }
}
```

- [ ] **Step 4: Mapper 发齐条件 UPDATE**

接口加：
```java
/**
 * 订单全部行已发齐（SHIPPED 箱量按订单行汇总 ≥ 行数量）且状态为 CONFIRMED/PRODUCING 时置 SHIPPED。
 * 返回 0=未发齐或状态不符。@SkipFactoryId + 显式 factory_id（事件链路）。
 */
@com.ruoyi.common.annotation.SkipFactoryId
int markShippedIfFullyDelivered(@org.apache.ibatis.annotations.Param("orderId") Long orderId,
                                @org.apache.ibatis.annotations.Param("factoryId") Long factoryId,
                                @org.apache.ibatis.annotations.Param("updateBy") String updateBy,
                                @org.apache.ibatis.annotations.Param("updateTime") java.util.Date updateTime);
```
XML 加：
```xml
<!-- 发齐推进：不存在「订单行数量 > 该行在非作废出库单下 SHIPPED 箱量合计」的行 -->
<update id="markShippedIfFullyDelivered">
    update qxx_sal_order o
    set o.status = 'SHIPPED', o.update_by = #{updateBy}, o.update_time = #{updateTime}
    where o.order_id = #{orderId}
      and o.factory_id = #{factoryId}
      and o.status in ('CONFIRMED', 'PRODUCING')
      and not exists (
          select 1 from qxx_sal_order_line l
          left join (
              select sl.sales_order_line_id as sol_id, sum(b.quantity) as shipped_qty
              from qxx_wm_product_sales s
              inner join qxx_wm_product_sales_line sl on sl.sales_id = s.sales_id and sl.factory_id = s.factory_id
              inner join qxx_wm_product_sales_box b on b.line_id = sl.line_id and b.factory_id = sl.factory_id
                                                   and b.status = 'SHIPPED'
              where s.sales_order_id = #{orderId}
                and s.factory_id = #{factoryId}
                and s.status &lt;&gt; 'CANCELED'
              group by sl.sales_order_line_id
          ) x on x.sol_id = l.line_id
          where l.order_id = #{orderId}
            and l.factory_id = #{factoryId}
            and l.quantity &gt; coalesce(x.shipped_qty, 0)
      )
</update>
```

- [ ] **Step 5: wm 发布点**

WmProductSalesShipmentServiceImpl 加字段：
```java
@org.springframework.beans.factory.annotation.Autowired
private org.springframework.context.ApplicationEventPublisher eventPublisher;
```
`updateHeaderAfterShip` 的 SHIPPED 分支（:189-191）改为：
```java
if (totalNeed.signum() > 0 && totalShipped.compareTo(totalNeed) >= 0) {
    newShipStatus = WmProductSalesConstants.SHIP_STATUS_SHIPPED;
    header.setStatus(WmProductSalesConstants.STATUS_SHIPPED);
    if (header.getSalesOrderId() != null) {
        eventPublisher.publishEvent(new com.ruoyi.system.event.mes.SalesShipmentCompletedEvent(
                header.getSalesId(), header.getSalesOrderId(), header.getFactoryId()));
    }
}
```

- [ ] **Step 6: 监听器加方法**

SalOrderLifecycleListener 加：
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onSalesShipped(com.ruoyi.system.event.mes.SalesShipmentCompletedEvent e) {
    if (e.getSalesOrderId() == null) return;
    int rows = salOrderMapper.markShippedIfFullyDelivered(
            e.getSalesOrderId(), e.getFactoryId(), currentUser(), DateUtils.getNowDate());
    log.info("发运事件推进订单已出货: orderId={}, salesId={}, affected={}",
            e.getSalesOrderId(), e.getSalesId(), rows);
}
```

- [ ] **Step 7: 跑单测**

Run: `cd backend && mvn -pl ruoyi-system test -Dtest=SalOrderLifecycleListenerTest,WmProductSalesShipmentServiceUnitTest -q`
Expected: PASS。

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(wm,sal): 出库发齐领域事件+订单维度箱量复核推进SHIPPED（部分发货不动）"
```

---

### Task 6: 集成测试——四态主链路 + D1 进度递增

**Files:**
- Modify: `backend/ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/sal/SalOrderIT.java`（重写）

**Interfaces:**
- Consumes: Task 2-5 全部接口/事件/进度聚合。

- [ ] **Step 1: 重写 SalOrderIT**

保留 BaseIntegrationTest 基类用法（`restTemplate`/`jdbcTemplate`/`authRequest()`/`truncateTables`）。

**两个测试线程陷阱（已核实，必须照做）：**
1. 直接调 `proWorkorderService.startProduction(...)` 时内部 `SecurityUtils.getUsername()` 依赖线程级登录态——必须仿 MaterialTraceE2ETest:67-75 在 `@BeforeAll` 里塞 `SecurityContextHolder`（admin/factoryId=1）。
2. `@TransactionalEventListener(AFTER_COMMIT)` 在无事务上下文时**根本不投递**。测试线程发事件必须包在真实 `TransactionTemplate` 里，事务提交后监听器才以 REQUIRES_NEW 同步执行（事件发布本身的 verify 由 Task 4/5 单测负责，IT 验证的是真实 Spring 投递 + 条件 UPDATE SQL）。

`@BeforeEach` 清理表（流转卡表也要清，开工自动建卡会写）：
```java
truncateTables("qxx_pro_card_process", "qxx_pro_card", "qxx_pro_task",
        "qxx_wm_product_sales_box", "qxx_wm_product_sales_line", "qxx_wm_product_sales",
        "qxx_pro_workorder", "qxx_sal_order_line", "qxx_sal_order");
```
类级常量与注入：
```java
private static final Long FACTORY = 1L;

@Autowired private com.ruoyi.system.service.mes.pro.IProWorkorderService proWorkorderService;
@Autowired private org.springframework.context.ApplicationEventPublisher publisher;
@Autowired private com.ruoyi.system.service.mes.sal.impl.SalOrderLifecycleListener listener;
@Autowired private org.springframework.transaction.PlatformTransactionManager txManager;

@BeforeAll
void setupAuth() {
    com.ruoyi.common.core.domain.entity.SysUser user = new com.ruoyi.common.core.domain.entity.SysUser();
    user.setUserId(1L); user.setUserName("admin"); user.setFactoryId(FACTORY);
    com.ruoyi.common.core.domain.model.LoginUser loginUser =
            new com.ruoyi.common.core.domain.model.LoginUser(user, java.util.Collections.emptySet());
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities()));
}
```
（若 SysUser 实际包名是 `com.ruoyi.common.core.domain.entity.SysUser` 编译不过，以 MaterialTraceE2ETest:4-5 的 import 为准。）

辅助方法（改写现有 createConfirmedOrder）：建单请求里 **不再传 status**、不再调 submit/approve；建完断言 DB `status='CONFIRMED'` 且 `factory_id=1`，再走 toWorkorder HTTP。toWorkorder 后用 jdbc 查 workorderId（`select workorder_id from qxx_pro_workorder where workorder_code=?`）。

写下列 @Test（一个主链路 + 两个门控）：

```java
@Test
@DisplayName("四态主链路：建单CONFIRMED→开工PRODUCING→连续报工进度递增状态不跳→发齐SHIPPED→结单CLOSED")
void four_status_main_flow() {
    Long orderId = createOrderAndWorkorder("SO-IT-M1", "WO-IT-M1", new BigDecimal("100"));
    Long lineId = getFirstLineId(orderId);
    Long woId = jdbcTemplate.queryForObject(
            "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M1'", Long.class);

    assertThat(queryStatus(orderId)).isEqualTo("CONFIRMED");

    // 开工 + 事件投递包在同一真实事务里：提交后 AFTER_COMMIT 监听器同步执行条件 UPDATE
    startWorkorderAndPublish(woId, lineId);
    assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

    // 模拟两道工序连续审核报工后的任务产量累加（audit 不写订单状态由 Task4 反射防线单测固化）
    seedTask(woId, "T1", new BigDecimal("100"));
    assertProgressAndProducing(orderId, 0);          // 还没报工：0/200
    jdbcTemplate.update("update qxx_pro_task set quantity_produced=50 where task_code='T1'");
    assertProgressAndProducing(orderId, 25);         // 50/200
    jdbcTemplate.update("update qxx_pro_task set quantity_produced=100 where task_code='T1'");
    assertProgressAndProducing(orderId, 50);
    seedTask(woId, "T2", new BigDecimal("100"));
    assertProgressAndProducing(orderId, 50);         // T2 已存在但产量0，分母变大结果不变
    jdbcTemplate.update("update qxx_pro_task set quantity_produced=100 where task_code='T2'");
    assertProgressAndProducing(orderId, 100);        // 全部报满：状态仍 PRODUCING，进度100

    // 部分发货 60/100：即使收到发运事件，订单维度 NOT EXISTS 复核不通过，仍 PRODUCING
    seedShippedBoxes(orderId, lineId, "WS-IT-1", new BigDecimal("60"));
    publishShipmentEventInTx(jdbcTemplate.queryForObject(
            "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-1'", Long.class));
    assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

    // 第二张出库单发剩余 40：累计 100 发齐 → SHIPPED
    seedShippedBoxes(orderId, lineId, "WS-IT-2", new BigDecimal("40"));
    publishShipmentEventInTx(jdbcTemplate.queryForObject(
            "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-2'", Long.class));
    assertThat(queryStatus(orderId)).isEqualTo("SHIPPED");

    ResponseEntity<Map> r = restTemplate.exchange(baseUrl() + "/close/" + orderId,
            HttpMethod.PUT, authRequest(), Map.class);
    assertThat(r.getBody().get("code")).isEqualTo(200);
    assertThat(queryStatus(orderId)).isEqualTo("CLOSED");
}

@Test
@DisplayName("PRODUCING 途中取消→CANCEL；重复开工事件状态不变（条件UPDATE幂等）")
void cancel_during_producing_then_event_noop() {
    Long orderId = createOrderAndWorkorder("SO-IT-M2", "WO-IT-M2", new BigDecimal("100"));
    Long lineId = getFirstLineId(orderId);
    Long woId = jdbcTemplate.queryForObject(
            "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M2'", Long.class);
    startWorkorderAndPublish(woId, lineId);
    assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");
    restTemplate.exchange(baseUrl() + "/cancel/" + orderId, HttpMethod.PUT, authRequest(), Map.class);
    assertThat(queryStatus(orderId)).isEqualTo("CANCEL");
    // 直接调监听方法模拟重复事件（REQUIRES_NEW 自行开事务）：WHERE status='CONFIRMED' 不命中
    listener.onWorkorderStarted(new com.ruoyi.system.event.mes.WorkorderStartedEvent(woId, lineId, FACTORY));
    assertThat(queryStatus(orderId)).isEqualTo("CANCEL");
}

@Test
@DisplayName("SHIPPED 前不可结单；PRODUCING 不可删（门控 HTTP 500）")
void gates_rejected() {
    Long orderId = createOrderAndWorkorder("SO-IT-M3", "WO-IT-M3", new BigDecimal("100"));
    Long woId = jdbcTemplate.queryForObject(
            "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M3'", Long.class);
    startWorkorderAndPublish(woId, getFirstLineId(orderId));
    ResponseEntity<Map> close = restTemplate.exchange(baseUrl() + "/close/" + orderId,
            HttpMethod.PUT, authRequest(), Map.class);
    assertThat(close.getBody().get("code")).isEqualTo(500);
    ResponseEntity<Map> del = restTemplate.exchange(baseUrl() + "/" + orderId,
            HttpMethod.DELETE, authRequest(), Map.class);
    assertThat(del.getBody().get("code")).isEqualTo(500);
}
```

辅助方法（完整实现，不得留空）：
```java
private String queryStatus(Long orderId) {
    return jdbcTemplate.queryForObject("select status from qxx_sal_order where order_id=?", String.class, orderId);
}

/** 真实事务内开工并发事件；提交后 AFTER_COMMIT 监听器同步推进 CONFIRMED→PRODUCING */
private void startWorkorderAndPublish(Long woId, Long lineId) {
    new org.springframework.transaction.support.TransactionTemplate(txManager).executeWithoutResult(s -> {
        proWorkorderService.startProduction(woId);
        publisher.publishEvent(new com.ruoyi.system.event.mes.WorkorderStartedEvent(woId, lineId, FACTORY));
    });
}

/** 发运事件同样要在事务内发布，AFTER_COMMIT 才会投递 */
private void publishShipmentEventInTx(Long salesId) {
    Long orderId = jdbcTemplate.queryForObject(
            "select sales_order_id from qxx_wm_product_sales where sales_id=?", Long.class, salesId);
    new org.springframework.transaction.support.TransactionTemplate(txManager).executeWithoutResult(s ->
            publisher.publishEvent(
                    new com.ruoyi.system.event.mes.SalesShipmentCompletedEvent(salesId, orderId, FACTORY)));
}

/** includeProgress=true 查列表，断言该订单状态恒 PRODUCING 且进度符合预期 */
private void assertProgressAndProducing(Long orderId, int expectedPercent) {
    ResponseEntity<Map> resp = restTemplate.exchange(
            baseUrl() + "/list?pageNum=1&pageSize=100&includeProgress=true", HttpMethod.GET, authRequest(), Map.class);
    @SuppressWarnings("unchecked")
    java.util.List<java.util.Map<String, Object>> rows =
            (java.util.List<java.util.Map<String, Object>>) resp.getBody().get("rows");
    java.util.Map<String, Object> mine = rows.stream()
            .filter(x -> orderId.equals(((Number) x.get("orderId")).longValue())).findFirst().orElseThrow();
    assertThat(mine.get("status")).isEqualTo("PRODUCING");
    assertThat(((Number) mine.get("progressPercent")).intValue()).isEqualTo(expectedPercent);
}

/**
 * 直插任务（绕开拦截器，显式 factory_id）。qxx_pro_task 这些列 NOT NULL：
 * task_name/workorder_code/workorder_name/workstation_*/route_id/process_id/item_*/unit_of_measure
 */
private void seedTask(Long woId, String code, BigDecimal qty) {
    Map<String, Object> wo = jdbcTemplate.queryForMap(
            "select workorder_code, workorder_name, product_id, product_code, product_name, unit_of_measure "
            + "from qxx_pro_workorder where workorder_id=?", woId);
    jdbcTemplate.update(
        "insert into qxx_pro_task (factory_id, task_code, task_name, workorder_id, workorder_code, workorder_name, "
        + "workstation_id, workstation_code, workstation_name, route_id, process_id, process_code, process_name, "
        + "item_id, item_code, item_name, unit_of_measure, quantity, quantity_produced, status, create_by, create_time) "
        + "values (1, ?, ?, ?, ?, ?, 1, 'WS-IT', 'IT工位', 1, 1, 'P1', '工序', ?, ?, ?, ?, ?, 0, 'PRODUCING', 'admin', NOW())",
        code, "任务-" + code, woId, wo.get("workorder_code"), wo.get("workorder_name"),
        wo.get("product_id"), wo.get("product_code"), wo.get("product_name"),
        wo.get("unit_of_measure") == null ? "PCS" : wo.get("unit_of_measure"), qty);
}

/** 建一张非作废出库单（头+行+SHIPPED箱）。头 client_id/warehouse_id、行 item_* /unit_* 均 NOT NULL */
private void seedShippedBoxes(Long orderId, Long lineId, String salesCode, BigDecimal boxQty) {
    jdbcTemplate.update("insert into qxx_wm_product_sales (factory_id, sales_code, sales_name, client_id, "
            + "warehouse_id, sales_order_id, total_quantity, shipped_quantity, ship_status, status, create_by, create_time) "
            + "values (1, ?, 'IT出库', 1, 1, ?, ?, ?, 'SHIPPED', 'SHIPPED', 'admin', NOW())",
            salesCode, orderId, boxQty, boxQty);
    Long salesId = jdbcTemplate.queryForObject(
            "select sales_id from qxx_wm_product_sales where sales_code=?", Long.class, salesCode);
    jdbcTemplate.update("insert into qxx_wm_product_sales_line (factory_id, sales_id, sales_order_line_id, "
            + "item_id, item_code, item_name, unit_of_measure, unit_name, quantity_sales, create_by, create_time) "
            + "values (1, ?, ?, 1, 'P001', '产品', 'PCS', '个', ?, 'admin', NOW())",
            salesId, lineId, boxQty);
    Long slId = jdbcTemplate.queryForObject(
            "select line_id from qxx_wm_product_sales_line where sales_id=?", Long.class, salesId);
    jdbcTemplate.update("insert into qxx_wm_product_sales_box (factory_id, sales_id, line_id, box_no, quantity, "
            + "status, create_by, create_time) values (1, ?, ?, ?, ?, 'SHIPPED', 'admin', NOW())",
            salesId, slId, salesCode + "-B1", boxQty);
}
```
`createOrderAndWorkorder` 由旧 `createConfirmedOrder` 改：去掉 status PREPARE 入参与 submit/approve 两次 HTTP 调用，建单后从 DB 断言 `status='CONFIRMED'` 与 `factory_id=1`，其余（建单行/toWorkorder HTTP）保留。
旧用例 `create_confirm_toWorkorder`：去掉 submit/approve 步骤与注释，建单后直接断言 CONFIRMED，其余断言保留；`toWorkorder_overConvertible_rejected` 中 helper 已无审核调用，保持断言语义。

注意：`startProduction` 内自动建卡已 try-catch 不抛错（ProWorkorderServiceImpl:749-754），缺卡片不影响测试；dispatch 无任务时是空操作，任务由本测试自行 seed。

- [ ] **Step 2: 跑集成测试（需 Docker）**

Run: `cd backend && mvn -pl ruoyi-admin -am verify -Dtest=SalOrderIT -q`
Expected: 3 个测试全 PASS。若发齐 SQL 口径断言失败，先打印实际箱量 join 结果排查，不得放宽断言。

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(sal): 四态主链路集成测试（开工/连续报工进度递增状态不跳/部分发货/发齐/结单/门控）"
```

---

### Task 7: 前端——API/类型/列表页/详情页/出库选单 + 组件测试

**Files:**
- Modify: `frontend/src/api/mes/sal/order.ts`
- Modify: `frontend/src/types/api/mes/sal/order.ts`
- Modify: `frontend/src/views/mes/sal/order/index.vue`
- Modify: `frontend/src/views/mes/sal/order/detail.vue`
- Modify: `frontend/src/views/mes/wm/product_sales/components/SaleOrderSelect.vue`
- Test: `frontend/src/views/mes/sal/order/__tests__/index.spec.ts`

- [ ] **Step 1: API 与类型**

`order.ts` 删除 submitOrder/approveOrder/rejectOrder/batchSubmitOrder/batchApproveOrder 五个导出。
`types/api/mes/sal/order.ts`：
```ts
/** 销售订单状态（四态主线 + CANCEL 链外） */
export type SalOrderStatus = 'CONFIRMED' | 'PRODUCING' | 'SHIPPED' | 'CLOSED' | 'CANCEL'
```
SalOrderQueryParams 的 `status?: string` 下加 `statusList?: SalOrderStatus[]`；SalOrder 的 `status?: string` 改 `status?: SalOrderStatus`，加 `progressPercent?: number`，approveBy/Time/Remark 加注释 `/** @deprecated 审核流已废弃，历史数据 */`。

- [ ] **Step 2: index.vue 模板与脚本改造**

- import 行（:275）删 submitOrder, approveOrder, rejectOrder, batchSubmitOrder, batchApproveOrder。
- 删顶部两个批量按钮（:36-37）；删行按钮 :59-61（提交审核/审核通过/驳回）；删驳回弹窗整块（:256-267）；data 删 rejectOpen/rejectTargetId/rejectTargetCode/rejectRemark；methods 删 handleSubmit/handleApprove/handleReject/confirmReject/handleBatchSubmit/handleBatchApprove/reportBatch。
- 行按钮区（:56-65 之间）：**保留 `查看` 按钮（:57）和末尾的 `导出` el-dropdown（:66-75，整块不动）**，只把中间 8 个按钮替换为下面 5 个（替换范围 = 现有「改/提交审核/审核通过/驳回/生成工单/关闭/取消/删除」八行）：
```html
<el-button v-if="scope.row.status==='CONFIRMED'" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:sal:order:edit']">改</el-button>
<el-button v-if="scope.row.status==='CONFIRMED' || scope.row.status==='PRODUCING'" link type="warning" size="small" @click="handleToWorkorder(scope.row)" v-hasPermi="['mes:sal:order:workorder']">生成工单</el-button>
<el-button v-if="scope.row.status==='SHIPPED'" link type="success" size="small" @click="handleClose(scope.row)" v-hasPermi="['mes:sal:order:edit']">结单</el-button>
<el-button v-if="scope.row.status==='CONFIRMED' || scope.row.status==='PRODUCING'" link type="danger" size="small" @click="handleCancel(scope.row)" v-hasPermi="['mes:sal:order:edit']">取消</el-button>
<el-button v-if="scope.row.status==='CONFIRMED'" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sal:order:remove']"></el-button>
```
- 状态列前新增进度列（:54 后）：
```html
<el-table-column label="生产进度" align="center" width="150">
  <template #default="s">
    <el-progress :percentage="Number(s.row.progressPercent || 0)" :stroke-width="10"
      :status="s.row.status==='CLOSED' || s.row.status==='CANCEL' ? 'exception' : (Number(s.row.progressPercent || 0) >= 100 ? 'success' : '')" />
  </template>
</el-table-column>
```
- getList 改为请求带 includeProgress：`listOrder({ ...this.queryParams, includeProgress: true })`。
- reset()（:354）status 初值改 'CONFIRMED'。
- canEditSelected（:524）改 `r.status === 'CONFIRMED'`。
- `statusMeta`（:340）与查询区状态下拉均由 `mes_sal_order_status` 字典驱动，V151 改字典后自动生效，**不动代码**；组件挂载处 `getDicts('mes_sal_order_status')` 保留。
- handleClose 文案改 `确认结单 "${row.orderCode}"？结单后不可恢复。`、成功提示「结单成功」。
- handleCancel 成功提示追加：`this.$modal.msgSuccess('取消成功，关联工单需另行处理')`。

- [ ] **Step 3: detail.vue 改造**

- 删驳回 alert（:17-19）；删整张「审核信息」el-card（:43-57）；删 approveHint 计算属性。
- statusTagType map（:115）改为：
```ts
const map: Record<string, string> = { CONFIRMED: 'success', PRODUCING: 'warning', SHIPPED: 'primary', CLOSED: 'info', CANCEL: 'danger' }
```
- 顶部状态 tag 后加进度条（:8 el-tag 后）：
```html
<el-progress v-if="order.orderCode" :percentage="Number(order.progressPercent || 0)" :stroke-width="12" style="width:220px;display:inline-flex;vertical-align:middle;margin-left:12px" />
```

- [ ] **Step 4: SaleOrderSelect 改两态查询 + 状态字典翻译**

- :19 状态列改为：
```html
<el-table-column label="状态" prop="status" width="80" align="center">
  <template #default="s">
    <el-tag size="small" :type="statusType(s.row.status)">{{ statusText(s.row.status) }}</el-tag>
  </template>
</el-table-column>
```
- script 加：
```ts
import { getDicts } from '@/api/system/dict/data'
const statusOptions = ref<any[]>([])
getDicts('mes_sal_order_status').then(r => { statusOptions.value = r.data || [] })
function statusText(s: string) { const d = statusOptions.value.find(x => x.dictValue === s); return d ? d.dictLabel : s }
function statusType(s: string) { const d = statusOptions.value.find(x => x.dictValue === s); return d?.listClass || '' }
```
- query 初值改 `{ pageNum: 1, pageSize: 10 }`，load() 用 URLSearchParams 传重复 key（Spring 绑定 List<String>）：
```ts
const params = new URLSearchParams()
params.append('pageNum', String(query.pageNum)); params.append('pageSize', String(query.pageSize))
params.append('includeProgress', 'false')
params.append('statusList', 'CONFIRMED'); params.append('statusList', 'PRODUCING')
if (query.orderCode) params.append('orderCode', query.orderCode)
if (query.clientName) params.append('clientName', query.clientName)
request.get('/mes/sal/order/list', { params })
```
需 `import request from '@/utils/request'`（替换 listOrder 导入；保持返回 r.rows/r.total 结构）。

- [ ] **Step 5: 重写组件测试 index.spec.ts**

保留现有 mock 骨架（vi.mock api、stubs），但 mock 导出对齐新 API（删除 submit/approve/reject/batch* 的 mock，不存在的导出不再引用）。把失效的「状态文本/标签」用例替换为：
```ts
it('生产中订单显示进度百分比且不渲染审核按钮', async () => {
  mockListOrder.mockResolvedValue({ rows: [
    { orderId: 1, orderCode: 'SO001', orderName: 'x', clientName: 'c', status: 'PRODUCING', progressPercent: 40 }
  ], total: 1 })
  const wrapper = mount(SalOrder, { global: globalStubs })
  await nextTick(); await nextTick()
  const html = wrapper.html()
  expect(html).toContain('40')              // 进度
  expect(html).not.toContain('提交审核')
  expect(html).not.toContain('审核通过')
  expect(html).toContain('生成工单')        // PRODUCING 可追加转单
})

it('已出货订单显示结单按钮，不显示取消', async () => {
  mockListOrder.mockResolvedValue({ rows: [
    { orderId: 2, orderCode: 'SO002', orderName: 'x', clientName: 'c', status: 'SHIPPED', progressPercent: 100 }
  ], total: 1 })
  const wrapper = mount(SalOrder, { global: globalStubs })
  await nextTick(); await nextTick()
  expect(wrapper.html()).toContain('结单')
  expect(wrapper.html()).not.toContain('取消')
})

it('getList 请求带 includeProgress=true', async () => {
  mockListOrder.mockResolvedValue({ rows: [], total: 0 })
  mount(SalOrder, { global: globalStubs })
  await nextTick()
  expect(mockListOrder.mock.calls[0][0]).toMatchObject({ includeProgress: true })
})
```
getDicts mock：现有未 mock `@/api/system/dict/data`，在文件 mock 区加 `vi.mock('@/api/system/dict/data', () => ({ getDicts: vi.fn().mockResolvedValue({ data: [] }) }))`，`@/api/system/user` 若因 listUser 报错同样 mock（listUser: vi.fn().mockResolvedValue({ rows: [] })）。el-progress 若未全局注册导致告警，在 stubs 加 `'el-progress': true`。

- [ ] **Step 6: 跑前端测试与类型检查**

Run:
```bash
cd frontend && npx vitest run src/views/mes/sal/order
npx vue-tsc --noEmit -p tsconfig.json 2>/dev/null | grep -E "sal/order|SaleOrderSelect" || echo "sal 相关无类型错误"
```
Expected: 组件测试 PASS；sal 相关无类型错误（仓库既有的全局历史错误不在本任务范围，不处理）。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(sal-fe): 列表进度列+四态按钮/详情去审核信息/出库选单两态，删审核API与类型收紧"
```

---

### Task 8: 全量回归、打包重启实测红线

**Files:**
- 无需改导出器：已核实 `SalOrderPdfExporter:168-172` 与 `SalOrderDetailExcelExporter:136-140` 的 statusText 都走 `SalOrderStatus.fromCode(s).getInfo()`，枚举改文案后自动生效；Task 2 重写枚举时确认新五态 getInfo 文案即为导出文案。

- [ ] **Step 1: 同步修订 spec 细化点**

编辑 `docs/superpowers/specs/2026-09-10-sal-order-four-status-design.md` 4.3：聚合表由发运明细 detail 改为「`wm_product_sales_box` 中 status='SHIPPED' 的箱 quantity 按订单行汇总」，注明与 header shipped_quantity 同口径。8.2 第 4 步补一句测试分层（反射防线 + IT 任务累加 + 手工实测真实报工）。

- [ ] **Step 2: 后端全量测试**

Run:
```bash
cd backend && mvn -pl ruoyi-system test -q
mvn -pl ruoyi-admin -am verify -Dtest=SalOrderIT -q
```
Expected: 全部 PASS（Docker 需运行；若有其他历史测试因删端点编译失败——grep 修复测试中对 submit/approve 的引用）。

- [ ] **Step 3: 重新打包 + 重启 + D1 真实链路实测（AGENTS.md 红线）**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# kill 旧 ruoyi-admin 进程后
nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
# 等待 curl -s http://localhost:8081/captchaImage 返回 200
```
取 token：`TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)`。用真实接口走一遍（可用 curl 或前端页面）：
1. 新建销售订单（2 行产品，数量 100）→ GET detail，`status=CONFIRMED`、`progressPercent=0`；
2. 转工单（选多工序路线）→ 状态仍 CONFIRMED；
3. 工单开工 → 刷新订单列表，状态 `PRODUCING`；
4. 移动端/后端对**第 1 道工序连续审核报工 2 次、再第 2 道工序报工**，每次后 `GET /mes/sal/order/list?includeProgress=true`：**status 恒为 PRODUCING，progressPercent 严格递增**（这是 D1 验收，截图留存）；
5. 建销售出库单、过账、装箱、发运部分数量 → 订单仍 PRODUCING；剩余发齐 → `SHIPPED`；
6. 结单 → `CLOSED`；对一张 PRODUCING 单点取消 → `CANCEL` 且提示关联工单需另行处理；
7. 验证旧按钮 404：`PUT /mes/sal/order/approve/{id}` 返回 404/405；
8. 导出 PDF/Excel 各一次，状态列文案为四态中文。

- [ ] **Step 4: 前端浏览器实测**

`cd frontend && npm run dev`（若未启动），浏览器实际操作：订单列表进度条渲染与随报工增长、四态按钮显隐、出库选单只出现 CONFIRMED/PRODUCING、详情页无审核卡、新增订单保存后直接已确认。

- [ ] **Step 5: 最终提交**

```bash
git add -A
git commit -m "chore(sal): spec 口径细化（SHIPPED箱量）；全量回归与实测通过"
```

---

## 自检对照（spec → tasks）

| spec 条目 | 覆盖任务 |
|---|---|
| §3 状态机四态 + CANCEL 门控 | T1（字典/存量）、T2（枚举/门控） |
| §4 领域事件（开工/发齐、AFTER_COMMIT、条件 UPDATE） | T4、T5 |
| §4.3 订单维度发齐 NOT EXISTS | T5（箱量口径，已细化） |
| §5 V151 迁移全部 5 项 | T1 |
| §6.1-6.2 枚举/删审核端点/门控/convertible | T2 |
| §6.3 进度聚合 + includeProgress + 详情 | T3 |
| §6.4 三处导出文案 | 实体 @Excel 在 T2；PDF/Excel 走枚举 getInfo 自动生效，无需改（T8 实测验证） |
| §7 前端全部点（index/detail/SaleOrderSelect/api/types/组件测试） | T7 |
| §8.1 单测（门控/监听器/报工零依赖/进度回填） | T2/T3/T4/T5 |
| §8.2 集成测试主链路 | T6 |
| §8.3 实测红线（连续报工状态不跳进度递增） | T8 |
| app/报表不动、approve 列保留 | 全程（无任务触碰） |
