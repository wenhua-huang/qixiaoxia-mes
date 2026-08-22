# 工单进度跟踪与统计 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建工单进度跟踪（工单→子工单流转卡→工序任务三级，计划vs实际/交期/完成率）、甘特图叠加实际进度条与延期着色、延期预警、生产统计报表（完成率/延期率/产能/工时/效率，按日期/车间/班组筛选）。

**Architecture:** 实时聚合方案——实际开始/结束时间由 `qxx_pro_card_process.input_time/output_time` 聚合，工时由 `qxx_pro_workrecord.work_duration` 聚合，不加冗余同步字段。班组用快照方式写入 `feedback.team_id`/`workrecord.team_id`。后端新增 ProProgressController/ProReportController + 纯函数 DelayLevelEvaluator（便于单测）。前端新增工单进度全屏 dialog、增强 GanttChart 组件双层条、增强 dashboard、新建 report 报表页 + Charts 公共组件。

**Tech Stack:** Spring Boot 4.0.3 + JDK 17 + MyBatis（包名 com.ruoyi）；JUnit5 + Mockito + AssertJ（纯 Mockito 单测，不启动 Spring）；Vue 3.5 + TS 5.6 + Element Plus 2.13 + ECharts 5.6.0；Flyway 迁移当前最新 V140，本计划新增 V141。

## Global Constraints

- 所有 SQL 查 `qxx_*` 表由 FactoryIdInterceptor 自动注入 `factory_id`，**XML 中不手写 factory_id 条件**；多参数 `@Param` 方法也会被拦截器改 SQL 字符串注入。仅跨厂查询才在 Mapper 方法加 `@SkipFactoryId`。
- Flyway 迁移内 INSERT 必须显式写 `factory_id`（裸 JDBC 不走拦截器）；业务 INSERT 不写 factory_id（拦截器注入）。
- 后端函数 ≤ 50 行，前端组件 ≤ 300 行，超出按任务拆分。
- 业务状态用 ProConstants 常量（报工状态 PREPARE/CONFIRMED/AUDITED 目前是裸字符串，本计划在用到处补常量）。
- 菜单 ID 使用 2320–2329 段（已确认未占用），`INSERT IGNORE` + `sysdate()`，授权用 `INSERT IGNORE INTO sys_role_menu SELECT ...`。
- 后端改完必须 `mvn -pl ruoyi-admin -am package -DskipTests` + 重启 jar + token 实测接口；前端改完必须浏览器实际点一次。
- 不要改 `ruoyi-admin/src/test/resources/sql/manual_tables.sql`（该文件过时，真实表以 Java domain/Mapper XML 为准）。

---

## File Structure

### 后端新建
- `ruoyi-system/.../domain/mes/pro/DelayLevel.java` — 风险等级枚举
- `ruoyi-system/.../domain/mes/pro/TaskActual.java` — 任务实际时间聚合 PO（taskId, actualStartTime, actualEndTime, quantityOutput）
- `ruoyi-system/.../domain/mes/pro/vo/WorkorderProgressVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/ProcessProgressRowVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/CardSuborderVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/DelayItemVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/ReportOverviewVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/ProductivityRowVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/TrendPointVO.java`
- `ruoyi-system/.../domain/mes/pro/vo/TaskStatusDistVO.java`
- `ruoyi-system/.../service/mes/pro/DelayLevelEvaluator.java` — 纯函数
- `ruoyi-system/.../service/mes/pro/TeamResolver.java`
- `ruoyi-system/.../service/mes/pro/IProProgressService.java` + `impl/ProProgressServiceImpl.java`
- `ruoyi-system/.../service/mes/pro/IProReportService.java` + `impl/ProReportServiceImpl.java`
- `ruoyi-system/.../mapper/mes/pro/ProProgressMapper.java` + `resources/mapper/mes/pro/ProProgressMapper.xml`
- `ruoyi-admin/.../controller/mes/pro/ProProgressController.java`
- `ruoyi-admin/.../controller/mes/pro/ProReportController.java`

### 后端修改
- `domain/mes/pro/ProConstants.java` — 补报工状态、风险等级、sys_config key 常量
- `domain/mes/pro/ProFeedback.java` + `ProFeedbackMapper.xml` — 加 userId/teamId/teamCode/teamName
- `domain/mes/pro/ProWorkrecord.java` + `ProWorkrecordMapper.xml` — 加 teamId/teamCode/teamName
- `service/mes/pro/impl/ProFeedbackServiceImpl.java` — 三处写 team 快照
- `service/mes/pro/impl/ProWorkrecordServiceImpl.java` — 上工写 team 快照
- `service/mes/pro/impl/GanttDataServiceImpl.java` — 批量注入实际进度字段
- `resources/db/migration/V141__pro_progress_report.sql`

### 前端新建
- `src/api/mes/pro/progress.ts`
- `src/api/mes/pro/report.ts`
- `src/views/mes/pro/workorder/progress.vue`
- `src/views/mes/pro/workorder/components/ProcessProgressTable.vue`
- `src/views/mes/pro/workorder/components/CardSuborderTable.vue`
- `src/views/mes/pro/report/index.vue`
- `src/components/Charts/BaseChart.vue`
- `src/components/Charts/BarChart.vue`、`LineChart.vue`、`PieChart.vue`

### 前端修改
- `src/types/api/mes/pro/gantt.ts` — 加 actualStartTime/actualEndTime/progressPercent/delayLevel/behindSchedule
- `src/components/GanttChart/index.vue` — showActual/readonly props + 双层条 + 风险色 + 进度
- `src/views/mes/pro/gantt/index.vue` — 透传字段 + 开关
- `src/views/mes/pro/dashboard/index.vue` — 延期预警 Tab + 新接口
- `src/views/mes/pro/workorder/index.vue` — "进度"按钮 + dialog


---

## Task 1: Flyway V141 — 班组快照字段 + 索引 + 菜单 + 配置

**Files:**
- Create: `backend/ruoyi-admin/src/main/resources/db/migration/V141__pro_progress_report.sql`

**Interfaces:**
- Produces: DB 列 `qxx_pro_feedback.user_id/team_id/team_code/team_name`、`qxx_pro_workrecord.team_id/team_code/team_name`；菜单 `menu_id=2320`（生产统计），权限 `mes:pro:report:query`；sys_config 4 个阈值键。

- [ ] **Step 1: 写迁移文件**

```sql
-- V141: 工单进度跟踪与统计 —— 班组快照/索引/菜单/配置

-- 1. 报工：补报工人 user_id + 班组快照
ALTER TABLE qxx_pro_feedback
  ADD COLUMN user_id   BIGINT       NULL COMMENT '报工人用户ID(关联sys_user)' AFTER user_name,
  ADD COLUMN team_id   BIGINT       NULL COMMENT '班组ID(快照)' AFTER user_id,
  ADD COLUMN team_code VARCHAR(64)  NULL COMMENT '班组编码(快照)' AFTER team_id,
  ADD COLUMN team_name VARCHAR(128) NULL COMMENT '班组名称(快照)' AFTER team_code;

-- 2. 上下工：班组快照（已有 user_id）
ALTER TABLE qxx_pro_workrecord
  ADD COLUMN team_id   BIGINT       NULL COMMENT '班组ID(快照)' AFTER task_code,
  ADD COLUMN team_code VARCHAR(64)  NULL COMMENT '班组编码(快照)' AFTER team_id,
  ADD COLUMN team_name VARCHAR(128) NULL COMMENT '班组名称(快照)' AFTER team_code;

-- 3. 历史回填：feedback.user_id 按 user_name 解析
UPDATE qxx_pro_feedback f
  JOIN sys_user u ON u.user_name = f.user_name AND u.del_flag = '0'
  SET f.user_id = u.user_id
  WHERE f.user_id IS NULL;

-- 4. 历史回填：feedback 班组（按 user_id + 同厂班组成员，取最早一条）
UPDATE qxx_pro_feedback f
  JOIN (
      SELECT m.user_id, m.factory_id, MIN(m.member_id) AS mid
      FROM qxx_cal_team_member m GROUP BY m.user_id, m.factory_id
  ) pick ON pick.user_id = f.user_id AND pick.factory_id = f.factory_id
  JOIN qxx_cal_team_member m ON m.member_id = pick.mid
  SET f.team_id = m.team_id, f.team_code = m.team_code, f.team_name = m.team_name
  WHERE f.team_id IS NULL;

-- 5. 历史回填：workrecord 班组
UPDATE qxx_pro_workrecord r
  JOIN (
      SELECT m.user_id, m.factory_id, MIN(m.member_id) AS mid
      FROM qxx_cal_team_member m GROUP BY m.user_id, m.factory_id
  ) pick ON pick.user_id = r.user_id AND pick.factory_id = r.factory_id
  JOIN qxx_cal_team_member m ON m.member_id = pick.mid
  SET r.team_id = m.team_id, r.team_code = m.team_code, r.team_name = m.team_name
  WHERE r.team_id IS NULL;

-- 6. 索引
CREATE INDEX idx_card_process_task_io ON qxx_pro_card_process(task_id, input_time, output_time);
CREATE INDEX idx_card_process_card_seq ON qxx_pro_card_process(card_id, seq_num);
CREATE INDEX idx_feedback_time_team     ON qxx_pro_feedback(feedback_time, team_id);
CREATE INDEX idx_workrecord_clock_team  ON qxx_pro_workrecord(clock_in_time, team_id);
CREATE INDEX idx_task_end_status        ON qxx_pro_task(end_time, status);

-- 7. 菜单：生产统计（挂在生产管理 parent_id=2003 下，排在甘特 2311/换型 2312 之后）
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2320, '生产统计', 2003, 8, 'report', 'mes/pro/report/index', 1, 0, 'C', '0', '0', 'mes:pro:report:query', 'chart', 'admin', sysdate());

-- 8. 系统参数
INSERT IGNORE INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) VALUES
('进度临期阈值(小时)', 'mes.progress.warnHours', '24', 'Y', 'admin', sysdate(), '任务计划结束前N小时未完成判临期'),
('工单临期阈值(天)',   'mes.progress.warnDays',  '2',  'Y', 'admin', sysdate(), '工单交期前N天未完工判临期'),
('进度滞后容差(百分点)','mes.progress.behindTolerance','10','Y','admin', sysdate(), '产出进度比时间进度低超N个点判滞后'),
('班组快照开关',       'mes.progress.snapshotTeam', 'true', 'Y', 'admin', sysdate(), '报工/上下工是否快照班组');

-- 9. 授权 admin 角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id = 2320;
```

- [ ] **Step 2: 验证迁移可执行（本地有跑库时）**

Run: `cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q && java -jar ruoyi-admin/target/ruoyi-admin.jar &` 等启动后 `curl -s http://localhost:8081/captchaImage`；检查日志无 Flyway 报错。若本地未跑 MySQL，至少确认 SQL 语法（`mysql --help` 不可用时跳过本步，在集成环境验证）。

- [ ] **Step 3: 提交**

```bash
git add backend/ruoyi-admin/src/main/resources/db/migration/V141__pro_progress_report.sql
git commit -m "feat(pro): V141 班组快照字段+索引+生产统计菜单+进度阈值配置"
```

---

## Task 2: ProConstants 补常量 + TeamResolver 班组解析

**Files:**
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProConstants.java`
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/TeamResolver.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/TeamResolverTest.java`

**Interfaces:**
- Produces: `TeamResolver.resolveByUserId(Long userId)` → `TeamSnapshot`（record，含 teamId/teamCode/teamName，可能为空字段）；`TeamResolver.resolveByUserName(String userName)` → `TeamSnapshot`。
- Consumes: `CalTeamMemberMapper.selectCalTeamMemberList(CalTeamMember query)`（已存在）。

- [ ] **Step 1: 写失败的测试**

