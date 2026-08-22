# 工单进度跟踪与统计 设计文档（一期）

- 日期：2026-08-22
- 分支：`feature/slitting-outsource`
- 领域：`pro`（生产）+ 新模块 `report`（生产统计）
- 状态：设计已评审，待生成实施计划

## 1. 背景与目标

现有系统已有工单、排产任务、流转卡、报工、甘特图排产、生产看板，但：

- 工单详情只有基本信息 + 静态 BOM/参数，**没有工序级计划与实际进度、没有时间线、没有交期风险标识**。
- 自研甘特图（`components/GanttChart`）只画计划条，**不叠加实际进度，不标识延期**。
- 生产看板（`pro/dashboard`）只有 4 个卡片 + 工单进度条 + 延迟表格，数据靠拼列表接口，**无后端聚合统计**。
- 报表/统计模块**完全空白**：无菜单、无页面、无 API（首页"报表分析"卡片指向不存在的 `/mes/report`，是死链）。
- 班组表 `qxx_cal_team` 存在，但工单/任务/报工/上下工均无 `team_id`，**无法按班组统计产能/工时/效率**。

一期目标：

1. **工单进度跟踪**：工单 → 子工单（流转卡）→ 工序任务三级，展示计划开始/结束、实际开始/结束、交期、状态、完成率。
2. **甘特图增强**：在现有排产甘特上叠加实际进度条，标识延期/临期/进度滞后风险。
3. **延期预警**：工单交期与工序任务计划结束时间的临期/延期判定，在看板与报表中呈现。
4. **统计报表**：完成率、延期率、产能、工时、效率分析；支持按日期/车间/班组筛选。

## 2. 关键产品决策（已与产品确认）

| 决策点 | 选择 | 说明 |
|---|---|---|
| 子工单层级 | **流转卡（card）作为子工单** | 不新增父子工单结构。工单 → 工序任务(task，排产/计划层) → 流转卡(card，批次/子工单实绩层)。一张流转卡对应一批/一卷的生产全程。 |
| 甘特方案 | **扩展现有甘特图叠加实际条** | 复用 `components/GanttChart`，在计划条上叠加实际进度条并按风险着色，不新建独立甘特页。 |
| 班组关联 | **报工/上下工时快照 `team_id`** | 给 `feedback`、`workrecord` 加工班快照字段，事件发生时按操作人当前所属班组写入；历史报表归属准确，人员调组不影响历史。 |
| 一期范围 | **进度详情 + 甘特叠加 + 延期预警 + 基础报表** | 父子工单结构、实时推送、每日快照、移动端看板、OEE 全套均放二期。 |
| 计算策略 | **实时聚合 + 索引** | 不引入冗余同步字段或快照表；实际时间/工时/产出走带索引的 SQL 实时聚合。 |

## 3. 现状数据模型（设计基线）

以下为真实库结构（以 Java domain + Mapper XML 为准；`ruoyi-admin/src/test/resources/sql/manual_tables.sql` 中部分表为过时 DDL，不作为依据）。

### 3.1 工单 `qxx_pro_workorder`（`ProWorkorder`）

- 编号/名称、产品、`quantity`（计划）、`quantity_produced`（已生产）、`quantity_scheduled`（已排产）。
- 时间：`request_date`（需求日期/客户交期）、`cancel_date`、`finish_date`（实际完成日期）。
- 状态 `status`：`PREPARE` 待生产 / `PRODUCING` 生产中 / `COMPLETED` 已完工 / `CANCEL` 已取消（常量见 `ProConstants`）。
- **无计划开始/结束时间、无实际开始时间、无父子工单字段、无车间/班组字段**。计划时间下钻到 task 层。

### 3.2 工序任务 `qxx_pro_task`（`ProTask`）

