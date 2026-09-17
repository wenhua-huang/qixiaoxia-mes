# 上下工序数据自动带出 + 质检不合格硬拦 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 跟单报工自动带工单/工序/机台/报工人与「上机数量」默认值并留修改痕迹；跟单检验不合格时硬拦下一工序报工，仅超管（或授 `mes:pro:task:release` 的角色）可填理由放行并留痕。

**Architecture:** 纯增量改造，不动既有状态机。①任务表加派工人/负责人快照列，甘特派工时配置；②报工表加 `quantity_input` 上机数量列，后端按「上工序累计已审报工数 − 本工序累计上机数」计算默认值，人工改动写 `qxx_pro_feedback_change`；③不加 BLOCKED 任务状态，阻塞态由 IPQC 判定结果 + 放行记录**实时派生**，在报工提交唯一 service 入口 `doInsertProFeedback` 硬门控（与领料门控同构），三个查询入口批量富化 `qcBlocked/blockReason`；④放行写 `qxx_qc_block_release` 并关闭拦截待办，FAIL 时按下游任务负责人生成待办。

**Tech Stack:** Spring Boot（com.ruoyi 包，若依框架）+ MyBatis + MySQL 8（Flyway，下一版本 **V150**，当前最大 V149，V145 缺号勿补）+ Redis(Redisson)；PC: Vue3 + Element Plus；App: uni-app + Vue3（options API）。JUnit5 + Mockito 单测（禁连库）。

## Global Constraints

- 只改本 worktree 项目文件；编辑 Vue/XML 前先 Read 上下文确认标签/`<if>` 嵌套。
- 新业务表/列必须带 `factory_id`；新写业务 SQL 的 WHERE 带 `<if test="factoryId != null"> and factory_id = #{factoryId}</if>`（拦截器注入值）；**Flyway 内 INSERT 业务表必须显式写 `factory_id`**（裸 JDBC）；业务代码 INSERT **不写** factory_id。
- Flyway 文件放 `backend/ruoyi-admin/src/main/resources/db/migration/`，命名 `V150__xxx.sql`…，DML 必须幂等（NOT EXISTS），已执行文件禁止修改。
- 锁规范：库存/状态变更用 Redisson **先锁后事务**（`RedisLockTemplate`），禁止在加锁方法上标 `@Transactional`。
- 后端函数 ≤50 行，前端组件 ≤300 行（report.vue 已 688 行，本次把新增区块抽子组件，不继续膨胀）；重复逻辑抽公共；状态用枚举/常量；魔法数字必须常量化。
- 后端拥有数据逻辑：默认值、阻塞态全部后端算，前端只展示；不改 API 既有字段语义。
- 后端验证红线：改 Java 后必须 `mvn -pl ruoyi-admin -am package -DskipTests` → kill 旧进程 → `nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &` → 等 `curl -s http://localhost:8081/captchaImage` 返回 200 → token 实测接口。
- 每个任务结束提交一次，提交信息中文，格式 `feat(pro): …` / `feat(qc): …`。
- 已锁定决策：①任务加报工人+负责人 6 列；②默认值取上工序 **AUDITED** 报工；③口径=上工序累计已审报工数 − 本工序累计上机数；④App+PC 统一 service 层硬拦，外协上游自动覆盖；⑤一期无异常单，FAIL 只生成待办；⑥放行权限默认仅超管（F 型菜单不授角色，超管靠 `*:*:*`），后续角色管理可配。

## File Structure

**新增后端文件：**
- `db/migration/V150__pro_task_worker_fields.sql` — 任务派工人/负责人 6 列
- `db/migration/V151__pro_feedback_quantity_input.sql` — 报工上机数量列
- `db/migration/V152__pro_feedback_change.sql` — 上机数量修改痕迹表
- `db/migration/V153__qc_block_release.sql` — 质检拦截放行表 + IPQC 索引
- `db/migration/V154__pro_task_release_perm.sql` — 放行按钮权限点
- `domain/mes/pro/ProFeedbackChange.java`、`mapper/mes/pro/ProFeedbackChangeMapper.java`(+XML)、`service/mes/pro/IProFeedbackChangeService.java` + `impl/ProFeedbackChangeServiceImpl.java`
- `domain/mes/qc/QcBlockRelease.java`、`mapper/mes/qc/QcBlockReleaseMapper.java`(+XML)
- `service/mes/pro/ProRouteFlowHelper.java` — 工艺路线相邻工序解析（纯组件）
- `service/mes/pro/ProInputQuantityResolver.java` — 上机数量默认值解析
- `service/mes/pro/IProQcBlockService.java` + `impl/ProQcBlockServiceImpl.java` — 阻塞判定/门控/放行/FAIL 后置动作
- `service/mes/pro/TaskReportDefaultsApplier.java` — 三入口批量富化（默认上机数 + 锁态）
- 单测：`ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProRouteFlowHelperTest.java`、`ProInputQuantityResolverTest.java`、`ProQcBlockServiceImplTest.java`

**修改后端文件：**
- `domain/mes/pro/ProTask.java`（+6 持久化字段，+3 瞬态字段 `defaultQuantityInput/qcBlocked/qcBlockReason`）、`ProFeedback.java`(+quantityInput)
- `mapper/mes/pro/ProTaskMapper.xml`（insert/update 6 列）、`mapper/mes/pro/ProFeedbackMapper.xml`（resultMap/insert/update + 2 个 sum 查询）、`mapper/mes/qc/QcIpqcMapper.java`+XML（按工序查最新已判 IPQC）
- `common/enums/TodoTypeEnum.java`（+PRO_QC_BLOCK）
- `service/.../pro/impl/ProTaskServiceImpl.java`（派人快照回填、富化 reportableList）、`ProFeedbackServiceImpl.java`（上机默认值/留痕/门控）、`ProCardServiceImpl.java`（scan 富化）
- `web/controller/mes/pro/ProWorkorderController.java`（feedbackEntry 富化）、`ProTaskController.java`（放行端点）、`ProFeedbackController.java`（痕迹查询端点）
- `service/.../qc/impl/QcIpqcServiceImpl.java`（FAIL 后置钩子，返回被拦工序名）

**修改/新增前端文件：**
- PC：`frontend/src/api/mes/pro/task.ts`、`feedback.ts`(+类型)、`views/mes/pro/gantt/index.vue`（弹窗派人）、`views/mes/pro/task/index.vue`（列）、`views/mes/pro/feedback/index.vue`（上机数量 + 痕迹抽屉）
- App：新增 `app/api/mes/pro/task.js`；`app/pages/mes/pro/report.vue`、新增组件 `app/pages/mes/pro/components/qc-block-bar.vue`、`input-qty-row.vue`；`report-list.vue`、`qc/inspect.vue`

---