```java
package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.cal.CalTeamMember;
import com.ruoyi.system.mapper.mes.cal.CalTeamMemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamResolverTest {

    @Mock private CalTeamMemberMapper teamMemberMapper;
    @InjectMocks private TeamResolver teamResolver;

    @Test
    @DisplayName("resolveByUserId: 有班组时返回 teamId/code/name")
    void should_return_team_when_member_exists() {
        CalTeamMember m = new CalTeamMember();
        m.setTeamId(10L); m.setTeamCode("DAY"); m.setTeamName("甲班");
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of(m));

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(7L);

        assertThat(snap.teamId()).isEqualTo(10L);
        assertThat(snap.teamCode()).isEqualTo("DAY");
        assertThat(snap.teamName()).isEqualTo("甲班");
    }

    @Test
    @DisplayName("resolveByUserId: 无班组返回空快照（字段为null，不抛异常）")
    void should_return_empty_when_no_team() {
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of());

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(7L);

        assertThat(snap.teamId()).isNull();
        assertThat(snap.teamCode()).isNull();
        assertThat(snap.teamName()).isNull();
    }

    @Test
    @DisplayName("resolveByUserId: 多班组取 memberId 最小的一条（order by member_id）")
    void should_return_first_when_multiple_teams() {
        CalTeamMember a = new CalTeamMember(); a.setMemberId(2L); a.setTeamId(20L); a.setTeamCode("B");
        CalTeamMember b = new CalTeamMember(); b.setMemberId(1L); b.setTeamId(30L); b.setTeamCode("A");
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of(a, b));

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(7L);

        assertThat(snap.teamId()).isEqualTo(30L);
        assertThat(snap.teamCode()).isEqualTo("A");
    }

    @Test
    @DisplayName("resolveByUserName: 入参为null/空返回空快照，不查库")
    void should_return_empty_for_blank_username() {
        assertThat(teamResolver.resolveByUserName(null).teamId()).isNull();
        assertThat(teamResolver.resolveByUserName("").teamId()).isNull();
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd backend && mvn -pl ruoyi-system -am test -Dtest=TeamResolverTest -q`
Expected: 编译失败（TeamResolver 不存在）。

- [ ] **Step 3: 补 ProConstants 常量**

在 `ProConstants.java` 末尾（`WS_CODE_AUTO` 之后）追加：

```java
    /** 报工状态 */
    public static final String FEEDBACK_STATUS_PREPARE   = "PREPARE";
    public static final String FEEDBACK_STATUS_CONFIRMED = "CONFIRMED";
    public static final String FEEDBACK_STATUS_AUDITED   = "AUDITED";

    /** 进度风险等级 */
    public static final String DELAY_NORMAL         = "NORMAL";
    public static final String DELAY_WARNING        = "WARNING";
    public static final String DELAY_OVERDUE        = "DELAY";
    public static final String DELAY_FINISHED_LATE  = "FINISHED_DELAY";
    public static final String DELAY_BEHIND         = "BEHIND";

    /** 进度阈值 sys_config key */
    public static final String CFG_WARN_HOURS        = "mes.progress.warnHours";
    public static final String CFG_WARN_DAYS         = "mes.progress.warnDays";
    public static final String CFG_BEHIND_TOLERANCE  = "mes.progress.behindTolerance";
```

- [ ] **Step 4: 写 TeamResolver**

```java
package com.ruoyi.system.service.mes.pro;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.cal.CalTeamMember;
import com.ruoyi.system.mapper.mes.cal.CalTeamMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 班组解析器：按用户反查当前所属班组，供报工/上下工快照。
 * 一期假设一人主属一个班组；多归属时取 member_id 最小的一条。
 */
@Service
public class TeamResolver {

    @Autowired
    private CalTeamMemberMapper teamMemberMapper;

    /** 班组快照（不可为空对象，字段可能为 null） */
    public record TeamSnapshot(Long teamId, String teamCode, String teamName) {
        public static TeamSnapshot empty() { return new TeamSnapshot(null, null, null); }
    }

    public TeamSnapshot resolveByUserId(Long userId) {
        if (userId == null) return TeamSnapshot.empty();
        CalTeamMember q = new CalTeamMember();
        q.setUserId(userId);
        List<CalTeamMember> list = teamMemberMapper.selectCalTeamMemberList(q);
        return list.stream()
            .min(Comparator.comparing(m -> m.getMemberId() == null ? Long.MAX_VALUE : m.getMemberId()))
            .map(m -> new TeamSnapshot(m.getTeamId(), m.getTeamCode(), m.getTeamName()))
            .orElseGet(TeamSnapshot::empty);
    }

    public TeamSnapshot resolveByUserName(String userName) {
        if (StringUtils.isEmpty(userName)) return TeamSnapshot.empty();
        CalTeamMember q = new CalTeamMember();
        q.setUserName(userName);
        List<CalTeamMember> list = teamMemberMapper.selectCalTeamMemberList(q);
        return list.stream()
            .min(Comparator.comparing(m -> m.getMemberId() == null ? Long.MAX_VALUE : m.getMemberId()))
            .map(m -> new TeamSnapshot(m.getTeamId(), m.getTeamCode(), m.getTeamName()))
            .orElseGet(TeamSnapshot::empty);
    }
}
```

> 注：`CalTeamMemberMapper.selectCalTeamMemberList` 默认 SQL 是否按 `user_name` 过滤需确认；若 XML 里没有 `user_name` 的 `<if>`，需要在 `CalTeamMemberMapper.xml` 的 selectCalTeamMemberList 的 `<where>` 中补一行 `<if test="userName != null and userName != ''"> and user_name = #{userName}</if>`。实施时打开该 XML 核对，没有就补。

- [ ] **Step 5: 运行测试，确认通过**

Run: `cd backend && mvn -pl ruoyi-system -am test -Dtest=TeamResolverTest -q`
Expected: BUILD SUCCESS，4 个测试通过。

- [ ] **Step 6: 提交**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProConstants.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/TeamResolver.java \
        backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/TeamResolverTest.java
git commit -m "feat(pro): TeamResolver 班组解析 + ProConstants 补报工/延期常量"
```

---

## Task 3: feedback/workrecord 实体与 Mapper 加班组字段 + 写入快照

**Files:**
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProFeedback.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProFeedbackMapper.xml`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProWorkrecord.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProWorkrecordMapper.xml`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProFeedbackServiceImpl.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProWorkrecordServiceImpl.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/TeamSnapshotWriteTest.java`

**Interfaces:**
- Consumes: `TeamResolver.resolveByUserId/resolveByUserName`（Task 2）。
- Produces: 报工/上下工写入时 `teamId/teamCode/teamName` 被填充；后续统计可按 team_id 聚合。

- [ ] **Step 1: ProFeedback.java 加字段**

在 `private String userName;`（约 L66）后追加，并加 getter/setter（注意 V141 新增了 `user_id` 列，一并补 `userId`）：

```java
    @Excel(name = "报工人ID") private Long userId;
    /** 班组ID(快照) */ private Long teamId;
    /** 班组编码(快照) */ private String teamCode;
    /** 班组名称(快照) */ private String teamName;
```

在 getter/setter 区追加（紧邻 userName 的 getter/setter）：

```java
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
```

- [ ] **Step 2: ProFeedbackMapper.xml 加映射**

resultMap（L49 `userName` 行之后）加：

```xml
        <result property="userId"    column="user_id"    />
        <result property="teamId"    column="team_id"    />
        <result property="teamCode"  column="team_code"  />
        <result property="teamName"  column="team_name"  />
```

`selectProFeedbackVo`（L66）的列清单中 `user_name, nick_name,` 后加 `user_id, team_id, team_code, team_name,`。

insert 语句（L165-166 `userName/nickName` 的 `<if>` 附近）加：

```xml
            <if test="userId != null">user_id,</if>
            <if test="teamId != null">team_id,</if>
            <if test="teamCode != null and teamCode != ''">team_code,</if>
            <if test="teamName != null and teamName != ''">team_name,</if>
```

在对应 values 区（紧邻 `<if test="nickName...">` 的 values 项）加：

```xml
            <if test="userId != null">#{userId},</if>
            <if test="teamId != null">#{teamId},</if>
            <if test="teamCode != null and teamCode != ''">#{teamCode},</if>
            <if test="teamName != null and teamName != ''">#{teamName},</if>
```

update 语句（L271-272 附近）加：

```xml
            <if test="userId != null">user_id = #{userId},</if>
            <if test="teamId != null">team_id = #{teamId},</if>
            <if test="teamCode != null and teamCode != ''">team_code = #{teamCode},</if>
            <if test="teamName != null and teamName != ''">team_name = #{teamName},</if>
```

- [ ] **Step 3: ProWorkrecord.java 加字段**

在 `private String processName;` 后加：

```java
    /** 班组ID(快照) */ private Long teamId;
    private String teamCode;
    private String teamName;
```

加 getter/setter：

```java
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
```

- [ ] **Step 4: ProWorkrecordMapper.xml 加映射**

resultMap 中加：

```xml
        <result property="teamId"    column="team_id"    />
        <result property="teamCode"  column="team_code"  />
        <result property="teamName"  column="team_name"  />
```

`selectProWorkrecordVo` 列清单加 `team_id, team_code, team_name,`。

insert 列与 values 加（与其他字段同款 `<if>`）：

```xml
            <if test="teamId != null">team_id,</if>
            <if test="teamCode != null and teamCode != ''">team_code,</if>
            <if test="teamName != null and teamName != ''">team_name,</if>
```
```xml
            <if test="teamId != null">#{teamId},</if>
            <if test="teamCode != null and teamCode != ''">#{teamCode},</if>
            <if test="teamName != null and teamName != ''">#{teamName},</if>
```

`closeSession`（下工结算）**不需要**改——team 在上工时已固化，下工只更 clock_out/work_duration/status。
`updateProWorkrecord` 动态 set 加：

```xml
            <if test="teamId != null">team_id = #{teamId},</if>
            <if test="teamCode != null and teamCode != ''">team_code = #{teamCode},</if>
            <if test="teamName != null and teamName != ''">team_name = #{teamName},</if>
```

- [ ] **Step 5: 写失败测试（验证三处报工路径 + 上工写入 team 快照）**

```java
package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;
import com.ruoyi.system.domain.mes.pro.TeamResolver;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkrecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamSnapshotWriteTest {

    @Mock private ProFeedbackMapper feedbackMapper;
    @Mock private ProWorkrecordMapper workrecordMapper;
    @Mock private TeamResolver teamResolver;

    // 注：实际 ProFeedbackServiceImpl/ProWorkrecordServiceImpl 依赖很多，
    // 这里用 ArgumentCaptor 验证写入点即可；构造 service 时传入 mock 的 mapper/teamResolver。
    // 若 service 依赖过多，改为 spy + doNothing 隔离，或者只测 TeamResolver 调用时机。
    // 下方为契约测试骨架，实施时按实际注入调整。

    @Test
    @DisplayName("报工 insert 时填充 team 快照")
    void feedback_insert_should_fill_team_snapshot() {
        TeamResolver.TeamSnapshot snap = new TeamResolver.TeamSnapshot(10L, "DAY", "甲班");
        when(teamResolver.resolveByUserId(any())).thenReturn(snap);
        when(feedbackMapper.insertProFeedback(any())).thenReturn(1);
        // ... 构造 service，调用 doInsertProFeedback(new ProFeedback())
        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).insertProFeedback(cap.capture());
        assertThat(cap.getValue().getTeamId()).isEqualTo(10L);
    }
}
```

> 说明：ProFeedbackServiceImpl 依赖 RedisLockTemplate/TransactionTemplate/多 Mapper，纯单测构造较重。实施时采用其现有单测（参考 `ProWorkrecordServiceUnitTest`）的 `mockStatic(SecurityUtils)` + 让 `lockTemplate.execute`/`txTemplate.execute` 直接调 lambda 的模式。若成本过高，至少保证 TeamResolver（Task 2）单测全过 + 集成接口实测（Step 8）覆盖写入路径。

- [ ] **Step 6: ProFeedbackServiceImpl 三处写入快照**

注入（类字段区）：

```java
    @Autowired private TeamResolver teamResolver;
```

(a) `doInsertProFeedback`（约 L243 当前用户兜底之后、L266 insert 之前）：

```java
        if (proFeedback.getUserId() == null) {
            try { proFeedback.setUserId(SecurityUtils.getUserId()); } catch (Exception ignored) {}
        }
        if (proFeedback.getTeamId() == null) {
            TeamResolver.TeamSnapshot snap = proFeedback.getUserId() != null
                ? teamResolver.resolveByUserId(proFeedback.getUserId())
                : teamResolver.resolveByUserName(proFeedback.getUserName());
            proFeedback.setTeamId(snap.teamId());
            proFeedback.setTeamCode(snap.teamCode());
            proFeedback.setTeamName(snap.teamName());
        }
```

(b) `confirmFeedback`（约 L711，`fb.setStatus("CONFIRMED")` 之后、update 之前）：