- 关联 `workorder_id`、`workstation_id`、`route_id`、`process_id`、`predecessor_id`（甘特前后置）、`snapshot_id`。
- 数量：`quantity`、`quantity_produced`、`quantity_qualified`、`quantity_unqualified`。
- 工时定额：`setup_duration`（调机分钟）、`unit_duration`（单件分钟）。
- 时间：`start_time`（计划开始）、`duration`（计划时长）、`end_time`（计划结束）、`request_date`（需求/交期）、`finish_date`（实际完成日期）。
- 状态：`NORMAL`/`PREPARE`/`PRODUCING`/`PAUSED`/`COMPLETED`/`CANCEL`。
- 无班组字段；车间需 `workstation_id → qxx_md_workstation.workshop_id` 派生。

### 3.3 流转卡 `qxx_pro_card` 与 `qxx_pro_card_process`

- `qxx_pro_card`：`card_code`、`workorder_id`、`task_id`、`batch_code`、`quantity_transfered`、`current_process_id`、`status`（`ACTIVE`/`COMPLETED`/`SCRAPPED`）。
- `qxx_pro_card_process`（流转卡工序实绩）：`card_id`、`seq_num`、`process_id`、`task_id`、`input_time`（进入工序时间）、`output_time`（出工序时间）、`quantity_input`、`quantity_output`、`quantity_unqualified`、`workstation_id`、`user_id`、`user_name`、`vendor_*`、`feedback_id`。
- **注意：`card_process` 表无 `workorder_id` 列**，按工单聚合实际时间须经 `task_id → qxx_pro_task.workorder_id` 或 `card_id → qxx_pro_card.workorder_id`。

### 3.4 报工 `qxx_pro_feedback`（`ProFeedback`）

- 关联 `workorder_id`、`task_id`、`card_id`、`process_id`、`workstation_id`。
- 数量：`quantity_feedback`、`quantity_qualified`、`quantity_unqualified`、`quantity_labor_scrap`（工废）、`quantity_material_scrap`（料废）、`quantity_other_scrap`。
- 报工人：`user_name`（String，**当前无 `user_id` 列**）、`nick_name`、`feedback_channel`。
- 时间：`feedback_time`。状态：`PREPARE`/`CONFIRMED`/`AUDITED`。

### 3.5 上下工 `qxx_pro_workrecord`（`ProWorkrecord`，会话模式）

- 一条记录 = 一次在岗会话：`clock_in_time`、`clock_out_time`、`work_duration`（分钟）、`status`（`ACTIVE`/`CLOSED`）。
- 关联：`user_id`、`workstation_id`、`workorder_id`、`task_id`、`process_name`。
- **人工工时的权威来源**：统计直接 `SUM(work_duration)`，仅统计 `status='CLOSED'` 的会话；`ACTIVE` 会话若跨统计周期，按 `now() - clock_in_time` 估算并标注"在岗中"。

### 3.6 组织主数据

- `qxx_md_workshop`（车间）：`workshop_id/code/name`。
- `qxx_md_workstation`（工作站）：`workstation_id/code/name`、`workshop_id`、`capacity`（个/小时）、`workstation_type`、`status`。
- `qxx_cal_team`（班组）：`team_id/code/name`、`team_type`（DAY/NIGHT/MIDDLE/ROTATION）。
- `qxx_cal_team_member`（班组成员）：`team_id`、`user_id`、`user_name`。
- 组织粒度：工厂 → 车间 → 工作站；**无产线实体**。

### 3.7 现有甘特/看板

- `ProGanttController`（`/mes/pro/gantt`）+ `GanttDataServiceImpl` 返回 `{tasks, links}`，支持工单/工作站双视角、拖拽、快照、自动排产。
- 前端 `components/GanttChart/index.vue` 为自研 div + 绝对定位甘特，支持时/日视图、拖拽、缩放、工作日标记。
- `pro/dashboard/index.vue` 用 `el-progress` + 表格，无 echarts，直接调列表接口。
- `ProTaskServiceImpl.selectProcessProgressByWorkorder` 已存在按工序聚合数量的进度接口，但前端无人调用。

## 4. 数据模型变更（Flyway V141）

新增迁移：`backend/ruoyi-admin/src/main/resources/db/migration/V141__pro_progress_report_team_snapshot.sql`