### Task 1: 任务派工人/负责人字段（库表 + 后端保存 + 甘特配置）

**Files:**
- Create: `backend/ruoyi-admin/src/main/resources/db/migration/V150__pro_task_worker_fields.sql`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProTask.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProTaskMapper.xml`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProTaskServiceImpl.java`（insert/update 快照回填）
- Modify: `frontend/src/api/mes/pro/task.ts`、`frontend/src/views/mes/pro/gantt/index.vue`、`frontend/src/views/mes/pro/task/index.vue`

**Interfaces:**
- Produces: `ProTask` 持久化字段 `Long workerId/String workerName/String workerNick/Long leaderId/String leaderName/String leaderNick`；瞬态 `BigDecimal defaultQuantityInput`、`Boolean qcBlocked`、`String qcBlockReason`（本任务只加字段，后两个瞬态字段也一并声明，后续任务用）。
- 保存语义：客户端只传 id，后端经 `ISysUserService.selectUserById(Long)` 回填账号(userName)/姓名(nickName)快照；id 为 null 则三列清空。

- [ ] **Step 1: 写 Flyway V150**

```sql
-- V150__pro_task_worker_fields.sql
-- 排产任务增加派工报工人、负责人（均为可空快照；自动排产任务默认 NULL，报工页退化为登录人兜底）
ALTER TABLE qxx_pro_task
    ADD COLUMN worker_id   bigint        DEFAULT NULL COMMENT '派工报工人用户ID'   AFTER quantity_changed,
    ADD COLUMN worker_name varchar(64)   DEFAULT NULL COMMENT '派工报工人账号快照' AFTER worker_id,
    ADD COLUMN worker_nick varchar(64)   DEFAULT NULL COMMENT '派工报工人姓名快照' AFTER worker_name,
    ADD COLUMN leader_id   bigint        DEFAULT NULL COMMENT '任务负责人用户ID'   AFTER worker_nick,
    ADD COLUMN leader_name varchar(64)   DEFAULT NULL COMMENT '任务负责人账号快照' AFTER leader_id,
    ADD COLUMN leader_nick varchar(64)   DEFAULT NULL COMMENT '任务负责人姓名快照' AFTER leader_name;
```

- [ ] **Step 2: 实体加字段**

`ProTask.java`（在 `quantityChanged` 后）：

```java
    @Excel(name = "派工报工人ID") private Long workerId;
    @Excel(name = "派工报工人") private String workerNick;
    private String workerName;
    @Excel(name = "任务负责人ID") private Long leaderId;
    @Excel(name = "任务负责人") private String leaderNick;
    private String leaderName;

    /** 报工默认上机数量（非持久化，查询入口富化） */
    private BigDecimal defaultQuantityInput;
    /** 质检不合格阻塞态（非持久化，实时派生） */
    private Boolean qcBlocked;
    /** 阻塞原因文案 */
    private String qcBlockReason;
```

- [ ] **Step 3: Mapper XML resultMap / insert / update 补 6 列**

`ProTaskMapper.xml`：resultMap 加 6 个 `<result property="workerId" column="worker_id"/>`（等），`selectProTaskVo` 列清单加 `t.worker_id,t.worker_name,t.worker_nick,t.leader_id,t.leader_name,t.leader_nick`；insert 的列/值加（`<if test="workerId != null">worker_id,</if>` 及对应 `#{workerId},`，遵循该文件现有动态列风格），update 加：

```xml
            <if test="workerId != null">worker_id = #{workerId},</if>
            <if test="workerName != null">worker_name = #{workerName},</if>
            <if test="workerNick != null">worker_nick = #{workerNick},</if>
            <if test="leaderId != null">leader_id = #{leaderId},</if>
            <if test="leaderName != null">leader_name = #{leaderName},</if>
            <if test="leaderNick != null">leader_nick = #{leaderNick},</if>
```

清空语义：改派/取消派人时先由 service 把三字段置空串走更新（MyBatis 动态 SQL 无法 set NULL），故 update 另加固定片段：

```xml
            worker_name = #{workerName},
            worker_nick = #{workerNick},
            leader_name = #{leaderName},
            leader_nick = #{leaderNick},
```

即 name/nick 恒更新（service 保证 id 为 null 时 name/nick 也为 null）；id 列保留 `<if>`（避免批量改态误置空）。

- [ ] **Step 4: Service 保存时回填人员快照**

`ProTaskServiceImpl` 注入 `ISysUserService`；新增私有方法（≤50 行）：

```java
    private void fillWorkerSnapshot(ProTask task) {
        fillOne(task.getWorkerId(), task::setWorkerId, task::setWorkerName, task::setWorkerNick);
        fillOne(task.getLeaderId(), task::setLeaderId, task::setLeaderName, task::setLeaderNick);
    }

    private void fillOne(Long userId, Consumer<Long> idSetter,
                         Consumer<String> nameSetter, Consumer<String> nickSetter) {
        if (userId == null) { nameSetter.accept(null); nickSetter.accept(null); return; }
        SysUser u = userService.selectUserById(userId);
        if (u == null) { throw new ServiceException("指定的人员不存在：" + userId); }
        nameSetter.accept(u.getUserName());
        nickSetter.accept(u.getNickName());
    }
```

在 `insertProTask`、`updateProTask`（controller 调入参为 ProTask 的两处）开头调 `fillWorkerSnapshot(task)`。注意：`dispatchTask/completeTask/cancelTask` 等状态流转构造的部分更新对象 **不调**（workerId 为 null，动态 SQL 跳过 id 列；name/nick 固定片段只在全量 insert/update 语句中加入，确认 XML 中 `updateProTask` 与 `updateStatusByWorkorder` 是不同语句，仅前者加固定片段）。

- [ ] **Step 5: 甘特弹窗加人员选择**

`gantt/index.vue` 任务弹窗（现状字段：工序/工作站/排产数/时间）：
- 用 `frontend/src/api/system/user`（若依自带，`listUser`）做两个 `el-select`（filterable remote，`:remote-method` 按 userName/nickName 模糊查，`status='0'`），字段 `workerId / leaderId`，label=`${user.nickName}（${user.userName}）`；option value=userId；编辑回显时用行内已有 workerName/workerNick 直接构 option，无列表时不发请求。
- 标签：「报工人（派工）」「负责人」，均可不选；placeholder「不派则报工时默认当前登录人」。
- 保存沿用现有 `POST/PUT /mes/pro/task`（gantt.ts 已有），只多传两个 id。

- [ ] **Step 6: 任务列表加两列**

`views/mes/pro/task/index.vue` 表格在「工作站」列后加「报工人」`prop="workerNick"`、「负责人」`prop="leaderNick"` 两列（无数据显示 `-`）。