```java
        if (fb.getTeamId() == null) {
            TeamResolver.TeamSnapshot snap = fb.getUserId() != null
                ? teamResolver.resolveByUserId(fb.getUserId())
                : teamResolver.resolveByUserName(fb.getUserName());
            fb.setTeamId(snap.teamId()); fb.setTeamCode(snap.teamCode()); fb.setTeamName(snap.teamName());
        }
```

(c) `auditFeedback`（约 L496，`fb.setStatus("AUDITED")` 之后、update 之前）：同样补一段（与 (b) 相同）。同时把裸字符串 `"AUDITED"`/`"CONFIRMED"`/`"PREPARE"` 在本文件用到的地方替换为 `ProConstants.FEEDBACK_STATUS_*`（限定本次改动触及的行，不顺手全局重构）。

> 班组解析失败时 TeamResolver 返回空快照（字段 null），不抛异常，不阻断主流程。

- [ ] **Step 7: ProWorkrecordServiceImpl 上工写入快照**

注入：

```java
    @Autowired private TeamResolver teamResolver;
```

在 `clockIn` 方法（约 L52-60，设置 userId/userName/nickName 之后）加：

```java
        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(userId);
        e.setTeamId(snap.teamId());
        e.setTeamCode(snap.teamCode());
        e.setTeamName(snap.teamName());
```

下工 `doClockOut` 不改 team 字段。

- [ ] **Step 8: 编译 + 重启 + 实测写入**

Run:
```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# 重启 jar（kill 旧 pid 后 nohup 启动），等 /captchaImage 200
# 用 token 上工打卡一次，再查库：
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s -X POST http://localhost:8081/mes/pro/workrecord/clockIn \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"workstationId":1}'
# SQL 验证：SELECT team_id,team_code,team_name FROM qxx_pro_workrecord ORDER BY record_id DESC LIMIT 1;
```
Expected: 新记录的 team_id/team_code/team_name 与该用户在 qxx_cal_team_member 的归属一致；无班组时为 NULL 且接口不报错。

- [ ] **Step 9: 提交**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProFeedback.java \
        backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProFeedbackMapper.xml \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProWorkrecord.java \
        backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProWorkrecordMapper.xml \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProFeedbackServiceImpl.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProWorkrecordServiceImpl.java
git commit -m "feat(pro): 报工/上下工写入班组快照(team_id/code/name)"
```

---

## Task 4: DelayLevelEvaluator 延期风险判定（纯函数）

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/DelayLevelEvaluator.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/DelayLevelEvaluatorTest.java`

**Interfaces:**
- Produces:
  - `DelayLevelEvaluator.evaluateTask(Date planStart, Date planEnd, Date actualStart, Date actualEnd, String status, BigDecimal quantity, BigDecimal quantityProduced, Integer warnHours, Integer behindTolerance, Date now)` → String（NORMAL/WARNING/DELAY/FINISHED_DELAY/BEHIND）
  - `DelayLevelEvaluator.evaluateWorkorder(Date requestDate, Date finishDate, String status, Integer warnDays, Date now)` → String
- 取消（CANCEL）统一返回 NORMAL（中性，不参与预警）。

- [ ] **Step 1: 写参数化测试**

```java
package com.ruoyi.system.service.mes.pro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.ruoyi.system.domain.mes.pro.ProConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

class DelayLevelEvaluatorTest {

    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static Date d(String s) throws Exception { return F.parse(s); }

    @ParameterizedTest(name = "[{index}] plan={0}~{1} actual={2}~{3} status={4} => {5}")
    @CsvSource({
        // 未到期：计划结束在未来 24h 之外
        "'2026-08-22 08:00','2026-08-30 18:00',,,'PRODUCING',NORMAL",
        // 临期：计划结束在未来 24h 内
        "'2026-08-22 08:00','2026-08-23 08:00',,,'PRODUCING',WARNING",
        // 延期：计划结束已过、未完成
        "'2026-08-20 08:00','2026-08-21 08:00',,,'PRODUCING',DELAY",
        // 完工按时
        "'2026-08-20 08:00','2026-08-22 08:00','2026-08-20 09:00','2026-08-22 07:00','COMPLETED',NORMAL",
        // 完工延期
        "'2026-08-20 08:00','2026-08-22 08:00','2026-08-20 09:00','2026-08-23 07:00','COMPLETED',FINISHED_DELAY",
        // 取消不判延期
        "'2026-08-20 08:00','2026-08-21 08:00',,,'CANCEL',NORMAL",
    })
    void task_levels(String ps, String pe, String as, String ae, String status, String expected) throws Exception {
        String level = DelayLevelEvaluator.evaluateTask(
            d(ps), d(pe), as==null?null:d(as), ae==null?null:d(ae),
            status, new BigDecimal("100"), new BigDecimal("50"), 24, 10, d("2026-08-22 12:00"));
        assertThat(level).isEqualTo(expected);
    }

    @ParameterizedTest(name = "workorder req={0} finish={1} status={2} => {3}")
    @CsvSource({
        "'2026-08-30',,,'PRODUCING',NORMAL",
        "'2026-08-23',,,'PRODUCING',WARNING",   // 交期在2天内
        "'2026-08-20',,,'PRODUCING',DELAY",      // 已过交期
        "'2026-08-20','2026-08-22','COMPLETED',NORMAL",       // 按时完工
        "'2026-08-20','2026-08-25','COMPLETED',FINISHED_DELAY", // 超交期完工
        "'2026-08-20',,,'CANCEL',NORMAL",
    })
    void workorder_levels(String req, String fin, String status, String expected) throws Exception {
        String level = DelayLevelEvaluator.evaluateWorkorder(
            d(req+" 00:00"), fin==null?null:d(fin+" 00:00"), status, 2, d("2026-08-22 12:00"));
        assertThat(level).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("进度滞后：产出进度比时间进度低超容差")
    void behind_schedule() throws Exception {
        // 计划 100h 的任务已过 80h（时间进度80%），只产出 50%（容差10，80-50=30>10）=> BEHIND
        // 为简化用日期差与数量：planStart 到 now 占 (end-start) 的 80%，produced/quantity=50%
        String level = DelayLevelEvaluator.evaluateTask(
            d("2026-08-01 00:00"), d("2026-08-11 00:00"), // 10 天
            d("2026-08-01 08:00"), null, "PRODUCING",
            new BigDecimal("100"), new BigDecimal("50"), 24, 10,
            d("2026-08-09 00:00")); // 过了 8 天 = 80%
        assertThat(level).isEqualTo(DELAY_BEHIND);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl ruoyi-system -am test -Dtest=DelayLevelEvaluatorTest -q`
Expected: 编译失败（类不存在）。

- [ ] **Step 3: 实现 DelayLevelEvaluator**

```java
package com.ruoyi.system.service.mes.pro;

import com.ruoyi.common.utils.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static com.ruoyi.system.domain.mes.pro.ProConstants.*;

/** 延期风险等级判定纯函数，无 Spring 依赖，便于单测。 */
public final class DelayLevelEvaluator {

    private DelayLevelEvaluator() {}

    public static String evaluateWorkorder(Date requestDate, Date finishDate, String status,
                                          Integer warnDays, Date now) {
        if (WORKORDER_STATUS_CANCEL.equals(status)) return DELAY_NORMAL;
        Date ref = now == null ? new Date() : now;
        if (WORKORDER_STATUS_COMPLETED.equals(status)) {
            if (requestDate != null && finishDate != null && finishDate.after(requestDate)) {
                return DELAY_FINISHED_LATE;
            }
            return DELAY_NORMAL;
        }
        if (requestDate == null) return DELAY_NORMAL;
        long warnMs = (warnDays == null ? 2 : warnDays) * 24L * 3600_000L;
        if (requestDate.getTime() < ref.getTime()) return DELAY_OVERDUE;
        if (requestDate.getTime() - ref.getTime() <= warnMs) return DELAY_WARNING;
        return DELAY_NORMAL;
    }

    public static String evaluateTask(Date planStart, Date planEnd, Date actualStart, Date actualEnd,
                                      String status, BigDecimal quantity, BigDecimal quantityProduced,
                                      Integer warnHours, Integer behindTolerance, Date now) {
        if (TASK_STATUS_CANCEL.equals(status)) return DELAY_NORMAL;
        Date ref = now == null ? new Date() : now;
        if (TASK_STATUS_COMPLETED.equals(status)) {
            if (planEnd != null && actualEnd != null && actualEnd.after(planEnd)) return DELAY_FINISHED_LATE;
            return DELAY_NORMAL;
        }
        if (planEnd == null) return DELAY_NORMAL; // 未排产不判延期
        long warnMs = (warnHours == null ? 24 : warnHours) * 3600_000L;
        if (planEnd.getTime() < ref.getTime()) return DELAY_OVERDUE;
        if (planEnd.getTime() - ref.getTime() <= warnMs) return DELAY_WARNING;
        // 进行中且进度滞后
        if (TASK_STATUS_PRODUCING.equals(status) && planStart != null && actualStart != null
                && quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0
                && quantityProduced != null) {
            long total = planEnd.getTime() - planStart.getTime();
            long elapsed = ref.getTime() - planStart.getTime();
            if (total > 0 && elapsed > 0) {
                int timePct = (int) Math.min(100, elapsed * 100 / total);
                int prodPct = quantityProduced.multiply(BigDecimal.valueOf(100))
                    .divide(quantity, 0, RoundingMode.HALF_UP).intValue();
                int tol = behindTolerance == null ? 10 : behindTolerance;
                if (prodPct < timePct - tol) return DELAY_BEHIND;
            }
        }
        return DELAY_NORMAL;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -pl ruoyi-system -am test -Dtest=DelayLevelEvaluatorTest -q`
Expected: BUILD SUCCESS。若"进度滞后"用例的日期比例算出来不是 BEHIND，调整 now 日期使时间进度严格大于产出进度+容差。

- [ ] **Step 5: 提交**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/DelayLevelEvaluator.java \
        backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/DelayLevelEvaluatorTest.java
git commit -m "feat(pro): DelayLevelEvaluator 延期风险纯函数(工单/任务/临期/延期/滞后)"
```

---

## Task 5: 工单进度后端（VO + Mapper + Service + Controller）

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/WorkorderProgressVO.java`
- Create: `.../vo/ProcessProgressRowVO.java`
- Create: `.../vo/CardSuborderVO.java`
- Create: `.../vo/DelayItemVO.java`
- Create: `.../mapper/mes/pro/ProProgressMapper.java`
- Create: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProProgressMapper.xml`
- Create: `.../service/mes/pro/IProProgressService.java`
- Create: `.../service/mes/pro/impl/ProProgressServiceImpl.java`
- Create: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProProgressController.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/impl/ProProgressServiceImplTest.java`

**Interfaces:**
- Consumes: `DelayLevelEvaluator`（Task 4）、`ProTaskMapper`、`ProWorkorderMapper`、`ProCardMapper`、`ProCardProcessMapper`、`IProConfigService`（读 sys_config，若依已有 `ISysConfigService`）。
- Produces:
  - `GET /mes/pro/progress/{workorderId}` → `AjaxResult` data = `WorkorderProgressVO`
  - `GET /mes/pro/progress/delayList` → `TableDataInfo`（分页），参数 `objectType`(WORKORDER/TASK)、`riskLevel`、`workshopId`、`teamId`、`keyword`

- [ ] **Step 1: 写 VO（4 个，POJO + getter/setter）**

`WorkorderProgressVO`：

```java
package com.ruoyi.system.domain.mes.pro.vo;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
public class WorkorderProgressVO {
    private Long workorderId;
    private String workorderCode, workorderName, productCode, productName, productSpc, status;
    private BigDecimal quantity, quantityProduced;
    private Date requestDate, finishDate;
    private Date planStartTime, planEndTime, actualStartTime, actualEndTime;
    private Integer completionRate;   // 0-100
    private String delayLevel;
    private List<ProcessProgressRowVO> processes;
    private List<CardSuborderVO> cards;
    // getter/setter 省略，实施时补齐全部字段
}
```

`ProcessProgressRowVO`：`taskId, taskCode, processId, processCode, processName, workstationId, workstationName, colorCode, status, quantity, quantityProduced, quantityQualified, quantityLaborScrap, quantityMaterialScrap, planStartTime, planEndTime, actualStartTime, actualEndTime, completionRate, delayLevel`。