> 迁移内 INSERT 必须显式写 `factory_id`；本迁移以 DDL + 回填 DML 为主，菜单种子需显式 `factory_id`。遵循 `db-migration` 规范，不修改已执行的迁移文件。

### 4.1 DDL：班组快照 + 报工人 user_id

```sql
-- 报工：补报工人 user_id + 班组快照
ALTER TABLE qxx_pro_feedback
  ADD COLUMN user_id   BIGINT       NULL COMMENT '报工人用户ID(关联sys_user)' AFTER user_name,
  ADD COLUMN team_id   BIGINT       NULL COMMENT '班组ID(快照)',
  ADD COLUMN team_code VARCHAR(64)  NULL COMMENT '班组编码(快照)',
  ADD COLUMN team_name VARCHAR(128) NULL COMMENT '班组名称(快照)';

-- 上下工：班组快照（已有 user_id）
ALTER TABLE qxx_pro_workrecord
  ADD COLUMN team_id   BIGINT       NULL COMMENT '班组ID(快照)',
  ADD COLUMN team_code VARCHAR(64)  NULL COMMENT '班组编码(快照)',
  ADD COLUMN team_name VARCHAR(128) NULL COMMENT '班组名称(快照)';
```

### 4.2 回填历史数据

- `workrecord`：按 `user_id` 关联 `qxx_cal_team_member` 回填 `team_id/code/name`（一人多班组时按 `member_id` 升序取第一条；回填结果为历史归属尽力还原）。
- `feedback`：先按 `user_name` 关联 `sys_user.user_name` 解析 `user_id` 并回填；再按 `user_id` 关联班组成员回填班组。无法匹配的留空（老数据可能为空，统计时归"未归属"）。

```sql
-- 示意（实施时按真实列名/工厂隔离核对）
UPDATE qxx_pro_feedback f
  JOIN sys_user u ON u.user_name = f.user_name AND u.del_flag = '0'
  SET f.user_id = u.user_id
  WHERE f.user_id IS NULL;

UPDATE qxx_pro_workrecord r
  JOIN qxx_cal_team_member m ON m.user_id = r.user_id
  SET r.team_id = m.team_id, r.team_code = m.team_code, r.team_name = m.team_name
  WHERE r.team_id IS NULL;
-- feedback 同理经 user_id 关联
```

> 若依 `sys_user` 多工厂共享，班组成员表带 `factory_id`；回填需加 `m.factory_id = r.factory_id` 条件，避免串厂。

### 4.3 索引（支撑实时聚合）

```sql
-- 按任务聚合实际进出时间（进度页、甘特实际条）
CREATE INDEX idx_card_process_task_io
  ON qxx_pro_card_process(task_id, input_time, output_time);
-- 按流转卡聚合工序实绩（子工单进度）
CREATE INDEX idx_card_process_card_seq
  ON qxx_pro_card_process(card_id, seq_num);
-- 报表：报工/工时按时间 + 班组
CREATE INDEX idx_feedback_time_team
  ON qxx_pro_feedback(feedback_time, team_id);
CREATE INDEX idx_workrecord_clock_team
  ON qxx_pro_workrecord(clock_in_time, team_id);
-- 延期预警：任务按计划结束 + 状态
CREATE INDEX idx_task_end_status
  ON qxx_pro_task(end_time, status);
```

### 4.4 菜单与权限种子

在"生产管理"（`parent_id` 为生产管理目录）下新增：

| 菜单名 | path | component | 权限标识 |
|---|---|---|---|
| 生产统计 | `report` | `mes/pro/report/index` | `mes:pro:report:query` |

- 类型为菜单（C），可见；排序置于生产看板之后。
- 给 admin 角色（`role_id=1`）授予该权限；其他角色按需在系统管理分配。
- 工单"进度"按钮：新增按钮权限 `mes:pro:workorder:progress`（可不强制，一期复用 `mes:pro:workorder:query`，在实施时确认）。
- 菜单 INSERT 显式写 `factory_id`（admin 工厂 id=1，与现有种子一致；多工厂在实施时核对 `qxx_md_factory`）。