- [ ] **Step 7: 验证（后端红线）**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests
# kill 旧 ruoyi-admin 进程后重启
nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
curl -s http://localhost:8081/captchaImage   # 等 200
```

实测：
```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
# 建/改任务带人（先从 /system/user/list 取一个真实 userId）
curl -s -X PUT http://localhost:8081/mes/pro/task -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"taskId":<某PRODUCING任务id>,"workerId":<userId>,"leaderId":<userId>}'
curl -s "http://localhost:8081/mes/pro/task/<taskId>" -H "Authorization: Bearer $TOKEN"
# 期望返回含 workerId/workerName/workerNick/leader*
```
PC：甘特弹窗能选能存、刷新回显；任务列表显示两列。

- [ ] **Step 8: Commit**

```bash
git add backend frontend && git commit -m "feat(pro): 排产任务增加派工报工人与负责人字段，甘特可配置"
```

---

### Task 2: 上机数量列 + 默认值解析 + 修改痕迹（TDD）

**Files:**
- Create: `V151__pro_feedback_quantity_input.sql`、`V152__pro_feedback_change.sql`
- Create: `domain/mes/pro/ProFeedbackChange.java`、`mapper/mes/pro/ProFeedbackChangeMapper.java`(+XML)、`service/mes/pro/IProFeedbackChangeService.java` + impl
- Create: `service/mes/pro/ProRouteFlowHelper.java`、`service/mes/pro/ProInputQuantityResolver.java`
- Modify: `domain/mes/pro/ProFeedback.java`、`ProFeedbackMapper.java`+XML、`ProFeedbackServiceImpl.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProRouteFlowHelperTest.java`、`ProInputQuantityResolverTest.java`

**Interfaces:**
- `ProRouteFlowHelper`（`@Component`，依赖 `ProRouteProcessMapper`）：
  - `List<ProRouteProcess> nodes(Long routeId)` — 按 order_num 升序
  - `Optional<ProRouteProcess> currentNode(Long routeId, Long processId)`
  - `Optional<ProRouteProcess> prevNode(Long routeId, Long processId)` — order_num 严格小于当前的最大节点
  - `Optional<ProRouteProcess> prevCheckNode(Long routeId, Long processId)` — 前驱中 `is_check='Y'` 的最近节点（供 Task 4）
  - `List<ProRouteProcess> nextWave(Long routeId, Long processId)` — order_num 严格大于当前的最小一波（同 order_num 全含，覆盖 FS 并行）
- `ProInputQuantityResolver`（`@Component`）：`BigDecimal resolveDefaultInput(Long workorderId, Long routeId, Long processId, Long cardId)`，无路线信息返回 null（不默认）。
- 新 mapper 方法（ProFeedbackMapper，均 `@Param`，XML WHERE 含 factory_id `<if>`）：
  - `BigDecimal sumAuditedQuantityFeedback(@Param("workorderId") Long, @Param("processId") Long, @Param("cardId") Long)`
  - `BigDecimal sumQuantityInput(@Param("workorderId") Long, @Param("processId") Long, @Param("cardId") Long)`
- 留痕：`IProFeedbackChangeService.recordInputChange(Long feedbackId, ProFeedback fb, BigDecimal oldVal, BigDecimal newVal, String source)`；`List<ProFeedbackChange> selectByFeedback(Long feedbackId)`、`selectByTask(Long taskId)`。
- `ProFeedback.quantityInput : BigDecimal`（本次上机数量）。

- [ ] **Step 1: Flyway V151 + V152**

```sql
-- V151__pro_feedback_quantity_input.sql
ALTER TABLE qxx_pro_feedback
    ADD COLUMN quantity_input decimal(14,2) DEFAULT NULL COMMENT '本次上机数量(投入数量)' AFTER quantity;