`CardSuborderVO`：`cardId, cardCode, batchCode, quantityTransfered, currentProcessId, currentProcessName, status, actualStartTime, actualEndTime, totalProcessCount, finishedProcessCount, completionRate`。

`DelayItemVO`：`objectType, objectId, objectCode, objectName, productName, workshopId, workshopName, teamId, teamName, workstationName, planTime, requestDate, actualEndTime, overdueDays, progressPercent, delayLevel, status`。

- [ ] **Step 2: 写 Mapper 接口 + XML（批量聚合实际时间，禁止 N+1）**

`ProProgressMapper.java`：

```java
package com.ruoyi.system.mapper.mes.pro;

import com.ruoyi.system.domain.mes.pro.vo.DelayItemVO;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ProProgressMapper {
    /** 按任务批量聚合实际开始/结束/产出：返回每行 task_id, actual_start, actual_end, qty_output */
    List<Map<String,Object>> aggregateActualByTaskIds(@Param("taskIds") List<Long> taskIds);

    /** 按工单聚合：plan_start=MIN(start_time), plan_end=MAX(end_time), actual_start, actual_end */
    Map<String,Object> aggregateWorkorderTime(@Param("workorderId") Long workorderId);

    /** 工单延期预警（未完成且 request_date 临期/延期） */
    List<DelayItemVO> selectWorkorderDelays(@Param("riskLevel") String riskLevel,
            @Param("workshopId") Long workshopId, @Param("keyword") String keyword);

    /** 任务延期预警（未完成且 end_time 临期/延期，或完工延期） */
    List<DelayItemVO> selectTaskDelays(@Param("riskLevel") String riskLevel,
            @Param("workshopId") Long workshopId, @Param("teamId") Long teamId,
            @Param("keyword") String keyword);
}
```