### 4.5 系统参数（sys_config）

| config_key | 默认值 | 说明 |
|---|---|---|
| `mes.progress.warnHours` | `24` | 任务临期阈值（小时）：计划结束前 N 小时未完成 |
| `mes.progress.warnDays` | `2` | 工单临期阈值（天）：交期前 N 天未完工 |
| `mes.progress.behindTolerance` | `10` | 进度滞后容差（百分点）：产出进度% 比时间流逝% 低超 N 个点判滞后 |
| `mes.progress.snapshotTeam` | `true` | 报工/上下工是否快照班组（开关，便于排查） |

## 5. 实际进度派生口径

### 5.1 实际开始/结束时间

| 层级 | 实际开始 | 实际结束 |
|---|---|---|
| 工序任务 | `MIN(card_process.input_time)` 该任务下所有流转卡工序进入时间；无则取该任务首条已确认报工 `feedback_time` | `MAX(card_process.output_time)`；无则用 `task.finish_date`（任务完成时回填） |
| 工单 | `MIN(各 task 实际开始)` | `MAX(各 task 实际结束)`，回退 `workorder.finish_date` |
| 子工单(流转卡) | `MIN(card_process.input_time)` | `MAX(card_process.output_time)` |

- 任务未开工（无 card_process 且无报工）时实际开始为 `null`，前端显示"—"。
- 计划时间：工单 = `MIN(task.start_time)` / `MAX(task.end_time)`；任务 = `task.start_time` / `task.end_time`。任务未排产时计划时间为 `null`，显示"未排产"。

### 5.2 完成率

- 工单完成率 = `quantity_produced / quantity × 100%`。
- 工序任务完成率 = `quantity_produced / task.quantity × 100%`（或用合格数，实施时统一口径；列表展示用 `quantity_produced`）。
- 流转卡完成率 = 已产出工序数（`output_time IS NOT NULL`）/ 路线总工序数 × 100%；并展示 `quantity_transfered`。

### 5.3 延期风险等级

| 等级 | 色值 | 工单判定 | 任务判定 |
|---|---|---|---|
| NORMAL 正常 | 蓝/绿 | 未到临期窗口 | 未到临期窗口 |
| WARNING 临期 | 黄 | 未完工且 `request_date` 在未来 `warnDays` 天内 | 未完成且 `end_time` 在未来 `warnHours` 小时内 |
| DELAY 延期 | 红 | 未完工/未取消且 `request_date < now()` | 未完成/未取消且 `end_time < now()` |
| FINISHED_DELAY 完工延期 | 橙 | `finish_date > request_date` | `actual_end > end_time` |
| BEHIND 进度滞后 | 紫（叠加） | — | 进行中，且 产出进度% < 时间进度% − `behindTolerance` |

- 时间进度%（任务）= `(now() - start_time) / (end_time - start_time) × 100%`，钳制在 0–100%。
- "延期"优先级高于"临期"；"完工延期"用于已完工对象；"进度滞后"可与进行中状态叠加显示。
- 取消（CANCEL）的工单/任务不参与延期判定，显示中性灰。

## 6. 后端设计

### 6.1 新增包与类

包名沿用现有 `com.ruoyi.system.domain.mes.pro`（项目实际基础包为 `com.ruoyi`）。Controller 在 `com.ruoyi.web.controller.mes.pro`。