```

```sql
-- V152__pro_feedback_change.sql
-- 报工上机数量修改痕迹（系统默认带出 vs 人工修改）
CREATE TABLE IF NOT EXISTS qxx_pro_feedback_change (
    change_id      bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id     bigint       NOT NULL COMMENT '工厂ID',
    feedback_id    bigint       NOT NULL COMMENT '报工记录ID',
    task_id        bigint       DEFAULT NULL COMMENT '任务ID',
    workorder_id   bigint       DEFAULT NULL COMMENT '工单ID',
    field_name     varchar(64)  NOT NULL DEFAULT 'quantity_input' COMMENT '变更字段',
    old_value      varchar(128) DEFAULT NULL COMMENT '原值(系统默认值)',
    new_value      varchar(128) NOT NULL COMMENT '新值(人工填写值)',
    change_source  varchar(16)  NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL-人工修改/SYSTEM-系统填充',
    change_reason  varchar(255) DEFAULT NULL COMMENT '变更说明',
    create_by      varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time    datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (change_id),
    KEY idx_fb (feedback_id),
    KEY idx_task (task_id),
    KEY idx_wo (workorder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报工字段变更痕迹';
```

- [ ] **Step 2: 写失败单测 ProRouteFlowHelperTest**

关键用例（mock `selectProRouteProcessByRouteId` 返回内存节点列表，节点用 setter 构造：processId/orderNum/isCheck/linkType）：

```java
@ExtendWith(MockitoExtension.class)
class ProRouteFlowHelperTest {
    @Mock ProRouteProcessMapper mapper;
    @InjectMocks ProRouteFlowHelper helper;

    private ProRouteProcess node(long pid, int order, String check) {
        ProRouteProcess n = new ProRouteProcess();
        n.setProcessId(pid); n.setOrderNum((long) order); n.setIsCheck(check);
        n.setLinkType("SS"); return n;
    }

    @Test @DisplayName("prevNode取order_num严格更小的最大节点")
    void should_return_greatest_smaller_when_prevNode() {
        when(mapper.selectProRouteProcessByRouteId(9L))
            .thenReturn(List.of(node(1,1,"N"), node(2,2,"Y"), node(3,3,"N")));
        assertThat(helper.prevNode(9L, 3L)).get().extracting(ProRouteProcess::getProcessId).isEqualTo(2L);
    }

    @Test @DisplayName("首道工序无前驱")
    void should_empty_when_first_node() { /* current=1 → Optional.empty() */ }

    @Test @DisplayName("prevCheckNode跳过非检验节点找最近检验节点")
    void should_return_nearest_check_when_prevCheckNode() { /* 节点 1Y,2N,3N current=3 → 1 */ }

    @Test @DisplayName("nextWave返回下一序号全部并行节点")
    void should_return_whole_next_wave_when_parallel() {
        // 1(N,SS), 2(FS),3(FS 同 order=2), 4(order=3)；current=1 → [2,3]
    }
}
```

- [ ] **Step 3: 跑测试确认失败 → 实现 ProRouteFlowHelper 至通过**

`mvn -pl ruoyi-system test -Dtest=ProRouteFlowHelperTest`，先类不存在编译失败（红），再写实现（绿）。实现要点：`nodes` 结果用 `TreeMap`/stream 按 orderNum 排序；prev 用 `filter(o < cur).max(comparing(orderNum))`；nextWave 先求 `min greater` 再 `filter(eq min)`。

- [ ] **Step 4: 写失败单测 ProInputQuantityResolverTest**

```java
@ExtendWith(MockitoExtension.class)
class ProInputQuantityResolverTest {
    @Mock ProRouteProcessHelperAccess route;   // 实际 mock ProRouteFlowHelper
    @Mock ProFeedbackMapper feedbackMapper;
    @InjectMocks ProInputQuantityResolver resolver;
    // 用例：
    // 1) 首道工序 → 返回 taskQuantity（方法签名需传首道默认值）
    // 2) 上工序已审500、本工序已上机0 → 500
    // 3) 上工序已审500、本工序已上机480 → 20
    // 4) 多批：上工序300+200=500 → 500
    // 5) 差额为负（上机>产出）→ 0（clamp）
    // 6) 找不到当前节点 → null
}
```

为支持用例 1，最终签名定为：
`BigDecimal resolveDefaultInput(Long workorderId, Long routeId, Long processId, Long cardId, BigDecimal firstProcessDefault)`；首道工序返回 `firstProcessDefault`（调用方传 `task.quantity`）。

- [ ] **Step 5: 实现 resolver 与两个 sum SQL 至测试通过**

XML：

```xml
    <select id="sumAuditedQuantityFeedback" resultType="java.math.BigDecimal">
        select coalesce(sum(quantity_feedback), 0) from qxx_pro_feedback
        where workorder_id = #{workorderId}
          and process_id = #{processId}
          and status = 'AUDITED'
          and quantity_feedback is not null
        <if test="cardId != null">and card_id = #{cardId}</if>
        <if test="factoryId != null">and factory_id = #{factoryId}</if>
    </select>

    <select id="sumQuantityInput" resultType="java.math.BigDecimal">
        select coalesce(sum(quantity_input), 0) from qxx_pro_feedback
        where workorder_id = #{workorderId}
          and process_id = #{processId}
          and quantity_input is not null
        <if test="cardId != null">and card_id = #{cardId}</if>
        <if test="factoryId != null">and factory_id = #{factoryId}</if>
    </select>
```

resolver 核心：

```java
    public BigDecimal resolveDefaultInput(Long workorderId, Long routeId, Long processId,
                                          Long cardId, BigDecimal firstProcessDefault) {
        if (routeId == null || processId == null) return null;
        Optional<ProRouteProcess> cur = flow.currentNode(routeId, processId);
        if (cur.isEmpty()) return null;
        Optional<ProRouteProcess> prev = flow.prevNode(routeId, processId);
        if (prev.isEmpty()) return firstProcessDefault;             // 首道：默认排产数
        BigDecimal produced = nz(feedbackMapper.sumAuditedQuantityFeedback(
                workorderId, prev.get().getProcessId(), cardId));
        BigDecimal used = nz(feedbackMapper.sumQuantityInput(workorderId, processId, cardId));
        return produced.subtract(used).max(BigDecimal.ZERO);
    }
```

- [ ] **Step 6: ProFeedback 实体 + Mapper XML 加 quantity_input**

resultMap 加 `<result property="quantityInput" column="quantity_input"/>`；列清单/insert（动态列）/update（`<if test="quantityInput != null">quantity_input = #{quantityInput},</if>`）同步。

- [ ] **Step 7: 留痕表 domain/mapper/service 全套（仿 ProWorkorderChange）**

`ProFeedbackChange` 字段：`changeId/factoryId/feedbackId/taskId/workorderId/fieldName/oldValue/newValue/changeSource/changeReason` + 继承 `BaseEntity`（createBy/createTime）。Mapper：`insert`、`selectByFeedback`、`selectByTask`（XML WHERE factory_id `<if>`，order by create_time desc）。Service 实现 `recordInputChange`：old/new 相等直接 return；`fieldName` 常量 `"quantity_input"`，source 枚举值用 ProConstants 新增常量 `CHANGE_SOURCE_MANUAL="MANUAL"` / `CHANGE_SOURCE_SYSTEM="SYSTEM"`。

- [ ] **Step 8: 报工 insert 接入默认值与留痕**

`ProFeedbackServiceImpl.doInsertProFeedback`：在 `autoFillCodes` 解析出 task 之后、insert 之前加（同一事务内）：

```java
    private void applyQuantityInput(ProFeedback fb, ProTask task) {
        BigDecimal def = inputQuantityResolver.resolveDefaultInput(
                fb.getWorkorderId(), fb.getRouteId(), fb.getProcessId(),
                fb.getCardId(), task != null ? task.getQuantity() : null);
        if (def == null) return;
        if (fb.getQuantityInput() == null) {
            fb.setQuantityInput(def);
            return;
        }
        if (fb.getQuantityInput().compareTo(def) != 0) {
            fb.changeInputPending = true; fb.inputDefaultValue = def; // 用参数对象传递，勿在实体加瞬态则改为方法返回值
        }
    }
```

实现注意：不要给 ProFeedback 加瞬态字段。把方法改成返回 `BigDecimal defaultVal`（null 表示无需记录），insert 主表拿到 `recordId` 后：若返回值非 null 且与提交值不等，调 `changeService.recordInputChange(recordId, fb, defaultVal, fb.getQuantityInput(), MANUAL)`，reason 固定文案「上机数量由系统默认值人工调整」。
非 INTERNAL（外协）报工同样适用（上游外协工序的产出经外协收货写报工后，下游厂内工序默认值自然带出）。

- [ ] **Step 9: 报工 update 加 diff 留痕**

`updateProFeedback`：先 `selectProFeedbackByRecordId` 取旧值；旧 `quantityInput` 与新值不等（含 null↔值）时 `recordInputChange(...MANUAL)`，reason「报工修改」。

- [ ] **Step 10: 痕迹查询端点**

`ProFeedbackController` 加：

```java
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:query')")
    @GetMapping("/change/list")
    public AjaxResult changeList(@RequestParam(required = false) Long feedbackId,
                                 @RequestParam(required = false) Long taskId) {
        return success(feedbackId != null
                ? changeService.selectByFeedback(feedbackId)
                : changeService.selectByTask(taskId));
    }
```

- [ ] **Step 11: 单测全绿 + 打包重启 + curl 验收默认值与痕迹**

```bash
cd backend && mvn -pl ruoyi-system test -Dtest=ProRouteFlowHelperTest,ProInputQuantityResolverTest
mvn -pl ruoyi-admin -am package -DskipTests
# 重启（命令同 Task 1 Step 7）
```

curl 场景（用已有工单/路线/任务，或造数据）：
1. 上工序报工 → confirm → audit（`/mes/pro/feedback`、`/confirm/{id}`、`/audit/{id}`，数量 500）；
2. `POST /mes/pro/feedback` 下工序**不传** quantityInput → 落库=500，change 表无记录；
3. 再传 `quantityInput:480` → 落库 480，`GET /mes/pro/feedback/change/list?feedbackId=<id>` 返回 old=500/new=480/create_by=当前人。

- [ ] **Step 12: Commit**

```bash
git add backend && git commit -m "feat(pro): 报工增加上机数量，默认值按上工序已审报工自动带出，人工修改留痕"
```

---

### Task 3: 三个报工入口富化默认值/派人 + App/PC 报工页改造

**Files:**
- Create: `service/mes/pro/TaskReportDefaultsApplier.java`
- Modify: `service/.../pro/impl/ProTaskServiceImpl.java`（selectReportableTaskList）、`ProCardServiceImpl.java`（scanForReport）、`web/controller/mes/pro/ProWorkorderController.java`（feedbackEntry，逻辑目前在 controller 内）
- Modify: `app/pages/mes/pro/report.vue`、新增 `app/pages/mes/pro/components/input-qty-row.vue`、`app/pages/mes/pro/report-list.vue`
- Modify: `frontend/src/views/mes/pro/feedback/index.vue`、`frontend/src/api/mes/pro/feedback.ts`

**Interfaces:**
- `TaskReportDefaultsApplier`（`@Component`，依赖 `ProInputQuantityResolver`、`IProQcBlockService`——后者 Task 4 才实现；本任务先只富化默认值，接口先定义 `applyOne/apply` 两个方法，内部只调 resolver，Task 4 往里追加锁态行）：
  - `void apply(ProTask task, Long cardId)`：设置 `defaultQuantityInput`
  - `void apply(Collection<ProTask> tasks, Long cardId)`

- [ ] **Step 1: 实现 applier 并挂到三个入口**

```java
@Component
public class TaskReportDefaultsApplier {
    private final ProInputQuantityResolver inputResolver;
    // 构造注入

    public void apply(Collection<ProTask> tasks, Long cardId) {
        if (tasks == null) return;
        tasks.forEach(t -> apply(t, cardId));
    }
    public void apply(ProTask t, Long cardId) {
        if (t == null) return;
        t.setDefaultQuantityInput(inputResolver.resolveDefaultInput(
                t.getWorkorderId(), t.getRouteId(), t.getProcessId(),
                cardId, t.getQuantity()));
    }
}
```

挂载点：
- `ProTaskServiceImpl.selectReportableTaskList`：return 前 `defaultsApplier.apply(list, null)`（待报工列表为工单维度，cardId 传 null）。
- `ProWorkorderController.feedbackEntry`：在 `fillPendingFeedbackCount(allTasks)` 后加 `defaultsApplier.apply(reportableTasks, null)`（注入 applier）。
- `ProCardServiceImpl.scanForReport`：构造 reportableTasks 后 `defaultsApplier.apply(tasks, card.getCardId())`。

- [ ] **Step 2: App 新增上机数量行组件**

`app/pages/mes/pro/components/input-qty-row.vue`（props: `modelValue:Number`、`defaultVal:Number`；emit `update:modelValue`）：一行 label「上机数量」+ `uni-number-box`（min 0）；当显示值与 defaultVal 不等时右侧小字橙色「已改·默认 {defaultVal}，保存后留痕」；首次挂载若 modelValue 为 null/undefined 自动 $emit defaultVal。

- [ ] **Step 3: App report.vue 接入（并控制膨胀）**

- 从待报工列表/扫码/工单进入并自动选中任务时（现有自动选中逻辑约 :382-390），把 `form.quantityInput` 初始化为 `task.defaultQuantityInput`；点选其他任务卡片同样重置。
- 在合格/不合格数量区块前插入 `<input-qty-row v-model="form.quantityInput" :default-val="selectedTask.defaultQuantityInput" />`。
- 选中任务区新增只读行「报工人：{{ selectedTask.workerNick || 当前登录人昵称 }}」（当前登录人取 user store 的 nickName/name），不可编辑；不传 userId，维持后端登录人兜底不变。
- 提交 body（约 :499-537）加 `quantityInput: this.form.quantityInput`。
- 模板/脚本增量预计 ~40 行；若 report.vue 因此超过 300 行脚本过多，本次不重构存量，仅保证新增逻辑都在子组件内。

- [ ] **Step 4: App 列表展示派人**

`report-list.vue` 卡片（:27-53）加一行小字「报工人：{{ item.workerNick || '未派工' }}」。

- [ ] **Step 5: PC 报工页加上机数量 + 痕迹查看**

`frontend/src/views/mes/pro/feedback/index.vue`：
- 表单在数量区加 `el-input-number`「上机数量」`v-model="form.quantityInput"`；任务选中回调（现有 `handleTaskChange/onTaskSelected`）用任务行新增字段 `defaultQuantityInput` 预填（该字段经 `/mes/pro/task/list` 已随实体返回 XML 全列映射，但列表接口不富化——仅三入口富化；PC 场景在 `onTaskSelected` 拿到单个任务后另调新端点 `GET /mes/pro/feedback/inputDefault/{taskId}`，见 Step 6）。
- 新增「修改痕迹」抽屉/弹窗：`el-table` 展示 create_time、create_by、old_value→new_value、change_reason，按 feedbackId 查询。
- `feedback.ts` 加 `getFeedbackChanges(params)` → GET `/mes/pro/feedback/change/list`；加 `getInputDefault(taskId)`。

- [ ] **Step 6: 单任务默认值端点**

`ProFeedbackController`：

```java
    @PreAuthorize("@ss.hasPermi('mes:pro:feedback:query')")
    @GetMapping("/inputDefault/{taskId}")
    public AjaxResult inputDefault(@PathVariable Long taskId) {
        ProTask t = proTaskService.selectProTaskByTaskId(taskId);
        if (t == null) return error("任务不存在");
        defaultsApplier.apply(t, null);
        return success(t.getDefaultQuantityInput());
    }
```

- [ ] **Step 7: 实测**

重启后端。App（HBuilderX/微信开发者工具跑现有 app 工程）：待报工列表进入 → 上机数量默认 500、报工人正确显示；改 480 提交后 PC 痕迹抽屉可见 500→480 + 操作人。PC 报工页同样验证。

- [ ] **Step 8: Commit**

```bash
git add backend frontend app && git commit -m "feat(pro): 报工入口自动带出租工人与上机默认值，App/PC报工页改造"
```

---

### Task 4: 质检不合格硬拦 + 放行 + 留痕 + 待办（TDD）

**Files:**
- Create: `V153__qc_block_release.sql`、`V154__pro_task_release_perm.sql`
- Create: `domain/mes/qc/QcBlockRelease.java`、`mapper/mes/qc/QcBlockReleaseMapper.java`(+XML)
- Create: `service/mes/pro/IProQcBlockService.java` + `impl/ProQcBlockServiceImpl.java`
- Modify: `mapper/mes/qc/QcIpqcMapper.java`+XML（新查询）、`common/enums/TodoTypeEnum.java`
- Modify: `ProFeedbackServiceImpl.java`（门控调用）、`TaskReportDefaultsApplier.java`（富化锁态）、`QcIpqcServiceImpl.java`（FAIL 钩子）
- Modify: `ProTaskController.java`（放行端点）
- Test: `ProQcBlockServiceImplTest.java`

**Interfaces:**
- `IProQcBlockService`：
  - `QcBlockInfo findBlock(Long workorderId, Long routeId, Long processId, Long cardId)` — 返回 null 表示放行；`QcBlockInfo`（record/简单类）：`ipqcId/ipqcCode/checkProcessName/reason`
  - `void assertReportable(ProFeedback fb)` — 有阻塞抛 `ServiceException`（文案含检验单号与检验工序、引导「请联系质检或有权限人员放行」）
  - `void release(Long taskId, String reason)` — 先锁后事务；解析阻塞→插放行记录→关闭拦截待办
  - `void onIpqcFailed(QcIpqc ipqc)` — FAIL 后置：对 nextWave 任务逐一生成拦截待办（指派给 task.leaderId）；返回被拦任务名列表由 controller 用
  - `List<String> blockedProcessNames(QcIpqc ipqc)`
- QcIpqcMapper 新增：
  - `QcIpqc selectLatestCompletedByProcess(@Param("workorderId") Long, @Param("processId") Long, @Param("cardId") Long)` — `status='COMPLETED'` order by inspect_time/record_id desc limit 1，WHERE 带 factory `<if>`。
- QcBlockReleaseMapper：`insert`、`existsByIpqcAndTask(ipqcId, targetTaskId)`、`selectByIpqc(ipqcId)`。

- [ ] **Step 1: Flyway V153 + V154**

```sql
-- V153__qc_block_release.sql
CREATE TABLE IF NOT EXISTS qxx_qc_block_release (
    release_id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id          bigint       NOT NULL COMMENT '工厂ID',
    ipqc_id             bigint       NOT NULL COMMENT '被放行的IPQC检验单ID',
    ipqc_code           varchar(64)  DEFAULT NULL COMMENT '检验单号',
    workorder_id        bigint       DEFAULT NULL COMMENT '工单ID',
    workorder_code      varchar(64)  DEFAULT NULL,
    card_id             bigint       DEFAULT NULL COMMENT '流转卡ID',
    card_code           varchar(64)  DEFAULT NULL,
    check_process_id    bigint       DEFAULT NULL COMMENT '检验工序ID',
    check_process_name  varchar(128) DEFAULT NULL COMMENT '检验工序名',
    target_task_id      bigint       NOT NULL COMMENT '被拦截(放行)任务ID',
    target_process_id   bigint       DEFAULT NULL COMMENT '被拦截工序ID',
    target_process_name varchar(128) DEFAULT NULL COMMENT '被拦截工序名',
    release_reason      varchar(500) NOT NULL COMMENT '放行理由',
    approver_id         bigint       DEFAULT NULL COMMENT '放行人用户ID',
    approver_name       varchar(64)  DEFAULT NULL COMMENT '放行人账号/姓名快照',
    approve_time        datetime     DEFAULT NULL COMMENT '放行时间',
    create_by           varchar(64)  DEFAULT NULL,
    create_time         datetime     DEFAULT NULL,
    PRIMARY KEY (release_id),
    UNIQUE KEY uk_ipqc_target (ipqc_id, target_task_id),
    KEY idx_wo (workorder_id),
    KEY idx_task (target_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检不合格拦截-放行记录';

-- 门控高频反查索引（按工单+工序找最新判定单）
CREATE INDEX idx_ipqc_wo_process ON qxx_qc_ipqc(workorder_id, process_id, status);
```

```sql
-- V154__pro_task_release_perm.sql
-- 放行按钮权限：默认不授任何角色（超管 *:*:* 天然拥有），后续角色管理自行分配
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '质检拦截放行', m.menu_id, 20, '', NULL, 1, 0, 'F', '0', '0',
       'mes:pro:task:release', '#', 'admin', now(), '跟单检验不合格时放行道工序报工'
FROM sys_menu m
WHERE m.perms = 'mes:pro:task:list' AND m.menu_type = 'C'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:pro:task:release');
```

- [ ] **Step 2: TodoTypeEnum 加枚举**

`ruoyi-common/.../enums/TodoTypeEnum.java` 加 `PRO_QC_BLOCK("PRO_QC_BLOCK", "质检不合格拦截")`（先 Read 该枚举确认构造形态）。

- [ ] **Step 3: 写失败单测 ProQcBlockServiceImplTest**

mock ProRouteFlowHelper / QcIpqcMapper / QcBlockReleaseMapper / ProTaskMapper / SysTodoListMapper：
```java
// 1) should_pass_when_no_prev_check_node：prevCheckNode empty → findBlock null
// 2) should_pass_when_ipqc_pass：最新 COMPLETED 单 checkResult=PASS → null
// 3) should_pass_when_concession：CONCESSION → null
// 4) should_block_when_fail_without_release：FAIL 且 existsByIpqcAndTask=false → 返回 QcBlockInfo
// 5) should_pass_when_fail_but_released：exists=true → null
// 6) should_pass_when_no_completed_ipqc：未判定 → null（只拦已判不合格）
// 7) release：findBlock 非空 → insert 被调用一次、unique 键 (ipqcId,targetTaskId) 正确；
//    findBlock 为空时抛 ServiceException("当前任务无需放行")
// 8) assertReportable 阻塞时抛 ServiceException 且文案含检验单号
```

- [ ] **Step 4: 实现 QcBlockRelease 全套 + QcBlockServiceImpl 至单测全绿**

findBlock 核心（≤50 行，拆小方法）：
1. `prevCheckNode(routeId, processId)` 为空 → null；
2. `ipqc = qcIpqcMapper.selectLatestCompletedByProcess(workorderId, prevCheck.processId, cardId)`；为空 → null；
3. `PASS/CONCESSION` → null；
4. 解析当前任务（proTaskMapper 按 workorderId+processId，必要时加 cardId 不参与任务匹配）→ `releaseMapper.existsByIpqcAndTask(ipqc.recordId, task.taskId)` → true 放行；false 返回 `QcBlockInfo`，reason=「上道检验工序「X」判定不合格（检验单号 IPQC…），本工序暂不可报工」。

`release(Long taskId, String reason)`：
- 校验 reason 非空（trim 后 ≥2 字符），否则 ServiceException「请填写放行理由」；
- `RedisLockTemplate.execute("pro:qc:release:" + taskId, 3, …)`，事务内：查任务→findBlock→空则报错→insert（approver 取 SecurityUtils 当前用户 id+userName/nickName，approveTime=now）→ 将该 ipqc+task 的 PENDING `PRO_QC_BLOCK` 待办批量置 COMPLETED、handleResult=「人工放行："+reason」。

`onIpqcFailed(QcIpqc ipqc)`（REQUIRES_NEW 由调用方包事务或本方法加事务模板；锁 key 复用 `qc:judge:IPQC:{id}` 调用链已持锁，直接在 judge 事务内执行）：
- `nextWave(ipqc.routeId, ipqc.processId)` → 每节点查任务（同工单）→ 每任务 insert `SysTodoList`：todoType=PRO_QC_BLOCK，userId=task.leaderId（可空），sourceDocType="IPQC"，sourceDocId=ipqc.recordId，sourceDocCode=ipqc.ipqcCode，title="质检不合格拦截：{工单编码}-{工序名} 待放行/处理"，priority 高，status=PENDING；已存在同 source+task 未关闭待办则跳过（幂等，mapper 加 `selectPendingBySourceAndUser` 或在 service 内存判重）。
- 去重唯一约束可选：不加 DB 唯一（leader 可能多人），service 判重。

`blockedProcessNames`：与 onIpqcFailed 同样的 nextWave 任务解析，返回 `工序名(taskCode)` 列表。

- [ ] **Step 5: 门控挂到报工提交入口**

`ProFeedbackServiceImpl.doInsertProFeedback`：在 `validateIssueBeforeFeedback(fb)`（约 :323 调用处）旁加：

```java
        qcBlockService.assertReportable(fb);
```

放在外协/顺序校验之后、领料门控之前；fb 上的 workorderId/routeId/processId/cardId 在 autoFillCodes 后均已权威填充。PC、App、外协收货写报工三条路径最终都经 `doInsertProFeedback`（分切直写两条路径确认：`ProSlittingServiceImpl` 是直接 mapper.insert，不经门控——分切属于物理加工无 IPQC 下游场景，保持不拦，计划内接受）。

- [ ] **Step 6: 富化锁态**

`TaskReportDefaultsApplier.apply(task, cardId)` 内追加：
```java
        QcBlockInfo blk = qcBlockService.findBlock(t.getWorkorderId(), t.getRouteId(),
                t.getProcessId(), cardId);
        t.setQcBlocked(blk != null);
        t.setQcBlockReason(blk == null ? null : blk.getReason());
```

- [ ] **Step 7: 放行端点**

`ProTaskController`：

```java
    @Log(title = "质检拦截放行", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('mes:pro:task:release')")
    @PutMapping("/releaseQcBlock/{taskId}")
    public AjaxResult releaseQcBlock(@PathVariable Long taskId,
                                     @RequestParam String reason) {
        qcBlockService.release(taskId, reason);
        return success();
    }
```

注入 `IProQcBlockService`。

- [ ] **Step 8: IPQC 判定 FAIL 钩子 + 返回拦截工序名**

`QcIpqcServiceImpl.doJudgeIpqc`（约 :309-345，COMPLETED 落定后）：
- 仅 IPQC（其他三单不拦生产）：若 `RESULT_FAIL.equals(ipqc.getCheckResult())`，调 `qcBlockService.onIpqcFailed(ipqc)`，并把 `blockedProcessNames(ipqc)` 放入返回；
- judge Controller（`QcIpqcController:116`）当前返回 `success()`，改为返回 `success(Map.of("blockedProcesses", names))`（PASS/CONCESSION 返回空列表）；重判（已有 FAIL 单被改判）不做待办回收（状态不可编辑，已判定即终态，符合现状）。

- [ ] **Step 9: 单测 + 打包重启 + curl 验收硬拦/放行**

```bash
mvn -pl ruoyi-system test -Dtest=ProQcBlockServiceImplTest
mvn -pl ruoyi-admin -am package -DskipTests   # 重启
```

实测全链路：
1. 工序1（路线节点 `is_check='Y'`）报工 → confirm（生成 IPQC）→ 录入判定 **FAIL**；响应含 blockedProcesses=工序2。
2. 下工序 `POST /mes/pro/feedback` → **code=500**，msg 含检验单号。
3. 待报工列表/扫码入口该任务 `qcBlocked=true`。
4. 无放行权限用户调 `PUT /mes/pro/task/releaseQcBlock/{id}`（不带 reason）→ 403/500 参数校验；超管带 reason → 成功；再报工 → 成功；`qxx_qc_block_release` 一行，approver/时间齐全。
5. FAIL 后 `sys_todo_list` 有 PRO_QC_BLOCK 待办（leader 配置时 userId=leader）；放行后待办 COMPLETED。

- [ ] **Step 10: Commit**

```bash
git add backend && git commit -m "feat(qc): 跟单检验不合格硬拦下道报工，授权角色放行留痕并联动待办"
```

---

### Task 5: App 锁态/放行 UI + 检验页回显（前端收口）

**Files:**
- Create: `app/api/mes/pro/task.js`、`app/pages/mes/pro/components/qc-block-bar.vue`
- Modify: `app/pages/mes/pro/report.vue`、`report-list.vue`、`app/pages/mes/qc/inspect.vue`、`app/api/mes/qc/index.js`（judge 已返回 data，无需改函数，确认透传）
- Modify(PC): `frontend/src/views/mes/pro/feedback/index.vue`（被拦报错已有全局 toast；放行入口可选做在 PC 报工页/任务页——本任务在 PC 任务列表加「放行」按钮 + 对话框，保证 PC 闭环）

**Interfaces:**
- `app/api/mes/pro/task.js`：
  ```js
  import request from '@/utils/request'
  export function releaseQcBlock(taskId, reason) {
    return request({ url: '/mes/pro/task/releaseQcBlock/' + taskId,
      method: 'put', params: { reason } })
  }
  ```

- [ ] **Step 1: 阻塞条组件 qc-block-bar.vue**

props: `task`（含 qcBlocked/qcBlockReason/taskId）。区块：红底卡片，锁图标 + reason 文案；按钮「放行（需授权）」`v-if="$auth.hasPermi('mes:pro:task:release')"`（options API 里用 `this.$auth.hasPermi(...)` 控制渲染）；点击 reason 输入：`uni.showModal({ editable: true, placeholderText: '请填写放行理由（必填）', success: … })`，trim 长度 <2 提示重填；调 `releaseQcBlock`，成功 toast 后 `this.$emit('released')`。无权限只展示原因，不显示按钮。

- [ ] **Step 2: report.vue 接入**

- 选中任务后若 `task.qcBlocked`：在数量区上方挂 `<qc-block-bar :task="selectedTask" @released="onReleased" />`，数量区/提交按钮 `:disabled`/隐藏；`onReleased` 重新拉当前入口数据（复用现有 onLoad 加载方法，刷新该任务 qcBlocked=false）后解锁。
- 未阻塞正常显示上机数量行。

- [ ] **Step 3: report-list.vue 锁标识**

卡片标题行加红色 tag「不可开工」`v-if="item.qcBlocked"`；点击仍进报工页（页内展示原因与放行）。

- [ ] **Step 4: inspect.vue 判定后回显拦截工序**

`doJudge` 成功分支：`res.data.blockedProcesses?.length` 时在结果横幅下追加红字「已拦截下道工序：{join('、')}，需放行后方可报工」（组件 data 加 blockedProcesses，提交前清空）。确认 judge API 封装把 response.data 原样返回（读 `app/api/mes/qc/index.js` 确认，必要时 return res.data）。

- [ ] **Step 5: PC 任务列表放行入口**

`views/mes/pro/task/index.vue`：行内「更多」或状态列加 `v-hasPermi="['mes:pro:task:release']"` 按钮「质检放行」（仅 qcBlocked 行显示——列表 `/mes/pro/task/list` 不富化锁态，PC 列表在前端查询返回后对 PRODUCING 行批量调一个轻量锁态接口）。

后端补一个批量端点（Task 4 的 controller 追加，若 Task 4 未做则本步补）：
`POST /mes/pro/task/qcBlockState`，body `[taskId,…]`（≤100），返回 `Map<taskId, {blocked, reason}>`；`@PreAuthorize mes:pro:task:list`。点击弹 `ElMessageBox.prompt` 收集理由 → 调 `releaseQcBlock(taskId, reason)`（task.ts 加同名函数 PUT）→ 刷新。

- [ ] **Step 6: 真机/H5 实测**

H5 或微信开发者工具：不合格后列表红 tag → 报工页红条且不能提交 → 超管账号放行 → 红条消失可报工；普通操作工账号无放行按钮；检验页提交 FAIL 后显示拦截工序名。PC 任务页同链路点一遍。

- [ ] **Step 7: Commit**

```bash
git add app frontend backend && git commit -m "feat(pro): 报工锁态展示与授权放行入口(App/PC)，检验页回显拦截工序"
```

---

### Task 6: 三条验收场景全链路实测 + 回归

**Files:** 无新增（如需补 e2e 脚本：`e2e/tests/pro/qc-block.spec.ts`，可作为后续，本任务以手工+curl 为准）

- [ ] **Step 1: 后端单测全量 + 前端类型检查**

```bash
cd backend && mvn -pl ruoyi-system test
cd ../frontend && npx vue-tsc --noEmit
cd ../app && （无独立 typecheck 则跳过，依赖 HBuilderX 编译无报错）
```

- [ ] **Step 2: 重新打包重启后端（红线）**

按 Global Constraints 完整流程：package → kill → nohup 启动 → captchaImage 200 → 看 `/tmp/ruoyi-backend.log` 无 Flyway/Bean 异常，`flyway_schema_history` V150-V154 success=1。

- [ ] **Step 3: 验收场景一（默认值 + 留痕）**

找/造一个至少两工序（SS 串行）、第二道非检验工序的工单：
1. 甘特派工：工序2 任务指定报工人张三、负责人李四；
2. 工单开工 → 工序1 报工 500 → confirm → audit；
3. 张三账号从 App 待报工列表进工序2：报工人显示张三、上机数量默认 **500**；
4. 改 **480** 提交成功；`GET /mes/pro/feedback/change/list?feedbackId=…` 查到 old=500,new=480,create_by=张三,create_time；
5. 再做一批：工序1 再报工 200 并审核 → 工序2 再进默认值=20（500+200−480，验证累计口径）；首道工序任务默认上机=排产数量。

- [ ] **Step 4: 验收场景二（硬拦 + 放行）**

1. 路线首道/某道 `is_check='Y'`：工序1 报工→confirm→IPQC 判定 **不合格**；
2. inspect 页显示「已拦截下道工序：工序2」；待报工列表工序2 红 tag「不可开工」；
3. 进报工页：红条+原因，不能提交；直接 curl `POST /mes/pro/feedback` 被 code=500 拦在 service；
4. 无放行权限账号看不到放行按钮；超管点放行，理由留空被拒，填「客户让步使用，批次 X」放行成功；
5. 放行记录（DB 查 qxx_qc_block_release）字段齐全；待办关闭；工序2 可报工。

- [ ] **Step 5: 回归清单**

- 既有报工：无检验工序的工单链路（报工/确认/审核/自动完工/末工序入库单生成）无变化；
- 领料门控、工序顺序校验仍生效；
- 外协工序：上游外协收货审核写报工后，下游厂内工序默认值能带出；外协节点不参与 QC 门控（被检查的是 is_check 厂内节点）；
- 分切报工两条直写路径不报 SQL 未知列错误（XML 新列均可空）；
- PC 甘特拖拽移动任务不丢派人（moveTask 更新语句不含 worker 列）；
- 自动排产出的任务 6 个人员列为 NULL，报工页报工人退化为当前登录人。

- [ ] **Step 6: 跑 crt-review 自检 skill，按三轮审查修复后收尾**

```bash
git log --oneline origin/main..HEAD   # 5 个任务提交齐全
```

---

## Self-Review 记录

- **需求覆盖**：自动带工单/工序（既有，三入口富化确认）✓ Task3；机台（既有）✓ Task1/3 展示；报工人/负责人 ✓ Task1+3；上机默认值+人工改+留痕 ✓ Task2+3；不合格硬拦（前端+绕过 API 双保险）✓ Task4；放行+权限+留痕 ✓ Task4+5；待办通知（替代异常单）✓ Task4；三条验收 ✓ Task6。
- **已知边界（计划内接受）**：并行 FS 只影响「下一波」解析，放行按 (ipqc, 任务) 粒度可分别放行；判定单为终态不可重判，故不处理 FAIL→PASS 的待办回收；分切直写报工不经门控；未判定（漏检）不拦，只拦已判 FAIL——与需求原文一致。
- **类型一致性**：`defaultQuantityInput/qcBlocked/qcBlockReason` 在 Task1 声明、Task3/4 富化、Task5 消费；`QcBlockInfo` 在 Task4 定义并被 applier 引用；release 端点 URL `/mes/pro/task/releaseQcBlock/{taskId}` + 参数 reason 在 Task4/5/App/PC 四处一致；权限点 `mes:pro:task:release` 在 V154/后端/`$auth`/v-hasPermi 四处一致。