`ProProgressMapper.xml`（关键 SQL；所有 FROM/JOIN 都是 qxx_ 表，factory_id 由拦截器自动注入，不要手写）：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.system.mapper.mes.pro.ProProgressMapper">

  <resultMap id="DelayItemMap" type="com.ruoyi.system.domain.mes.pro.vo.DelayItemVO">
    <result property="objectType" column="object_type"/>
    <result property="objectId" column="object_id"/>
    <result property="objectCode" column="object_code"/>
    <result property="objectName" column="object_name"/>
    <result property="productName" column="product_name"/>
    <result property="workshopId" column="workshop_id"/>
    <result property="workshopName" column="workshop_name"/>
    <result property="teamId" column="team_id"/>
    <result property="teamName" column="team_name"/>
    <result property="workstationName" column="workstation_name"/>
    <result property="planTime" column="plan_time"/>
    <result property="requestDate" column="request_date"/>
    <result property="actualEndTime" column="actual_end_time"/>
    <result property="overdueDays" column="overdue_days"/>
    <result property="progressPercent" column="progress_percent"/>
    <result property="status" column="status"/>
  </resultMap>

  <select id="aggregateActualByTaskIds" resultType="java.util.HashMap">
    SELECT cp.task_id AS taskId,
           MIN(cp.input_time)  AS actualStart,
           MAX(cp.output_time) AS actualEnd,
           SUM(cp.quantity_output) AS qtyOutput
    FROM qxx_pro_card_process cp
    WHERE cp.task_id IN
    <foreach collection="taskIds" item="id" open="(" separator="," close=")">#{id}</foreach>
    GROUP BY cp.task_id
  </select>

  <select id="aggregateWorkorderTime" resultType="java.util.HashMap">
    SELECT MIN(t.start_time) AS planStart, MAX(t.end_time) AS planEnd,
           MIN(cp.input_time) AS actualStart, MAX(cp.output_time) AS actualEnd
    FROM qxx_pro_task t
    LEFT JOIN qxx_pro_card_process cp ON cp.task_id = t.task_id
    WHERE t.workorder_id = #{workorderId}
  </select>

  <select id="selectWorkorderDelays" resultMap="DelayItemMap">
    SELECT 'WORKORDER' AS object_type, w.workorder_id AS object_id, w.workorder_code AS object_code,
           w.workorder_name AS object_name, w.product_name,
           w.request_date, w.finish_date AS actual_end_time, w.status,
           DATEDIFF(NOW(), w.request_date) AS overdue_days,
           ROUND(IFNULL(w.quantity_produced,0)/NULLIF(w.quantity,0)*100,0) AS progress_percent
    FROM qxx_pro_workorder w
    <where>
      w.status IN ('PREPARE','PRODUCING')
      AND w.request_date IS NOT NULL
      <if test="riskLevel == 'DELAY'"> AND w.request_date &lt; CURDATE()</if>
      <if test="riskLevel == 'WARNING'"> AND w.request_date &gt;= CURDATE()
            AND w.request_date &lt;= DATE_ADD(CURDATE(), INTERVAL 2 DAY)</if>
      <if test="keyword != null and keyword != ''">
        AND (w.workorder_code LIKE CONCAT('%',#{keyword},'%') OR w.workorder_name LIKE CONCAT('%',#{keyword},'%'))
      </if>
    </where>
    ORDER BY w.request_date ASC
  </select>

  <select id="selectTaskDelays" resultMap="DelayItemMap">
    SELECT 'TASK' AS object_type, t.task_id AS object_id, t.task_code AS object_code,
           t.task_name AS object_name, w.product_name,
           ws.workshop_id, ws.workshop_name, t.workstation_name,
           t.end_time AS plan_time, w.request_date, t.finish_date AS actual_end_time, t.status,
           DATEDIFF(NOW(), t.end_time) AS overdue_days,
           ROUND(IFNULL(t.quantity_produced,0)/NULLIF(t.quantity,0)*100,0) AS progress_percent
    FROM qxx_pro_task t
    LEFT JOIN qxx_pro_workorder w ON w.workorder_id = t.workorder_id
    LEFT JOIN qxx_md_workstation ws ON ws.workstation_id = t.workstation_id
    <where>
      t.status IN ('PREPARE','NORMAL','PRODUCING','PAUSED')
      AND t.end_time IS NOT NULL
      <if test="workshopId != null"> AND ws.workshop_id = #{workshopId}</if>
      <if test="teamId != null">
        AND EXISTS (SELECT 1 FROM qxx_pro_feedback fb
          WHERE fb.task_id = t.task_id AND fb.team_id = #{teamId} LIMIT 1)
      </if>
      <if test="riskLevel == 'DELAY'"> AND t.end_time &lt; NOW()</if>
      <if test="riskLevel == 'WARNING'"> AND t.end_time &gt;= NOW()
            AND t.end_time &lt;= DATE_ADD(NOW(), INTERVAL 24 HOUR)</if>
      <if test="keyword != null and keyword != ''">
        AND (t.task_code LIKE CONCAT('%',#{keyword},'%') OR t.task_name LIKE CONCAT('%',#{keyword},'%'))
      </if>
    </where>
    ORDER BY t.end_time ASC
  </select>
</mapper>
```

> 注意：`qxx_md_workstation` 也带 factory_id，JOIN 后拦截器会自动给主表注入 factory_id；若出现 ambiguous column 错误，给 t/w/cp 加别名限定（上面已用别名）。

- [ ] **Step 3: 写 IProProgressService + ProProgressServiceImpl**

```java
public interface IProProgressService {
    WorkorderProgressVO getWorkorderProgress(Long workorderId);
    List<DelayItemVO> selectDelayList(String objectType, String riskLevel,
        Long workshopId, Long teamId, String keyword);
}
```

`ProProgressServiceImpl`（逻辑分段，私有方法保持每段 ≤50 行）：

```java
@Service
public class ProProgressServiceImpl implements IProProgressService {
    @Autowired private ProWorkorderMapper workorderMapper;
    @Autowired private ProTaskMapper taskMapper;
    @Autowired private ProCardMapper cardMapper;
    @Autowired private ProCardProcessMapper cardProcessMapper;
    @Autowired private ProProgressMapper progressMapper;
    @Autowired private ISysConfigService configService;

    @Override
    public WorkorderProgressVO getWorkorderProgress(Long workorderId) {
        ProWorkorder wo = workorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) throw new ServiceException("工单不存在");
        WorkorderProgressVO vo = new WorkorderProgressVO();
        BeanUtils.copyProperties(wo, vo);
        fillTimes(wo, vo);
        vo.setCompletionRate(percent(wo.getQuantityProduced(), wo.getQuantity()));
        int warnDays = configService.selectConfigByInt(ProConstants.CFG_WARN_DAYS, 2);
        vo.setDelayLevel(DelayLevelEvaluator.evaluateWorkorder(
            wo.getRequestDate(), wo.getFinishDate(), wo.getStatus(), warnDays, new Date()));
        vo.setProcesses(buildProcessRows(workorderId));
        vo.setCards(buildCards(workorderId));
        return vo;
    }

    private void fillTimes(ProWorkorder wo, WorkorderProgressVO vo) {
        Map<String,Object> agg = progressMapper.aggregateWorkorderTime(wo.getWorkorderId());
        vo.setPlanStartTime((Date) agg.get("planStart"));
        vo.setPlanEndTime((Date) agg.get("planEnd"));
        vo.setActualStartTime((Date) agg.get("actualStart"));
        vo.setActualEndTime((Date) agg.get("actualEnd"));
    }

    private List<ProcessProgressRowVO> buildProcessRows(Long workorderId) {
        ProTask q = new ProTask(); q.setWorkorderId(workorderId);
        List<ProTask> tasks = taskMapper.selectProTaskList(q);
        if (tasks.isEmpty()) return List.of();
        List<Long> taskIds = tasks.stream().map(ProTask::getTaskId).toList();
        Map<Long,Map<String,Object>> actualMap = progressMapper.aggregateActualByTaskIds(taskIds)
            .stream().collect(Collectors.toMap(
                m -> ((Number)m.get("taskId")).longValue(), m -> m, (a,b)->a));
        int warnHours = configService.selectConfigByInt(ProConstants.CFG_WARN_HOURS, 24);
        int tol = configService.selectConfigByInt(ProConstants.CFG_BEHIND_TOLERANCE, 10);
        return tasks.stream().map(t -> toProcessRow(t, actualMap.get(t.getTaskId()), warnHours, tol)).toList();
    }

    private ProcessProgressRowVO toProcessRow(ProTask t, Map<String,Object> actual, int warnHours, int tol) {
        ProcessProgressRowVO row = new ProcessProgressRowVO();
        BeanUtils.copyProperties(t, row);
        if (actual != null) {
            row.setActualStartTime((Date) actual.get("actualStart"));
            row.setActualEndTime((Date) actual.get("actualEnd"));
        }
        row.setCompletionRate(percent(t.getQuantityProduced(), t.getQuantity()));
        row.setDelayLevel(DelayLevelEvaluator.evaluateTask(t.getStartTime(), t.getEndTime(),
            row.getActualStartTime(), row.getActualEndTime(), t.getStatus(),
            t.getQuantity(), t.getQuantityProduced(), warnHours, tol, new Date()));
        return row;
    }

    private List<CardSuborderVO> buildCards(Long workorderId) {
        ProCard cq = new ProCard(); cq.setWorkorderId(workorderId);
        List<ProCard> cards = cardMapper.selectProCardList(cq);
        return cards.stream().map(this::toCardRow).toList();
    }

    private CardSuborderVO toCardRow(ProCard c) {
        CardSuborderVO row = new CardSuborderVO();
        BeanUtils.copyProperties(c, row);
        ProCardProcess pq = new ProCardProcess(); pq.setCardId(c.getCardId());
        List<ProCardProcess> cps = cardProcessMapper.selectProCardProcessList(pq);
        Date actualStart = cps.stream().map(ProCardProcess::getInputTime).filter(Objects::nonNull).min(Date::compareTo).orElse(null);
        Date actualEnd = cps.stream().map(ProCardProcess::getOutputTime).filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        long finished = cps.stream().filter(p -> p.getOutputTime() != null).count();
        row.setActualStartTime(actualStart); row.setActualEndTime(actualEnd);
        row.setTotalProcessCount(cps.size()); row.setFinishedProcessCount((int) finished);
        row.setCompletionRate(cps.isEmpty() ? 0 : (int)(finished*100/cps.size()));
        return row;
    }

    @Override
    public List<DelayItemVO> selectDelayList(String objectType, String riskLevel,
            Long workshopId, Long teamId, String keyword) {
        if ("TASK".equals(objectType)) {
            List<DelayItemVO> list = progressMapper.selectTaskDelays(riskLevel, workshopId, teamId, keyword);
            list.forEach(this::applyTaskDelayLevel);
            return list;
        }
        List<DelayItemVO> list = progressMapper.selectWorkorderDelays(riskLevel, workshopId, keyword);
        list.forEach(this::applyWorkorderDelayLevel);
        return list;
    }
    // applyTaskDelayLevel/applyWorkorderDelayLevel: 根据 overdueDays/progress 用 DelayLevelEvaluator 二次判定
    // （SQL 已按 riskLevel 预筛；这里补全 delayLevel 字段与临期天数，约 20 行）

    private Integer percent(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) return 0;
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }
}
```

> 实施时补 `applyTaskDelayLevel`/`applyWorkorderDelayLevel`：从数据库读阈值，用 evaluator 算 level 并 set；SQL 预筛已保证只返回临期/延期项。`selectProCardList`/`selectProCardProcessList` 是现有 Mapper 方法（探索已确认存在）。

- [ ] **Step 4: 写 Controller**

```java
@RestController
@RequestMapping("/mes/pro/progress")
public class ProProgressController extends BaseController {
    @Autowired private IProProgressService progressService;

    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/{workorderId}")
    public AjaxResult getProgress(@PathVariable Long workorderId) {
        return success(progressService.getWorkorderProgress(workorderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:query')")
    @GetMapping("/delayList")
    public TableDataInfo delayList(@RequestParam(required=false) String objectType,
                                   @RequestParam(required=false) String riskLevel,
                                   @RequestParam(required=false) Long workshopId,
                                   @RequestParam(required=false) Long teamId,
                                   @RequestParam(required=false) String keyword) {
        startPage();
        return getDataTable(progressService.selectDelayList(objectType, riskLevel, workshopId, teamId, keyword));
    }
}
```

- [ ] **Step 5: 写单测**

`ProProgressServiceImplTest`（纯 Mockito，mock 6 个 Mapper + configService）：
- 测 `getWorkorderProgress`：mock workorder/task/card/mapper 返回，验证返回 VO 的 planStart/planEnd 来自 task、completionRate 正确、delayLevel 对一个已过 request_date 的未完工工单返回 DELAY。
- 测 `selectDelayList`：objectType=TASK 走 task mapper，WORKORDER 走 workorder mapper；mock 返回 1 条，验证 list 非空。
- 参考 `GanttDataServiceImplUnitTest` 的 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` 写法。

- [ ] **Step 6: 编译 + 实测接口**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# 重启 jar，TOKEN 实测：
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/mes/pro/progress/1 -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -40
curl -s "http://localhost:8081/mes/pro/progress/delayList?objectType=WORKORDER&pageNum=1&pageSize=10" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -30
```
Expected: progress 返回 summary+processes+cards；delayList 返回 rows+total。无数据时返回空数组而不是 500。

- [ ] **Step 7: 提交**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/ \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/pro/ProProgressMapper.java \
        backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProProgressMapper.xml \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/IProProgressService.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProProgressServiceImpl.java \
        backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProProgressController.java \
        backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/impl/ProProgressServiceImplTest.java
git commit -m "feat(pro): 工单进度详情与延期预警后端接口"
```

---

## Task 6: 甘特数据扩展实际进度字段

**Files:**
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/GanttDataServiceImpl.java`
- Modify: `frontend/src/types/api/mes/pro/gantt.ts`

**Interfaces:**
- Consumes: `ProProgressMapper.aggregateActualByTaskIds`（Task 5）。
- Produces: 甘特 task map 新增字段 `actualStartTime`、`actualEndTime`、`progressPercent`、`delayLevel`、`behindSchedule`。

- [ ] **Step 1: 注入 ProProgressMapper**

在 `GanttDataServiceImpl` 字段区加：

```java
    @Autowired private ProProgressMapper progressMapper;
    @Autowired private ISysConfigService configService;
```

- [ ] **Step 2: buildWorkOrderGantt 批量回填实际字段**

在 L84（`selectProTaskList` 拿到 taskList）之后、组装 children 循环之前，批量查一次：

```java
        List<Long> ganttTaskIds = taskList.stream().map(ProTask::getTaskId).toList();
        Map<Long, Map<String,Object>> actualMap = ganttTaskIds.isEmpty() ? Map.of()
            : progressMapper.aggregateActualByTaskIds(ganttTaskIds).stream()
                .collect(Collectors.toMap(m -> ((Number)m.get("taskId")).longValue(), m -> m, (a,b)->a));
        int warnHours = configService.selectConfigByInt(ProConstants.CFG_WARN_HOURS, 24);
        int tol = configService.selectConfigByInt(ProConstants.CFG_BEHIND_TOLERANCE, 10);
```

在 for 循环 `item.put(...)` 末尾（`quantityProduced` 之后）加：

```java
            Map<String,Object> act = actualMap.get(pt.getTaskId());
            Date aStart = act == null ? null : (Date) act.get("actualStart");
            Date aEnd   = act == null ? null : (Date) act.get("actualEnd");
            item.put("actualStartTime", aStart != null ? sdf.format(aStart) : null);
            item.put("actualEndTime",   aEnd   != null ? sdf.format(aEnd)   : null);
            int pct = (pt.getQuantity() == null || pt.getQuantity().compareTo(BigDecimal.ZERO)==0) ? 0
                : pt.getQuantityProduced().multiply(BigDecimal.valueOf(100))
                    .divide(pt.getQuantity(), 0, RoundingMode.HALF_UP).intValue();
            item.put("progressPercent", pct);
            String level = DelayLevelEvaluator.evaluateTask(pt.getStartTime(), pt.getEndTime(),
                aStart, aEnd, pt.getStatus(), pt.getQuantity(), pt.getQuantityProduced(),
                warnHours, tol, new Date());
            item.put("delayLevel", level);
            item.put("behindSchedule", ProConstants.DELAY_BEHIND.equals(level));
```

- [ ] **Step 3: buildWorkstationGantt 同样处理**

工作站甘特同样在 L166 拿到 taskList 后，用相同代码批量回填（抽一个私有方法 `enrichWithActual(List<ProTask> taskList, List<Map<String,Object>> children, SimpleDateFormat sdf)` 复用，避免重复，控制函数长度）。

- [ ] **Step 4: 前端类型加字段**

`gantt.ts` 的 `GanttTask` interface 加：

```ts
  actualStartTime?: string | null
  actualEndTime?: string | null
  progressPercent?: number
  delayLevel?: 'NORMAL' | 'WARNING' | 'DELAY' | 'FINISHED_DELAY' | 'BEHIND'
  behindSchedule?: boolean
```

- [ ] **Step 5: 编译 + 实测甘特接口含新字段**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# 重启后：
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/mes/pro/gantt/workorder/1 -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | grep -E "actualStartTime|progressPercent|delayLevel" | head
```
Expected: 每个 task 节点含 actualStartTime/actualEndTime/progressPercent/delayLevel（无实际数据时为 null/0/NORMAL）。

- [ ] **Step 6: 提交**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/GanttDataServiceImpl.java \
        frontend/src/types/api/mes/pro/gantt.ts
git commit -m "feat(pro): 甘特数据批量注入实际进度/完成率/延期等级字段"
```

---

## Task 7: 统计报表后端（overview/productivity/trend/taskStatus/detail）

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/ReportOverviewVO.java`
- Create: `.../vo/ProductivityRowVO.java`
- Create: `.../vo/TrendPointVO.java`
- Create: `.../vo/TaskStatusDistVO.java`
- Create: `.../mapper/mes/pro/ProReportMapper.java`
- Create: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProReportMapper.xml`
- Create: `.../service/mes/pro/IProReportService.java`
- Create: `.../service/mes/pro/impl/ProReportServiceImpl.java`
- Create: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProReportController.java`
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/impl/ProReportServiceImplTest.java`

**Interfaces:**
- Produces:
  - `GET /mes/pro/report/overview?beginTime&endTime&workshopIds&teamIds` → `ReportOverviewVO`
  - `GET /mes/pro/report/productivity?groupBy=WORKSHOP|TEAM|WORKSTATION|PROCESS&beginTime&endTime...` → `List<ProductivityRowVO>`
  - `GET /mes/pro/report/trend?beginTime&endTime&workshopId` → `List<TrendPointVO>`
  - `GET /mes/pro/report/taskStatus?beginTime&endTime&workshopId` → `List<TaskStatusDistVO>`
  - `GET /mes/pro/report/detail?beginTime&endTime&workshopId&pageNum&pageSize` → `TableDataInfo`

- [ ] **Step 1: 写 VO**

`ReportOverviewVO`：`workorderTotal, workorderCompleted, completionRate, workorderDelayed, delayRate, workorderInProgress, standardMinutes, actualMinutes, efficiencyPercent`（全部 Integer/BigDecimal，允许 null）。

`ProductivityRowVO`：`groupId, groupName, workorderCount, completedCount, delayedCount, completionRate, delayRate, standardOutput, actualOutput, standardMinutes, actualMinutes, efficiencyPercent, qualifiedQty, scrapQty, qualifiedRate`。

`TrendPointVO`：`statDate(Date), createdCount, completedCount, delayedCount, qualifiedQty, actualMinutes`。

`TaskStatusDistVO`：`status, count`。

- [ ] **Step 2: 写 ProReportMapper 接口 + XML**

接口：

```java
public interface ProReportMapper {
    Map<String,Object> selectOverview(@Param("begin") Date begin, @Param("end") Date end,
        @Param("workshopIds") List<Long> workshopIds, @Param("teamIds") List<Long> teamIds);
    List<Map<String,Object>> selectProductivityByWorkshop(@Param("begin") Date begin, @Param("end") Date end,
        @Param("workshopIds") List<Long> workshopIds);
    List<Map<String,Object>> selectProductivityByTeam(@Param("begin") Date begin, @Param("end") Date end,
        @Param("teamIds") List<Long> teamIds);
    List<Map<String,Object>> selectProductivityByWorkstation(@Param("begin") Date begin, @Param("end") Date end,
        @Param("workshopIds") List<Long> workshopIds);
    List<Map<String,Object>> selectTrend(@Param("begin") Date begin, @Param("end") Date end,
        @Param("workshopId") Long workshopId);
    List<Map<String,Object>> selectTaskStatus(@Param("begin") Date begin, @Param("end") Date end,
        @Param("workshopId") Long workshopId);
    List<Map<String,Object>> selectProductivityByProcess(@Param("begin") Date begin, @Param("end") Date end,
        @Param("workshopIds") List<Long> workshopIds);
}
```

XML 关键 SQL（factory_id 由拦截器注入；JOIN 用别名；注意 workrecord 会话只统计 CLOSED 的 work_duration，ACTIVE 按到 now 估算）：

```xml
<mapper namespace="com.ruoyi.system.mapper.mes.pro.ProReportMapper">

  <select id="selectOverview" resultType="java.util.HashMap">
    SELECT
      (SELECT COUNT(*) FROM qxx_pro_workorder w
        WHERE w.create_time BETWEEN #{begin} AND #{end}
        <if test="workshopIds != null and workshopIds.size() > 0">
          AND EXISTS (SELECT 1 FROM qxx_pro_task t JOIN qxx_md_workstation ws ON ws.workshop_id = t.workstation_id
            WHERE t.workorder_id = w.workorder_id AND ws.workshop_id IN <foreach collection='workshopIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)
        </if>
        AND w.status &lt;&gt; 'CANCEL') AS workorderTotal,
      (SELECT COUNT(*) FROM qxx_pro_workorder w WHERE w.status='COMPLETED'
        AND w.finish_date BETWEEN #{begin} AND #{end}) AS workorderCompleted,
      (SELECT COUNT(*) FROM qxx_pro_workorder w WHERE w.status &lt;&gt; 'CANCEL'
        AND w.request_date IS NOT NULL AND w.request_date &lt; NOW()
        AND (w.status &lt;&gt; 'COMPLETED' OR w.finish_date &gt; w.request_date)) AS workorderDelayed,
      (SELECT COUNT(*) FROM qxx_pro_workorder w WHERE w.status='PRODUCING') AS workorderInProgress,
      (SELECT IFNULL(SUM(t.setup_duration + t.unit_duration * t.quantity_produced),0)
         FROM qxx_pro_task t WHERE t.status &lt;&gt; 'CANCEL'
         AND (t.finish_date BETWEEN #{begin} AND #{end} OR t.status='PRODUCING')) AS standardMinutes,
      (SELECT IFNULL(SUM(r.work_duration),0) FROM qxx_pro_workrecord r
         WHERE r.status='CLOSED' AND r.clock_in_time BETWEEN #{begin} AND #{end}
         <if test="teamIds != null and teamIds.size() > 0">
           AND r.team_id IN <foreach collection='teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
         </if>) AS actualMinutes
  </select>
```

productivity 按 workshop 示例（其他维度仿照，group by 换表/列）：

```xml
  <select id="selectProductivityByWorkshop" resultType="java.util.HashMap">
    SELECT ws.workshop_id AS groupId, ws.workshop_name AS groupName,
           COUNT(DISTINCT t.workorder_id) AS workorderCount,
           SUM(CASE WHEN w.status='COMPLETED' THEN 1 ELSE 0 END) AS completedCount,
           SUM(CASE WHEN w.request_date IS NOT NULL AND w.request_date &lt; NOW()
                 AND (w.status &lt;&gt; 'COMPLETED' OR w.finish_date &gt; w.request_date) THEN 1 ELSE 0 END) AS delayedCount,
           IFNULL(SUM(t.setup_duration + t.unit_duration * t.quantity_produced),0) AS standardMinutes,
           IFNULL(SUM(t.quantity_qualified),0) AS qualifiedQty,
           IFNULL(SUM(t.quantity_unqualified),0) AS scrapQty
    FROM qxx_pro_task t
    JOIN qxx_md_workstation ws ON ws.workstation_id = t.workstation_id
    LEFT JOIN qxx_pro_workorder w ON w.workorder_id = t.workorder_id
    <where>
      (t.finish_date BETWEEN #{begin} AND #{end} OR t.status IN ('PRODUCING','NORMAL','PREPARE','PAUSED'))
      <if test="workshopIds != null and workshopIds.size() > 0">
        AND ws.workshop_id IN <foreach collection='workshopIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
      </if>
    </where>
    GROUP BY ws.workshop_id, ws.workshop_name
  </select>
```

> 实际工时按 workshop 维度需要从 workrecord JOIN workstation 聚合，在 Service 层合并（SQL 分别查 task 侧产出和 workrecord 侧工时，内存按 groupId join），避免一个超长 SQL。trend 用 `DATE(t.finish_date)` group by，MySQL 用日历表或在 Service 层补齐无数据日期为 0。taskStatus 直接 `SELECT status, COUNT(*) FROM qxx_pro_task ... GROUP BY status`。

- [ ] **Step 3: 写 Service**

```java
@Service
public class ProReportServiceImpl implements IProReportService {
    @Autowired private ProReportMapper reportMapper;

    @Override
    public ReportOverviewVO overview(Date begin, Date end, List<Long> workshopIds, List<Long> teamIds) {
        Map<String,Object> m = reportMapper.selectOverview(begin, end, workshopIds, teamIds);
        ReportOverviewVO vo = new ReportOverviewVO();
        vo.setWorkorderTotal(asInt(m.get("workorderTotal")));
        vo.setWorkorderCompleted(asInt(m.get("workorderCompleted")));
        vo.setWorkorderDelayed(asInt(m.get("workorderDelayed")));
        vo.setWorkorderInProgress(asInt(m.get("workorderInProgress")));
        vo.setStandardMinutes(asInt(m.get("standardMinutes")));
        int actual = asInt(m.get("actualMinutes"));
        vo.setActualMinutes(actual);
        vo.setCompletionRate(rate(vo.getWorkorderCompleted(), vo.getWorkorderTotal()));
        vo.setDelayRate(rate(vo.getWorkorderDelayed(), vo.getWorkorderTotal()));
        vo.setEfficiencyPercent(efficiency(vo.getStandardMinutes(), actual));
        return vo;
    }

    @Override
    public List<ProductivityRowVO> productivity(String groupBy, Date begin, Date end,
            List<Long> workshopIds, List<Long> teamIds) {
        List<Map<String,Object>> rows = switch (groupBy == null ? "WORKSHOP" : groupBy) {
            case "TEAM" -> reportMapper.selectProductivityByTeam(begin, end, teamIds);
            case "WORKSTATION" -> reportMapper.selectProductivityByWorkstation(begin, end, workshopIds);
            case "PROCESS" -> reportMapper.selectProductivityByProcess(begin, end, workshopIds);
            default -> reportMapper.selectProductivityByWorkshop(begin, end, workshopIds);
        };
        return rows.stream().map(this::toProductivityRow).toList();
    }
    // trend/taskStatus/detail 同理；detail 用 PageHelper 分页（Controller startPage）

    private Integer rate(Integer part, Integer total) {
        if (total == null || total == 0) return 0;
        return Math.round(part * 100f / total);
    }
    private Integer efficiency(Integer standard, Integer actual) {
        if (actual == null || actual == 0) return null; // 无实际工时时前端显示"—"
        return Math.round(standard * 100f / actual);
    }
    private int asInt(Object o) { return o == null ? 0 : ((Number)o).intValue(); }
}
```

- [ ] **Step 4: 写 Controller**

```java
@RestController
@RequestMapping("/mes/pro/report")
public class ProReportController extends BaseController {
    @Autowired private IProReportService reportService;

    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required=false) Date beginTime,
                               @RequestParam(required=false) Date endTime,
                               @RequestParam(required=false) List<Long> workshopIds,
                               @RequestParam(required=false) List<Long> teamIds) {
        Date[] range = defaultRange(beginTime, endTime);
        return success(reportService.overview(range[0], range[1], workshopIds, teamIds));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/productivity")
    public AjaxResult productivity(@RequestParam(defaultValue="WORKSHOP") String groupBy,
            @RequestParam(required=false) Date beginTime, @RequestParam(required=false) Date endTime,
            @RequestParam(required=false) List<Long> workshopIds,
            @RequestParam(required=false) List<Long> teamIds) {
        Date[] r = defaultRange(beginTime, endTime);
        return success(reportService.productivity(groupBy, r[0], r[1], workshopIds, teamIds));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam(required=false) Date beginTime,
                            @RequestParam(required=false) Date endTime,
                            @RequestParam(required=false) Long workshopId) {
        Date[] r = defaultRange(beginTime, endTime);
        return success(reportService.trend(r[0], r[1], workshopId));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/taskStatus")
    public AjaxResult taskStatus(@RequestParam(required=false) Date beginTime,
                                 @RequestParam(required=false) Date endTime,
                                 @RequestParam(required=false) Long workshopId) {
        Date[] r = defaultRange(beginTime, endTime);
        return success(reportService.taskStatus(r[0], r[1], workshopId));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:report:query')")
    @GetMapping("/detail")
    public TableDataInfo detail(@RequestParam(required=false) Date beginTime,
                                @RequestParam(required=false) Date endTime,
                                @RequestParam(required=false) Long workshopId,
                                @RequestParam(required=false) Long teamId) {
        Date[] r = defaultRange(beginTime, endTime);
        startPage();
        return getDataTable(reportService.detail(r[0], r[1], workshopId, teamId));
    }

    /** 默认本月 1 日 00:00 到 now */
    private Date[] defaultRange(Date begin, Date end) {
        if (begin != null && end != null) return new Date[]{begin, end};
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH,1);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0);
        return new Date[]{c.getTime(), new Date()};
    }
}
```

- [ ] **Step 5: 写单测**

`ProReportServiceImplTest`（纯 Mockito）：
- mock reportMapper.selectOverview 返回含 standardMinutes=600、actualMinutes=300 的 map，断言 efficiencyPercent=200。
- actualMinutes=0 时 efficiencyPercent=null。
- productivity groupBy=TEAM 调用对应 mapper 方法；未知 groupBy 默认 WORKSHOP。

- [ ] **Step 6: 编译 + 重启 + 实测 5 个接口**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# 重启 jar
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
for ep in overview productivity trend taskStatus; do
  echo "=== $ep ==="
  curl -s "http://localhost:8081/mes/pro/report/$ep?beginTime=2026-08-01&endTime=2026-08-22" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -20
done
curl -s "http://localhost:8081/mes/pro/report/detail?pageNum=1&pageSize=10" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -20
```
Expected: overview 含 9 个指标；productivity 返回数组；trend 按日；taskStatus 含各状态计数；detail 返回 rows/total。无数据时返回 0 或空数组，不报 500。

- [ ] **Step 7: 提交**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/ReportOverviewVO.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/ProductivityRowVO.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/TrendPointVO.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/vo/TaskStatusDistVO.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/pro/ProReportMapper.java \
        backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProReportMapper.xml \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/IProReportService.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProReportServiceImpl.java \
        backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProReportController.java \
        backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/impl/ProReportServiceImplTest.java
git commit -m "feat(pro): 生产统计报表后端(KPI/产能/工时/效率/趋势/状态分布/明细)"
```

---

## Task 8: 前端 Charts 公共组件 + 统计报表页

**Files:**
- Create: `frontend/src/components/Charts/BaseChart.vue`
- Create: `frontend/src/components/Charts/BarChart.vue`
- Create: `frontend/src/components/Charts/LineChart.vue`
- Create: `frontend/src/components/Charts/PieChart.vue`
- Create: `frontend/src/api/mes/pro/report.ts`
- Create: `frontend/src/views/mes/pro/report/index.vue`

**Interfaces:**
- Consumes: Task 7 的 5 个接口；`BaseChart` 封装 echarts init/setOption/resize/dispose。
- Produces: 菜单"生产统计"对应的页面（菜单已在 V141 创建）。

- [ ] **Step 1: 写 BaseChart.vue（修复现有 cache 页面的 dispose 泄漏）**

```vue
<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }" />
</template>
<script setup lang="ts">
import * as echarts from 'echarts'
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'

const props = defineProps<{ option: any; height?: string }>()
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
const HEIGHT = props.height || '320px'

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(props.option || {}, true)
}
function handleResize() { chart?.resize() }
onMounted(async () => { await nextTick(); render(); window.addEventListener('resize', handleResize) })
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose(); chart = null
})
watch(() => props.option, render, { deep: true })
</script>
```

- [ ] **Step 2: 写 BarChart/LineChart/PieChart 薄封装**

每个接收 `data`（已处理好的 series/xAxis 数据）和可选 `title`，内部拼 option 传给 BaseChart。示例 `BarChart.vue`：

```vue
<template><BaseChart :option="opt" :height="height" /></template>
<script setup lang="ts">
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'
const props = defineProps<{ xData: string[]; series: any[]; title?: string; height?: string }>()
const opt = computed(() => ({
  title: props.title ? { text: props.title, textStyle: { fontSize: 14 } } : undefined,
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  grid: { left: 40, right: 20, top: 40, bottom: 40 },
  xAxis: { type: 'category', data: props.xData },
  yAxis: { type: 'value' },
  series: props.series
}))
</script>
```

LineChart/PieChart 同理（pie 的 data 是 `{name,value}[]`，type='pie'，radius 等）。

- [ ] **Step 3: 写 report.ts**

```ts
import request from '@/utils/request'
export function getOverview(params: any) { return request({ url: '/mes/pro/report/overview', method: 'get', params }) }
export function getProductivity(params: any) { return request({ url: '/mes/pro/report/productivity', method: 'get', params }) }
export function getTrend(params: any) { return request({ url: '/mes/pro/report/trend', method: 'get', params }) }
export function getTaskStatus(params: any) { return request({ url: '/mes/pro/report/taskStatus', method: 'get', params }) }
export function getReportDetail(params: any) { return request({ url: '/mes/pro/report/detail', method: 'get', params }) }
```

- [ ] **Step 4: 写 report/index.vue（拆区块，整体 ≤300 行）**

布局按设计文档第 7.4 节：筛选栏（日期范围默认本月、车间多选、班组多选）+ 8 个 KPI 卡片（el-row/el-col，用 el-card 显示数值，延期率红色、效率根据 >100 绿色）+ 4 个图表（el-card 包 BarChart/LineChart/PieChart，各占 12 栅格）+ 明细表（el-table 列：分组、工单数、完成数、延期数、完成率、延期率、标准工时、实际工时、效率、合格率；底部分页）+ 延期预警表（调 Task 5 的 delayList，单独区块）。

关键 script 结构：

```ts
import { getOverview, getProductivity, getTrend, getTaskStatus, getReportDetail } from '@/api/mes/pro/report'
import { listDelay } from '@/api/mes/pro/progress'
import BarChart from '@/components/Charts/BarChart.vue'
import LineChart from '@/components/Charts/LineChart.vue'
import PieChart from '@/components/Charts/PieChart.vue'

const queryParams = ref({ beginTime: monthFirst(), endTime: new Date(), workshopIds: [] as number[], teamIds: [] as number[] })
const overview = ref<any>({})
const trendData = ref({ x: [] as string[], created: [] as number[], completed: [] as number[] })
const statusData = ref<{name:string;value:number}[]>([])
const productivity = ref<any[]>([])
const detailList = ref<any[]>([])
const delayList = ref<any[]>([])
const total = ref(0)

async function loadAll() {
  const p = { ...queryParams.value, beginTime: fmt(queryParams.value.beginTime), endTime: fmt(queryParams.value.endTime) }
  const [o, t, s, prod, d, dl] = await Promise.all([
    getOverview(p), getTrend(p), getTaskStatus(p),
    getProductivity({ ...p, groupBy: 'WORKSHOP' }),
    getReportDetail({ ...p, pageNum: 1, pageSize: 20 }),
    listDelay({ objectType: 'WORKORDER', pageNum: 1, pageSize: 10 })
  ])
  overview.value = (o as any).data
  trendData.value = mapTrend((t as any).data)
  statusData.value = ((s as any).data || []).map((x:any)=>({ name: statusText(x.status), value: x.count }))
  productivity.value = (prod as any).data || []
  detailList.value = (d as any).rows || []
  total.value = (d as any).total || 0
  delayList.value = (dl as any).rows || []
}
```

KPI 卡片用 `el-tag`/文本颜色：完成率/效率绿色，延期率红色。导出 Excel 用若依现成的 `proxy.download('/mes/pro/report/export', ...)`（一期若时间紧可只留按钮占位，download 走 detail 的同参数；后端 export 可放到后续小任务，本期先用页面表格 + 浏览器打印不强求）。

- [ ] **Step 5: 前端类型检查 + 浏览器实测**

Run: `cd frontend && npx vue-tsc --noEmit -p tsconfig.json 2>&1 | head -40`（项目若无该脚本则 `npm run build` 验证编译）。
浏览器实测：用 admin 登录 → 左侧菜单出现"生产统计" → 进入页面 → 默认本月数据加载 → 8 个 KPI 有值 → 4 个图表渲染 → 切换日期/车间筛选点查询 → 明细表刷新 → 延期表显示临期/延期工单。验证切换页面后无 echarts 控制台报错（dispose 生效）。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/components/Charts/ frontend/src/api/mes/pro/report.ts frontend/src/views/mes/pro/report/
git commit -m "feat(pro): 生产统计报表前端(KPI卡+图表+明细+延期)与echarts公共组件"
```

---

## Task 9: GanttChart 组件增强（双层实际条 + 风险色 + 进度）

**Files:**
- Modify: `frontend/src/components/GanttChart/index.vue`
- Modify: `frontend/src/views/mes/pro/gantt/index.vue`（透传 showActual 开关，不加 readonly）

**Interfaces:**
- Consumes: `GanttTask` 新字段（Task 6）：`actualStartTime/actualEndTime/progressPercent/delayLevel/behindSchedule`。
- Produces: 新 props `showActual?: boolean`（默认 true）、`readonly?: boolean`（默认 false）；readonly 时禁用拖拽/resize，供进度页内嵌。

- [ ] **Step 1: 扩展 props**

在 `defineProps<{ tasks: GanttTask[]; loading?: boolean }>()`（L64）改为：

```ts
const props = withDefaults(defineProps<{
  tasks: GanttTask[]
  loading?: boolean
  showActual?: boolean
  readonly?: boolean
}>(), { showActual: true, readonly: false })
```

- [ ] **Step 2: 任务行渲染双层条 + 左边框风险色**

把现有 `.gc-bar` 模板（L43-50）替换为：

```html
<div v-if="r.t==='t' && r.s && r.e" class="gc-bar"
  :class="[constrained?'constrained':'', 'risk-'+(r.raw.delayLevel||'NORMAL'), { readonly: readonly }]"
  :style="{left:posX(r.s)+'px',width:Math.max(posX(r.e)-posX(r.s),4)+'px',background:r.c}"
  @mouseup="onBarClick($event, r)"
  @mousedown="onBarDown($event, r)">
  <!-- 实际进度叠层 -->
  <div v-if="showActual && r.raw.actualStartTime" class="gc-bar-actual"
    :class="{ongoing: !r.raw.actualEndTime}"
    :style="actualStyle(r)"></div>
  <!-- 数量进度填充 -->
  <div v-if="r.raw.progressPercent>0" class="gc-bar-progress"
    :style="{width: Math.min(100,r.raw.progressPercent)+'%'}"></div>
  <div v-if="!readonly" class="gc-resize-l" @mousedown.stop="onResizeDown($event, r, 'left')" />
  <div v-if="!readonly" class="gc-resize-r" @mousedown.stop="onResizeDown($event, r, 'right')" />
</div>
```

在 script 中加：

```ts
function actualStyle(r: any) {
  const s = posX(r.s), e = posX(r.e)
  const as = r.raw.actualStartTime ? posX(r.raw.actualStartTime) : s
  const ae = r.raw.actualEndTime ? posX(r.raw.actualEndTime) : Math.min(e, posXNow())
  return { left: (as - s) + 'px', width: Math.max(2, ae - as) + 'px' }
}
function posXNow() {
  // 用当前时间映射到 x 坐标：参考现有 posX 对 Date 的解析
  return posX(formatNow())
}
function formatNow() {
  const d = new Date(), p = (n:number)=>String(n).padStart(2,'0')
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
```

> 现有 `posX(s)` 接受字符串时间（组件内 day 模式按小时、week 模式按天计算）。确认 posX 能解析 `yyyy-MM-dd HH:mm`；若不能，用组件已有的时间解析方式（读 posX 实现，复用同一 parse）。

- [ ] **Step 3: 加 CSS（风险边框、实际条、进度填充）**

在 `<style scoped>` 末尾加：

```css
.gc-bar { position: absolute; top: 6px; height: 22px; border-radius: 3px; box-sizing: border-box; border-left: 4px solid transparent; }
.gc-bar.risk-NORMAL { border-left-color: #409eff; }
.gc-bar.risk-WARNING { border-left-color: #e6a23c; }
.gc-bar.risk-DELAY { border-left-color: #f56c6c; box-shadow: 0 0 0 1px #f56c6c inset; }
.gc-bar.risk-FINISHED_DELAY { border-left-color: #e6a23c; }
.gc-bar.risk-BEHIND { border-left-color: #9b59b6; }
.gc-bar.readonly { cursor: default; }
.gc-bar-progress { position:absolute; left:0; top:0; bottom:0; background: rgba(255,255,255,0.35); border-radius: 3px; pointer-events:none; }
.gc-bar-actual { position:absolute; top:3px; bottom:3px; background: rgba(64,158,255,0.55); border-radius:2px; pointer-events:none; }
.gc-bar-actual.ongoing {
  background: repeating-linear-gradient(45deg, rgba(64,158,255,0.55), rgba(64,158,255,0.55) 4px, rgba(64,158,255,0.25) 4px, rgba(64,158,255,0.25) 8px);
}
```

- [ ] **Step 4: readonly 禁用拖拽**

在 `onBarDown`（L186）和 `onResizeDown`（L187）开头加：

```ts
if (props.readonly) return
```

- [ ] **Step 5: 甘特页加"显示实际进度"开关**

`gantt/index.vue` 工具栏（L35-55 的 el-row 内）加：

```html
<el-col :span="1.5">
  <el-switch v-model="showActual" active-text="实际进度" inline-prompt @change="ganttRef?.render()" />
</el-col>
```

script 中加 `const showActual = ref(true)`，并在 `<GanttChart>` 标签（L60）加 `:show-actual="showActual"`。

- [ ] **Step 6: 浏览器实测**

`cd frontend && npm run dev`，浏览器：打开甘特图排产 → 选一个有报工记录的工单 → 任务条左侧出现风险色边框 → 条内有半透明实际进度叠层和白色进度填充 → 关闭"实际进度"开关后叠层消失 → 拖拽移动仍正常（非 readonly）→ 无控制台报错。

- [ ] **Step 7: 提交**

```bash
git add frontend/src/components/GanttChart/index.vue frontend/src/views/mes/pro/gantt/index.vue
git commit -m "feat(pro/gantt): 甘特条叠加实际进度+完成率+延期风险色, 支持只读模式"
```

---

## Task 10: 工单进度页（全屏 dialog + 工序表 + 子工单表 + 内嵌甘特）

**Files:**
- Create: `frontend/src/api/mes/pro/progress.ts`
- Create: `frontend/src/views/mes/pro/workorder/components/ProcessProgressTable.vue`
- Create: `frontend/src/views/mes/pro/workorder/components/CardSuborderTable.vue`
- Create: `frontend/src/views/mes/pro/workorder/progress.vue`
- Modify: `frontend/src/views/mes/pro/workorder/index.vue`（加按钮 + dialog）

**Interfaces:**
- Consumes: `GET /mes/pro/progress/{id}`（Task 5）、GanttChart 组件（Task 9，readonly）、`getWorkOrderGantt`（已有）。

- [ ] **Step 1: 写 progress.ts**

```ts
import request from '@/utils/request'
export function getWorkorderProgress(workorderId: number) {
  return request({ url: '/mes/pro/progress/' + workorderId, method: 'get' })
}
export function listDelay(params: any) {
  return request({ url: '/mes/pro/progress/delayList', method: 'get', params })
}
```

- [ ] **Step 2: 写 ProcessProgressTable.vue**

Props `rows: ProcessProgressRowVO[]`。用 `el-table`：列 工序、工作站、计划开始/结束、实际开始/结束（无值显示"—"，未排产整行显示"未排产"）、计划/产出/合格/报废、进度（`el-progress`，颜色函数复用 dashboard 的 getProgressColor）、状态（dict-tag 或本地 statusMap）、风险（el-tag：NORMAL info、WARNING warning、DELAY danger、FINISHED_DELAY warning plain、BEHIND '' 紫色）。行可展开（`type="expand"`），展开区显示该工序下的流转卡（可选，一期展开区可先留"该工序流转卡"占位，主要子工单表在 Tab2）。

时间格式化用全局 `parseTime(time, '{y}-{m}-{d} {h}:{i}')`。

- [ ] **Step 3: 写 CardSuborderTable.vue**

Props `cards: CardSuborderVO[]`。`el-table`：列 卡号、批次、流转数量、当前工序、工序进度（"finishedProcessCount/totalProcessCount" + el-progress）、实际开始/结束、状态。行展开显示该卡各工序明细——需要按 cardId 调 `listCardProcess({ cardId })`（已有的 cardprocess API）展示 input_time/output_time/操作人/工作站。一期可以直接在 progress 接口的 cards 里不带明细，展开时懒加载。

- [ ] **Step 4: 写 progress.vue（容器，<250 行）**

```vue
<template>
  <el-dialog v-model="visible" :title="'工单进度 — '+(data?.workorderName||'')"
    width="92%" top="3vh" append-to-body :close-on-click-modal="false" destroy-on-close @close="visible=false">
    <div v-loading="loading">
      <el-descriptions :column="4" border size="small" class="mb16">
        <el-descriptions-item label="工单编号">{{ data?.workorderCode }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ data?.productName }}</el-descriptions-item>
        <el-descriptions-item label="计划/已产">{{ data?.quantity }} / {{ data?.quantityProduced }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag>{{ data?.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="计划周期">{{ fmt(data?.planStartTime) }} ~ {{ fmt(data?.planEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="实际周期">{{ fmt(data?.actualStartTime) }} ~ {{ fmt(data?.actualEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="交期">{{ fmt(data?.requestDate) }}</el-descriptions-item>
        <el-descriptions-item label="风险"><el-tag :type="riskType(data?.delayLevel)">{{ riskText(data?.delayLevel) }}</el-tag></el-descriptions-item>
      </el-descriptions>
      <el-tabs v-model="tab">
        <el-tab-pane label="工序进度" name="process">
          <ProcessProgressTable :rows="data?.processes || []" />
        </el-tab-pane>
        <el-tab-pane label="子工单(流转卡)" name="card">
          <CardSuborderTable :cards="data?.cards || []" />
        </el-tab-pane>
        <el-tab-pane label="甘特视图" name="gantt" lazy>
          <GanttChart v-if="ganttTasks.length" :tasks="ganttTasks" :readonly="true" :show-actual="true" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>
```

script：`defineProps<{ modelValue:boolean; workorderId:number|null }>`，watch workorderId 变化时调 `getWorkorderProgress`；切到甘特 tab 时调 `getWorkOrderGantt(workorderId)` 赋给 ganttTasks。`fmt` 用 parseTime。`riskType/riskText` 映射 delayLevel。emits `update:modelValue`。

- [ ] **Step 5: workorder/index.vue 接入**

该组件是 Options API。在 template 末尾（其他 el-dialog 同级，约 L300 后）加：

```html
<WorkorderProgress v-model="progressOpen" :workorder-id="progressWorkorderId" />
```

在操作列（L40"查看"按钮之前）加：

```html
<el-tooltip content="进度" placement="top">
  <el-button link type="primary" icon="DataLine" @click="handleProgress(scope.row)"></el-button>
</el-tooltip>
```

操作列 width 从 220 调到 250。

script 中：`import WorkorderProgress from './progress.vue'`；components 注册；data 加 `progressOpen: false, progressWorkorderId: null`；methods 加：

```js
handleProgress(row) {
  this.progressWorkorderId = row.workorderId
  this.progressOpen = true
},
```

- [ ] **Step 6: 浏览器实测**

工单列表 → 点"进度"图标 → 全屏 dialog 打开 → 头部 8 个信息正确 → 工序进度表显示各工序计划/实际时间、进度条、风险标签 → 切"子工单"tab 显示流转卡，展开能看到工序明细 → 切"甘特视图"显示只读甘特（不能拖拽）→ 关闭再开另一个工单数据刷新 → 无控制台报错。

- [ ] **Step 7: 提交**

```bash
git add frontend/src/api/mes/pro/progress.ts \
        frontend/src/views/mes/pro/workorder/components/ProcessProgressTable.vue \
        frontend/src/views/mes/pro/workorder/components/CardSuborderTable.vue \
        frontend/src/views/mes/pro/workorder/progress.vue \
        frontend/src/views/mes/pro/workorder/index.vue
git commit -m "feat(pro): 工单进度全屏弹窗(工序/子工单流转卡/只读甘特)"
```

---

## Task 11: 生产看板延期预警增强

**Files:**
- Modify: `frontend/src/views/mes/pro/dashboard/index.vue`

**Interfaces:**
- Consumes: `listDelay({objectType, pageNum, pageSize})`（Task 10 的 progress.ts）。

- [ ] **Step 1: 替换延期数据加载**

删掉现有的 `loadDelay`（L203-219，前端拉 100 条工单过滤）。改为：

```ts
import { listDelay } from '@/api/mes/pro/progress'
const delayTab = ref('WORKORDER')
const delayList = ref<any[]>([])
const delayLoading = ref(false)
async function loadDelay() {
  delayLoading.value = true
  try {
    const r: any = await listDelay({ objectType: delayTab.value, pageNum: 1, pageSize: 20 })
    delayList.value = r.rows || []
  } finally { delayLoading.value = false }
}
watch(delayTab, loadDelay)
```

- [ ] **Step 2: 延期表格加 Tab 和列**

把延迟预警区（L91-111）改为带 `el-radio-group`（工单/任务）+ 表格列：

```html
<div class="delay-list" v-loading="delayLoading">
  <el-radio-group v-model="delayTab" size="small" class="mb8">
    <el-radio-button value="WORKORDER">工单</el-radio-button>
    <el-radio-button value="TASK">工序任务</el-radio-button>
  </el-radio-group>
  <el-table :data="delayList" size="small" max-height="380" stripe>
    <el-table-column label="编号" prop="objectCode" :show-overflow-tooltip="true" min-width="110" />
    <el-table-column label="名称" prop="objectName" :show-overflow-tooltip="true" min-width="120" />
    <el-table-column label="车间" prop="workshopName" width="90" />
    <el-table-column label="计划/交期" width="110">
      <template #default="s">{{ parseTime(s.row.planTime||s.row.requestDate, '{m}-{d} {h}:{i}') }}</template>
    </el-table-column>
    <el-table-column label="延期" width="70">
      <template #default="s"><el-tag type="danger" size="small">{{ s.row.overdueDays>0?s.row.overdueDays+'天':'临期' }}</el-tag></template>
    </el-table-column>
    <el-table-column label="进度" width="80">
      <template #default="s"><el-progress :percentage="s.row.progressPercent||0" :stroke-width="6" /></template>
    </el-table-column>
  </el-table>
</div>
```

工单行点击可跳进度页/甘特（`@row-click`，工单跳 workorder 进度，任务跳 gantt 带 workorderId）。

- [ ] **Step 3: 顶部加"延期工单数"卡片**

现有 4 卡片（在制/今日报工/今日合格/今日不合格）后加第 5 个：延期工单数，红色，值取 overview 接口或直接 `listDelay({objectType:'WORKORDER',pageSize:1})` 的 total。一期可在 loadDelay 时单独请求一次 total（或复用 Task 8 的 overview，但看板保持轻量，用 delayList 的 total 即可，需把分页 total 也存下来）。

- [ ] **Step 4: 浏览器实测**

看板页 → 延期预警区有工单/任务切换 → 切换后表格刷新 → 显示车间、延期天数、进度条 → 30 秒自动刷新仍工作 → 无控制台报错。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/mes/pro/dashboard/index.vue
git commit -m "feat(pro/dashboard): 延期预警改为后端接口+工单/任务Tab+进度列"
```

---

## Task 12: 端到端联调与验证（红线，不可跳过）

**Files:** 无新增，全量验证。

- [ ] **Step 1: 后端打包 + 重启**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# 找到旧 pid: ps aux | grep ruoyi-admin.jar | grep -v grep
# kill <pid>
nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
for i in $(seq 1 30); do curl -s http://localhost:8081/captchaImage >/dev/null && echo "UP" && break; sleep 2; done
```

- [ ] **Step 2: 跑全部新单测**

Run: `cd backend && mvn -pl ruoyi-system test -Dtest='TeamResolverTest,DelayLevelEvaluatorTest,ProProgressServiceImplTest,ProReportServiceImplTest,TeamSnapshotWriteTest' -q`
Expected: BUILD SUCCESS，全部测试通过。

- [ ] **Step 3: 接口实测清单（用 token）**

```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
# 进度详情
curl -s http://localhost:8081/mes/pro/progress/1 -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json;d=json.load(sys.stdin);print('summary delayLevel=',d['data']['delayLevel'],'processes=',len(d['data']['processes']),'cards=',len(d['data']['cards']))"
# 延期列表
curl -s "http://localhost:8081/mes/pro/progress/delayList?objectType=WORKORDER" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json;d=json.load(sys.stdin);print('delay total=',d['total'])"
curl -s "http://localhost:8081/mes/pro/progress/delayList?objectType=TASK" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json;d=json.load(sys.stdin);print('task delay total=',d['total'])"
# 甘特含新字段
curl -s http://localhost:8081/mes/pro/gantt/workorder/1 -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json;d=json.load(sys.stdin);t=d['data']['tasks'][0];print('gantt task keys=',[k for k in t if 'actual' in k or 'progress' in k or 'delay' in k])"
# 统计报表 5 接口
for ep in overview productivity trend taskStatus; do
  curl -s "http://localhost:8081/mes/pro/report/$ep?beginTime=2026-08-01&endTime=2026-08-23" -H "Authorization: Bearer $TOKEN" -o /dev/null -w "$ep HTTP %{http_code}\n"
done
curl -s "http://localhost:8081/mes/pro/report/detail?pageNum=1&pageSize=10" -H "Authorization: Bearer $TOKEN" -o /dev/null -w "detail HTTP %{http_code}\n"
```

Expected:
- progress 返回 delayLevel 非空，processes/cards 数量 ≥0；
- delayList WORKORDER/TASK 均返回 200；
- 甘特 task 含 actualStartTime/actualEndTime/progressPercent/delayLevel 键；
- 5 个 report 接口全 200。

- [ ] **Step 4: 班组快照实测**

用一个绑定了班组的账号上工、报工，然后：

```sql
SELECT team_id,team_code,team_name FROM qxx_pro_workrecord ORDER BY record_id DESC LIMIT 1;
SELECT team_id,team_code,team_name,user_id FROM qxx_pro_feedback ORDER BY record_id DESC LIMIT 1;
```

Expected: 新上工记录和新报工记录的 team_* 字段与该用户在 qxx_cal_team_member 的归属一致（无班组则为 NULL 且不报错）。

- [ ] **Step 5: 工厂隔离验证**

若有两个工厂的数据，用 A 厂账号 token 调 `/mes/pro/report/overview` 和 `/mes/pro/progress/delayList`，确认结果不含 B 厂工单/任务（对比数据库中两厂数据）。单厂环境至少在代码评审确认所有新 SQL 查的是 qxx_ 表（拦截器会注入），无 @SkipFactoryId。

- [ ] **Step 6: 前端浏览器实测清单**

- 工单列表 → 点进度图标 → 工序/子工单/甘特三 tab 均正常；
- 甘特图排产页 → 实际进度开关、拖拽不受影响、风险色边框显示；
- 生产统计菜单出现 → KPI/图表/明细/延期表渲染 → 改日期和车间筛选查询生效 → 切换路由无 echarts 控制台报错；
- 生产看板 → 延期 Tab 切换、自动刷新。

- [ ] **Step 7: 类型检查**

Run: `cd frontend && npx vue-tsc --noEmit 2>&1 | head -30`
Expected: 无新增 TS 报错（若项目本就有存量错误，确认本次新增文件无错）。

- [ ] **Step 8: 提交任何验证期修复**

若验证中改了代码，按改动内容分提交（fix: ...）。最终 `git status` 干净。

---

## Self-Review 结果（计划作者已核对）

**Spec 覆盖：**
- 工单→子工单(流转卡)→工序任务三级进度 → Task 5 + Task 10 ✓
- 计划/实际开始结束/交期/状态/完成率 → Task 5 VO 与聚合 SQL ✓
- 甘特叠加实际条 + 延期/临期/滞后着色 → Task 6 + Task 9 ✓
- 延期预警（工单交期+任务结束，看板+报表）→ Task 5 delayList + Task 11 ✓
- 统计报表（完成率/延期率/产能/工时/效率，按日期/车间/班组筛选）→ Task 7 + Task 8 ✓
- 班组快照 team_id + feedback 补 user_id → Task 1 + Task 3 ✓
- Flyway V141（字段/索引/菜单/配置）→ Task 1 ✓
- 实时聚合、无冗余同步字段 → Task 5/6/7 全部走 SQL 聚合 ✓
- 测试（DelayLevelEvaluator 参数化、TeamResolver、Service 单测、工厂隔离）→ Task 2/4/5/7 + Task 12 Step 5 ✓
- 验证红线（打包+重启+token 实测+浏览器）→ Task 3/5/6/7/8/10/11 + Task 12 ✓

**范围外（不做，二期）：** 父子工单结构、实时推送、每日快照表、移动端看板、OEE 全套 —— 各任务均未涉及 ✓。

**类型一致性：**
- `TeamResolver.TeamSnapshot.teamId/teamCode/teamName()` 在 Task 2 定义，Task 3 调用一致。
- `DelayLevelEvaluator.evaluateTask/evaluateWorkorder` 签名在 Task 4 定义，Task 5/6 调用参数顺序一致。
- 甘特字段 `actualStartTime/actualEndTime/progressPercent/delayLevel/behindSchedule` 后端 Map key（Task 6）、前端 GanttTask 类型（Task 6 Step 4）、GanttChart 读取 `r.raw.xxx`（Task 9）三处命名一致。
- 接口路径 `/mes/pro/progress/{id}`、`/delayList`、`/mes/pro/report/*` 前后端一致（Task 5/7 后端，Task 8/10 前端）。

**已知实施注意点（写在任务内）：**
- `CalTeamMemberMapper.xml` 若不支持按 `user_name` 过滤需补 `<if>`（Task 2 Step 4 注）。
- `ISysConfigService.selectConfigByInt` 的实际方法名若依版本可能为 `selectConfigByKey` 返回 String，实施时按项目现有用法 Integer.parseInt（Task 5/6 用到 configService 的地方核对）。
- 报表 SQL 中 workshop 维度的实际工时需要 Service 层内存合并（Task 7 Step 2 注）。