| 类 | 职责 |
|---|---|
| `ProProgressController` (`/mes/pro/progress`) | 工单进度详情、延期列表 |
| `ProReportController` (`/mes/pro/report`) | 统计报表聚合接口 |
| `IProProgressService` / `ProProgressServiceImpl` | 进度组装、延期判定 |
| `IProReportService` / `ProReportServiceImpl` | KPI/产能/工时/效率/趋势聚合 |
| `TeamResolver`（`com.ruoyi.system.service.mes.pro`） | 按 `userId`/`userName` 解析当前班组，供报工/上下工写入快照。一期假设一人主属一个班组，多归属时按 `member_id` 升序取第一条，记 warn 日志。 |
| `DelayLevelEvaluator` | 纯函数：输入计划/实际/状态/数量 → 风险等级（便于单测） |
| VO：`WorkorderProgressVO`、`ProcessProgressRowVO`、`CardSuborderVO`、`DelayItemVO`、`ReportOverviewVO`、`ProductivityRowVO`、`TrendPointVO`、`TaskStatusDistVO` | 接口返回结构 |
| Mapper：`ProProgressMapper` + XML | 批量聚合实际时间/产出，避免 N+1 |

### 6.2 接口清单

#### 工单进度

- `GET /mes/pro/progress/{workorderId}` → `WorkorderProgressVO`
  - `summary`：工单基本信息、计划开始/结束、实际开始/结束、交期、状态、完成率、风险等级。
  - `processes[]`：每个工序任务（task）的计划/实际时间、数量（计划/产出/合格/工废/料废）、完成率、风险等级、工作站。
  - `cards[]`：子工单（流转卡）卡号、批次、数量、当前工序、工序进度、实际进/出、状态。
- `GET /mes/pro/progress/delayList` → 分页
  - 参数：`objectType`（WORKORDER/TASK）、`riskLevel`、`workshopId`、`teamId`、`workstationId`、`keyword`、`pageNum/pageSize`。
  - 返回 `DelayItemVO`：对象类型、编号、名称、产品、工作站/车间、班组、计划结束/交期、实际结束、延期天数、风险等级、进度%。

#### 统计报表

- `GET /mes/pro/report/overview` → `ReportOverviewVO`
  - 参数：日期范围、workshopIds、teamIds、workstationId、processId。
  - 返回 KPI：工单数、完成数、完成率、延期数、延期率、在制数、标准工时、实际工时、工时效率。
- `GET /mes/pro/report/productivity` → `List<ProductivityRowVO>`
  - 参数：`groupBy`（WORKSHOP/TEAM/WORKSTATION/PROCESS）、日期范围、车间/班组筛选。
  - 每行：分组 id/名称、工单数、完成数、延期数、完成率、延期率、标准产出、实际合格产出、标准工时、实际工时、工时效率、合格数、报废数、合格率。
- `GET /mes/pro/report/trend` → `List<TrendPointVO>`
  - 参数：日期范围、粒度（DAY，默认按日）、车间/班组。
  - 返回每日：新建工单数、完工数、延期数、报工合格数、实际工时。
- `GET /mes/pro/report/taskStatus` → `List<TaskStatusDistVO>`
  - 参数：日期范围、车间/班组。
  - 返回任务状态分布（待排产/生产中/暂停/完成/取消）数量。
- `GET /mes/pro/report/detail` → 分页明细
  - 按工单维度返回：工单编号、产品、车间/班组、计划/实际时间、交期、完成率、风险、合格/报废、工时；支持车间→班组下钻（传 `parentGroupId`）。

#### 甘特接口扩展（不新增路由）

扩展 `GanttDataServiceImpl` 返回的 task 对象，新增字段：

- `actualStartTime`、`actualEndTime`
- `progressPercent`（`quantity_produced/quantity`）
- `delayLevel`（NORMAL/WARNING/DELAY/FINISHED_DELAY/BEHIND）
- `behindSchedule`（boolean）

实现方式：在 `buildWorkOrderGantt` / `buildWorkstationGantt` 中，**一次性**按本视图涉及的 task_id 批量查 `card_process` 聚合（`ProProgressMapper.aggregateActualByTaskIds`），回填到每个 task；不得对每个 task 单独查库（禁止 N+1）。

### 6.3 指标口径

- **标准工时**（分钟）= Σ(`task.setup_duration` + `task.unit_duration × quantity_produced`)，按筛选范围内有产出的任务聚合。
- **实际工时**（分钟）= Σ(`workrecord.work_duration`，`status='CLOSED'`，`clock_in_time` 落在区间内)；`ACTIVE` 会话按 `now() - clock_in_time` 估算并在明细中标注。
- **工时效率** = 标准工时 / 实际工时 × 100%。实际工时为 0 或 null 时返回 `null`，前端显示"—"。
- **标准产出/产能**（个）= `workstation.capacity`（个/小时）× 实际出勤工时（小时）。`capacity` 为空或 0 的工作站不计入产能对比，在产能图表中归为"未配置产能"。
- **合格率** = 合格 / (合格 + 工废 + 料废) × 100%；分母为 0 时 `null`。
- **完成率（统计口径）** = 完成数（COMPLETED）/ 工单数（不含 CANCEL）× 100%。
- **延期率** = 延期 + 完工延期数 / 工单数（不含 CANCEL）× 100%。

### 6.4 工厂隔离与锁

- 所有查询 SQL 必须带 `factory_id` 的 `<if>` 条件（MyBatis 拦截器注入参数值），聚合 JOIN 表也要带工厂条件，禁止跨厂数据泄露。单测必须覆盖"A 厂看不到 B 厂数据"。
- 本模块以读为主，**不加分布式锁**。
- `TeamResolver` 解析班组为只读查询，不加锁。
- 报工/上下工写入 team 快照发生在现有写事务内，不新增锁路径；快照失败不得阻断主流程（解析不到班组时字段留空，记 warn 日志）。

### 6.5 班组快照写入点

- **报工确认**（`ProFeedbackServiceImpl` 确认/审核路径）：调用 `TeamResolver.resolveByUserId(userId)`，写入 `team_id/code/name`。新增报工时若请求带 `userId` 一并落库（V141 新增列）。
- **外协代填报工**（`feedback_type=OUTSOURCE_AGENT`）：记录人为内部员工，按录单人 `record_user` 解析班组。
- **上下工**（`ProWorkrecordServiceImpl` 上工）：上工 INSERT 时按 `userId` 解析并写入；下工结算时不变更班组（保持上工时快照）。

## 7. 前端设计

### 7.1 工单进度页

新页面：`frontend/src/views/mes/pro/workorder/progress.vue`，从工单列表行内"进度"按钮以**全屏 el-dialog** 打开（与现有工单详情弹窗模式一致，不新增路由、不加菜单，避免动态路由依赖）。组件内部用 `el-tabs` 切换三个视图，关闭弹窗时销毁内部甘特实例（避免 echarts/甘特 DOM 泄漏）。

组件拆分（每个 ≤300 行）：

- `progress.vue`：容器，拉取数据、布局、Tab 切换。
- `components/WorkorderProgressHeader.vue`：工单头部 + 4 摘要卡（计划周期/实际周期/交期/完成率）+ 状态/延期标签。
- `components/ProcessProgressTable.vue`：工序进度表，列含工序、计划/实际开始结束、数量（计划/产出/合格/报废）、进度条、状态、风险标签；行可展开看该工序下流转卡。
- `components/CardSuborderTable.vue`：子工单（流转卡）表，列含卡号、批次、数量、当前工序、工序进度（如 3/5）、实际进/出、状态；点击展开该卡工序时间线（复用 card_process 数据，展示 input/output/操作人/工作站）。
- 甘特 Tab：内嵌只读 `GanttChart`，传 `:readonly="true"`、`:showActual="true"`，仅本工单数据。

新 API：`frontend/src/api/mes/pro/progress.ts`（`getWorkorderProgress`、`listDelay`）。

### 7.2 甘特图增强

`frontend/src/components/GanttChart/index.vue`：

- 任务条改为双层：底层计划条（原色），上层实际条（深色半透明，`actualStartTime→actualEndTime`）。
- 进行中任务（无 actualEndTime）实际条画到 `now` 位置或当前视口右端，用斜线纹理填充表示"进行中"。
- 任务条按 `delayLevel` 加左边框/角标颜色：NORMAL 蓝、WARNING 黄、DELAY 红、FINISHED_DELAY 橙、BEHIND 紫。
- 任务行左侧加小型进度条（`progressPercent`）。
- 工具栏新增 switch："显示实际进度"（默认开）；关闭后只渲染计划条，回退现状。
- 新增 props：`showActual`（Boolean，默认 true）、`readonly`（Boolean，复用现有只读控制）。
- **不改动拖拽排产、快照、自动排产逻辑**；只读模式下禁用拖拽。

`frontend/src/views/mes/pro/gantt/index.vue`：透传后端新字段到 GanttChart；加"显示实际进度"开关。

### 7.3 生产看板增强（`pro/dashboard/index.vue`）

- 顶部卡片由 4 个增至 5 个，增加"延期工单数"（红/橙强调）。
- 右侧"延迟预警"表格升级为 Tab 切换：工单 / 工序任务；列增加延期天数、风险等级、进度%；点击行跳转工单进度页或甘特页并定位。
- 数据改调新聚合接口（`/progress/delayList`），不再拼列表接口。
- 保留 30 秒自动刷新。

### 7.4 统计报表页

新页面：`frontend/src/views/mes/pro/report/index.vue`（由新菜单"生产统计"进入）。

布局：

1. **筛选栏**：日期范围（默认本月，`el-date-picker` daterange）、车间多选（`workshopSelect`，无现有封装则用 el-select multiple）、班组多选、工作站、工序；查询/重置按钮。
2. **KPI 卡片区**：工单数、完成数、完成率、延期数、延期率、在制数、实际工时、工时效率；数值带同比/环比不做（一期只显示当期值）。
3. **图表区**（echarts 5.6.0，已安装）：
   - 工单趋势（折线/柱状：新建/完工/延期，按日）
   - 任务状态分布（饼图）
   - 车间/班组产能对比（柱状：标准产出 vs 实际合格产出）
   - 工时分析（堆叠柱：标准工时 vs 实际工时）
   - 效率排名（横向柱：班组/工作站工时效率%）
4. **明细表**：`el-table` 按分组（车间/班组/工作站/工序）展示指标，支持车间→班组下钻（点击行展开），列含完成率、延期率、工时效率、合格率；支持导出 Excel（复用若依导出）。
5. **延期预警区块**：嵌入 `delayList` 表格（分页），与看板共用接口，支持风险等级筛选。

新 API：`frontend/src/api/mes/pro/report.ts`（`getOverview`、`getProductivity`、`getTrend`、`getTaskStatus`、`getDetail`）。

### 7.5 公共图表组件

新增 `frontend/src/components/Charts/`：

- `BaseChart.vue`：封装 echarts `init`、`setOption`、窗口 resize、`unmounted` 销毁，接收 `option` prop。
- 按需导出 `BarChart.vue`、`LineChart.vue`、`PieChart.vue` 薄封装，避免每个页面重复 init/dispose 逻辑（现有 `monitor/cache` 页面内联 echarts 用法可作为参考，但新组件统一封装）。

### 7.6 状态与风险标签

风险等级用 `el-tag` 统一颜色映射，在 `utils/tags.ts` 或报表页局部常量定义：

- NORMAL `info`/primary、WARNING `warning`、DELAY `danger`、FINISHED_DELAY `danger`（plain）、BEHIND `warning`（plain）。
- 工单/任务状态字典若已配置则用 `dict-tag`，否则用本地 statusMap（沿用现有页面做法）。

## 8. 测试策略

遵循 `docs/设计文档/测试约定.md` 与项目三层测试。

### 8.1 后端单元测试

- `DelayLevelEvaluator` 参数化测试：未到期/临期/延期、已完工按时/完工延期、进度滞后（含容差边界：正好差 10 个点不判滞后）、取消状态不判延期、无计划时间（未排产）不判延期。
- `ProProgressServiceImpl`：
  - 有 card_process 时实际开始=MIN(input_time)、实际结束=MAX(output_time)。
  - 无 card_process 回退 feedback.feedback_time / task.finish_date。
  - 部分工序未开始、工单未排产。
  - 批量聚合无 N+1（可用 Mapper mock 验证只调一次）。
- `ProReportServiceImpl`：
  - 标准工时 = setup + unit×produced；实际工时 = SUM(work_duration)。
  - 实际工时为 0 时效率返回 null。
  - ACTIVE 会话跨区间估算。
  - 产能：capacity 为空时归"未配置"，不产生除零。
  - 合格率分母为 0 返回 null。
- `TeamResolver`：用户无班组、用户多班组、按 userName 解析、factory 隔离。
- **工厂隔离**：所有聚合接口单测构造两厂数据，断言 A 厂查询不含 B 厂数据。
- 班组快照写入：报工确认、外协代填、上下工三条路径均断言 team 字段被写入；解析失败不抛异常阻断主流程。

### 8.2 前端测试/验证

- 进度页：工单摘要、工序表展开子工单、空数据态、未排产显示"—"、风险标签颜色。
- 甘特：`showActual` 开关切换、实际条渲染、只读模式不可拖拽（手测）。
- 报表：筛选栏联动查询、KPI/图表渲染、明细下钻、导出。
- 看板：延期 Tab 切换、行跳转。

### 8.3 端到端验证红线（不可跳过）

后端改动后必须：

1. `cd backend && mvn -pl ruoyi-admin -am package -DskipTests`
2. 重启运行中的 `ruoyi-admin.jar`，等 `/captchaImage` 返回 200。
3. 用 token 实测：`/mes/pro/progress/{id}`、`/progress/delayList`、`/report/overview`、`/report/productivity`、`/report/trend`、`/report/taskStatus`、`/report/detail`、甘特接口新字段，看到数据符合预期。
4. 前端在浏览器实际点一次：工单列表→进度页、甘特实际条开关、报表筛选/图表/导出、看板延期 Tab。

未完成上述验证不得宣称"完成"。

## 9. 一期范围边界

**做：** 工单进度详情（工单/子工单/工序三级）、甘特叠加实际条与延期着色、延期预警（看板+报表内）、生产统计报表（KPI+5 图表+明细下钻+导出）、班组快照与按班组统计、echarts 公共图表组件。

**不做（二期）：**

- 父子工单层级结构（子工单用流转卡承载，不加 `parent_workorder_id`）。
- 实时推送/站内信/邮件/短信预警。
- 每日快照表、数据仓库、历史趋势预聚合。
- 移动端（app）进度看板与统计。
- OEE 全套（可用率/性能/质量三维）；一期只做"工时效率"和"合格率"两个轻量指标。
- 产能基线维护页面（直接用 `workstation.capacity`，未配置则不计入产能对比）。
- 交期/排产自动重算建议、What-if 模拟。

## 10. 风险与注意事项

- **`manual_tables.sql` 过时**：`workrecord` 测试 DDL 是旧的事件流模式，真实库为会话模式（含 `clock_in_time/clock_out_time/work_duration/workorder_id`）。实施与编写测试时以 Java domain + Mapper XML 及真实库为准，必要时更新测试 SQL。
- **`feedback` 无 `user_id`**：V141 补列；新报工写入路径需透传当前登录用户 id。历史数据按 `user_name` 回填，可能存在改名/重名导致少量无法匹配，接受为空。
- **card_process 无 workorder_id**：按工单聚合必须 join `task` 或 `card`，SQL 注意 join 路径与 factory_id 条件。
- **`ProRouteProcess.process_type` 列 Java/XML 未映射**（探索发现）：若报表按工序类型（自制/外协/分切）筛选统计，需在实施时补齐映射；一期工序筛选按 `process_id` 即可，process_type 不作为筛选维度，避免扩大范围。
- **性能**：实时聚合依赖第 4.3 节索引；若后续单厂工单/报工量达十万级且报表慢，再评估二期加每日快照表。
- **函数长度**：后端 Service 方法 ≤ 50 行，组装逻辑拆私有方法；前端组件 ≤ 300 行，超出按第 7 节拆分。
